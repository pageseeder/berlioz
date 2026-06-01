package org.pageseeder.mock.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

class MockHttpServletRequestTest {

  @Test
  void createWithoutQuery() {
    MockHttpServletRequest request = MockHttpServletRequest.create("https://example.org/index.html", "GET");

    Assertions.assertEquals("https", request.getScheme());
    Assertions.assertEquals("example.org", request.getServerName());
    Assertions.assertEquals(443, request.getServerPort());
    Assertions.assertFalse(request.getParameterNames().hasMoreElements());
  }

  @Test
  void createWithQueryParameters() {
    MockHttpServletRequest request = MockHttpServletRequest.create(
        "http://example.org/search?q=berlioz&page=1&empty", "GET");

    Assertions.assertEquals("berlioz", request.getParameter("q"));
    Assertions.assertEquals("1", request.getParameter("page"));
    Assertions.assertEquals("", request.getParameter("empty"));
  }

  @Test
  void readsBodyThroughInputStreamAndReader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent("hello");

    ServletInputStream stream = request.getInputStream();
    Assertions.assertEquals('h', stream.read());
    Assertions.assertTrue(stream.isReady());
    Assertions.assertFalse(stream.isFinished());

    BufferedReader reader = request.getReader();
    Assertions.assertEquals("hello", reader.readLine());
    Assertions.assertEquals(5, request.getContentLength());
  }

  @Test
  void sessionsAreCreatedAndCanChangeId() {
    MockHttpServletRequest request = new MockHttpServletRequest();

    Assertions.assertNull(request.getSession(false));
    HttpSession session = request.getSession();
    String original = session.getId();

    Assertions.assertTrue(session.isNew());
    Assertions.assertTrue(request.isRequestedSessionIdValid());
    Assertions.assertNotEquals(original, request.changeSessionId());
    Assertions.assertTrue(request.isRequestedSessionIdValid());
  }

  @Test
  void cookiesAndLocalesAreStored() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addCookie(new Cookie("flavour", "vanilla"));
    request.setLocale(Locale.CANADA_FRENCH);

    Assertions.assertEquals("flavour", request.getCookies()[0].getName());
    Enumeration<Locale> locales = request.getLocales();
    Assertions.assertEquals(Locale.CANADA_FRENCH, Collections.list(locales).get(0));
  }

  @Test
  void characterEncodingIsUsedForStringContent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCharacterEncoding(StandardCharsets.UTF_16.name());
    request.setContent("A");

    Assertions.assertEquals("A", request.getReader().readLine());
  }
}
