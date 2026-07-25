package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.xslt.XsltTransformException;
import org.pageseeder.berlioz.servlet.fixtures.CacheableXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.DirectJsonGenerator;
import org.pageseeder.berlioz.servlet.fixtures.EchoXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.FailingEtagXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.ParameterEchoXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.RedirectXmlGenerator;
import org.pageseeder.berlioz.servlet.fixtures.RetryAfterJsonGenerator;

class BerliozServletTest {

  private static final String ECHO_XML = EchoXmlGenerator.class.getName();
  private static final String CACHEABLE_XML = CacheableXmlGenerator.class.getName();
  private static final String FAILING_ETAG_XML = FailingEtagXmlGenerator.class.getName();
  private static final String RETRY_AFTER_JSON = RetryAfterJsonGenerator.class.getName();
  private static final String REDIRECT_XML = RedirectXmlGenerator.class.getName();
  private static final String DIRECT_JSON = DirectJsonGenerator.class.getName();
  private static final String PARAMETER_ECHO_XML = ParameterEchoXmlGenerator.class.getName();

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
  void query_matchingServiceDispatchesAndReturnsContent() throws Exception {
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(queryRequest("/search.xml", "application/octet-stream"), recorder.build());

    assertEquals(200, recorder.status);
    assertTrue(recorder.content().contains("<message path=\"/search\">hello</message>"), recorder.content());
  }

  @Test
  void query_matchingServiceWithoutContentTypeSendsBadRequest() throws Exception {
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request("QUERY", "/search.xml"), recorder.build());

