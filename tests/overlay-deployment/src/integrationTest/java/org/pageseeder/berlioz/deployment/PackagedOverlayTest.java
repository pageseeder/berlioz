package org.pageseeder.berlioz.deployment;

import com.google.gson.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.*;
import org.xml.sax.SAXException;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises real packaged deployment: the runner cannot load Berlioz or fixture classes. */
@Timeout(60)
class PackagedOverlayTest {
  @TempDir Path temporary;
  private Server server;
  private WebAppContext application;
  private URI base;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  private void start(boolean classpathOnly) throws Exception {
    Path war = Path.of(System.getProperty("overlay.war"));
    if (classpathOnly) {
      Path variant = temporary.resolve("classpath-only.war");
      try (ZipFile source = new ZipFile(war.toFile()); ZipOutputStream target = new ZipOutputStream(Files.newOutputStream(variant))) {
        Enumeration<? extends ZipEntry> entries = source.entries();
        while (entries.hasMoreElements()) {
          ZipEntry entry = entries.nextElement();
          if (entry.getName().equals("WEB-INF/config/services.xml")) continue;
          target.putNextEntry(new ZipEntry(entry.getName()));
          try (var input = source.getInputStream(entry)) { input.transferTo(target); }
          target.closeEntry();
        }
      }
      war = variant;
    }
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setHost("127.0.0.1");
    connector.setPort(0);
    server.addConnector(connector);
    application = new WebAppContext();
    application.setContextPath("/");
    application.setWar(war.toAbsolutePath().toString());
    application.setExtractWAR(true);
    application.setTempDirectory(Files.createDirectory(temporary.resolve("jetty")).toFile());
    application.setParentLoaderPriority(false);
    application.setThrowUnavailableOnStartupException(true);
    server.setHandler(application);
    server.start();
    assertTrue(application.isAvailable(), "WAR must deploy successfully");
    base = URI.create("http://127.0.0.1:" + connector.getLocalPort());
  }

  @AfterEach void stop() throws Exception {
    if (server != null) {
      try { server.stop(); } finally { server.destroy(); }
    }
  }

  @Test void runnerClasspathExcludesBerliozAndFixtureClasses() {
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.pageseeder.berlioz.servlet.BerliozServlet"));
    assertThrows(ClassNotFoundException.class, () -> Class.forName("org.pageseeder.berlioz.fixture.OverlayGenerator"));
  }

  @Test void discoversJarGeneratorAndExecutesPackagedXsltDependencies() throws Exception {
    start(false);
    Document xml = xml(get("/overlay.xml").body());
    Element overlay = (Element) xml.getElementsByTagName("overlay").item(0);
    assertNotNull(overlay);
    assertEquals("overlay-from-jar", overlay.getAttribute("message"));
    assertEquals("true", overlay.getAttribute("packaged"));
    String html = get("/overlay.html").body();
    assertTrue(html.contains("overlay-from-jar"), html);
    assertTrue(html.contains("imported-from-jar"), html);
    assertTrue(html.contains("included-from-jar"), html);
  }

  @Test void servesPackagedStaticAssetAndRegistersFragmentFilter() throws Exception {
    start(false);
    HttpResponse<String> response = get("/overlay/marker.txt");
    assertEquals("packaged-static-resource\n", response.body());
    assertEquals("active", response.headers().firstValue("X-Overlay-Fragment").orElseThrow());
    assertEquals("active", get("/overlay.xml").headers().firstValue("X-Overlay-Fragment").orElseThrow());
  }

  @Test void filesystemRouteWinsAndWarningIdentifiesBothOrigins() throws Exception {
    start(false);
    String body = get("/conflict.xml").body();
    assertFalse(body.contains("overlay-from-jar"), body);
    String runtime = get("/admin/runtime.json").body();
    assertTrue(runtime.contains("host-conflict"), runtime);
    assertFalse(runtime.contains("overlay-conflict"), runtime);
    Document sources = xml(get("/admin/sources.xml").body());
    NodeList warnings = sources.getElementsByTagName("warning");
    assertTrue(warnings.getLength() > 0);
    String warning = warnings.item(0).getTextContent();
    assertTrue(warning.contains("overlay-fixture"), warning);
    assertTrue(warning.contains("META-INF/berlioz/services.xml"), warning);
    assertTrue(warning.contains("from services.xml overrides"), warning);
    assertTrue(warning.contains("host-conflict"), warning);
    assertTrue(warning.contains("overlay-conflict"), warning);
  }

