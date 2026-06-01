package org.pageseeder.berlioz.aeson;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public class JSONWriterFactoryTest {

  // ---------------------------------------------------------------------------
  // Functional correctness — the produced writer must emit valid JSON
  // ---------------------------------------------------------------------------

  @Test
  public void testNewInstanceWriterProducesValidJSON() {
    StringWriter sw = new StringWriter();
    try (JSONWriter writer = JSONWriterFactory.newInstance(sw)) {
      writer.startObject()
            .property("key", "value")
            .property("count", 42L)
            .property("ratio", 1.5)
            .property("flag", true)
            .writeNull("missing")
            .end();
    }
    String json = sw.toString();
    Assertions.assertTrue(json.startsWith("{"));
    Assertions.assertTrue(json.endsWith("}"));
    Assertions.assertTrue(json.contains("\"key\":\"value\""));
    Assertions.assertTrue(json.contains("\"count\":42"));
    Assertions.assertTrue(json.contains("\"flag\":true"));
    Assertions.assertTrue(json.contains("\"missing\":null"));
  }

  @Test
  public void testNewInstanceStreamProducesValidJSON() throws Exception {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (JSONWriter writer = JSONWriterFactory.newInstance(out)) {
      writer.startArray()
            .value("a")
            .value(1L)
            .value(true)
            .writeNull()
            .end();
    }
    String json = out.toString("UTF-8");
    Assertions.assertTrue(json.startsWith("["));
    Assertions.assertTrue(json.endsWith("]"));
    Assertions.assertTrue(json.contains("\"a\""));
    Assertions.assertTrue(json.contains("true"));
    Assertions.assertTrue(json.contains("null"));
  }

  @Test
  public void testNestedStructure() {
    StringWriter sw = new StringWriter();
    try (JSONWriter writer = JSONWriterFactory.newInstance(sw)) {
      writer.startObject()
              .startArray("items")
                .startObject().property("id", 1L).end()
                .startObject().property("id", 2L).end()
              .end()
            .end();
    }
    String json = sw.toString();
    Assertions.assertTrue(json.contains("\"items\""));
    Assertions.assertTrue(json.contains("\"id\":1"));
    Assertions.assertTrue(json.contains("\"id\":2"));
  }

  @Test
  public void testValueNaNThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> JSONWriterFactory.newInstance(new StringWriter()).value(Double.NaN));
  }

  @Test
  public void testPropertyInfinityThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> JSONWriterFactory.newInstance(new StringWriter()).startObject().property("x", Double.POSITIVE_INFINITY));
  }

  // ---------------------------------------------------------------------------
  // Robustness — init() must be idempotent and concurrent-safe
  // ---------------------------------------------------------------------------

  @Test
  public void testInitIsIdempotent() {
    // Calling init() multiple times must not throw or corrupt state
    JSONWriterFactory.init();
    JSONWriterFactory.init();
    JSONWriterFactory.init();

    // After repeated init, the factory must still produce a working writer
    StringWriter sw = new StringWriter();
    try (JSONWriter writer = JSONWriterFactory.newInstance(sw)) {
      writer.startObject().property("ok", true).end();
    }
    Assertions.assertTrue(sw.toString().contains("true"));
  }

  @Test
  public void testConcurrentNewInstance() throws InterruptedException {
    // Multiple threads obtaining writers must not race or corrupt each other's output
    int threads = 8;
    Thread[] workers = new Thread[threads];
    String[] results = new String[threads];
    for (int i = 0; i < threads; i++) {
      final int idx = i;
      workers[i] = new Thread(() -> {
        StringWriter sw = new StringWriter();
        try (JSONWriter writer = JSONWriterFactory.newInstance(sw)) {
          writer.startObject().property("thread", (long) idx).end();
        }
        results[idx] = sw.toString();
      });
    }
    for (Thread t : workers) t.start();
    for (Thread t : workers) t.join();

    for (int i = 0; i < threads; i++) {
      Assertions.assertNotNull(results[i], "Thread " + i + " produced no output");
      Assertions.assertTrue(results[i].contains("\"thread\":" + i), "Thread " + i + " produced invalid JSON");
    }
  }

}
