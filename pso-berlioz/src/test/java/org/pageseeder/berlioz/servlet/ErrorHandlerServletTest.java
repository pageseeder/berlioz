package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlerServletTest {

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

  // Null-safety on getErrorCode with valid Integer attribute (tested via constant)

  @Test
  void testBerliozErrorIdConstant_isNonNull() {
    assertNotNull(ErrorHandlerServlet.BERLIOZ_ERROR_ID);
    assertFalse(ErrorHandlerServlet.BERLIOZ_ERROR_ID.isEmpty());
  }

  // Legacy format (default: berlioz.errors.problem = false)
  //
  // Note: the test JVM uses the built-in XSLT 1.0 processor (no Saxon). The failsafe XSLT
  // relies on XSLT 2.0 features (format-dateTime), so transformFailSafe() falls back to
  // returning the raw XML. Content type is therefore application/xml, not text/html.
  // In a runtime environment that includes Saxon, the XSLT produces HTML instead.

  @Test
  void handle_legacyFormat_404_emitsClientErrorXml() throws Exception {
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 404)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Resource not found")
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
    new ErrorHandlerServlet().handle(req, res.build());
    String body = res.content();
    assertAll(
        () -> assertEquals(404, res.status),
        () -> assertEquals("application/xml;charset=UTF-8", res.contentType),
        () -> assertTrue(body.contains("<client-error"),   "root element should be client-error"),
        () -> assertTrue(body.contains("Resource not found"), "message should be present"),
        () -> assertFalse(body.contains("<problem>"),      "legacy format must not emit <problem>")
    );
  }

  @Test
  void handle_legacyFormat_500_emitsServerErrorXml() throws Exception {
    HttpServletRequest req = ServletTestSupport.request().uri("/test.html")
        .attribute(ErrorHandlerServlet.ERROR_STATUS_CODE, 500)
        .attribute(ErrorHandlerServlet.ERROR_MESSAGE, "Unexpected error")
        .build();
    ServletTestSupport.ResponseRecorder res = ServletTestSupport.response();
    new ErrorHandlerServlet().handle(req, res.build());
    String body = res.content();
    assertAll(
        () -> assertEquals(500, res.status),
        () -> assertEquals("application/xml;charset=UTF-8", res.contentType),
        () -> assertTrue(body.contains("<server-error"),   "root element should be server-error"),
        () -> assertTrue(body.contains("Unexpected error"), "message should be present"),
        () -> assertFalse(body.contains("<problem>"),      "legacy format must not emit <problem>")
    );
  }

  // Problem format (opt-in: berlioz.errors.problem = true)
  //
  // Same XSLT caveat applies: in the test environment the XSLT falls back to raw XML,
  // so we assert on the problem+xml content type and the RFC 9457 XML structure directly.

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
      assertAll(
          () -> assertEquals(404, res.status),
          () -> assertEquals("application/problem+xml;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("<problem>"),              "root element should be <problem>"),
          () -> assertTrue(body.contains("<status>404</status>"),   "status member should be 404"),
          () -> assertTrue(body.contains("<title>Not Found</title>"), "title member should be present"),
          () -> assertTrue(body.contains("<detail>Resource not found</detail>"), "detail should match message"),
          () -> assertTrue(body.contains("urn:berlioz:problem:not-found"), "type URN should be present"),
          () -> assertFalse(body.contains("<client-error"),         "legacy root element must not appear")
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
      assertAll(
          () -> assertEquals(500, res.status),
          () -> assertEquals("application/problem+xml;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("<problem>"),                       "root element should be <problem>"),
          () -> assertTrue(body.contains("<status>500</status>"),            "status member should be 500"),
          () -> assertTrue(body.contains("<title>Internal Server Error</title>"), "title should be present"),
          () -> assertTrue(body.contains("urn:berlioz:problem:"),            "type URN should be present"),
          () -> assertFalse(body.contains("<server-error"),                  "legacy root element must not appear")
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
      assertAll(
          () -> assertEquals(405, res.status),
          () -> assertEquals("application/problem+xml;charset=UTF-8", res.contentType),
          () -> assertTrue(body.contains("urn:berlioz:problem:method-not-allowed"), "type URN should name the problem")
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

  // Helpers

  @SuppressWarnings("unchecked")
  private static void setOption(BerliozOption option, boolean value) throws ReflectiveOperationException {
    // GlobalSettings.load() catches IllegalStateException when no environment is configured
    // and sets SETTINGS to an empty mutable HashMap — safe to call unconditionally.
    try { GlobalSettings.load(); } catch (IllegalStateException ignored) {}
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    Map<String, String> settings = ((AtomicReference<Map<String, String>>) f.get(null)).get();
    if (settings != null) {
      if (value) settings.put(option.property(), "true");
      else settings.remove(option.property());
    }
  }

  @SuppressWarnings("unchecked")
  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    try { GlobalSettings.load(); } catch (IllegalStateException ignored) {}
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    Map<String, String> settings = ((AtomicReference<Map<String, String>>) f.get(null)).get();
    if (settings != null) {
      settings.put(option.property(), value);
    }
  }
}
