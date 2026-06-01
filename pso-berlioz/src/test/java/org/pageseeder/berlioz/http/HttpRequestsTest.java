package org.pageseeder.berlioz.http;

import javax.servlet.http.HttpServletRequest;

import org.junit.Assert;
import org.junit.Test;

public class HttpRequestsTest {

  @Test
  public void testIsSafeRedirectURL_Relative() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("/docs/index.html", request));
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("../other", request));
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("search?q=test", request));
  }

  @Test
  public void testIsSafeRedirectURL_SameOrigin() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org:8080/docs", request));
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("//example.org:8080/docs", request));
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("https://EXAMPLE.ORG:8080/docs", request));
  }

  @Test
  public void testIsSafeRedirectURL_DefaultPorts() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 443).build();
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org/docs", request));
    Assert.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org:443/docs", request));
  }

  @Test
  public void testIsSafeRedirectURL_Unsafe() {
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assert.assertFalse(HttpRequests.isSafeRedirectURL(null, request));
    Assert.assertFalse(HttpRequests.isSafeRedirectURL("https://elsewhere.example/docs", request));
    Assert.assertFalse(HttpRequests.isSafeRedirectURL("https://example.org:9090/docs", request));
    Assert.assertFalse(HttpRequests.isSafeRedirectURL("/docs\r\nLocation: https://elsewhere.example", request));
    Assert.assertFalse(HttpRequests.isSafeRedirectURL("http://[invalid", request));
  }

}
