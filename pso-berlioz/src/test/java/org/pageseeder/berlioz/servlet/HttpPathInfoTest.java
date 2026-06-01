package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class HttpPathInfoTest {

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
  public void testExtensionMappingHtml() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/path/page.html"));
    Assertions.assertEquals(info.path(), "/path/page");
    Assertions.assertEquals(info.extension(), ".html");
    Assertions.assertEquals(info.prefix(), "");
    Assertions.assertEquals(info.context(), "");
  }

  @Test
  public void testExtensionMappingXml() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/feed.xml"));
    Assertions.assertEquals(info.path(), "/feed");
    Assertions.assertEquals(info.extension(), ".xml");
  }

  @Test
  public void testExtensionMappingJson() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/api/data.json"));
    Assertions.assertEquals(info.path(), "/api/data");
    Assertions.assertEquals(info.extension(), ".json");
  }

  @Test
  public void testExtensionMappingNoExtension() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/about"));
    Assertions.assertEquals(info.path(), "/about");
    Assertions.assertEquals(info.extension(), "");
  }

  @Test
  public void testExtensionMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assertions.assertEquals(info.context(), "/app");
    Assertions.assertEquals(info.path(), "/home");
    Assertions.assertEquals(info.extension(), ".html");
    Assertions.assertEquals(info.prefix(), "");
  }

  @Test
  public void testExtensionMappingRootPath() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/.html"));
    Assertions.assertEquals(info.path(), "/");
    Assertions.assertEquals(info.extension(), ".html");
  }

  // Prefix mapping tests (/prefix/* style)
  // ---------------------------------------------------------------------------

  @Test
  public void testPrefixMapping() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/api", "/users/42"));
    Assertions.assertEquals(info.path(), "/users/42");
    Assertions.assertEquals(info.prefix(), "/api");
    Assertions.assertEquals(info.extension(), "");
    Assertions.assertEquals(info.context(), "");
  }

  @Test
  public void testPrefixMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/myapp", "/svc", "/data"));
    Assertions.assertEquals(info.context(), "/myapp");
    Assertions.assertEquals(info.prefix(), "/svc");
    Assertions.assertEquals(info.path(), "/data");
    Assertions.assertEquals(info.extension(), "");
  }

  @Test
  public void testPrefixMappingEmptyPath() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/html", ""));
    Assertions.assertEquals(info.path(), "");
    Assertions.assertEquals(info.prefix(), "/html");
  }

  // toString tests
  // ---------------------------------------------------------------------------

  @Test
  public void testToStringExtensionMapped() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assertions.assertEquals(info.toString(), "/app/home.html");
  }

  @Test
  public void testToStringPrefixMapped() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/app", "/api", "/items"));
    Assertions.assertEquals(info.toString(), "/app/api/items");
  }

  // toXML tests
  // ---------------------------------------------------------------------------

  @Test
  public void testToXmlExtensionMapped() throws IOException {
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
  public void testToXmlPrefixMapped() throws IOException {
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
  public void testToXmlWithContext() throws IOException {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/page.json"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assertions.assertTrue(out.contains("context=\"/app\""), "Should contain context attribute");
    Assertions.assertTrue(out.contains("extension=\".json\""), "Should contain extension attribute");
  }
}
