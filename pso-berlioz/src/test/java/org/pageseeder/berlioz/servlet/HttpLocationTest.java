package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class HttpLocationTest {

  // build() tests
  // ---------------------------------------------------------------------------

  @Test
  public void testBuildExtractsSchemeHostPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(8080)
        .uri("/page.html").query(null)
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assert.assertEquals("http", loc.scheme());
    Assert.assertEquals("example.org", loc.host());
    Assert.assertEquals(8080, loc.port());
  }

  @Test
  public void testBuildExtractsPathAndQuery() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("example.org").port(443)
        .uri("/search").query("q=berlioz&page=2")
        .contextPath("").servletPath("/search.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assert.assertEquals("/search", loc.path());
    Assert.assertEquals("q=berlioz&page=2", loc.query());
  }

  @Test
  public void testBuildNullQueryBecomesEmpty() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80)
        .uri("/page").query(null)
        .contextPath("").servletPath("/page.html").pathInfo(null)
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assert.assertEquals("", loc.query());
  }

  @Test
  public void testBuildPathInfoIsPopulated() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80)
        .uri("/api/users").query(null)
        .contextPath("").servletPath("/api").pathInfo("/users")
        .build();
    HttpLocation loc = HttpLocation.build(req);
    Assert.assertNotNull(loc.info());
    Assert.assertEquals("/users", loc.info().path());
    Assert.assertEquals("/api", loc.info().prefix());
  }

  // toBaseURL() — default port suppression
  // ---------------------------------------------------------------------------

  @Test
  public void testToBaseUrlDefaultHttpPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(80).uri("/").build();
    Assert.assertEquals("http://example.org", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testToBaseUrlNonDefaultHttpPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("example.org").port(8080).uri("/").build();
    Assert.assertEquals("http://example.org:8080", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testToBaseUrlDefaultHttpsPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("secure.example.org").port(443).uri("/").build();
    Assert.assertEquals("https://secure.example.org", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testToBaseUrlNonDefaultHttpsPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("secure.example.org").port(8443).uri("/").build();
    Assert.assertEquals("https://secure.example.org:8443", HttpLocation.toBaseURL(req).toString());
  }

  // toBaseURL() — reverse proxy header handling
  // ---------------------------------------------------------------------------

  @Test
  public void testReverseProxySchemeOverridesServerScheme() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .build();
    Assert.assertEquals("https://internal", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testReverseProxyPortExtractedFromForwardedHost() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .header("X-Forwarded-Host", "public.example.org:8443")
        .build();
    Assert.assertEquals("https://internal:8443", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testReverseProxyForwardedHostWithoutPortUsesDefaultPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("http").host("internal").port(8080)
        .header("X-Forwarded-Proto", "https")
        .header("X-Forwarded-Host", "public.example.org")
        .build();
    // No port in forwarded host → port treated as -1 → omitted (default)
    Assert.assertEquals("https://internal", HttpLocation.toBaseURL(req).toString());
  }

  @Test
  public void testNoReverseProxyHeadersUsesServerPort() {
    HttpServletRequest req = ServletTestSupport.request()
        .scheme("https").host("app.example.org").port(443).uri("/").build();
    Assert.assertEquals("https://app.example.org", HttpLocation.toBaseURL(req).toString());
  }

  // toXML()
  // ---------------------------------------------------------------------------

  @Test
  public void testToXmlContainsSchemeHostPort() throws IOException {
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
    Assert.assertTrue(out.contains("scheme=\"http\""));
    Assert.assertTrue(out.contains("host=\"example.org\""));
    Assert.assertTrue(out.contains("port=\"8080\""));
    Assert.assertTrue(out.contains("query=\"a=1\""));
    Assert.assertTrue(out.contains("base=\"http://example.org:8080\""));
    Assert.assertTrue(out.contains("http://example.org:8080/page.html?a=1"));
  }

  @Test
  public void testToXmlOmitsPortFromBaseWhenDefault() throws IOException {
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
    Assert.assertTrue(out.contains("base=\"http://example.org\""));
    Assert.assertFalse("Base URL should not contain :80", out.contains(":80"));
  }
}
