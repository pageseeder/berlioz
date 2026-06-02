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

class GetThreadInfoTest {

  @Test
  void testProcess_negativeId_setsBadRequest() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    ContentRequest req = request(-1L, status);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetThreadInfo().process(req, xml);

    assertEquals(ContentStatus.BAD_REQUEST, status.get());
  }

  @Test
  void testProcess_negativeId_writesComment() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    ContentRequest req = request(-1L, status);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetThreadInfo().process(req, xml);

    // Output should be a comment (no element written)
    String out = xml.toString();
    assertTrue(out.contains("<!--"), "Should write XML comment for invalid ID");
  }

  @Test
  void testProcess_validId_writesThreadElement() throws Exception {
    AtomicReference<ContentStatus> status = new AtomicReference<>(ContentStatus.OK);
    // Any non-negative value triggers current-thread lookup
    ContentRequest req = request(0L, status);
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    new GetThreadInfo().process(req, xml);

    assertEquals(ContentStatus.OK, status.get(), "Valid ID should not set error status");
    Document doc = parse(xml.toString());
    Element root = doc.getDocumentElement();
    assertEquals("thread", root.getTagName(), "Should write <thread> element");
    assertFalse(root.getAttribute("id").isEmpty());
    assertFalse(root.getAttribute("name").isEmpty());
    assertFalse(root.getAttribute("state").isEmpty());
  }

  @Test
  void testProcess_validId_includesStacktrace() throws Exception {
    ContentRequest req = request(1L, new AtomicReference<>(ContentStatus.OK));
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    new GetThreadInfo().process(req, xml);

    assertTrue(xml.toString().contains("<stacktrace"), "Should include stacktrace");
  }

  private static ContentRequest request(long threadId, AtomicReference<ContentStatus> statusRef) {
    return (ContentRequest) Proxy.newProxyInstance(
        ContentRequest.class.getClassLoader(),
        new Class<?>[]{ContentRequest.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getLongParameter": return "id".equals(args[0]) ? threadId : (Long) args[1];
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
