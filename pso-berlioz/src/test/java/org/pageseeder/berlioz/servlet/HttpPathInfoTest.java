package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Test;
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
    Assert.assertEquals("/path/page", info.path());
    Assert.assertEquals(".html", info.extension());
    Assert.assertEquals("", info.prefix());
    Assert.assertEquals("", info.context());
  }

  @Test
  public void testExtensionMappingXml() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/feed.xml"));
    Assert.assertEquals("/feed", info.path());
    Assert.assertEquals(".xml", info.extension());
  }

  @Test
  public void testExtensionMappingJson() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/api/data.json"));
    Assert.assertEquals("/api/data", info.path());
    Assert.assertEquals(".json", info.extension());
  }

  @Test
  public void testExtensionMappingNoExtension() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/about"));
    Assert.assertEquals("/about", info.path());
    Assert.assertEquals("", info.extension());
  }

  @Test
  public void testExtensionMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assert.assertEquals("/app", info.context());
    Assert.assertEquals("/home", info.path());
    Assert.assertEquals(".html", info.extension());
    Assert.assertEquals("", info.prefix());
  }

  @Test
  public void testExtensionMappingRootPath() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("", "/.html"));
    Assert.assertEquals("/", info.path());
    Assert.assertEquals(".html", info.extension());
  }

  // Prefix mapping tests (/prefix/* style)
  // ---------------------------------------------------------------------------

  @Test
  public void testPrefixMapping() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/api", "/users/42"));
    Assert.assertEquals("/users/42", info.path());
    Assert.assertEquals("/api", info.prefix());
    Assert.assertEquals("", info.extension());
    Assert.assertEquals("", info.context());
  }

  @Test
  public void testPrefixMappingWithContext() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/myapp", "/svc", "/data"));
    Assert.assertEquals("/myapp", info.context());
    Assert.assertEquals("/svc", info.prefix());
    Assert.assertEquals("/data", info.path());
    Assert.assertEquals("", info.extension());
  }

  @Test
  public void testPrefixMappingEmptyPath() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/html", ""));
    Assert.assertEquals("", info.path());
    Assert.assertEquals("/html", info.prefix());
  }

  // toString tests
  // ---------------------------------------------------------------------------

  @Test
  public void testToStringExtensionMapped() {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/home.html"));
    Assert.assertEquals("/app/home.html", info.toString());
  }

  @Test
  public void testToStringPrefixMapped() {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("/app", "/api", "/items"));
    Assert.assertEquals("/app/api/items", info.toString());
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
    Assert.assertTrue("Should contain info attribute", out.contains("info=\"/home\""));
    Assert.assertTrue("Should contain extension attribute", out.contains("extension=\".html\""));
    Assert.assertFalse("Should not have context attribute when empty", out.contains("context="));
    Assert.assertFalse("Should not have prefix attribute when empty", out.contains("prefix="));
  }

  @Test
  public void testToXmlPrefixMapped() throws IOException {
    HttpPathInfo info = new HttpPathInfo(prefixMapped("", "/svc", "/items"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assert.assertTrue("Should contain prefix attribute", out.contains("prefix=\"/svc\""));
    Assert.assertTrue("Should contain info attribute", out.contains("info=\"/items\""));
    Assert.assertFalse("Should not have extension attribute when empty", out.contains("extension="));
  }

  @Test
  public void testToXmlWithContext() throws IOException {
    HttpPathInfo info = new HttpPathInfo(extensionMapped("/app", "/page.json"));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    info.toXML(xml);
    xml.flush();
    String out = xml.toString();
    Assert.assertTrue("Should contain context attribute", out.contains("context=\"/app\""));
    Assert.assertTrue("Should contain extension attribute", out.contains("extension=\".json\""));
  }
}
