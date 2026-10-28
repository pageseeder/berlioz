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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

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

  @Test
  void testProcess_writesContentionAndNativeDetails() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new GetThreadInfo().generate(request(null), out);

    Element root = parse(out.toString()).getDocumentElement();
    assertFalse(root.getAttribute("blocked-count").isEmpty());
    assertFalse(root.getAttribute("waited-count").isEmpty());
    assertEquals("false", root.getAttribute("in-native"));
    assertEquals("false", root.getAttribute("deadlocked"));
    assertFalse(root.hasAttribute("stack-limit"), "A shallow stack should not state a limit");
  }

  @Test
  void testProcess_shallowStack_writesEmptyLockArrays() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    new GetThreadInfo().generate(request(null), out);

    Document doc = parse(out.toString());
    assertEquals(1, doc.getElementsByTagName("locked-monitors").getLength());
    assertEquals(1, doc.getElementsByTagName("locked-synchronizers").getLength());
    assertEquals(0, doc.getElementsByTagName("monitor").getLength());
    assertEquals(0, doc.getElementsByTagName("lock").getLength(), "A running thread waits for no lock");
  }

  @Test
  void testProcess_deepStack_statesLimit() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    ThreadsTest.recurse(Threads.MAX_STACK_DEPTH + 10, () -> new GetThreadInfo().generate(request(null), out));

    Document doc = parse(out.toString());
    assertEquals(String.valueOf(Threads.MAX_STACK_DEPTH), doc.getDocumentElement().getAttribute("stack-limit"));
    assertEquals(Threads.MAX_STACK_DEPTH, doc.getElementsByTagName("element").getLength());
  }

  @Test
  void testProcess_heldMonitor_listedWithDepth() throws Exception {
    Object monitor = new Object();
    OutputWriter out = new XmlOutputAdapter();
    synchronized (monitor) {
      new GetThreadInfo().generate(request(null), out);
    }

    Element held = (Element) parse(out.toString()).getElementsByTagName("monitor").item(0);
    assertNotNull(held, out.toString());
    assertEquals("java.lang.Object", held.getAttribute("class"));
    assertEquals(Integer.toHexString(System.identityHashCode(monitor)), held.getAttribute("identity-hash"));
    assertFalse(held.getAttribute("depth").isEmpty());
  }

  @Test
  void testProcess_heldSynchronizer_listed() throws Exception {
    ReentrantLock lock = new ReentrantLock();
    OutputWriter out = new XmlOutputAdapter();
    lock.lock();
    try {
      new GetThreadInfo().generate(request(null), out);
    } finally {
      lock.unlock();
    }

    Element held = (Element) parse(out.toString()).getElementsByTagName("synchronizer").item(0);
    assertNotNull(held, out.toString());
    assertTrue(held.getAttribute("class").startsWith("java.util.concurrent.locks.ReentrantLock"));
  }

  @Test
  void testProcess_blockedThread_writesLockWithOwner() throws Exception {
    Object monitor = new Object();
    Thread blocked = new Thread(() -> {
      synchronized (monitor) {
        monitor.hashCode();
      }
    }, "blocked-on-monitor");
    blocked.setDaemon(true);
    OutputWriter out = new XmlOutputAdapter();
    synchronized (monitor) {
      blocked.start();
      long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
      while (blocked.getState() != Thread.State.BLOCKED && System.nanoTime() < deadline) {
        Thread.sleep(10);
      }
      new GetThreadInfo().generate(request(blocked.getId()), out);
    }
    blocked.join(5000);

    Element lock = (Element) parse(out.toString()).getElementsByTagName("lock").item(0);
    assertNotNull(lock, out.toString());
    assertEquals("java.lang.Object", lock.getAttribute("class"));
    assertEquals(String.valueOf(Thread.currentThread().getId()), lock.getAttribute("owner-id"));
    assertEquals(Thread.currentThread().getName(), lock.getAttribute("owner-name"));
  }

  @Test
  void testProcess_deadlockedThread_flagged() throws Exception {
    OutputWriter out = new XmlOutputAdapter();
    try (ThreadsTest.Deadlock deadlock = new ThreadsTest.Deadlock()) {
      new GetThreadInfo().generate(request(deadlock.first.getId()), out);
    }

    Element root = parse(out.toString()).getDocumentElement();
    assertEquals("true", root.getAttribute("deadlocked"), out.toString());
    Element lock = (Element) root.getElementsByTagName("lock").item(0);
    assertNotNull(lock);
    assertEquals("deadlock-2", lock.getAttribute("owner-name"));
  }

  @Test
  void testProcess_json_writesDetailsAndLockArrays() {
    OutputWriter out = new JsonOutputAdapter();
    new GetThreadInfo().generate(request(null), out);

    String json = out.toString();
    assertTrue(json.contains("\"blockedCount\":"), json);
    assertTrue(json.contains("\"waitedCount\":"), json);
    assertTrue(json.contains("\"inNative\":false"), json);
    assertTrue(json.contains("\"deadlocked\":false"), json);
    assertTrue(json.contains("\"lockedMonitors\":["), json);
    assertTrue(json.contains("\"lockedSynchronizers\":["), json);
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
