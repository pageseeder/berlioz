package org.pageseeder.berlioz.http;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpRequestsTest {

  @Test
  void testIsSafeRedirectURL_Relative() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("/docs/index.html", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("../other", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("search?q=test", request));
  }

  @Test
  void testIsSafeRedirectURL_SameOrigin() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org:8080/docs", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("//example.org:8080/docs", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://EXAMPLE.ORG:8080/docs", request));
  }

  @Test
  void testIsSafeRedirectURL_DefaultPorts() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 443).build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org/docs", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org:443/docs", request));
  }

  @Test
  void testIsSafeRedirectURL_Unsafe() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL(null, request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("https://elsewhere.example/docs", request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("https://example.org:9090/docs", request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("/docs\r\nLocation: https://elsewhere.example", request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("http://[invalid", request));
  }

}
