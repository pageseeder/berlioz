package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import org.pageseeder.berlioz.content.Request;

import static org.junit.jupiter.api.Assertions.*;

class GetRuntimeInfoTest {

  @Test
  void testProcess_writesRuntimeElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetRuntimeInfo().generate(emptyRequest(), xml);
    String out = xml.toString();
    assertTrue(out.contains("<runtime"), "Should write <runtime> element");
    assertTrue(out.contains("processors="), "Should include processors attribute");
  }

  @Test
  void testProcess_writesMemoryChild() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetRuntimeInfo().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    Element runtime = doc.getDocumentElement();
    assertEquals("runtime", runtime.getTagName());

    Element memory = (Element) runtime.getElementsByTagName("memory").item(0);
    assertNotNull(memory, "Should have <memory> child");
    assertFalse(memory.getAttribute("free").isEmpty());
    assertFalse(memory.getAttribute("total").isEmpty());
    assertFalse(memory.getAttribute("max").isEmpty());
  }

  @Test
  void testProcess_processorCountPositive() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetRuntimeInfo().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    int processors = Integer.parseInt(doc.getDocumentElement().getAttribute("processors"));
    assertTrue(processors > 0, "Processor count should be positive");
  }

  @Test
  void testProcess_memoryValuesNumeric() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetRuntimeInfo().generate(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    Element memory = (Element) doc.getDocumentElement().getElementsByTagName("memory").item(0);
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("free")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("total")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("max")));
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
