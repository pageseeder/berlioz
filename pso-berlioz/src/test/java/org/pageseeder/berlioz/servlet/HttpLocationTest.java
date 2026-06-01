package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

class HttpLocationTest {

  // build() tests
  // ---------------------------------------------------------------------------

  @Test
  void testBuildExtractsSchemeHostPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(8080)
        .uri("/page.html").query(null)
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assertions.assertEquals("http", loc.scheme());
    Assertions.assertEquals("example.org", loc.host());
    Assertions.assertEquals(8080, loc.port());
  }

  @Test
  void testBuildExtractsPathAndQuery() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("example.org").port(443)
        .uri("/search").query("q=berlioz&page=2")
        .contextPath("").servletPath("/search.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assertions.assertEquals("/search", loc.path());
    Assertions.assertEquals("q=berlioz&page=2", loc.query());
  }

  @Test
  void testBuildNullQueryBecomesEmpty() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80)
        .uri("/page").query(null)
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assertions.assertEquals("", loc.query());
  }

  @Test
  void testBuildPathInfoIsPopulated() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80)
        .uri("/api/users").query(null)
        .contextPath("").servletPath("/api").pathInfo("/users")
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assertions.assertNotNull(loc.info());
    Assertions.assertEquals("/users", loc.info().path());
    Assertions.assertEquals("/api", loc.info().prefix());
  }

  // toBaseURL() — default port suppression
  // ---------------------------------------------------------------------------

  @Test
  void testToBaseUrlDefaultHttpPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80).uri("/").build();
    Assertions.assertEquals("http://example.org", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testToBaseUrlNonDefaultHttpPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(8080).uri("/").build();
    Assertions.assertEquals("http://example.org:8080", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testToBaseUrlDefaultHttpsPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("secure.example.org").port(443).uri("/").build();
    Assertions.assertEquals("https://secure.example.org", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testToBaseUrlNonDefaultHttpsPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("secure.example.org").port(8443).uri("/").build();
    Assertions.assertEquals("https://secure.example.org:8443", HttpLocation.toBaseURL(req).toString());
  }

  // toBaseURL() — reverse proxy header handling
  // ---------------------------------------------------------------------------

  @Test
  void testReverseProxySchemeOverridesServerScheme() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .build();
    Assertions.assertEquals("https://internal", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testReverseProxyPortExtractedFromForwardedHost() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .header("X-Forwarded-Host", "public.example.org:8443")
        .build();
    Assertions.assertEquals("https://internal:8443", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testReverseProxyForwardedHostWithoutPortUsesDefaultPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .header("X-Forwarded-Host", "public.example.org")
        .build();
    // No port in forwarded host → port treated as -1 → omitted (default)
    Assertions.assertEquals("https://internal", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  void testNoReverseProxyHeadersUsesServerPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("app.example.org").port(443).uri("/").build();
    Assertions.assertEquals("https://app.example.org", HttpLocation.toBaseURL(req).toString());
  }

  // toXML()
  // ---------------------------------------------------------------------------

  @Test
  void testToXmlContainsSchemeHostPort() throws IOException {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(8080)
        .uri("/page.html").query("a=1")
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    loc.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("scheme=\"http\""));
    Assertions.assertTrue(out.contains("host=\"example.org\""));
    Assertions.assertTrue(out.contains("port=\"8080\""));
    Assertions.assertTrue(out.contains("query=\"a=1\""));
    Assertions.assertTrue(out.contains("base=\"http://example.org:8080\""));
    Assertions.assertTrue(out.contains("http://example.org:8080/page.html?a=1"));
  }

  @Test
  void testToXmlOmitsPortFromBaseWhenDefault() throws IOException {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80)
        .uri("/page.html").query(null)
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    loc.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("base=\"http://example.org\""));
    Assertions.assertFalse(out.contains(":80"), "Base URL should not contain :80");
  }
}
