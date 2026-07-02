package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerServletTest {

  // Initialise SETTINGS to an empty map before any test runs, so that GlobalSettings.has()
  // inside ErrorHandlerServlet.toXML() does not trigger a spurious "Unable to load configuration"
  // warning when ENV is also null (the no-environment test scenario).
  // We bypass GlobalSettings.load() because load() always logs a warning when ENV is null.
  @BeforeAll
  static void initSettings() throws ReflectiveOperationException {
    settingsRef().compareAndSet(null, new HashMap<>());
  }

  // Public error attribute constants

  @Test
  void testErrorAttributeConstants() {
    assertEquals("javax.servlet.error.exception",      ErrorHandlerServlet.ERROR_EXCEPTION);
    assertEquals("javax.servlet.error.exception_type", ErrorHandlerServlet.ERROR_EXCEPTION_TYPE);
    assertEquals("javax.servlet.error.message",        ErrorHandlerServlet.ERROR_MESSAGE);
    assertEquals("javax.servlet.error.request_uri",    ErrorHandlerServlet.ERROR_REQUEST_URI);
    assertEquals("javax.servlet.error.servlet_name",   ErrorHandlerServlet.ERROR_SERVLET_NAME);
    assertEquals("javax.servlet.error.status_code",    ErrorHandlerServlet.ERROR_STATUS_CODE);
    assertEquals("org.pageseeder.berlioz.error_id",    ErrorHandlerServlet.BERLIOZ_ERROR_ID);
  }

  // getErrorCode() - accessible indirectly through doGet; tested via attribute behaviour

  @Test
  void testGetErrorCode_nullAttribute_returns200() throws Exception {
    // When ERROR_STATUS_CODE attribute is absent, handle() must respond with 200 OK
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html").build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
    new ErrorHandlerServlet().handle(req, res.build());
    assertEquals(200, res.status);
  }

  // Servlet instantiation

  @Test
  void testErrorHandlerServlet_canBeInstantiated() {
    assertDoesNotThrow(ErrorHandlerServlet::new);
  }

  @Test
  void init_withDefaultParametersDoesNotThrow() {
    ServletConfig config = (ServletConfig) Proxy.newProxyInstance(
        ServletConfig.class.getClassLoader(),
        new Class<?>[]{ServletConfig.class},
        (proxy, m, args) -> {
          if ("getServletName".equals(m.getName())) return "error";
          if ("getInitParameter".equals(m.getName())) return null;
          return ServletTestSupport.defaultValue(m.getReturnType());
        });
    assertDoesNotThrow(() -> new ErrorHandlerServlet().init(config));
  }

  // Null-safety on getErrorCode with valid Integer attribute (tested via constant)

  @Test
  void testBerliozErrorIdConstant_isNonNull() {
    assertNotNull(ErrorHandlerServlet.BERLIOZ_ERROR_ID);
    assertFalse(ErrorHandlerServlet.BERLIOZ_ERROR_ID.isEmpty());
  }

  // Legacy format (default: berlioz.errors.problem = false)
  //
  // Saxon-HE is on the test runtime classpath so the XSLT 2.0 failsafe transform succeeds and
  // produces HTML. The failsafe template embeds the original error XML in a hidden <div> via
  // <xsl:copy-of>, so raw-XML element names such as <client-error> are still present in the body.

  @Tag("error-samples")
  @Test
  void handle_legacyFormat_404_emitsClientErrorXml() throws Exception {
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Resource not found")
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
    new ErrorHandlerServlet().handle(req, res.build());
    String body = res.content();
    writePreview("integration-legacy-client-error-404", body);
    assertAll(
        () -> assertEquals(404, res.status),
        () -> assertEquals("text/html;charset=UTF-8", res.contentType),
        () -> assertTrue(body.contains("<client-error"),   "original XML is embedded in the HTML"),
        () -> assertTrue(body.contains("Resource not found"), "message should be present"),
        () -> assertFalse(body.contains("<problem>"),      "legacy format must not emit <problem>")
    );
  }

  @Tag("error-samples")
  @Test
  void handle_legacyFormat_500_emitsServerErrorXml() throws Exception {
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
    new ErrorHandlerServlet().handle(req, res.build());
    String body = res.content();
    writePreview("integration-legacy-server-error-500", body);
    assertAll(
        () -> assertEquals(500, res.status),
        () -> assertEquals("text/html;charset=UTF-8", res.contentType),
        () -> assertTrue(body.contains("<server-error"),   "original XML is embedded in the HTML"),
        () -> assertTrue(body.contains("Unexpected error"), "message should be present"),
        () -> assertFalse(body.contains("<problem>"),      "legacy format must not emit <problem>")
    );
  }

  @Test
  void handle_legacyFormat_fullDetail_redactsSensitiveHeadersAndParameters() throws Exception {
    RuntimeException cause = new RuntimeException("something went wrong");
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
        .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, cause)
        .header("Authorization", "Bearer header-secret")
        .header("Cookie", "JSESSIONID=cookie-secret")
        .header("X-Api_Key", "api-key-secret")
        .header("X-Request-ID", "request-42")
        .parameter("password", "parameter-secret")
        .parameter("access_token", "token-secret")
        .parameter("private-key", "private-key-secret")
        .parameter("q", "visible-query")
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();

    new ErrorHandlerServlet().handle(req, res.build());

    String body = res.content();
    assertAll(
        () -> assertEquals(500, res.status),
        () -> assertTrue(body.contains("[REDACTED]"),       "sensitive values should be replaced"),
        () -> assertTrue(body.contains("request-42"),       "non-sensitive header values should remain visible"),
        () -> assertTrue(body.contains("visible-query"),    "non-sensitive parameter values should remain visible"),
        () -> assertFalse(body.contains("header-secret"),   "authorization value must not be exposed"),
        () -> assertFalse(body.contains("cookie-secret"),   "cookie value must not be exposed"),
        () -> assertFalse(body.contains("api-key-secret"),  "API key header value must not be exposed"),
        () -> assertFalse(body.contains("parameter-secret"), "password value must not be exposed"),
        () -> assertFalse(body.contains("token-secret"),    "token value must not be exposed"),
        () -> assertFalse(body.contains("private-key-secret"),
            "private key value must not be exposed")
    );
  }

  @Test
  void handle_legacyFormat_fullDetail_includesCollectedErrors() throws Exception {
    setOption(BerliozOption.ERROR_DETAIL, "full");
    ErrorCollector<Exception> collector = new ErrorCollector<>();
    collector.collectQuietly(CollectedError.Level.ERROR, new IOException("first collected"));
    CompoundBerliozException cause = new CompoundBerliozException("compound failure",
        BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
        .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, cause)
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();

    new ErrorHandlerServlet().handle(req, res.build());

    String body = res.content();
    assertAll(
        () -> assertEquals(500, res.status),
        () -> assertTrue(body.contains("<collected-errors"), "collected errors should be serialized"),
        () -> assertTrue(body.contains("first collected"), "collected error message should be present")
    );
  }

  @Test
  void privateFallbackHelpersProduceSafeValues() throws Exception {
    Method fallbackProblemXml = ErrorHandlerServlet.class.getDeclaredMethod("fallbackProblemXml", int.class);
    fallbackProblemXml.setAccessible(true);
    Method replaceAutoURI = ErrorHandlerServlet.class.getDeclaredMethod("replaceAutoURI",
        String.class, String.class, String.class);
    replaceAutoURI.setAccessible(true);

    String problem = (String) fallbackProblemXml.invoke(null, 500);
    String forward = (String) replaceAutoURI.invoke(null, "/context/errors/not-found.auto", ".json", "/context");

    assertAll(
        () -> assertTrue(problem.contains("<problem"), "fallback problem should be serialized"),
        () -> assertTrue(problem.contains("Unable to serialize problem details."), "fallback detail should be present"),
        () -> assertEquals("/errors/not-found.json", forward)
    );
  }

  // Problem format (opt-in: berlioz.errors.problem = true)
  //
  // The problem XSLT template renders to HTML without embedding the original XML, so assertions
  // target the rendered HTML structure (heading, message paragraph, type URN in <code>).

  @Tag("error-samples")
  @Nested
  class WithProblemFormat {

    @BeforeEach
    void enable() throws Exception { setOption(BerliozOption.ERROR_PROBLEM_FORMAT, true); }

    @AfterEach
    void disable() throws Exception { setOption(BerliozOption.ERROR_PROBLEM_FORMAT, false); }

    @Test
    void handle_problemFormat_404_emitsProblemXml() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Resource not found")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      writePreview("integration-problem-not-found-404", body);
      assertAll(
          () -> assertEquals(404, res.status),
          () -> assertEquals("text/html;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("404 - Not Found"),            "status and title should appear in heading"),
          () -> assertTrue(body.contains("Resource not found"),          "detail should appear as message"),
          () -> assertTrue(body.contains("urn:berlioz:problem:not-found"), "type URN should be present"),
          () -> assertFalse(body.contains("<client-error"),              "legacy root element must not appear")
      );
    }

    @Test
    void handle_problemFormat_500_emitsProblemXml() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      writePreview("integration-problem-server-error-500", body);
      assertAll(
          () -> assertEquals(500, res.status),
          () -> assertEquals("text/html;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("500 - Internal Server Error"),  "status and title should appear in heading"),
          () -> assertTrue(body.contains("Unexpected error"),             "detail should appear as message"),
          () -> assertTrue(body.contains("urn:berlioz:problem:"),         "type URN should be present"),
          () -> assertFalse(body.contains("<server-error"),               "legacy root element must not appear")
      );
    }

    @Test
    void handle_problemFormat_405_includesMethodNotAllowedType() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 405)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Only GET is allowed")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      writePreview("integration-problem-method-not-allowed-405", body);
      assertAll(
          () -> assertEquals(405, res.status),
          () -> assertEquals("text/html;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("urn:berlioz:problem:method-not-allowed"), "type URN should name the problem")
      );
    }

    @Test
    void handle_problemFormat_401_emitsValidProblemDocument() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 401)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Authentication required")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      assertAll(
          () -> assertEquals(401, res.status),
          () -> assertEquals("text/html;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("401 - Unauthorized"), "status and title should appear in heading"),
          () -> assertTrue(body.contains("Authentication required"), "detail should appear as message"),
          () -> assertTrue(body.contains("urn:berlioz:problem:error"), "generic problem type should be present")
      );
    }
  }

  // Detail level: standard (exception summary only, no headers/parameters)

  @Nested
  class WithStandardDetail {

    @BeforeEach
    void enable() throws Exception { setOption(BerliozOption.ERROR_DETAIL, "standard"); }

    @AfterEach
    void restore() throws Exception { setOption(BerliozOption.ERROR_DETAIL, "full"); }

    @Tag("error-samples")
    @Test
    void handle_standardDetail_withThrowable_includesExceptionSummaryOnly() throws Exception {
      RuntimeException cause = new RuntimeException("something went wrong");
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
          .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, cause)
          .header("X-Forwarded-For", "10.0.0.1")
          .parameter("q", "test")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      writePreview("integration-legacy-server-error-500-standard", body);
      assertAll(
          () -> assertEquals(500, res.status),
          () -> assertTrue(body.contains("<server-error"),         "root element should be server-error"),
          () -> assertTrue(body.contains("Unexpected error"),      "error message should be present"),
          () -> assertTrue(body.contains("<exception"),            "exception summary should be present"),
          () -> assertTrue(body.contains("RuntimeException"),      "exception class should be present"),
          () -> assertTrue(body.contains("something went wrong"),  "exception message should be present"),
          () -> assertFalse(body.contains("<stack-trace"),         "stack trace must not appear"),
          () -> assertFalse(body.contains("<http-headers"),        "HTTP headers must not appear"),
          () -> assertFalse(body.contains("<http-parameters"),     "HTTP parameters must not appear")
      );
    }

    @Test
    void handle_standardDetail_withoutThrowable_emitsNoExceptionElement() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Resource not found")
          .header("Accept", "text/html")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      assertAll(
          () -> assertEquals(404, res.status),
          () -> assertTrue(body.contains("Resource not found"),  "message should be present"),
          () -> assertFalse(body.contains("<exception"),         "exception element must not appear when no throwable"),
          () -> assertFalse(body.contains("<http-headers"),      "HTTP headers must not appear"),
          () -> assertFalse(body.contains("<http-parameters"),   "HTTP parameters must not appear")
      );
    }
  }

  // Detail level: minimal (status, title, message only)

  @Nested
  class WithMinimalDetail {

    @BeforeEach
    void enable() throws Exception { setOption(BerliozOption.ERROR_DETAIL, "minimal"); }

    @AfterEach
    void restore() throws Exception { setOption(BerliozOption.ERROR_DETAIL, "full"); }

    @Tag("error-samples")
    @Test
    void handle_minimalDetail_suppressesAllDiagnostics() throws Exception {
      RuntimeException cause = new RuntimeException("something went wrong");
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
          .attribute(ErrorHandlerServlet.ERROR_EXCEPTION, cause)
          .header("X-Forwarded-For", "10.0.0.1")
          .parameter("q", "test")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      writePreview("integration-legacy-server-error-500-minimal", body);
      assertAll(
          () -> assertEquals(500, res.status),
          () -> assertTrue(body.contains("<server-error"),      "root element should be server-error"),
          () -> assertTrue(body.contains("Unexpected error"),   "error message should be present"),
          () -> assertFalse(body.contains("<exception"),        "exception element must not appear"),
          () -> assertFalse(body.contains("<stack-trace"),      "stack trace must not appear"),
          () -> assertFalse(body.contains("<http-headers"),     "HTTP headers must not appear"),
          () -> assertFalse(body.contains("<http-parameters"),  "HTTP parameters must not appear")
      );
    }

    @Test
    void handle_minimalDetail_404_suppressesAllDiagnostics() throws Exception {
      HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
          .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
          .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Resource not found")
          .header("Accept", "text/html")
          .parameter("id", "42")
          .build();
      ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
      new ErrorHandlerServlet().handle(req, res.build());
      String body = res.content();
      assertAll(
          () -> assertEquals(404, res.status),
          () -> assertTrue(body.contains("Resource not found"),  "message should be present"),
          () -> assertFalse(body.contains("<exception"),         "exception element must not appear"),
          () -> assertFalse(body.contains("<http-headers"),      "HTTP headers must not appear"),
          () -> assertFalse(body.contains("<http-parameters"),   "HTTP parameters must not appear")
      );
    }
  }

  // resolveErrorStylesheet() — unit tests for the fallback chain

  @Nested
  class ResolveErrorStylesheet {

    @AfterEach
    void clearStylesheet() throws ReflectiveOperationException {
      setOption(BerliozOption.ERROR_STYLESHEET, "");
    }

    @Test
    void resolveErrorStylesheet_defaultEmpty_returnsFailsafe() {
      URL url = ErrorHandlerServlet.resolveErrorStylesheet();
      assertNotNull(url, "Should return the built-in failsafe URL when no option is set");
      assertTrue(url.toString().contains("failsafe-error-html.xsl"), "URL should point to the classpath failsafe");
    }

    @Test
    void resolveErrorStylesheet_nonExistentPath_fallsBackToFailsafe() throws ReflectiveOperationException {
      setOption(BerliozOption.ERROR_STYLESHEET, "xslt/does-not-exist.xsl");
      URL url = ErrorHandlerServlet.resolveErrorStylesheet();
      assertNotNull(url, "Should fall back to built-in failsafe when custom file is missing");
      assertTrue(url.toString().contains("failsafe-error-html.xsl"), "Fallback URL should be the classpath failsafe");
    }

    @Test
    void resolveErrorStylesheet_configuredWithoutWebInf_fallsBackToFailsafe() throws ReflectiveOperationException {
      setWebInf(null);
      setOption(BerliozOption.ERROR_STYLESHEET, "xslt/error.xsl");
      URL url = ErrorHandlerServlet.resolveErrorStylesheet();
      assertNotNull(url, "Should fall back to built-in failsafe when WEB-INF is unavailable");
      assertTrue(url.toString().contains("failsafe-error-html.xsl"), "Fallback URL should be the classpath failsafe");
    }

    @Test
    void resolveErrorStylesheet_customFile_returnsCustomUrl(@TempDir File tempDir) throws Exception {
      // Write a minimal XSLT to the temp directory (simulating WEB-INF)
      File xsl = new File(tempDir, "error.xsl");
      Files.writeString(xsl.toPath(),
          "<?xml version=\"1.0\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"><xsl:template match=\"/\"><html/></xsl:template></xsl:stylesheet>");

      setWebInf(tempDir);
      setOption(BerliozOption.ERROR_STYLESHEET, "error.xsl");
      try {
        URL url = ErrorHandlerServlet.resolveErrorStylesheet();
        assertNotNull(url, "Should return the custom stylesheet URL");
        assertEquals(xsl.toURI().toURL(), url, "URL should point to the custom file");
      } finally {
        setWebInf(null);
      }
    }

    @Test
    void handle_withCustomStylesheet_producesHtml(@TempDir File tempDir) throws Exception {
      // A minimal identity-to-HTML stylesheet
      File xsl = new File(tempDir, "error.xsl");
      Files.writeString(xsl.toPath(),
          "<?xml version=\"1.0\"?><xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"
          + "<xsl:template match=\"/\"><html><body>CUSTOM</body></html></xsl:template></xsl:stylesheet>");

      setWebInf(tempDir);
      setOption(BerliozOption.ERROR_STYLESHEET, "error.xsl");
      try {
        HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
            .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
            .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Test error")
            .build();
        ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
        new ErrorHandlerServlet().handle(req, res.build());
        String body = res.content();
        assertAll(
            () -> assertEquals(500, res.status),
            () -> assertEquals("text/html;charset=UTF-8", res.contentType),
            () -> assertTrue(body.contains("CUSTOM"), "Custom stylesheet output should appear")
        );
      } finally {
        setWebInf(null);
      }
    }
  }

  // Helpers

  private static void writePreview(String name, String html) throws IOException {
    if (!Boolean.getBoolean("berlioz.generateSamples")) return;
    Path outDir = Paths.get("build/error-samples");
    Files.createDirectories(outDir);
    Files.writeString(outDir.resolve(name + ".html"), html, StandardCharsets.UTF_8);
  }

  private static void setOption(BerliozOption option, boolean value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    Map<String, String> settings = ref.get();
    if (value) settings.put(option.property(), "true");
    else settings.remove(option.property());
  }

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }

  private static void setWebInf(File dir) throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("ENV");
    f.setAccessible(true);
    if (dir == null) {
      ((AtomicReference<?>) f.get(null)).set(null);
    } else {
      GlobalSettings.setup(dir);
    }
  }
}
