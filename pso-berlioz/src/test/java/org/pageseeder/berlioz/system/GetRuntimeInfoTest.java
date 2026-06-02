package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetRuntimeInfoTest {

  @Test
  void testProcess_writesRuntimeElement() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetRuntimeInfo().process(emptyRequest(), xml);
    String out = xml.toString();
    assertTrue(out.contains("<runtime"), "Should write <runtime> element");
    assertTrue(out.contains("processors="), "Should include processors attribute");
  }

  @Test
  void testProcess_writesMemoryChild() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetRuntimeInfo().process(emptyRequest(), xml);
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
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetRuntimeInfo().process(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    int processors = Integer.parseInt(doc.getDocumentElement().getAttribute("processors"));
    assertTrue(processors > 0, "Processor count should be positive");
  }

  @Test
  void testProcess_memoryValuesNumeric() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetRuntimeInfo().process(emptyRequest(), xml);
    Document doc = parse(xml.toString());
    Element memory = (Element) doc.getDocumentElement().getElementsByTagName("memory").item(0);
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("free")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("total")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("max")));
  }

  private static ContentRequest emptyRequest() {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, m, args) -> null);
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
