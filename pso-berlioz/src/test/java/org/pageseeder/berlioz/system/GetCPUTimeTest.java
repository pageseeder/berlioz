package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GetCPUTimeTest {

  @Test
  void testProcess_zeroInterval_setsBadRequest() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetCPUTime().process(request(0, -1L, status), xml);

    assertEquals(ContentStatus.BAD_REQUEST, status.get());
  }

  @Test
  void testProcess_negativeInterval_setsBadRequest() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetCPUTime().process(request(-5, -1L, status), xml);

    assertEquals(ContentStatus.BAD_REQUEST, status.get());
  }

  @Test
  void testProcess_zeroInterval_writesComment() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetCPUTime().process(request(0, -1L, new AtomicReference<>(ContentStatus.OK)), xml);
    assertTrue(xml.toString().contains("<!--"), "Should write comment on bad interval");
  }

  @Test
  void testProcess_allThreads_writesSampleElement() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    // interval=1ms, all threads (thread=-1)
    new GetCPUTime().process(request(1, -1L, status), xml);

    assertEquals(ContentStatus.OK, status.get(), "Valid call should not set error status");
    Document doc = parse(xml.toString());
    Element sample = doc.getDocumentElement();
    assertEquals("sample", sample.getTagName());
    assertEquals("1", sample.getAttribute("interval"));
    assertFalse(sample.getAttribute("cpu").isEmpty());
    assertFalse(sample.getAttribute("user").isEmpty());
    assertFalse(sample.getAttribute("system").isEmpty());
  }

  @Test
  void testProcess_singleThread_writesSampleElement() throws Exception {
    long currentId = Thread.currentThread().getId();
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetCPUTime().process(request(1, currentId, status), xml);

    assertEquals(ContentStatus.OK, status.get());
    assertTrue(xml.toString().contains("<sample"), "Should write <sample> element");
  }

  @Test
  void testProcess_cpuAttributeIsNumeric() throws Exception {
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetCPUTime().process(request(1, -1L, new AtomicReference<>(ContentStatus.OK)), xml);
    Document doc = parse(xml.toString());
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("cpu")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("user")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("system")));
  }

  private static ContentRequest request(int interval, long threadId,
      AtomicReference<ContentStatus> statusRef) {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getIntParameter":  return "interval".equals(args[0]) ? interval : (Integer) args[1];
            case "getLongParameter": return "thread".equals(args[0]) ? threadId : (Long) args[1];
            case "setStatus":        statusRef.set((ContentStatus) args[0]); return null;
            default:                 return defaultValue(m.getReturnType());
          }
        });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (boolean.class.equals(type)) return false;
    if (int.class.equals(type))     return 0;
    if (long.class.equals(type))    return 0L;
    return null;
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
