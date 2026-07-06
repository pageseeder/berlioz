package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.ParameterBuilder;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
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

class GetCPUTimeTest {

  // Out-of-range `interval` is a flow-control signal: TypedParameter#optional() throws
  // InvalidParameterException, which the servlet dispatch layer (GeneratorDispatch) catches
  // and maps to a 400 Bad Request problem response. Calling generate() directly, as these
  // tests do, bypasses that layer, so the exception itself is what we can observe here.

  @Test
  void testProcessZeroIntervalThrowsInvalidParameterException() {
    OutputWriter out = new XmlOutputAdapter();
    InvalidParameterException ex = assertThrows(InvalidParameterException.class,
        () -> new GetCPUTime().generate(request(0, -1L), out));
    assertEquals("interval", ex.getParameterName());
    assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
    assertEquals(400, ex.toProblem().status());
  }

  @Test
  void testProcessNegativeIntervalThrowsInvalidParameterException() {
    OutputWriter out = new XmlOutputAdapter();
    InvalidParameterException ex = assertThrows(InvalidParameterException.class,
        () -> new GetCPUTime().generate(request(-5, -1L), out));
    assertEquals("interval", ex.getParameterName());
    assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
  }

  @Test
  void testProcessZeroIntervalWritesNothingToOutput() {
    OutputWriter out = new XmlOutputAdapter();
    assertThrows(InvalidParameterException.class, () -> new GetCPUTime().generate(request(0, -1L), out));
    assertEquals("", out.toString(), "Generator should not write body content when the parameter is rejected");
  }

  // process() tests — XML
  // ---------------------------------------------------------------------------

  @Test
  void testProcessAllThreadsWritesSampleElement() throws Exception {
    OutputWriter out = new XmlOutputAdapter();

    // interval=1ms, all threads (thread=-1)
    Response response = new GetCPUTime().generate(request(1, -1L), out);

    assertEquals(ContentStatus.OK, response.status(), "Valid call should not set error status");
    assertFalse(response.isProblem());
    Document doc = parseXml(out.toString());
    Element sample = doc.getDocumentElement();
    assertEquals("sample", sample.getTagName());
    assertEquals("1", sample.getAttribute("interval"));
    assertFalse(sample.getAttribute("cpu").isEmpty());
    assertFalse(sample.getAttribute("user").isEmpty());
    assertFalse(sample.getAttribute("system").isEmpty());
  }

  @Test
  void testProcessSingleThreadWritesSampleElement() {
    long currentId = Thread.currentThread().getId();
    OutputWriter out = new XmlOutputAdapter();

    Response response = new GetCPUTime().generate(request(1, currentId), out);

    assertEquals(ContentStatus.OK, response.status());
    assertTrue(out.toString().contains("<sample"), "Should write <sample> element");
  }

  @Test
  void testProcessCpuAttributeIsNumeric() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new GetCPUTime().generate(request(1, -1L), out);
    Document doc = parseXml(out.toString());
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("cpu")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("user")));
    assertDoesNotThrow(() -> Long.parseLong(doc.getDocumentElement().getAttribute("system")));
  }

  // process() tests — JSON
  // ---------------------------------------------------------------------------

  @Test
  void testProcessJsonAllThreadsWritesSampleObject() {
    OutputWriter out = new JsonOutputAdapter();

    Response response = new GetCPUTime().generate(request(1, -1L), out);

    assertEquals(ContentStatus.OK, response.status());
    String json = out.toString();
    assertTrue(json.startsWith("{\"interval\":1,"), "Should include interval property");
    assertTrue(json.contains("\"cpu\":"));
    assertTrue(json.contains("\"user\":"));
    assertTrue(json.contains("\"system\":"));
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

  private static Document parseXml(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(xml)));
  }
}
