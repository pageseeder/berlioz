package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.servlet.fixtures.CacheableXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.DirectJsonGenerator;
import org.pageseeder.berlioz.servlet.fixtures.EchoXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.RedirectXmlGenerator;

class BerliozServletTest {

  private static final String ECHO_XML = EchoXmlGenerator.class.getName();
  private static final String CACHEABLE_XML = CacheableXmlGenerator.class.getName();
  private static final String REDIRECT_XML = RedirectXmlGenerator.class.getName();
  private static final String DIRECT_JSON = DirectJsonGenerator.class.getName();

  @TempDir
  Path temp;

  private Path webRoot;
  private Path webInf;
  private BerliozServlet servlet;

  @BeforeEach
  void setup() throws IOException {
    this.webRoot = this.temp.resolve("webapp");
    this.webInf = this.webRoot.resolve("WEB-INF");
    Files.createDirectories(this.webInf.resolve("config"));
    writeConfig();
    ServiceLoader.getInstance().clear();
    GlobalSettings.setup(this.webInf.toFile());
  }

  @AfterEach
  void cleanup() {
    if (this.servlet != null) {
      this.servlet.destroy();
      this.servlet = null;
    }
    ServiceLoader.getInstance().clear();
    XsltTransformer.clearAllCache();
  }

