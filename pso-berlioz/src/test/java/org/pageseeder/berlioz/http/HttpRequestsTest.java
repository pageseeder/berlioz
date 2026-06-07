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
  void testIsSafeRedirectURL_HttpToHttpsUpgrade() {
    // Port mismatch is allowed when upgrading from HTTP to HTTPS
    HttpServletRequest request = HttpTestSupport.request()
        .server("example.org", 8080).scheme("http").build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org:8443/new", request),
        "HTTP→HTTPS upgrade should be permitted even with a port change");
    // Same scheme with port mismatch is still blocked
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("http://example.org:9090/new", request),
        "Same-scheme redirect to a different port should be blocked");
  }

  @Test
  void testIsSafeRedirectURL_HttpsToHttpsPortMismatch() {
    // HTTPS→HTTPS with a different port is not an upgrade — must be blocked
    HttpServletRequest request = HttpTestSupport.request()
        .server("example.org", 8443).scheme("https").build();
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("https://example.org:9090/new", request));
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

  // Proxy-aware tests ---------------------------------------------------------------------

  @Test
  void testIsSafeRedirectURL_ProxyHost() {
    // Backend sees "localhost" but the public host is "example.org" via X-Forwarded-Host
    HttpServletRequest request = HttpTestSupport.request()
        .server("localhost", 8080)
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org")
        .build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("/new", request),
        "Relative URL is always safe");
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org/new", request),
        "Redirect to effective (forwarded) host should be permitted");
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("https://localhost/new", request),
        "Backend host should not be treated as safe when X-Forwarded-Host is set");
  }

  @Test
  void testIsSafeRedirectURL_ProxyHttpToHttpsUpgrade() {
    // Proxy terminates HTTPS: X-Forwarded-Proto=https, backend sees http on port 8080
    // A redirect from the public HTTPS origin to HTTPS on 443 should be safe
    HttpServletRequest request = HttpTestSupport.request()
        .server("example.org", 8080)
        .scheme("http")
        .header(HttpHeaders.X_FORWARDED_PROTO, "http")
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org:80")
        .build();
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://example.org/new", request),
        "HTTP→HTTPS upgrade should be permitted via proxy");
  }

  @Test
  void testEffectiveHost_ForwardedHeader() {
    HttpServletRequest withPort = HttpTestSupport.request()
        .server("localhost", 8080)
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org:443")
        .build();
    Assertions.assertEquals("example.org", HttpRequests.effectiveHost(withPort));

    HttpServletRequest withoutPort = HttpTestSupport.request()
        .server("localhost", 8080)
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org")
        .build();
    Assertions.assertEquals("example.org", HttpRequests.effectiveHost(withoutPort));

    HttpServletRequest noHeader = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertEquals("example.org", HttpRequests.effectiveHost(noHeader));
  }

  @Test
  void testEffectivePort_ForwardedHeader() {
    HttpServletRequest withPort = HttpTestSupport.request()
        .server("localhost", 8080)
        .header(HttpHeaders.X_FORWARDED_PROTO, "https")
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org:8443")
        .build();
    Assertions.assertEquals(8443, HttpRequests.effectivePort(withPort));

    HttpServletRequest noPort = HttpTestSupport.request()
        .server("localhost", 8080)
        .header(HttpHeaders.X_FORWARDED_PROTO, "https")
        .header(HttpHeaders.X_FORWARDED_HOST, "example.org")
        .build();
    Assertions.assertEquals(-1, HttpRequests.effectivePort(noPort));

    HttpServletRequest noProxy = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertEquals(8080, HttpRequests.effectivePort(noProxy));
  }

}
