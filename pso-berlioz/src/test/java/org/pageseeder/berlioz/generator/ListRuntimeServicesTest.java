package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.servlet.HttpEnvironment;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.regex.Pattern;
import java.util.jar.JarOutputStream;

class ListRuntimeServicesTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  private static final String PROLOG = "<!DOCTYPE service-config PUBLIC \"-//Berlioz//DTD::Services 1.0//EN\" "
      + "\"https://pageseeder.org/schema/berlioz/services-1.0.dtd\">";

  private static final String EMPTY_SERVICES = PROLOG + "<service-config version=\"1.0\"/>";

  private static final String NO_CONTENT = "<generator class=\"org.pageseeder.berlioz.generator.NoContent\"/>";

  private static String services(String services) {
    return PROLOG + "<service-config version=\"1.0\"><services group=\"default\">" + services + "</services></service-config>";
  }

  @TempDir
  Path tmp;

  @BeforeEach
  void setUp() {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader.getInstance().clear();
  }

  @AfterEach
  void tearDown() {
    ServiceLoader.getInstance().clear();
    GlobalSettings.setup((InitEnvironment) null);
  }

  @Test
  void testProcessWritesLiveServicesElementWhenRegistryEmpty() throws Exception {
    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=0");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assertions.assertTrue(out.contains("live-services"), "Should produce a <live-services> element");
  }

  @Test
  void testETagReflectsRegistryVersion() {
    ListRuntimeServices gen = new ListRuntimeServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag((Request) req);
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.isEmpty());
  }

  @Test
  void testProcessWithLoadedServicesContainsServiceEntries() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assertions.assertTrue(out.contains("service"), "Live services output should contain service data");
  }

  @Test
  void testProcessJsonWithLoadedServicesContainsServiceEntries() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = processJson(req);
    Assertions.assertTrue(out.contains("\"liveServices\":["), out);
  }

  @Test
  void failedReloadIsVisibleInBothFormatsAndInvalidatesETag() throws Exception {
    Path config = Files.createDirectories(tmp.resolve("config"));
    Path services = config.resolve("services.xml");
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), services);
    GlobalSettings.setup(tmp.toFile());
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.loadIfRequired();
    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(),
        Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=0");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    ListRuntimeServices generator = new ListRuntimeServices();
    String healthy = generator.getETag((Request) req);
    Files.writeString(services, "<sensitive-invalid-root/>");
    loader.requestReload();
    Assertions.assertFalse(loader.loadIfRequired());
    Assertions.assertNotEquals(healthy, generator.getETag((Request) req));
    String xml = process(req);
    String json = processJson(req);
    Assertions.assertTrue(xml.contains("load-status=\"reload-failed\""), xml);
    Assertions.assertTrue(json.contains("\"loadStatus\":\"reload-failed\""), json);
    Assertions.assertFalse(xml.contains("sensitive-invalid-root"));
    Assertions.assertFalse(json.contains(tmp.toString()));
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), services,
        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    loader.requestReload();
    Assertions.assertTrue(loader.loadIfRequired());
    Assertions.assertTrue(processJson(req).contains("\"loadStatus\":\"ready\""));
  }

  @Test
  void listsSourcesInLoadOrderWithClasspathFirst() throws Exception {
    Path config = Files.createDirectories(tmp.resolve("config"));
    Files.writeString(config.resolve("services.xml"), EMPTY_SERVICES);
    Files.writeString(config.resolve("services!x.xml"), EMPTY_SERVICES);
    Path jar = tmp.resolve("overlay-1.0.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      out.putNextEntry(new JarEntry("META-INF/berlioz/services.xml"));
      out.write(EMPTY_SERVICES.getBytes(StandardCharsets.UTF_8));
      out.closeEntry();
    }
    GlobalSettings.setup(tmp.toFile());
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader overlay = new URLClassLoader(new URL[] {jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(overlay);
      ServiceLoader.getInstance().loadIfRequired();
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
    ContentRequest req = GeneratorTestSupport.request().environment(environment()).build();

    String xml = process(req);
    int jarSource = xml.indexOf("<source kind=\"classpath\" name=\"overlay-1.0.jar!META-INF/berlioz/services.xml\"/>");
    int main = xml.indexOf("<source kind=\"filesystem\" name=\"services.xml\"/>");
    int module = xml.indexOf("<source kind=\"filesystem\" name=\"services!x.xml\"/>");
    Assertions.assertTrue(jarSource >= 0 && jarSource < main && main < module, xml);
    Assertions.assertTrue(xml.matches("(?s)<live-services load-status=\"ready\" loaded=\"[^\"]+Z\">.*"), xml);
    Assertions.assertFalse(xml.contains("failed="), xml);
    Assertions.assertFalse(xml.contains(tmp.toString()), xml);

    String json = processJson(req);
    Assertions.assertTrue(json.contains("\"sources\":[{\"kind\":\"classpath\",\"name\":\"overlay-1.0.jar!META-INF/berlioz/services.xml\"},"
        + "{\"kind\":\"filesystem\",\"name\":\"services.xml\"},{\"kind\":\"filesystem\",\"name\":\"services!x.xml\"}]"), json);
  }

  @Test
  void servicesNameTheirSourceAndTheDeclarationsTheyOverride() throws Exception {
    Path config = Files.createDirectories(tmp.resolve("config"));
    Files.writeString(config.resolve("services.xml"), services("<service id=\"app-home\" method=\"get\">"
        + "<url pattern=\"/home\"/><url pattern=\"/app\"/>" + NO_CONTENT + "</service>"));
    Files.writeString(config.resolve("services!x.xml"), services("<service id=\"module-home\" method=\"get\">"
        + "<url pattern=\"/home\"/>" + NO_CONTENT + "</service>"));
    Path jar = tmp.resolve("overlay-1.0.jar");
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
      out.putNextEntry(new JarEntry("META-INF/berlioz/services.xml"));
      out.write(services("<service id=\"jar-home\" method=\"get\"><url pattern=\"/home\"/>" + NO_CONTENT + "</service>"
          + "<service id=\"jar-only\" method=\"get\"><url pattern=\"/only\"/>" + NO_CONTENT + "</service>")
          .getBytes(StandardCharsets.UTF_8));
      out.closeEntry();
    }
    GlobalSettings.setup(tmp.toFile());
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader overlay = new URLClassLoader(new URL[] {jar.toUri().toURL()}, original)) {
      Thread.currentThread().setContextClassLoader(overlay);
      ServiceLoader.getInstance().loadIfRequired();
    } finally {
      Thread.currentThread().setContextClassLoader(original);
    }
    ContentRequest req = GeneratorTestSupport.request().environment(environment()).build();

    String xml = process(req);
    String moduleHome = serviceElement(xml, "module-home");
    Assertions.assertTrue(startTag(moduleHome).endsWith(" source=\"services!x.xml\">"), xml);
    Assertions.assertTrue(moduleHome.endsWith(
        "<overridden service=\"jar-home\" source=\"overlay-1.0.jar!META-INF/berlioz/services.xml\" pattern=\"/home\"/>"
        + "<overridden service=\"app-home\" source=\"services.xml\" pattern=\"/home\"/></service>"), xml);
    String appHome = serviceElement(xml, "app-home");
    Assertions.assertTrue(startTag(appHome).endsWith(" source=\"services.xml\">"), xml);
    Assertions.assertTrue(appHome.contains("<url pattern=\"/app\"/>"), xml);
    Assertions.assertFalse(appHome.contains("<overridden"), xml);
    Assertions.assertTrue(startTag(serviceElement(xml, "jar-only"))
        .endsWith(" source=\"overlay-1.0.jar!META-INF/berlioz/services.xml\">"), xml);
    Assertions.assertFalse(xml.contains("<service id=\"jar-home\""), xml);
    Assertions.assertFalse(xml.contains(tmp.toString()), xml);

    String json = processJson(req);
    Assertions.assertTrue(json.contains("\"source\":\"services!x.xml\""), json);
    Assertions.assertTrue(json.contains("\"overridden\":[{\"service\":\"jar-home\",\"source\":\"overlay-1.0.jar!META-INF/berlioz/services.xml\",\"pattern\":\"/home\"},"
        + "{\"service\":\"app-home\",\"source\":\"services.xml\",\"pattern\":\"/home\"}]}"), json);
  }

  @Test
  void singleFileLoadReportsItsSource() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));
    ContentRequest req = GeneratorTestSupport.request().environment(environment()).build();
    String xml = process(req);
    String home = startTag(serviceElement(xml, "home"));
    Assertions.assertTrue(home.contains(" source=\"") && home.endsWith("services.xml\">"), xml);
    Assertions.assertFalse(xml.contains("<overridden"), xml);
  }

  @ParameterizedTest
  @ValueSource(strings = {"minimal", "standard"})
  void listsWarningsOfLastLoadWithSourceAndLineOnlyWithDetails(String detail) throws Exception {
    Path config = Files.createDirectories(tmp.resolve("config"));
    Files.writeString(config.resolve("config.properties"), BerliozOption.ERROR_DETAIL.property() + "=" + detail);
    Files.copy(new File(WEB_INF, "config/services-invalid-direct.xml").toPath(), config.resolve("services.xml"));
    GlobalSettings.setup(tmp.toFile());
    Assertions.assertTrue(GlobalSettings.load());
    ServiceLoader.getInstance().loadIfRequired();
    ContentRequest req = GeneratorTestSupport.request().environment(environment()).build();

    String xml = process(req);
    Assertions.assertTrue(xml.contains("<warning source=\"services.xml\""), xml);
    Assertions.assertTrue(xml.contains("no output format; service will not be registered</warning>"), xml);
    Assertions.assertEquals(!"minimal".equals(detail), xml.matches("(?s).*<warning source=\"services.xml\" line=\"\\d+\">.*"), xml);
    String json = processJson(req);
    Assertions.assertTrue(json.contains("\"warnings\":[{\"source\":\"services.xml\","), json);
    Assertions.assertEquals(!"minimal".equals(detail), json.contains("\"line\":"), json);
  }

  @ParameterizedTest
  @ValueSource(strings = {"minimal", "standard"})
  void failedModuleReloadReportsLoadFailureUntilRecovery(String detail) throws Exception {
    Path config = Files.createDirectories(tmp.resolve("config"));
    Files.writeString(config.resolve("config.properties"), BerliozOption.ERROR_DETAIL.property() + "=" + detail);
    Files.copy(new File(WEB_INF, "config/services.xml").toPath(), config.resolve("services.xml"));
    Path module = config.resolve("services!x.xml");
    Files.writeString(module, EMPTY_SERVICES);
    GlobalSettings.setup(tmp.toFile());
    Assertions.assertTrue(GlobalSettings.load());
    ServiceLoader loader = ServiceLoader.getInstance();
    loader.loadIfRequired();
    ContentRequest req = GeneratorTestSupport.request().environment(environment()).build();
    Assertions.assertFalse(process(req).contains("load-failure"));

    Files.writeString(module, PROLOG + "<service-config version=\"1.0\"><secret-element>");
    loader.requestReload();
    Assertions.assertFalse(loader.loadIfRequired());

    String xml = process(req);
    Assertions.assertTrue(xml.matches("(?s)<live-services load-status=\"reload-failed\" loaded=\"[^\"]+\" failed=\"[^\"]+\" retry-after=\"[^\"]+\">.*"), xml);
    Assertions.assertTrue(xml.contains("<service "), "The last valid services must still be listed");
    Assertions.assertTrue(xml.contains("<source kind=\"filesystem\" name=\"services!x.xml\"/>"), xml);
    Assertions.assertTrue(xml.contains("<load-failure source=\"services!x.xml\""), xml);
    Assertions.assertFalse(xml.contains(tmp.toString()), xml);
    String json = processJson(req);
    Assertions.assertTrue(json.contains("\"loadFailure\":{\"source\":\"services!x.xml\","), json);
    if ("minimal".equals(detail)) {
      Assertions.assertTrue(xml.contains("<load-failure source=\"services!x.xml\">Unable to parse services configuration file.</load-failure>"), xml);
      Assertions.assertFalse(xml.contains("secret-element"), xml);
      Assertions.assertFalse(json.contains("\"line\":"), json);
    } else {
      Assertions.assertTrue(Pattern.compile("<load-failure source=\"services!x\\.xml\" line=\"\\d+\">[^<]+</load-failure>")
          .matcher(xml).find(), xml);
      Assertions.assertFalse(xml.contains("Unable to parse services configuration file."), xml);
    }

    Files.writeString(module, EMPTY_SERVICES);
    loader.requestReload();
    Assertions.assertTrue(loader.loadIfRequired());
    String recovered = process(req);
    Assertions.assertFalse(recovered.contains("load-failure"), recovered);
    Assertions.assertFalse(recovered.contains("failed="), recovered);
  }

  // helpers
  // ---------------------------------------------------------------------------

  private HttpEnvironment environment() throws java.io.IOException {
    return new HttpEnvironment(Files.createDirectory(tmp.resolve("public")).toFile(),
        Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=0");
  }

  /**
   * Returns the service element with the specified ID, from its start tag to its end tag.
   */
  private static String serviceElement(String xml, String id) {
    int start = xml.indexOf("<service id=\"" + id + "\"");
    Assertions.assertTrue(start >= 0, xml);
    int end = xml.indexOf("</service>", start);
    Assertions.assertTrue(end >= 0, xml);
    return xml.substring(start, end + "</service>".length());
  }

  private static String startTag(String element) {
    return element.substring(0, element.indexOf('>') + 1);
  }

  private static String process(ContentRequest req) {
    ListRuntimeServices gen = new ListRuntimeServices();
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }

  private static String processJson(ContentRequest req) {
    ListRuntimeServices gen = new ListRuntimeServices();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }
}