    assertEquals(400, recorder.status);
    assertEquals("QUERY requests require a Content-Type header", recorder.errorMessage);
  }

  @Test
  void query_matchingXmlServiceWithoutContentTypeWritesProblemXml() throws Exception {
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request("QUERY", "/search.xml"), recorder.build());

    assertEquals(400, recorder.status);
    assertEquals("application/xml;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().contains("<problem"), recorder.content());
    assertTrue(recorder.content().contains("<status>400</status>"), recorder.content());
  }

  @Test
  void query_matchingJsonServiceWithoutContentTypeWritesProblemJson() throws Exception {
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request("QUERY", "/search.json"), recorder.build());

    assertEquals(400, recorder.status);
    assertEquals("application/problem+json;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().contains("\"status\":400"), recorder.content());
    assertTrue(recorder.content().contains("QUERY requests require a Content-Type header"), recorder.content());
  }

  @Test
  void query_unknownPathSendsNotFound() throws Exception {
    writeServices(service("known", "get", "/known", "generator", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request("QUERY", "/missing.xml"), recorder.build());

    assertEquals(404, recorder.status);
  }

  @Test
  void query_pathKnownOnlyForGetStillSendsNotFound() throws Exception {
    // QUERY is treated as a safe/idempotent method like GET/HEAD: a miss is a plain 404,
    // it does not probe other registered methods for a 405.
    writeServices(service("get-only", "get", "/get-only", "generator", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request("QUERY", "/get-only.xml"), recorder.build());

    assertEquals(404, recorder.status);
  }

  @Test
  void doPut_queryOnlyPathSendsMethodNotAllowedIncludingQuery() throws Exception {
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doPut(request("PUT", "/search.xml"), recorder.build());

    assertEquals(405, recorder.status);
    assertTrue(recorder.header(HttpHeaders.ALLOW).contains("QUERY"), recorder.header(HttpHeaders.ALLOW));
  }

  @Test
  void doOptions_includesQueryInAllowHeader() throws Exception {
    writeServices(service("search", "query", "/search", "handler", ECHO_XML));
    initServlet(Collections.emptyMap());
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doOptions(request("OPTIONS", "/search.xml"), recorder.build());

    assertTrue(recorder.header(HttpHeaders.ALLOW).contains("QUERY"), recorder.header(HttpHeaders.ALLOW));
  }

  @Test
  void query_bodyParametersEmulatedForFormUrlEncodedBody() throws Exception {
    writeServices(service("search", "query", "/search", "handler", PARAMETER_ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    HttpServletRequest request = ServletTestSupport.request()
        .method("QUERY").servletPath("/search.xml").uri("/search.xml")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello+world")
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request, recorder.build());

    assertEquals(200, recorder.status);
    assertTrue(recorder.content().contains("q=\"hello world\""), recorder.content());
  }

  @Test
  void query_bodyParametersNotParsedForNonFormContentType() throws Exception {
    writeServices(service("search", "query", "/search", "handler", PARAMETER_ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    HttpServletRequest request = ServletTestSupport.request()
        .method("QUERY").servletPath("/search.xml").uri("/search.xml")
        .contentType("application/json")
        .body("{\"q\":\"hello\"}")
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request, recorder.build());

    assertEquals(200, recorder.status);
    assertTrue(recorder.content().contains("q=\"\""), recorder.content());
  }

  @Test
  void query_malformedFormBodySendsBadRequest() throws Exception {
    writeServices(service("search", "query", "/search", "handler", PARAMETER_ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    HttpServletRequest request = ServletTestSupport.request()
        .method("QUERY").servletPath("/search.xml").uri("/search.xml")
        .contentType("application/x-www-form-urlencoded")
        .body("q=%2")
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request, recorder.build());

    assertEquals(400, recorder.status);
    assertEquals("Malformed application/x-www-form-urlencoded QUERY request body", recorder.errorMessage);
    assertNotNull(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
  }

  @Test
  void query_oversizedFormBodySendsPayloadTooLarge() throws Exception {
    writeServices(service("search", "query", "/search", "handler", PARAMETER_ECHO_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    HttpServletRequest request = ServletTestSupport.request()
        .method("QUERY").servletPath("/search.xml").uri("/search.xml")
        .contentType("application/x-www-form-urlencoded")
        .body("q=" + "x".repeat(1024 * 1024))
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(request, recorder.build());

    assertEquals(413, recorder.status);
    assertEquals("QUERY request body exceeds 1048576 bytes", recorder.errorMessage);
  }

  @Test
  void query_cacheableServiceIsNotCachedOrGivenAnEtag() throws Exception {
    writeServices(service("cached", "query", "/cached", "generator", CACHEABLE_XML));
    initServlet(Map.of("content-type", "application/xml;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.service(queryRequest("/cached.xml", "application/octet-stream"), recorder.build());

    assertEquals(200, recorder.status);
    assertNull(recorder.header(HttpHeaders.ETAG));
    assertEquals("no-cache", recorder.header(HttpHeaders.CACHE_CONTROL));
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
  void doGet_etagCallbackThrowsHttpException_handlesSignalAtServletBoundary() throws Exception {
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(service("failing-etag", "get", "/failing-etag", "generator", FAILING_ETAG_XML));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    HttpServletRequest request = request("GET", "/failing-etag.json");
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request, recorder.build());

    assertEquals(503, recorder.status);
    assertEquals("30", recorder.header("Retry-After"));
    assertTrue(recorder.content().contains("\"detail\":\"HTTP 503\""), recorder.content());
    assertNotNull(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
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
  void doGet_jsonServletServiceLoadErrorWritesProblemJson() throws Exception {
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
  }

  @Test
  @SuppressWarnings("removal") // ERROR_PROBLEM_FORMAT removed in 1.0; covers legacy migration path
  void doGet_jsonServletServiceLoadError_legacyFormatStillWritesProblemJson() throws Exception {
    // The deprecated berlioz.errors.problem=false escape hatch only restores the legacy XML/HTML
    // output; there was never a legacy JSON representation, so the direct-error shortcut in
    // BerliozServlet.sendError() must keep emitting problem+json regardless of the flag.
    writeConfig(true, false);
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
  void doGet_staticXsltFailure_delegatesToContainerOnce() throws Exception {
    writeServices(service("broken", "get", "/broken", "generator", ECHO_XML));
    writeBrokenStylesheet();
    initServlet(Map.of("stylesheet", "transform.xsl", "content-type", "text/html;charset=utf-8"));
    HttpServletRequest request = request("GET", "/broken.html");
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request, recorder.build());

    assertEquals(500, recorder.status);
    assertEquals("The service failed during XSLT transformation", recorder.errorMessage);
    assertEquals(1, request.getAttribute(ErrorHandlerServlet.ERROR_RENDERING_DEPTH));
    assertTrue(request.getAttribute(RequestDispatcher.ERROR_EXCEPTION) instanceof XsltTransformException);
    assertEquals("", recorder.content());
  }

  @Test
  void doGet_xsltFailureDuringErrorRendering_usesTerminalRendererAndPreservesOriginal() throws Exception {
    writeServices(service("error-page", "get", "/error-page", "generator", ECHO_XML));
    writeBrokenStylesheet();
    initServlet(Map.of("stylesheet", "transform.xsl", "content-type", "text/html;charset=utf-8"));
    RuntimeException original = new RuntimeException("original exception");
    HttpServletRequest request = ServletTestSupport.request()
        .method("GET").servletPath("/error-page.html").uri("/error-page.html")
        .attribute(RequestDispatcher.ERROR_STATUS_CODE, 404)
        .attribute(RequestDispatcher.ERROR_MESSAGE, "Original failure")
        .attribute(RequestDispatcher.ERROR_REQUEST_URI, "/original.html")
        .attribute(RequestDispatcher.ERROR_EXCEPTION, original)
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request, recorder.build());

    assertEquals(404, recorder.status);
    assertEquals(original, request.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
    assertEquals(original, request.getAttribute(ErrorHandlerServlet.ORIGINAL_ERROR_EXCEPTION));
    assertTrue(recorder.content().contains("Original failure"), recorder.content());
    assertEquals("no-store", recorder.header(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  void doGet_namedErrorHandlerFailure_fallsBackWithoutRedispatch() throws Exception {
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(service("broken", "get", "/broken", "generator", ECHO_XML));
    writeBrokenStylesheet();
    AtomicInteger forwards = new AtomicInteger();
    RequestDispatcher handler = new RequestDispatcher() {
      @Override
      public void forward(ServletRequest request, ServletResponse response) {
        forwards.incrementAndGet();
        throw new IllegalStateException("handler failed");
      }
      @Override
      public void include(ServletRequest request, ServletResponse response) {
        // not used by this test
      }
    };
    initServlet(Map.of("stylesheet", "transform.xsl", "content-type", "text/html;charset=utf-8"), handler);
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/broken.html"), recorder.build());

    assertEquals(1, forwards.get());
    assertEquals(500, recorder.status);
    assertTrue(recorder.content().contains("XSLT"), recorder.content());
    assertEquals("no-store", recorder.header(HttpHeaders.CACHE_CONTROL));
  }

  @Test
  void doGet_jsonConfiguredServlet_errorRedispatch_resolvesJsonMediaTypeForErrorHandler() throws Exception {
    // Simulates the container re-dispatching to this servlet as its own <error-page> target
    // (RequestDispatcher.ERROR_STATUS_CODE already set), which routes through dispatchError()
    // rather than the direct writeProblemJson() shortcut. ErrorHandlerServlet must still resolve
    // JSON from BerliozConfig's configured media type rather than the .json URL extension alone.
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    HttpServletRequest request = ServletTestSupport.request()
        .method("GET").uri("/missing.json")
        .attribute(RequestDispatcher.ERROR_STATUS_CODE, 404)
        .build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request, recorder.build());

    assertEquals(404, recorder.status);
    assertEquals("application/json", request.getAttribute(ErrorHandlerServlet.BERLIOZ_ERROR_MEDIA_TYPE));
    assertEquals("application/problem+json;charset=UTF-8", recorder.contentType);
    assertTrue(recorder.content().startsWith("{"), recorder.content());
  }

  @Test
  void doGet_jsonHandlerThrowsHttpExceptionWithHeader_propagatesRetryAfterHeader() throws Exception {
    // generator-catch=false (writeConfig(true)) routes the error through checkAndSendError() ->
    // sendError(), which wraps the original HttpException in a BerliozException; the response
    // must still surface the Retry-After header via HttpException.headersIn() unwrapping the cause.
    writeConfig(true);
    GlobalSettings.setup(this.webInf.toFile());
    writeServices(service("retry-after", "get", "/retry-after", "handler", RETRY_AFTER_JSON));
    initServlet(Map.of("content-type", "application/json;charset=utf-8"));
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    this.servlet.doGet(request("GET", "/retry-after.json"), recorder.build());

    assertEquals(503, recorder.status);
    assertEquals("30", recorder.header("Retry-After"));
    assertEquals("application/problem+json;charset=UTF-8", recorder.contentType);
    assertEquals("no-store", recorder.header(HttpHeaders.CACHE_CONTROL));
    assertEquals(Integer.toString(recorder.content().getBytes(StandardCharsets.UTF_8).length),
        recorder.header(HttpHeaders.CONTENT_LENGTH));
    assertNotEquals("0", recorder.header(HttpHeaders.DATE));
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
    initServlet(initParams, null);
  }

  private void initServlet(Map<String, String> initParams, RequestDispatcher errorHandler) throws Exception {
    this.servlet = new BerliozServlet();
    this.servlet.init(servletConfig(initParams, errorHandler));
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

  private HttpServletRequest queryRequest(String servletPath, String contentType) {
    return ServletTestSupport.request()
        .method("QUERY")
        .scheme("http")
        .host("localhost")
        .port(80)
        .contextPath("")
        .servletPath(servletPath)
        .pathInfo(null)
        .uri(servletPath)
        .contentType(contentType)
        .build();
  }

  private ServletConfig servletConfig(Map<String, String> initParams, RequestDispatcher errorHandler) {
    ServletContext context = (ServletContext) Proxy.newProxyInstance(
        ServletContext.class.getClassLoader(),
        new Class<?>[]{ServletContext.class},
        (proxy, method, args) -> {
          if ("getRealPath".equals(method.getName())) return realPath((String) args[0]);
          if ("getNamedDispatcher".equals(method.getName())) return errorHandler;
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
    writeConfig(handleErrors, true);
  }

  private void writeConfig(boolean handleErrors, boolean problemFormat) throws IOException {
    Files.write(this.webInf.resolve("config").resolve("config.xml"), String.join("\n",
        "<?xml version=\"1.0\"?>",
        "<global>",
        "  <berlioz>",
        "    <errors handle=\"" + handleErrors + "\" generator-catch=\"false\" problem=\"" + problemFormat + "\"/>",
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

  private void writeBrokenStylesheet() throws IOException {
    Files.writeString(this.webInf.resolve("transform.xsl"),
        "<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
            + "<xsl:template match=\"/\"><xsl:value-of select=\"unknown:(\"/></xsl:template>"
            + "</xsl:stylesheet>", StandardCharsets.UTF_8);
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