  @Test
  void doGet_matchingXmlServiceTransformsContent() throws Exception {
    writeServices(service("hello", "get", "/hello", "generator", ECHO_XML));
    writeStylesheet();
    initServlet(Map.of("stylesheet", "transform.xsl", "content-type", "text/html;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/hello.html"), recorder.build());

    assertEquals(200, recorder.status);
    assertEquals("text/plain;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().contains("transformed:hello"), recorder.content());
  }

  @Test
  void doGet_directXmlServiceBypassesEnvelopeAndStylesheet() throws Exception {
    writeServices(service("direct", "get", "/direct", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/direct.xml"), recorder.build());

    assertEquals(200, recorder.status);
    assertEquals("application/xml;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().contains("<message path=\"/direct\">hello</message>"), recorder.content());
    assertFalse(recorder.content().contains("<root"), recorder.content());
  }

  @Test
  void doHead_matchingServiceSetsLengthWithoutBody() throws Exception {
    writeServices(service("head", "get", "/head", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doHead(request("HEAD", "/head.xml"), recorder.build());

    assertEquals(200, recorder.status);
    assertEquals("", recorder.content());
    assertNotNull(recorder.header(HttpHeaders.CONTENT_LENGTH));
    assertTrue(Integer.parseInt(recorder.header(HttpHeaders.CONTENT_LENGTH)) > 0);
  }

  @Test
  void doOptions_firstRequestReturnsAllowHeader() throws Exception {
    writeServices(String.join("\n",
        serviceConfigStart(),
        "  <services group=\"default\">",
        serviceElement("options-get", "get", "/options", "generator", ECHO_XML),
        serviceElement("options-post", "post", "/options", "generator", ECHO_XML),
        "  </services>",
        "</service-config>"));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doOptions(request("OPTIONS", "/options.xml"), recorder.build());

    String allow = recorder.header(HttpHeaders.ALLOW);
    assertNotNull(allow);
    assertTrue(allow.contains("GET"), allow);
    assertTrue(allow.contains("HEAD"), allow);
    assertTrue(allow.contains("POST"), allow);
  }

  @Test
  void doGet_unknownPathSendsNotFound() throws Exception {
    writeServices(service("known", "get", "/known", "generator", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/missing.xml"), recorder.build());

    assertEquals(404, recorder.status);
    assertEquals("Resource not found", recorder.errorMessage);
  }

  @Test
  void doPut_getOnlyPathSendsMethodNotAllowed() throws Exception {
    writeServices(service("get-only", "get", "/get-only", "generator", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doPut(request("PUT", "/get-only.xml"), recorder.build());

    assertEquals(405, recorder.status);
    assertTrue(recorder.header(HttpHeaders.ALLOW).contains("GET"));
    assertTrue(recorder.header(HttpHeaders.ALLOW).contains("HEAD"));
  }

  @Test
  void doGet_cacheableServiceSupportsConditionalRequest() throws Exception {
    writeServices(service("cached", "get", "/cached", "generator", CACHEABLE_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder first = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/cached.xml"), first.build());

    String etag = first.header(HttpHeaders.ETAG);
    assertEquals(200, first.status);
    assertNotNull(etag);
    assertNotNull(first.header(HttpHeaders.CACHE_CONTROL));

    ServletTestSupport.ResponseRecorder second = ServletTestSupport.response();
    this.servlet.doGet(request("GET", "/cached.xml", Map.of(HttpHeaders.IF_NONE_MATCH, etag)), second.build());

    assertEquals(304, second.status);
    assertEquals("", second.content());
  }

  @Test
  void doGet_directJsonServiceReturnsJson() throws Exception {
    writeServices(service("json", "get", "/json", "handler", DIRECT_JSON));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/json.json"), recorder.build());

    assertEquals(200, recorder.status);
    assertEquals("application/json;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().contains("\"message\""), recorder.content());
    assertTrue(recorder.content().contains("\"hello\""), recorder.content());
    assertTrue(recorder.content().contains("\"path\""), recorder.content());
  }

  @Test
  void doGet_jsonServletServiceLoadErrorWritesDetailedProblemJson() throws Exception {
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<service-config version=\"1.0\">",
        "  <services group=\"default\">",
        "    <service id=\"broken\" method=\"get\">"));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/broken.json"), recorder.build());

    String body = recorder.content();
    assertEquals(503, recorder.status);
    assertEquals("application/problem+json;charset=UTF-8", recorder.contentType);
    assertTrue(body.contains("\"type\":\"urn:berlioz:problem:services-malformed\""), body);
    assertTrue(body.contains("\"status\":503"), body);
    assertTrue(body.contains("\"exception\""), body);
    assertTrue(body.contains("SAXParseException"), body);
  }

  @Test
  void doGet_jsonOnlyHandlerOnXmlServletReturnsNotFound() throws Exception {
    writeServices(service("json-only", "get", "/json-only", "handler", DIRECT_JSON));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/json-only.xml"), recorder.build());

    assertEquals(404, recorder.status);
  }

  @Test
  void doGet_xmlOnlyGeneratorOnJsonServletFallsBackToXslt() throws Exception {
    writeServices(service("xml-only", "get", "/xml-only", "generator", ECHO_XML));
    writeStylesheet();
    initServlet(Map.of("stylesheet", "transform.xsl", "content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/xml-only.json"), recorder.build());

    assertEquals(200, recorder.status);
    assertTrue(recorder.content().contains("transformed:hello"), recorder.content());
  }

  @Test
  void doGet_jsonOnlyHandlerOnJsonServletReturnsOk() throws Exception {
    writeServices(service("json-ok", "get", "/json-ok", "handler", DIRECT_JSON));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/json-ok.json"), recorder.build());

    assertEquals(200, recorder.status);
    assertTrue(recorder.content().contains("\"message\""), recorder.content());
  }

  @Test
  void doGet_redirectResponseSetsLocation() throws Exception {
    writeServices(service("redirect", "get", "/redirect", "generator", REDIRECT_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/redirect.xml"), recorder.build());

    assertEquals(303, recorder.status);
    assertEquals("/elsewhere", recorder.header("Location"));
    assertTrue(recorder.resetCalled);
    assertEquals("", recorder.content());
  }

  private void initServlet(Map<String, String> initParams) throws Exception {
    this.servlet = new BerliozServlet();
    this.servlet.init(servletConfig(initParams));
  }

  private HttpServletRequest request(String method, String servletPath) {
    return request(method, servletPath, Collections.emptyMap());
  }

  private HttpServletRequest request(String method, String servletPath, Map<String, String> headers) {
    ServletTestSupport.RequestBuilder builder = ServletTestSupport.request()
        .method(method)
        .scheme("http")
        .host("localhost")
        .port(80)
        .contextPath("")
        .servletPath(servletPath)
        .pathInfo(null)
        .uri(servletPath);
    headers.forEach(builder::header);
    return builder.build();
  }

  private ServletConfig servletConfig(Map<String, String> initParams) {
    ServletContext context = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, method, args) -> {
          if ("getRealPath".equals(method.getName())) return realPath((String) args[0]);
          if ("getNamedDispatcher".equals(method.getName())) return null;
          return ServletTestSupport.defaultValue(method.getReturnType());
        });
    return (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, method, args) -> {
          if ("getServletContext".equals(method.getName())) return context;
          if ("getServletName".equals(method.getName())) return "berlioz-integration";
          if ("getInitParameter".equals(method.getName())) return initParams.get(args[0]);
          if ("getInitParameterNames".equals(method.getName())) return Collections.enumeration(initParams.keySet());
          return ServletTestSupport.defaultValue(method.getReturnType());
        });
  }

  private String realPath(String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) return this.webRoot.toString();
    String relative = path.startsWith("/") ? path.substring(1) : path;
    return this.webRoot.resolve(relative).toString();
  }

  private void writeServices(String xml) throws IOException {
    Files.write(this.webInf.resolve("config").resolve("services.xml"), xml.getBytes(StandardCharsets.UTF_8));
  }

  private void writeConfig() throws IOException {
    writeConfig(false);
  }

  private void writeConfig(boolean handleErrors) throws IOException {
    Files.write(this.webInf.resolve("config").resolve("config.xml"), String.join("\n",
        "<?xml version=\"1.0\"?>",
        "<global>",
        "  <berlioz>",
        "    <errors handle=\"" + handleErrors + "\" generator-catch=\"false\"/>",
        "    <http compression=\"false\" get-via-post=\"true\"/>",
        "  </berlioz>",
        "</global>").getBytes(StandardCharsets.UTF_8));
  }

  private void writeStylesheet() throws IOException {
    Files.write(this.webInf.resolve("transform.xsl"), String.join("\n",
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">",
        "  <xsl:output method=\"text\" media-type=\"text/plain\" encoding=\"UTF-8\"/>",
        "  <xsl:template match=\"/\">",
        "    <xsl:text>transformed:</xsl:text>",
        "    <xsl:value-of select=\"//message\"/>",
        "  </xsl:template>",
        "</xsl:stylesheet>").getBytes(StandardCharsets.UTF_8));
  }

  private static String service(String id, String method, String pattern, String element, String className) {
    return String.join("\n",
        serviceConfigStart(),
        "  <services group=\"default\">",
        serviceElement(id, method, pattern, element, className),
        "  </services>",
        "</service-config>");
  }

  private static String serviceConfigStart() {
    return String.join("\n",
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<!DOCTYPE service-config PUBLIC \"-//Berlioz//DTD::Services 1.0//EN\"",
        "    \"https://pageseeder.org/schema/berlioz/services-1.0.dtd\">",
        "<service-config version=\"1.0\">");
  }

  private static String serviceElement(String id, String method, String pattern, String element, String className) {
    return String.join("\n",
        "    <service id=\"" + id + "\" method=\"" + method + "\">",
        "      <url pattern=\"" + pattern + "\"/>",
        "      <" + element + " class=\"" + className + "\"/>",
        "    </service>");
  }
}