  @Test void diagnosticsDescribeDeployedServletsAndAgreeAcrossFormats() throws Exception {
    start(false);
    JsonArray servlets = diagnostic("config", "berlioz-config");
    assertEquals(3, servlets.size());
    assertEquals("BerliozHTML", servlets.get(0).getAsJsonObject().get("name").getAsString());
    JsonObject html = servlets.get(0).getAsJsonObject();
    assertEquals(JsonParser.parseString("[\"*.html\"]"), html.get("mappings"));
    assertEquals("text/html", html.get("mediaType").getAsString());
    assertEquals("manual", html.get("xsltCache").getAsString());
    assertEquals("classpath:overlay/xslt/main.xsl", html.getAsJsonObject("stylesheet").get("path").getAsString());
    JsonArray errors = diagnostic("errors", "error-handler-config");
    assertEquals(1, errors.size());
    JsonObject error = errors.get(0).getAsJsonObject();
    assertEquals("ErrorHandlerServlet", error.get("name").getAsString());
    assertEquals(JsonParser.parseString("[\"*.error\"]"), error.get("mappings"));
    assertEquals("discovered", error.getAsJsonObject("forward").get("source").getAsString());
    assertEquals(JsonParser.parseString("[\"html\",\"json\",\"xml\"]"), error.getAsJsonObject("forward").get("extensions"));
  }

  @Test void malformedReloadRetainsCompleteRegistryAndRecovers() throws Exception {
    start(false);
    get("/overlay.xml");
    String warningsBefore = get("/admin/sources.xml").body();
    Path services = Path.of(application.getServletContext().getRealPath("/WEB-INF/config/services.xml"));
    String valid = Files.readString(services);
    Files.writeString(services, valid
        .replace("org.pageseeder.berlioz.generator.NoContent", "org.pageseeder.berlioz.fixture.OverlayGenerator")
        .replace("</services></service-config>", "<broken>"));
    assertTrue(get("/overlay.xml?reload-services=true").body().contains("overlay-from-jar"));
    assertFalse(get("/conflict.xml").body().contains("overlay-from-jar"));
    assertEquals("reload-failed", jsonData(get("/admin/runtime.json").body()).get("loadStatus").getAsString());
    Document before = xml(warningsBefore);
    Document after = xml(get("/admin/sources.xml").body());
    assertEquals(before.getElementsByTagName("warning").item(0).getTextContent(),
        after.getElementsByTagName("warning").item(0).getTextContent());
    Files.writeString(services, valid);
    get("/overlay.xml?reload-services=true");
    assertEquals("ready", jsonData(get("/admin/runtime.json").body()).get("loadStatus").getAsString());
  }

  @Test void startsWithoutFilesystemServiceConfiguration() throws Exception {
    start(true);
    assertNull(application.getServletContext().getResource("/WEB-INF/config/services.xml"));
    assertTrue(get("/overlay.xml").body().contains("overlay-from-jar"));
    assertTrue(get("/conflict.xml").body().contains("overlay-from-jar"));
    assertEquals("ready", jsonData(get("/admin/runtime.json").body()).get("loadStatus").getAsString());
  }

  private HttpResponse<String> get(String path) throws IOException, InterruptedException {
    HttpResponse<String> response = client.send(HttpRequest.newBuilder(base.resolve(path))
        .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, response.statusCode(), path + ": " + response.body());
    return response;
  }

  private JsonArray diagnostic(String path, String root) throws Exception {
    HttpResponse<String> response = get("/admin/" + path + ".json");
    String etag = response.headers().firstValue("ETag").orElseThrow();
    HttpResponse<String> conditional = client.send(HttpRequest.newBuilder(base.resolve("/admin/" + path + ".json"))
        .timeout(Duration.ofSeconds(10)).header("If-None-Match", etag).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(304, conditional.statusCode());
    assertEquals("", conditional.body());
    JsonArray json = jsonData(response.body()).getAsJsonArray("servlets");
    Document xml = xml(get("/admin/" + path + ".xml").body());
    Element config = (Element) xml.getElementsByTagName(root).item(0);
    assertNotNull(config);
    JsonArray converted = new JsonArray();
    for (Node child = config.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) converted.add(fromXml((Element) child));
    }
    assertEquals(json, converted, "XML and JSON must contain equivalent typed values");
    assertFalse(response.body().contains(temporary.toString()));
    assertFalse(response.body().contains("jar:file:"));
    return json;
  }

  private static JsonObject fromXml(Element element) {
    JsonObject result = new JsonObject();
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node attribute = attributes.item(i);
      String name = camelCase(attribute.getNodeName());
      result.add(name, attributeValue(name, attribute.getNodeValue()));
    }
    for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child instanceof Element) result.add(camelCase(child.getNodeName()), fromXml((Element) child));
    }
    return result;
  }

  private static JsonElement attributeValue(String name, String value) {
    if (name.equals("mappings") || name.equals("extensions")) return splitValues(value);
    if (name.equals("compression") || name.equals("problemXml")) return new JsonPrimitive(Boolean.parseBoolean(value));
    return new JsonPrimitive(value);
  }

  private static JsonArray splitValues(String value) {
    JsonArray values = new JsonArray();
    if (!value.isEmpty()) for (String part : value.split(" ")) values.add(part);
    return values;
  }

  private static String camelCase(String name) {
    String[] parts = name.split("-");
    StringBuilder result = new StringBuilder(parts[0]);
    for (int i = 1; i < parts.length; i++) result.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
    return result.toString();
  }

  private static JsonObject jsonData(String body) {
    return JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("data");
  }

  private static Document xml(String body) throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    return factory.newDocumentBuilder().parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));
  }
}
