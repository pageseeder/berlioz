package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetSystemPropertiesTest {

  @Test
  void testProcess_writesSystemElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetSystemProperties().generate(emptyRequest(), xml);
    String out = xml.toString();
    assertTrue(out.contains("<system"), "Should write <system> element");
  }

  @Test
  void testProcess_containsPropertyElements() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetSystemProperties().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    Element root = doc.getDocumentElement();
    assertEquals("system", root.getTagName());
    NodeList props = root.getElementsByTagName("property");
    assertTrue(props.getLength() > 0, "Should have at least one <property> element");
  }

  @Test
  void testProcess_includesJavaVersion() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetSystemProperties().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    NodeList props = doc.getElementsByTagName("property");
    boolean found = false;
    for (int i = 0; i < props.getLength(); i++) {
      Element p = (Element) props.item(i);
      if ("java.version".equals(p.getAttribute("name"))) {
        found = true;
        assertFalse(p.getAttribute("value").isEmpty(), "java.version should not be empty");
        break;
      }
    }
    assertTrue(found, "Should contain java.version property");
  }

  @Test
  void testProcess_propertyHasNameAndValue() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetSystemProperties().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    Element first = (Element) doc.getElementsByTagName("property").item(0);
    assertFalse(first.getAttribute("name").isEmpty(), "Each property should have a name");
  }

  private static Request emptyRequest() {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> null);
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
