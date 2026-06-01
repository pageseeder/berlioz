package org.pageseeder.berlioz.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.PrintWriter;
import java.io.StringWriter;

final class BuiltinJsonWriterTest extends JsonWriterTestBase {

  BuiltinJsonWriter newJsonWriter(StringWriter json) {
    return new BuiltinJsonWriter(new PrintWriter(json));
  }

  @Test
  void testValueDoubleNaN() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.NaN));
  }

  @Test
  void testValueDoublePositiveInfinity() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.POSITIVE_INFINITY));
  }

  @Test
  void testValueDoubleNegativeInfinity() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).value(Double.NEGATIVE_INFINITY));
  }

  @Test
  void testFieldDoubleNaN() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> newJsonWriter(new StringWriter()).startObject().field("v", Double.NaN));
  }

  @Test
  void testEndArrayOnObject() {
    Assertions.assertThrows(IllegalStateException.class, () -> newJsonWriter(new StringWriter()).startObject().endArray());
  }

  @Test
  void testEndObjectOnArray() {
    Assertions.assertThrows(IllegalStateException.class, () -> newJsonWriter(new StringWriter()).startArray().endObject());
  }

}
