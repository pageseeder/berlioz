package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.ParameterBuilder;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.ProblemDetails;
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

  @Test
  void testProcessZeroIntervalReturnsBadRequestProblem() {
    OutputWriter out = new XmlOutputAdapter();
    Response response = new GetCPUTime().generate(request(0, -1L), out);
    assertTrue(response.isProblem());
    assertEquals(ContentStatus.BAD_REQUEST, response.status());
    ProblemDetails problem = response.problem();
    assertNotNull(problem);
    assertEquals(400, problem.status());
  }

  @Test
  void testProcessNegativeIntervalReturnsBadRequestProblem() {
    OutputWriter out = new XmlOutputAdapter();
    Response response = new GetCPUTime().generate(request(-5, -1L), out);
    assertTrue(response.isProblem());
    assertEquals(ContentStatus.BAD_REQUEST, response.status());
  }

  @Test
  void testProcessZeroIntervalWritesNothingToOutput() {
    OutputWriter out = new XmlOutputAdapter();
    new GetCPUTime().generate(request(0, -1L), out);
    assertEquals("", out.toString(), "Generator should not write body content on a problem response");
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
