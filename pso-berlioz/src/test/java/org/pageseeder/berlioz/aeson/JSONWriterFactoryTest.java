package org.pageseeder.berlioz.aeson;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertTrue(json.startsWith("{"));
    Assert.assertTrue(json.endsWith("}"));
    Assert.assertTrue(json.contains("\"key\":\"value\""));
    Assert.assertTrue(json.contains("\"count\":42"));
    Assert.assertTrue(json.contains("\"flag\":true"));
    Assert.assertTrue(json.contains("\"missing\":null"));
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
    Assert.assertTrue(json.startsWith("["));
    Assert.assertTrue(json.endsWith("]"));
    Assert.assertTrue(json.contains("\"a\""));
    Assert.assertTrue(json.contains("true"));
    Assert.assertTrue(json.contains("null"));
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
    Assert.assertTrue(json.contains("\"items\""));
    Assert.assertTrue(json.contains("\"id\":1"));
    Assert.assertTrue(json.contains("\"id\":2"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueNaNThrows() {
    JSONWriterFactory.newInstance(new StringWriter()).value(Double.NaN);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyInfinityThrows() {
    JSONWriterFactory.newInstance(new StringWriter()).startObject().property("x", Double.POSITIVE_INFINITY);
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
    Assert.assertTrue(sw.toString().contains("true"));
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
      Assert.assertNotNull("Thread " + i + " produced no output", results[i]);
      Assert.assertTrue("Thread " + i + " produced invalid JSON",
          results[i].contains("\"thread\":" + i));
    }
  }

}
