package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetRuntimeInfoTest {

  // process() tests — XML
  // ---------------------------------------------------------------------------

  @Test
  void testProcessWritesRuntimeElement() {
    String out = process();
    assertTrue(out.startsWith("<runtime processors=\""), "Should write <runtime> element with processors attribute");
  }

  @Test
  void testProcessWritesMemoryChild() throws Exception {
    Document doc = parseXml(process());
    Element runtime = doc.getDocumentElement();
    assertEquals("runtime", runtime.getTagName());

    Element memory = (Element) runtime.getElementsByTagName("memory").item(0);
    assertNotNull(memory, "Should have <memory> child");
    assertFalse(memory.getAttribute("free").isEmpty());
    assertFalse(memory.getAttribute("total").isEmpty());
    assertFalse(memory.getAttribute("max").isEmpty());
  }

  @Test
  void testProcessProcessorCountPositive() throws Exception {
    Document doc = parseXml(process());
    int processors = Integer.parseInt(doc.getDocumentElement().getAttribute("processors"));
    assertTrue(processors > 0, "Processor count should be positive");
  }

  @Test
  void testProcessMemoryValuesNumeric() throws Exception {
    Document doc = parseXml(process());
    Element memory = (Element) doc.getDocumentElement().getElementsByTagName("memory").item(0);
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("free")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("total")));
    assertDoesNotThrow(() -> Long.parseLong(memory.getAttribute("max")));
  }

  // process() tests — JSON
  // ---------------------------------------------------------------------------

  @Test
  void testProcessJsonHasProcessorsAndMemoryObject() {
    String out = processJson();
    assertTrue(out.startsWith("{\"processors\":"), "Should include processors property");
    assertTrue(out.contains("\"memory\":{"), "Should include memory object");
    assertTrue(out.contains("\"free\":"));
    assertTrue(out.contains("\"total\":"));
    assertTrue(out.contains("\"max\":"));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process() {
    GetRuntimeInfo gen = new GetRuntimeInfo();
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(emptyRequest(), out);
    return out.toString();
  }

  private static String processJson() {
    GetRuntimeInfo gen = new GetRuntimeInfo();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(emptyRequest(), out);
    return out.toString();
  }

  private static Request emptyRequest() {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> null);
  }

  private static Document parseXml(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
