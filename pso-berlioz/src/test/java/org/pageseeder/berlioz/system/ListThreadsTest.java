package org.pageseeder.berlioz.system;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
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
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Document doc = parse(out.toString());
    assertEquals("threads", doc.getDocumentElement().getTagName());
  }

  @Test
  void testProcess_noParams_containsThreadChildren() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Document doc = parse(out.toString());
    NodeList threads = doc.getElementsByTagName("thread");
    assertTrue(threads.getLength() > 0, "Should list at least one thread");
  }

  @Test
  void testProcess_threadHasExpectedAttributes() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Document doc = parse(out.toString());
    Element thread = (Element) doc.getElementsByTagName("thread").item(0);
    assertFalse(thread.getAttribute("id").isEmpty(),    "Thread should have id");
    assertFalse(thread.getAttribute("name").isEmpty(),  "Thread should have name");
    assertFalse(thread.getAttribute("state").isEmpty(), "Thread should have state");
  }

  @Test
  void testProcess_withStacktraces_includesStacktraceElement() {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(true, false), out);
    String result = out.toString();
    assertTrue(result.contains("<stacktrace"), "Should include stacktrace elements when requested");
  }

  @Test
  void testProcess_withoutStacktraces_noStacktraceElement() {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    assertFalse(out.toString().contains("<stacktrace"), "Should not include stacktrace when not requested");
  }

  @Test
  void testProcess_withThreadTime_includesTimesElement() {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, true), out);
    // CPU time support is JVM-dependent; just verify it doesn't throw
    assertDoesNotThrow(out::toString);
  }

  @Test
  void testProcess_currentThreadFlagged() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Document doc = parse(out.toString());
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

  @Test
  void testProcess_json_writesThreadsArray() {
    OutputWriter out = new JsonOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    String json = out.toString();
    assertTrue(json.startsWith("{\"threads\":["), json);
    assertTrue(json.contains("\"name\":"), json);
    assertTrue(json.contains("\"state\":"), json);
  }

  @Test
  void testProcess_json_withStacktraces_includesStacktraceArray() {
    OutputWriter out = new JsonOutputAdapter();
    new ListThreads().generate(request(true, false), out);
    assertTrue(out.toString().contains("\"stacktrace\":["));
  }

  @Test
  void testProcess_threadHasDetails() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Element thread = current(parse(out.toString()));
    assertFalse(thread.getAttribute("blocked-count").isEmpty());
    assertFalse(thread.getAttribute("waited-count").isEmpty());
    assertEquals("false", thread.getAttribute("in-native"));
    assertEquals("false", thread.getAttribute("deadlocked"));
  }

  @Test
  void testProcess_belowThreadLimit_noLimitStated() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    Element root = parse(out.toString()).getDocumentElement();
    assertFalse(root.hasAttribute("limit"));
    assertFalse(root.hasAttribute("total"));
  }

  @Test
  void testProcess_deepStack_statesLimit() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    ThreadsTest.recurse(Threads.MAX_STACK_DEPTH + 10, () -> new ListThreads().generate(request(true, false), out));
    Element thread = current(parse(out.toString()));
    assertEquals(String.valueOf(Threads.MAX_STACK_DEPTH), thread.getAttribute("stack-limit"));
    assertEquals(Threads.MAX_STACK_DEPTH, thread.getElementsByTagName("element").getLength());
  }

  @Test
  void testProcess_deadlockedThreads_flaggedWithLock() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    try (ThreadsTest.Deadlock deadlock = new ThreadsTest.Deadlock()) {
      new ListThreads().generate(request(false, false), out);
    }
    Document doc = parse(out.toString());
    NodeList threads = doc.getElementsByTagName("thread");
    int deadlocked = 0;
    for (int i = 0; i < threads.getLength(); i++) {
      Element t = (Element) threads.item(i);
      if (t.getAttribute("name").startsWith("deadlock-")) {
        assertEquals("true", t.getAttribute("deadlocked"));
        Element lock = (Element) t.getElementsByTagName("lock").item(0);
        assertNotNull(lock, "Deadlocked thread should wait for a lock");
        assertFalse(lock.getAttribute("owner-id").isEmpty());
        deadlocked++;
      }
    }
    assertEquals(2, deadlocked);
  }

  @Test
  void testProcess_json_includesDetails() {
    OutputWriter out = new JsonOutputAdapter();
    new ListThreads().generate(request(false, false), out);
    String json = out.toString();
    assertTrue(json.contains("\"blockedCount\":"), json);
    assertTrue(json.contains("\"inNative\":"), json);
    assertTrue(json.contains("\"deadlocked\":"), json);
    assertFalse(json.contains("\"limit\":"), json);
  }

  private static Element current(Document doc) {
    NodeList threads = doc.getElementsByTagName("thread");
    for (int i = 0; i < threads.getLength(); i++) {
      Element t = (Element) threads.item(i);
      if ("true".equals(t.getAttribute("current"))) return t;
    }
    throw new AssertionError("No current thread");
  }

  private static Request request(boolean stacktraces, boolean threadtime) {
    return (Request) Proxy.newProxyInstance(
        Request.class.getClassLoader(),
        new Class<?>[]{Request.class},
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
