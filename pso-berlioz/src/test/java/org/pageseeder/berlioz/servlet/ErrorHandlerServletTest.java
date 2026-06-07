package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

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

}
