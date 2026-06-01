package org.pageseeder.mock.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

class MockHttpServletResponseTest {

  @Test
  void capturesWriterOutput() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    PrintWriter writer = response.getWriter();
    writer.write("hello");

    Assertions.assertEquals("hello", response.getOutputAsString());
  }

  @Test
  void capturesOutputStreamOutput() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    ServletOutputStream stream = response.getOutputStream();
    stream.write("hello".getBytes(StandardCharsets.UTF_8));

    Assertions.assertEquals("hello", response.getOutputAsString());
  }

  @Test
  void flushCommitsResponse() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assertions.assertFalse(response.isCommitted());
    response.flushBuffer();

    Assertions.assertTrue(response.isCommitted());
  }

  @Test
  void resetClearsBufferHeadersCookiesAndStatus() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.getWriter().write("body");
    response.setHeader("X-Test", "yes");
    response.addCookie(new Cookie("a", "b"));
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    response.reset();

    Assertions.assertEquals("", response.getOutputAsString());
    Assertions.assertNull(response.getHeader("X-Test"));
    Assertions.assertTrue(response.getCookies().isEmpty());
    Assertions.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  void sendRedirectCommitsWithLocation() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    response.sendRedirect("/next");

    Assertions.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
    Assertions.assertEquals("/next", response.getHeader("Location"));
    Assertions.assertTrue(response.isCommitted());
  }
}
