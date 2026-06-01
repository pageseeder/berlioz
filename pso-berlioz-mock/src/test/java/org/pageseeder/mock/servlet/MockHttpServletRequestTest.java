package org.pageseeder.mock.servlet;

import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

public class MockHttpServletRequestTest {

  @Test
  public void createWithoutQuery() {
    MockHttpServletRequest request = MockHttpServletRequest.create("https://example.org/index.html", "GET");

    Assert.assertEquals("https", request.getScheme());
    Assert.assertEquals("example.org", request.getServerName());
    Assert.assertEquals(443, request.getServerPort());
    Assert.assertFalse(request.getParameterNames().hasMoreElements());
  }

  @Test
  public void createWithQueryParameters() {
    MockHttpServletRequest request = MockHttpServletRequest.create(
        "http://example.org/search?q=berlioz&page=1&empty", "GET");

    Assert.assertEquals("berlioz", request.getParameter("q"));
    Assert.assertEquals("1", request.getParameter("page"));
    Assert.assertEquals("", request.getParameter("empty"));
  }

  @Test
  public void readsBodyThroughInputStreamAndReader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent("hello");

    ServletInputStream stream = request.getInputStream();
    Assert.assertEquals('h', stream.read());
    Assert.assertTrue(stream.isReady());
    Assert.assertFalse(stream.isFinished());

    BufferedReader reader = request.getReader();
    Assert.assertEquals("hello", reader.readLine());
    Assert.assertEquals(5, request.getContentLength());
  }

  @Test
  public void sessionsAreCreatedAndCanChangeId() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    Assert.assertNull(request.getSession(false));
    HttpSession session = request.getSession();
    String original = session.getId();

    Assert.assertTrue(session.isNew());
    Assert.assertTrue(request.isRequestedSessionIdValid());
    Assert.assertNotEquals(original, request.changeSessionId());
    Assert.assertTrue(request.isRequestedSessionIdValid());
  }

  @Test
  public void cookiesAndLocalesAreStored() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addCookie(new Cookie("flavour", "vanilla"));
    request.setLocale(Locale.CANADA_FRENCH);

    Assert.assertEquals("flavour", request.getCookies()[0].getName());
    Enumeration<Locale> locales = request.getLocales();
    Assert.assertEquals(Locale.CANADA_FRENCH, Collections.list(locales).get(0));
  }

  @Test
  public void characterEncodingIsUsedForStringContent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCharacterEncoding(StandardCharsets.UTF_16.name());
    request.setContent("A");

    Assert.assertEquals("A", request.getReader().readLine());
  }
}
