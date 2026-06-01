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
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.NaN));
  }

  @Test
  void testValueDoublePositiveInfinity() {
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.POSITIVE_INFINITY));
  }

  @Test
  void testValueDoubleNegativeInfinity() {
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.NEGATIVE_INFINITY));
  }

  @Test
  void testFieldDoubleNaN() {
    JsonWriter json = newJsonWriter(new StringWriter()).startObject();
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.field("v", Double.NaN));
  }

  @Test
  void testEndArrayOnObject() {
    JsonWriter json = newJsonWriter(new StringWriter()).startObject();
    Assertions.assertThrows(IllegalStateException.class, json::endArray);
  }

  @Test
  void testEndObjectOnArray() {
    JsonWriter json = newJsonWriter(new StringWriter()).startArray();
    Assertions.assertThrows(IllegalStateException.class, json::endObject);
  }

}
