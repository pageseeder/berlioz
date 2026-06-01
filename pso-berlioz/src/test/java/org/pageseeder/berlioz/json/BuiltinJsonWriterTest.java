package org.pageseeder.berlioz.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class BuiltinJsonWriterTest extends JsonWriterTestBase {

  BuiltinJsonWriter newJsonWriter(StringWriter json) {
    return new BuiltinJsonWriter(new PrintWriter(json));
  }

  @Test
  public void testValueDoubleNaN() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.NaN));
  }

  @Test
  public void testValueDoublePositiveInfinity() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.POSITIVE_INFINITY));
  }

  @Test
  public void testValueDoubleNegativeInfinity() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.NEGATIVE_INFINITY));
  }

  @Test
  public void testFieldDoubleNaN() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).startObject().field("v", Double.NaN));
  }

  @Test
  public void testEndArrayOnObject() {
    Assertions.assertThrows(IllegalStateException.class, () -> newJsonWriter(new StringWriter()).startObject().endArray());
  }

  @Test
  public void testEndObjectOnArray() {
    Assertions.assertThrows(IllegalStateException.class, () -> newJsonWriter(new StringWriter()).startArray().endObject());
  }

}
