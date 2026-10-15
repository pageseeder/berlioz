package org.pageseeder.berlioz.http;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

class HttpRequestsTest {

  @AfterEach
  void resetRedirectSettings() throws ReflectiveOperationException {
    removeOption(BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS);
  }

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

  @Test
  void testIsSafeRedirectURL_TripleSlashBypass() {
    // SEC-02: Java's URI parser reports no authority for "///host" and "////host"
    // (the authority component would be empty), but browsers resolve these as
    // network-path references to an absolute URL on the attacker's host.
    HttpServletRequest request = HttpTestSupport.request().server("example.org", 8080).build();
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("///attacker.example/path", request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("////attacker.example/path", request));
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("///attacker.example", request));
    // Ordinary relative paths and the legitimate protocol-relative (two-slash) same-origin
    // case must still be accepted.
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("/docs/index.html", request));
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("//example.org:8080/docs", request));
  }

  // Proxy-aware tests ---------------------------------------------------------------------
  //
  // SEC-03: X-Forwarded-Host/-Proto are attacker-controllable headers, so they are ignored for
  // the same-host redirect decision unless BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS is
  // explicitly enabled. The "_SecureDefault" tests below prove the header is ignored out of the
  // box; the others enable the opt-in to prove the (still-available) proxy-aware behaviour.

  @Test
  void testIsSafeRedirectURL_ForwardedHostIgnoredByDefault() {
    // No REDIRECT_TRUST_FORWARDED_HEADERS configured: a forged X-Forwarded-Host must not be
    // able to make an external host pass as "the current host".
    HttpServletRequest request = HttpTestSupport.request()
        .server("app.example", 443).scheme("https")
        .header(HttpHeaders.X_FORWARDED_HOST, "attacker.example")
        .build();
    Assertions.assertFalse(HttpRequests.isSafeRedirectURL("https://attacker.example/phish", request),
        "Forged X-Forwarded-Host must not grant same-host status by default");
    Assertions.assertTrue(HttpRequests.isSafeRedirectURL("https://app.example/new", request),
        "The real server name is still treated as same-host");
  }

  @Test
  void testEffectiveHost_ForwardedHeaderIgnoredByDefault() {
    HttpServletRequest request = HttpTestSupport.request()
        .server("app.example", 8080)
        .header(HttpHeaders.X_FORWARDED_HOST, "attacker.example")
        .build();
    Assertions.assertEquals("app.example", HttpRequests.effectiveHost(request));
  }

  @Test
  void testEffectiveScheme_ForwardedHeaderIgnoredByDefault() {
    HttpServletRequest request = HttpTestSupport.request()
        .server("app.example", 8080).scheme("http")
        .header(HttpHeaders.X_FORWARDED_PROTO, "https")
        .build();
    Assertions.assertEquals("http", HttpRequests.effectiveScheme(request));
  }

  @Test
  void testEffectivePort_ForwardedHeaderIgnoredByDefault() {
    HttpServletRequest request = HttpTestSupport.request()
        .server("app.example", 8080)
        .header(HttpHeaders.X_FORWARDED_PROTO, "https")
        .header(HttpHeaders.X_FORWARDED_HOST, "app.example:8443")
        .build();
    Assertions.assertEquals(8080, HttpRequests.effectivePort(request));
  }

  @Test
  void testIsSafeRedirectURL_ProxyHost() throws ReflectiveOperationException {
    setOption(BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS, "true");
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
  void testIsSafeRedirectURL_ProxyHttpToHttpsUpgrade() throws ReflectiveOperationException {
    setOption(BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS, "true");
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
  void testEffectiveHost_ForwardedHeader() throws ReflectiveOperationException {
    setOption(BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS, "true");
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
  void testEffectivePort_ForwardedHeader() throws ReflectiveOperationException {
    setOption(BerliozOption.REDIRECT_TRUST_FORWARDED_HEADERS, "true");
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

  @Test
  void testAcceptsGZipCompression() {
    HttpServletRequest accepted = HttpTestSupport.request()
        .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
        .build();
    HttpServletRequest rejected = HttpTestSupport.request()
        .header(HttpHeaders.ACCEPT_ENCODING, "gzip;q=0")
        .build();
    HttpServletRequest absent = HttpTestSupport.request().build();

    Assertions.assertTrue(HttpRequests.acceptsGZipCompression(accepted));
    Assertions.assertFalse(HttpRequests.acceptsGZipCompression(rejected));
    Assertions.assertFalse(HttpRequests.acceptsGZipCompression(absent));
  }

  // GlobalSettings test helpers ------------------------------------------------------------

  private static void setOption(BerliozOption option, String value) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    ref.compareAndSet(null, new HashMap<>());
    ref.get().put(option.property(), value);
  }

  private static void removeOption(BerliozOption option) throws ReflectiveOperationException {
    AtomicReference<Map<String, String>> ref = settingsRef();
    if (ref.get() != null) ref.get().remove(option.property());
  }

  @SuppressWarnings("unchecked")
  private static AtomicReference<Map<String, String>> settingsRef() throws ReflectiveOperationException {
    Field f = GlobalSettings.class.getDeclaredField("SETTINGS");
    f.setAccessible(true);
    return (AtomicReference<Map<String, String>>) f.get(null);
  }

}
