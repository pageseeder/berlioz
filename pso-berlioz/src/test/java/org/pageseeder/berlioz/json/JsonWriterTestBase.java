/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A base class to test JsonWriter implementations.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
abstract class JsonWriterTestBase {

  /**
   * @param json StringWriter used in test
   *
   * @return JsonWriter instance to test.
   */
  abstract JsonWriter newJsonWriter(StringWriter json);

  // Arrays
  // ------------------------------------------------------------------------------------------------

  @Test
  void testNumberLong() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Long.MAX_VALUE).flush();
    Assertions.assertEquals("9223372036854775807", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Long.MIN_VALUE).flush();
    Assertions.assertEquals("-9223372036854775808", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0L).flush();
    Assertions.assertEquals("0", json.toString());
  }

  @Test
  void testNumberInteger() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Integer.MAX_VALUE).flush();
    Assertions.assertEquals("2147483647", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Integer.MIN_VALUE).flush();
    Assertions.assertEquals("-2147483648", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0).flush();
    Assertions.assertEquals("0", json.toString());
  }

  @Test
  void testNumberDouble() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Double.MAX_VALUE).flush();
    Assertions.assertEquals("1.7976931348623157E308", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Double.MIN_VALUE).flush();
    Assertions.assertEquals("4.9E-324", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0.0).flush();
    Assertions.assertEquals("0.0", json.toString());
  }

  @Test
  void testNumberFloat() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Float.MAX_VALUE).flush();
    Assertions.assertEquals("3.4028234663852886E38", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Float.MIN_VALUE).flush();
    Assertions.assertEquals("1.401298464324817E-45", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0.0f).flush();
    Assertions.assertEquals("0.0", json.toString());
  }

  @Test
  void testValueNaNThrows() {
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.NaN));
  }

  @Test
  void testValuePositiveInfinityThrows() {
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.POSITIVE_INFINITY));
  }

  @Test
  void testValueNegativeInfinityThrows() {
    JsonWriter json = newJsonWriter(new StringWriter());
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.value(Double.NEGATIVE_INFINITY));
  }

  @Test
  void testFieldNaNThrows() {
    JsonWriter json = newJsonWriter(new StringWriter()).startObject();
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.field("value", Double.NaN));
  }

  @Test
  void testFieldInfinityThrows() {
    JsonWriter json = newJsonWriter(new StringWriter()).startObject();
    Assertions.assertThrows(IllegalArgumentException.class, () -> json.field("value", Double.POSITIVE_INFINITY));
  }

  @Test
  void testNumberIsZero() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(0).flush();
    Assertions.assertEquals("0", json.toString());
  }

  @Test
  void testStringIsEmpty() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value("")
        .flush();
    Assertions.assertEquals("\"\"", json.toString());
  }

  @Test
  void testString() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value("Caf\u00e9\n \"Test\" \t\\")
        .flush();
    Assertions.assertEquals("\"Caf\u00e9\\n \\\"Test\\\" \\t\\\\\"", json.toString());
  }

  @Test
  void testBooleanIsTrue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value(true)
        .flush();
    Assertions.assertEquals("true", json.toString());
  }

  @Test
  void testBooleanIsFalse() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value(false)
        .flush();
    Assertions.assertEquals("false", json.toString());
  }

  @Test
  void testNull() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).nullValue().flush();
    Assertions.assertEquals("null", json.toString());
  }

  @Test
  void testValueNullString() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value((String) null).flush();
    Assertions.assertEquals("null", json.toString());
  }

  @Test
  void testFieldNullStringValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).startObject().field("k", (String) null).endObject().flush();
    Assertions.assertEquals("{\"k\":null}", json.toString());
  }

  @Test
  void testEmptyArray() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .endArray()
        .flush();
    Assertions.assertEquals("[]", json.toString());
  }

  @Test
  void testArrayWithStringValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value("")
        .endArray()
        .flush();
    Assertions.assertEquals("[\"\"]", json.toString());
  }

  @Test
  void testArrayWithNumberValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value(0)
        .endArray()
        .flush();
    Assertions.assertEquals("[0]", json.toString());
  }

  @Test
  void testArrayWithBooleanValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value(false)
        .endArray()
        .flush();
    Assertions.assertEquals("[false]", json.toString());
  }

  @Test
  void testArrayWithNullValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .nullValue()
        .endArray()
        .flush();
    Assertions.assertEquals("[null]", json.toString());
  }

  @Test
  void testArrayWithArray() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .startArray()
        .endArray()
        .endArray()
        .flush();
    Assertions.assertEquals("[[]]", json.toString());
  }

  @Test
  void testArrayWithNestedArrays() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json);
    for (int i=0; i<32; i++) {
      w.startArray();
    }
    for (int i=0; i<32; i++) {
      w.endArray();
    }
    w.flush();
    Assertions.assertEquals("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]", json.toString());
  }

  @Test
  void testArrayWithDeepNestedArrays() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json);
    StringBuilder expected = new StringBuilder();
    int depth = 96;
    for (int i = 0; i < depth; i++) {
      w.startArray();
      expected.append('[');
    }
    for (int i = 0; i < depth; i++) {
      w.endArray();
      expected.append(']');
    }
    w.flush();
    Assertions.assertEquals(expected.toString(), json.toString());
  }

  @Test
  void testArrayWithObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .startObject()
        .endObject()
        .endArray()
        .flush();
    Assertions.assertEquals("[{}]", json.toString());
  }

  @Test
  void testArrayWithMixedValues() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray().value("abc")
        .value(123L)
        .value(Math.PI)
        .value(true)
        .value(false)
        .startArray().endArray()
        .startObject().endObject()
        .nullValue()
        .endArray()
        .flush();
    Assertions.assertEquals("[\"abc\",123,3.141592653589793,true,false,[],{},null]", json.toString());
  }

  @Test
  void testObjectIsEmpty() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .endObject()
        .flush();
    Assertions.assertEquals("{}", json.toString());
  }

  @Test
  void testSimpleObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .field("a", "b")
        .endObject()
        .flush();
    Assertions.assertEquals("{\"a\":\"b\"}", json.toString());
  }

  @Test
  void testObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).startObject()
        .field("a", "xyz")
        .field("b", 123L)
        .field("c", Math.PI)
        .field("d", true)
        .field("e", false)
        .nullValue("f")
        .endObject()
        .flush();
    Assertions.assertEquals("{\"a\":\"xyz\",\"b\":123,\"c\":3.141592653589793,\"d\":true,\"e\":false,\"f\":null}", json.toString());
  }

  @Test
  void testObjectWithObjectField() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .startObject("abc")
        .endObject()
        .endObject()
        .flush();
    Assertions.assertEquals("{\"abc\":{}}", json.toString());
  }

  @Test
  void testObjectWithArrayField() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .startArray("abc")
        .endArray()
        .endObject()
        .flush();
    Assertions.assertEquals("{\"abc\":[]}", json.toString());
  }

  @Test
  void testObjectWithNames() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .name("a")
        .value(1)
        .name("b")
        .value(2)
        .name("c")
        .value(3)
        .endObject()
        .flush();
    Assertions.assertEquals("{\"a\":1,\"b\":2,\"c\":3}", json.toString());
  }

  @Test
  void testNameFollowedByArray() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .name("items")
        .startArray()
        .value(1L)
        .value(2L)
        .endArray()
        .endObject()
        .flush();
    Assertions.assertEquals("{\"items\":[1,2]}", json.toString());
  }

  @Test
  void testNameFollowedByObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .name("inner")
        .startObject()
        .field("x", "y")
        .endObject()
        .endObject()
        .flush();
    Assertions.assertEquals("{\"inner\":{\"x\":\"y\"}}", json.toString());
  }

  @Test
  void testProperties() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("a", "1");
    map.put("b", "2");
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .properties(map)
        .endObject()
        .flush();
    Assertions.assertEquals("{\"a\":\"1\",\"b\":\"2\"}", json.toString());
  }

  @Test
  void testValueInt() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(42).flush();
    Assertions.assertEquals("42", json.toString());
  }

  @Test
  void testFieldInt() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .field("count", 42)
        .endObject()
        .flush();
    Assertions.assertEquals("{\"count\":42}", json.toString());
  }

  @Test
  void testInObjectAtTopLevel() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json);
    Assertions.assertFalse(w.inObject());
  }

  @Test
  void testInObjectInsideObject() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json).startObject();
    Assertions.assertTrue(w.inObject());
    w.endObject().flush();
  }

  @Test
  void testInObjectInsideArray() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json).startArray();
    Assertions.assertFalse(w.inObject());
    w.endArray().flush();
  }

  @Test
  void testInObjectNested() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json).startObject().startArray("items");
    Assertions.assertFalse(w.inObject());
    w.endArray();
    Assertions.assertTrue(w.inObject());
    w.endObject().flush();
  }

}
