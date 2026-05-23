package org.pageseeder.berlioz.json;

import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class BuiltinJsonWriterTest extends JsonWriterTestBase {

  BuiltinJsonWriter newJsonWriter(StringWriter json) {
    return new BuiltinJsonWriter(new PrintWriter(json));
  }

  @Test(expected = NullPointerException.class)
  public void testValueStringNull() {
    newJsonWriter(new StringWriter()).value((String) null);
  }

  @Test(expected = NullPointerException.class)
  public void testFieldStringNull() {
    newJsonWriter(new StringWriter()).startObject().field("k", (String) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueDoubleNaN() {
    newJsonWriter(new StringWriter()).value(Double.NaN);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueDoublePositiveInfinity() {
    newJsonWriter(new StringWriter()).value(Double.POSITIVE_INFINITY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueDoubleNegativeInfinity() {
    newJsonWriter(new StringWriter()).value(Double.NEGATIVE_INFINITY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFieldDoubleNaN() {
    newJsonWriter(new StringWriter()).startObject().field("v", Double.NaN);
  }

  @Test(expected = IllegalStateException.class)
  public void testEndArrayOnObject() {
    newJsonWriter(new StringWriter()).startObject().endArray();
  }

  @Test(expected = IllegalStateException.class)
  public void testEndObjectOnArray() {
    newJsonWriter(new StringWriter()).startArray().endObject();
  }

}
