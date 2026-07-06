package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.error.InvalidParameterException;
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

class GetThreadInfoTest {

  @Test
  void testProcess_negativeId_throwsInvalidParameterException() {
    OutputWriter out = new XmlOutputAdapter();
    Generator generator = new GetThreadInfo();
    Request req = request(-1L);
    InvalidParameterException ex = assertThrows(InvalidParameterException.class,
        () -> generator.generate(req, out));
    assertEquals("id", ex.getParameterName());
    assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
    assertEquals(400, ex.toProblem().status());
  }

  @Test
  void testProcess_negativeId_writesNothingToOutput() {
    OutputWriter out = new XmlOutputAdapter();
    Generator generator = new GetThreadInfo();
    Request req = request(-1L);
    assertThrows(InvalidParameterException.class, () -> generator.generate(req, out));
    assertEquals("", out.toString(), "Generator should not write body content when the parameter is rejected");
  }

  @Test
  void testProcess_omittedId_defaultsToCurrentThread() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    Response response = new GetThreadInfo().generate(request(null), out);

    assertEquals(ContentStatus.OK, response.status());
    Document doc = parse(out.toString());
    Element root = doc.getDocumentElement();
    assertEquals("thread", root.getTagName());
    assertEquals(String.valueOf(Thread.currentThread().getId()), root.getAttribute("id"));
  }

  @Test
  void testProcess_explicitCurrentThreadId_returnsThatThread() throws Exception {
    long currentId = Thread.currentThread().getId();
    OutputWriter out = new XmlOutputAdapter();
    Response response = new GetThreadInfo().generate(request(currentId), out);

    assertEquals(ContentStatus.OK, response.status(), "Valid ID should not set error status");
    assertFalse(response.isProblem());
    Document doc = parse(out.toString());
    Element root = doc.getDocumentElement();
    assertEquals("thread", root.getTagName(), "Should write <thread> element");
    assertEquals(String.valueOf(currentId), root.getAttribute("id"));
    assertFalse(root.getAttribute("name").isEmpty());
    assertFalse(root.getAttribute("state").isEmpty());
  }

  @Test
  void testProcess_explicitCurrentThreadId_includesStacktrace() {
    OutputWriter out = new XmlOutputAdapter();
    new GetThreadInfo().generate(request(Thread.currentThread().getId()), out);

    assertTrue(out.toString().contains("<stacktrace"), "Should include stacktrace");
  }

  @Test
  void testProcess_nonExistentId_returnsNoThread() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    Response response = new GetThreadInfo().generate(request(Long.MAX_VALUE), out);

    assertEquals(ContentStatus.OK, response.status());
    Document doc = parse(out.toString());
    Element root = doc.getDocumentElement();
    assertEquals("no-thread", root.getTagName());
    assertEquals(String.valueOf(Long.MAX_VALUE), root.getAttribute("id"));
  }

  @Test
  void testProcess_json_writesThreadObject() {
    OutputWriter out = new JsonOutputAdapter();
    Response response = new GetThreadInfo().generate(request(Thread.currentThread().getId()), out);

    assertEquals(ContentStatus.OK, response.status());
    String json = out.toString();
    assertTrue(json.contains("\"name\":"), json);
    assertTrue(json.contains("\"state\":"), json);
    assertTrue(json.contains("\"stacktrace\":["), json);
  }

  private static Request request(Long threadId) {
    String value = threadId != null ? String.valueOf(threadId) : null;
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getParameter":
              return "id".equals(args[0]) ? value : (args.length > 1 ? (String) args[1] : null);
            case "parameter":
              String name = (String) args[0];
              if ("id".equals(name)) return new ParameterBuilder(name, value);
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
