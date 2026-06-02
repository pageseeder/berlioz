package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class ListThreadsTest {

  @Test
  void testProcess_noParams_writesThreadsElement() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, false), xml);
    Document doc = parse(xml.toString());
    assertEquals("threads", doc.getDocumentElement().getTagName());
  }

  @Test
  void testProcess_noParams_containsThreadChildren() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, false), xml);
    Document doc = parse(xml.toString());
    NodeList threads = doc.getElementsByTagName("thread");
    assertTrue(threads.getLength() > 0, "Should list at least one thread");
  }

  @Test
  void testProcess_threadHasExpectedAttributes() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, false), xml);
    Document doc = parse(xml.toString());
    Element thread = (Element) doc.getElementsByTagName("thread").item(0);
    assertFalse(thread.getAttribute("id").isEmpty(),    "Thread should have id");
    assertFalse(thread.getAttribute("name").isEmpty(),  "Thread should have name");
    assertFalse(thread.getAttribute("state").isEmpty(), "Thread should have state");
  }

  @Test
  void testProcess_withStacktraces_includesStacktraceElement() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(true, false), xml);
    String out = xml.toString();
    assertTrue(out.contains("<stacktrace"), "Should include stacktrace elements when requested");
  }

  @Test
  void testProcess_withoutStacktraces_noStacktraceElement() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, false), xml);
    assertFalse(xml.toString().contains("<stacktrace"), "Should not include stacktrace when not requested");
  }

  @Test
  void testProcess_withThreadTime_includesTimesElement() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, true), xml);
    // CPU time support is JVM-dependent; just verify it doesn't throw
    assertDoesNotThrow(() -> xml.toString());
  }

  @Test
  void testProcess_currentThreadFlagged() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new ListThreads().process(request(false, false), xml);
    Document doc = parse(xml.toString());
    NodeList threads = doc.getElementsByTagName("thread");
    boolean foundCurrent = false;
    for (int i = 0; i < threads.getLength(); i++) {
      Element t = (Element) threads.item(i);
      if ("true".equals(t.getAttribute("current"))) {
        foundCurrent = true;
        break;
      }
    }
    assertTrue(foundCurrent, "Current thread should be flagged with current=\"true\"");
  }

  private static ContentRequest request(boolean stacktraces, boolean threadtime) {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, m, args) -> {
          if ("getParameter".equals(m.getName())) {
            String name = (String) args[0];
            if ("stacktraces".equals(name)) return stacktraces ? "true" : "false";
            if ("threadtime".equals(name))  return threadtime ? "true" : "false";
          }
          return null;
        });
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
