package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

class HttpPathInfoTest {

  private static HttpServletRequest extensionMapped(String contextPath, String servletPath) {
    return ServletTestSupport.request()
        .contextPath(contextPath)
        .servletPath(servletPath)
        .pathInfo(null)
        .build();
  }

  private static HttpServletRequest prefixMapped(String contextPath, String servletPath, String pathInfo) {
    return ServletTestSupport.request()
        .contextPath(contextPath)
        .servletPath(servletPath)
        .pathInfo(pathInfo)
        .build();
  }

  // Extension mapping tests (*.html, *.xml style)
  // ---------------------------------------------------------------------------

  @Test
  void testExtensionMappingHtml() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/path/page.html"));
    Assertions.assertEquals("/path/page", info.path());
    Assertions.assertEquals(".html", info.extension());
    Assertions.assertEquals("", info.prefix());
    Assertions.assertEquals("", info.context());
  }

  @Test
  void testExtensionMappingXml() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/feed.xml"));
    Assertions.assertEquals("/feed", info.path());
    Assertions.assertEquals(".xml", info.extension());
  }

  @Test
  void testExtensionMappingJson() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/api/data.json"));
    Assertions.assertEquals("/api/data", info.path());
    Assertions.assertEquals(".json", info.extension());
  }

  @Test
  void testExtensionMappingNoExtension() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/about"));
    Assertions.assertEquals("/about", info.path());
    Assertions.assertEquals("", info.extension());
  }

  @Test
  void testExtensionMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assertions.assertEquals("/app", info.context());
    Assertions.assertEquals("/home", info.path());
    Assertions.assertEquals(".html", info.extension());
    Assertions.assertEquals("", info.prefix());
  }

  @Test
  void testExtensionMappingRootPath() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/.html"));
    Assertions.assertEquals("/", info.path());
    Assertions.assertEquals(".html", info.extension());
  }

  // Prefix mapping tests (/prefix/* style)
  // ---------------------------------------------------------------------------

  @Test
  void testPrefixMapping() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/api", "/users/42"));
    Assertions.assertEquals("/users/42", info.path());
    Assertions.assertEquals("/api", info.prefix());
    Assertions.assertEquals("", info.extension());
    Assertions.assertEquals("", info.context());
  }

  @Test
  void testPrefixMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/myapp", "/svc", "/data"));
    Assertions.assertEquals("/myapp", info.context());
    Assertions.assertEquals("/svc", info.prefix());
    Assertions.assertEquals("/data", info.path());
    Assertions.assertEquals("", info.extension());
  }

  @Test
  void testPrefixMappingEmptyPath() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/html", ""));
    Assertions.assertEquals("", info.path());
    Assertions.assertEquals("/html", info.prefix());
  }

  // toString tests
  // ---------------------------------------------------------------------------

  @Test
  void testToStringExtensionMapped() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assertions.assertEquals("/app/home.html", info.toString());
  }

  @Test
  void testToStringPrefixMapped() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/app", "/api", "/items"));
    Assertions.assertEquals("/app/api/items", info.toString());
  }

  // toXML tests
  // ---------------------------------------------------------------------------

  @Test
  void testToXmlExtensionMapped() throws IOException {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/home.html"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("info=\"/home\""), "Should contain info attribute");
    Assertions.assertTrue(out.contains("extension=\".html\""), "Should contain extension attribute");
    Assertions.assertFalse(out.contains("context="), "Should not have context attribute when empty");
    Assertions.assertFalse(out.contains("prefix="), "Should not have prefix attribute when empty");
  }

  @Test
  void testToXmlPrefixMapped() throws IOException {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/svc", "/items"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("prefix=\"/svc\""), "Should contain prefix attribute");
    Assertions.assertTrue(out.contains("info=\"/items\""), "Should contain info attribute");
    Assertions.assertFalse(out.contains("extension="), "Should not have extension attribute when empty");
  }

  @Test
  void testToXmlWithContext() throws IOException {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/page.json"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("context=\"/app\""), "Should contain context attribute");
    Assertions.assertTrue(out.contains("extension=\".json\""), "Should contain extension attribute");
  }
}
