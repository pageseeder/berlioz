package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.ParameterBuilder;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class GetThreadInfoTest {

  @Test
  void testProcess_negativeId_setsBadRequest() {
    Request req = request(-1L);
    XmlStringBuilder xml = new XmlStringBuilder();

    Response response = new GetThreadInfo().generate(req, xml);

    assertEquals(ContentStatus.BAD_REQUEST, response.status());
  }

  @Test
  void testProcess_negativeId_writesComment() {
    Request req = request(-1L);
    XmlStringBuilder xml = new XmlStringBuilder();

    new GetThreadInfo().generate(req, xml);

    String out = xml.toString();
    assertTrue(out.contains("<!--"), "Should write XML comment for invalid ID");
  }

  @Test
  void testProcess_validId_writesThreadElement() throws Exception {
    // Any non-negative value triggers current-thread lookup
    Request req = request(0L);
    XmlStringBuilder xml = new XmlStringBuilder();

    Response response = new GetThreadInfo().generate(req, xml);

    assertEquals(ContentStatus.OK, response.status(), "Valid ID should not set error status");
    Document doc = parse(xml.toString());
    Element root = doc.getDocumentElement();
    assertEquals("thread", root.getTagName(), "Should write <thread> element");
    assertFalse(root.getAttribute("id").isEmpty());
    assertFalse(root.getAttribute("name").isEmpty());
    assertFalse(root.getAttribute("state").isEmpty());
  }

  @Test
  void testProcess_validId_includesStacktrace() {
    Request req = request(1L);
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetThreadInfo().generate(req, xml);

    assertTrue(xml.toString().contains("<stacktrace"), "Should include stacktrace");
  }

  private static Request request(long threadId) {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getParameter":
              return "id".equals(args[0]) ? String.valueOf(threadId) : (args.length > 1 ? (String) args[1] : null);
            case "parameter":
              String name = (String) args[0];
              if ("id".equals(name)) return new ParameterBuilder(name, String.valueOf(threadId));
              return new ParameterBuilder(name, null);
            default:
              return null;
          }
        });
  }

  private static Document parse(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
