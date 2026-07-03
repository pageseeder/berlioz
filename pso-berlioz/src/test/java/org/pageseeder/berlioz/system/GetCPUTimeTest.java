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

class GetCPUTimeTest {

  @Test
  void testProcess_zeroInterval_setsBadRequest() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Response response = new GetCPUTime().generate(request(0, -1L), xml);
    assertEquals(ContentStatus.BAD_REQUEST, response.status());
  }

  @Test
  void testProcess_negativeInterval_setsBadRequest() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Response response = new GetCPUTime().generate(request(-5, -1L), xml);
    assertEquals(ContentStatus.BAD_REQUEST, response.status());
  }

  @Test
  void testProcess_zeroInterval_writesComment() {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetCPUTime().generate(request(0, -1L), xml);
    assertTrue(xml.toString().contains("<!--"), "Should write comment on bad interval");
  }

  @Test
  void testProcess_allThreads_writesSampleElement() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();

    // interval=1ms, all threads (thread=-1)
    Response response = new GetCPUTime().generate(request(1, -1L), xml);

    assertEquals(ContentStatus.OK, response.status(), "Valid call should not set error status");
    Document doc = parse(xml.toString());
    Element sample = doc.getDocumentElement();
    assertEquals("sample", sample.getTagName());
    assertEquals("1", sample.getAttribute("interval"));
    assertFalse(sample.getAttribute("cpu").isEmpty());
    assertFalse(sample.getAttribute("user").isEmpty());
    assertFalse(sample.getAttribute("system").isEmpty());
  }

  @Test
  void testProcess_singleThread_writesSampleElement() {
    long currentId = Thread.currentThread().getId();
    XmlStringBuilder xml = new XmlStringBuilder();

    Response response = new GetCPUTime().generate(request(1, currentId), xml);

    assertEquals(ContentStatus.OK, response.status());
    assertTrue(xml.toString().contains("<sample"), "Should write <sample> element");
  }

  @Test
  void testProcess_cpuAttributeIsNumeric() throws Exception {
    XmlStringBuilder xml = new XmlStringBuilder();
    new GetCPUTime().generate(request(1, -1L), xml);
    Document doc = parse(xml.toString());
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("cpu")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("user")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("system")));
  }

  private static Request request(int interval, long threadId) {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
        (proxy, m, args) -> {
          switch (m.getName()) {
            case "getParameter":
              String name = (String) args[0];
              if ("interval".equals(name)) return String.valueOf(interval);
              if ("thread".equals(name)) return String.valueOf(threadId);
              return args.length > 1 ? (String) args[1] : null;
            case "parameter":
              String pname = (String) args[0];
              if ("interval".equals(pname)) return new ParameterBuilder(pname, String.valueOf(interval));
              if ("thread".equals(pname)) return new ParameterBuilder(pname, String.valueOf(threadId));
              return new ParameterBuilder(pname, null);
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
