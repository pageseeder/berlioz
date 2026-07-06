package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;

/**
 * Golden-value tests for {@link GetParameters}.
 *
 * <p>Assertions on {@link #process(ContentRequest)} and {@link #processJson(ContentRequest)}
 * use exact string equality rather than {@code contains}, so that any change to the XML shape
 * (backward compatibility) or the JSON shape must be a deliberate, visible edit to this file
 * rather than a side effect that slips through a looser check.
 */
class GetParametersTest {

  // process() tests — XML
  // ---------------------------------------------------------------------------

  @Test
  void testProcessNoParametersWritesEmptyElement() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals("<parameters/>", out);
  }

  @Test
  void testProcessSingleParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("name", "Alice")
        .build();
    String out = process(req);
    Assertions.assertEquals("<parameters><parameter name=\"name\">Alice</parameter></parameters>", out);
  }

  @Test
  void testProcessMultipleParameters() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("a", "1")
        .parameter("b", "2")
        .build();
    String out = process(req);
    Assertions.assertEquals(
        "<parameters><parameter name=\"a\">1</parameter><parameter name=\"b\">2</parameter></parameters>", out);
  }

  @Test
  void testProcessMultiValueParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("color", "red", "blue", "green")
        .build();
    String out = process(req);
    Assertions.assertEquals("<parameters>"
        + "<parameter name=\"color\">red</parameter>"
        + "<parameter name=\"color\">blue</parameter>"
        + "<parameter name=\"color\">green</parameter>"
        + "</parameters>", out);
  }

  @Test
  void testProcessNameOverMaxLengthIsSkipped() {
    String longName = "n".repeat(101);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter(longName, "v")
        .parameter("ok", "1")
        .build();
    String out = process(req);
    Assertions.assertEquals("<parameters><parameter name=\"ok\">1</parameter></parameters>", out);
  }

  @Test
  void testProcessValueOverMaxLengthIsTruncated() {
    String longValue = "x".repeat(2_010);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("big", longValue)
        .build();
    String out = process(req);
    String truncated = "x".repeat(2_000);
    Assertions.assertEquals(
        "<parameters><parameter name=\"big\" truncated=\"true\">" + truncated + "</parameter></parameters>", out);
  }

  @Test
  void testProcessValueAtMaxLengthIsNotTruncated() {
    String value = "y".repeat(2_000);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("exact", value)
        .build();
    String out = process(req);
    Assertions.assertFalse(out.contains("truncated"), "Value at exactly the limit should not be truncated");
  }

  @Test
  void testProcessOverMaxParametersIsCapped() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    for (int i = 0; i < 55; i++) builder.parameter("p" + i, "v" + i);
    ContentRequest req = builder.build();
    String out = process(req);
    Assertions.assertTrue(out.contains("name=\"p49\""), "50th parameter should be included");
    Assertions.assertFalse(out.contains("name=\"p50\""), "51st parameter should be dropped");
  }

  @Test
  void testProcessOverMaxValuesIsCapped() {
    String[] values = new String[25];
    for (int i = 0; i < values.length; i++) values[i] = "v" + i;
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("multi", values)
        .build();
    String out = process(req);
    Assertions.assertTrue(out.contains(">v19<"), "20th value should be included");
    Assertions.assertFalse(out.contains(">v20<"), "21st value should be dropped");
  }

  // process() tests — JSON
  // ---------------------------------------------------------------------------

  @Test
  void testProcessJsonNoParametersWritesEmptyArray() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = processJson(req);
    Assertions.assertEquals("{\"parameters\":[]}", out);
  }

  @Test
  void testProcessJsonSingleParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("name", "Alice")
        .build();
    String out = processJson(req);
    Assertions.assertEquals("{\"parameters\":[{\"name\":\"name\",\"value\":\"Alice\"}]}", out);
  }

  @Test
  void testProcessJsonMultipleParameters() {
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("a", "1")
        .parameter("b", "2")
        .build();
    String out = processJson(req);
    Assertions.assertEquals(
        "{\"parameters\":[{\"name\":\"a\",\"value\":\"1\"},{\"name\":\"b\",\"value\":\"2\"}]}", out);
  }

  @Test
  void testProcessJsonMultiValueParameter() {
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("color", "red", "blue", "green")
        .build();
    String out = processJson(req);
    Assertions.assertEquals("{\"parameters\":["
        + "{\"name\":\"color\",\"value\":\"red\"},"
        + "{\"name\":\"color\",\"value\":\"blue\"},"
        + "{\"name\":\"color\",\"value\":\"green\"}"
        + "]}", out);
  }

  @Test
  void testProcessJsonNameOverMaxLengthIsSkipped() {
    String longName = "n".repeat(101);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter(longName, "v")
        .parameter("ok", "1")
        .build();
    String out = processJson(req);
    Assertions.assertEquals("{\"parameters\":[{\"name\":\"ok\",\"value\":\"1\"}]}", out);
  }

  @Test
  void testProcessJsonValueOverMaxLengthIsTruncated() {
    String longValue = "x".repeat(2_010);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("big", longValue)
        .build();
    String out = processJson(req);
    String truncated = "x".repeat(2_000);
    Assertions.assertEquals(
        "{\"parameters\":[{\"name\":\"big\",\"truncated\":true,\"value\":\"" + truncated + "\"}]}", out);
  }

  @Test
  void testProcessJsonValueAtMaxLengthIsNotTruncated() {
    String value = "y".repeat(2_000);
    ContentRequest req = GeneratorTestSupport.request()
        .parameter("exact", value)
        .build();
    String out = processJson(req);
    Assertions.assertFalse(out.contains("truncated"), "Value at exactly the limit should not be truncated");
  }

  @Test
  void testProcessJsonOverMaxParametersIsCapped() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    for (int i = 0; i < 55; i++) builder.parameter("p" + i, "v" + i);
    ContentRequest req = builder.build();
    String out = processJson(req);
    Assertions.assertTrue(out.contains("\"name\":\"p49\""), "50th parameter should be included");
    Assertions.assertFalse(out.contains("\"name\":\"p50\""), "51st parameter should be dropped");
  }

  @Test
  void testProcessJsonOverMaxValuesIsCapped() {
    String[] values = new String[25];
    for (int i = 0; i < values.length; i++) values[i] = "v" + i;
    ContentRequest req = GeneratorTestSupport.request()
        .multiParameter("multi", values)
        .build();
    String out = processJson(req);
    Assertions.assertTrue(out.contains("\"value\":\"v19\""), "20th value should be included");
    Assertions.assertFalse(out.contains("\"value\":\"v20\""), "21st value should be dropped");
  }

  // getETag() tests
  // ---------------------------------------------------------------------------

  @Test
  void testETagWithNoParametersIsStable() {
    GetParameters gen = new GetParameters();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag((Request) req);
    String etag2 = gen.getETag((Request) req);
    Assertions.assertEquals(etag1, etag2, "ETag should be stable for same request");
    Assertions.assertNotNull(etag1);
    Assertions.assertFalse(etag1.isEmpty());
  }

  @Test
  void testETagDiffersWithDifferentParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest empty  = GeneratorTestSupport.request().build();
    ContentRequest withParam = GeneratorTestSupport.request().parameter("q", "test").build();
    Assertions.assertNotEquals(gen.getETag((Request) empty), gen.getETag((Request) withParam));
  }

  @Test
  void testETagSameForIdenticalParameters() {
    GetParameters gen = new GetParameters();
    ContentRequest req1 = GeneratorTestSupport.request().parameter("q", "foo").build();
    ContentRequest req2 = GeneratorTestSupport.request().parameter("q", "foo").build();
    Assertions.assertEquals(gen.getETag((Request) req1), gen.getETag((Request) req2));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetParameters gen = new GetParameters();
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }

  private static String processJson(ContentRequest req) {
    GetParameters gen = new GetParameters();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }
}
