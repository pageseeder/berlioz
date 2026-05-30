package org.pageseeder.mock.servlet;

import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class MockHttpServletResponseTest {

  @Test
  public void capturesWriterOutput() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    PrintWriter writer = response.getWriter();
    writer.write("hello");

    Assert.assertEquals("hello", response.getOutputAsString());
  }

  @Test
  public void capturesOutputStreamOutput() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());

    ServletOutputStream stream = response.getOutputStream();
    stream.write("hello".getBytes(StandardCharsets.UTF_8));

    Assert.assertEquals("hello", response.getOutputAsString());
  }

  @Test
  public void flushCommitsResponse() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    Assert.assertFalse(response.isCommitted());
    response.flushBuffer();

    Assert.assertTrue(response.isCommitted());
  }

  @Test
  public void resetClearsBufferHeadersCookiesAndStatus() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    response.getWriter().write("body");
    response.setHeader("X-Test", "yes");
    response.addCookie(new Cookie("a", "b"));
    response.setStatus(HttpServletResponse.SC_NOT_FOUND);

    response.reset();

    Assert.assertEquals("", response.getOutputAsString());
    Assert.assertNull(response.getHeader("X-Test"));
    Assert.assertTrue(response.getCookies().isEmpty());
    Assert.assertEquals(HttpServletResponse.SC_OK, response.getStatus());
  }

  @Test
  public void sendRedirectCommitsWithLocation() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();

    response.sendRedirect("/next");

    Assert.assertEquals(HttpServletResponse.SC_FOUND, response.getStatus());
    Assert.assertEquals("/next", response.getHeader("Location"));
    Assert.assertTrue(response.isCommitted());
  }
}
