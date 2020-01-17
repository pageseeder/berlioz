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

import org.junit.Assert;
import org.junit.Test;

import java.io.StringWriter;

/**
 * A base class to test JsonWriter implementations.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public abstract class JsonWriterTestBase {

  /**
   * @param json StringWriter used in test
   *
   * @return JsonWriter instance to test.
   */
  abstract JsonWriter newJsonWriter(StringWriter json);

  // Arrays
  // ------------------------------------------------------------------------------------------------

  @Test
  public void testNumberLong() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Long.MAX_VALUE).flush();
    Assert.assertEquals("9223372036854775807", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Long.MIN_VALUE).flush();
    Assert.assertEquals("-9223372036854775808", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0L).flush();
    Assert.assertEquals("0", json.toString());
  }

  @Test
  public void testNumberInteger() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Integer.MAX_VALUE).flush();
    Assert.assertEquals("2147483647", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Integer.MIN_VALUE).flush();
    Assert.assertEquals("-2147483648", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0).flush();
    Assert.assertEquals("0", json.toString());
  }

  @Test
  public void testNumberDouble() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Double.MAX_VALUE).flush();
    Assert.assertEquals("1.7976931348623157E308", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Double.MIN_VALUE).flush();
    Assert.assertEquals("4.9E-324", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0.0).flush();
    Assert.assertEquals("0.0", json.toString());
  }

  @Test
  public void testNumberFloat() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(Float.MAX_VALUE).flush();
    Assert.assertEquals("3.4028234663852886E38", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(Float.MIN_VALUE).flush();
    Assert.assertEquals("1.401298464324817E-45", json.toString());
    json = new StringWriter();
    newJsonWriter(json).value(0.0f).flush();
    Assert.assertEquals("0.0", json.toString());
  }

  @Test
  public void testNumberIsZero() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).value(0).flush();
    Assert.assertEquals("0", json.toString());
  }

  @Test
  public void testStringIsEmpty() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value("")
        .flush();
    Assert.assertEquals("\"\"", json.toString());
  }

  @Test
  public void testString() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value("Caf\u00e9\n \"Test\" \t\\")
        .flush();
    Assert.assertEquals("\"Caf\u00e9\\n \\\"Test\\\" \\t\\\\\"", json.toString());
  }

  @Test
  public void testBooleanIsTrue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value(true)
        .flush();
    Assert.assertEquals("true", json.toString());
  }

  @Test
  public void testBooleanIsFalse() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .value(false)
        .flush();
    Assert.assertEquals("false", json.toString());
  }

  @Test
  public void testNull() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).nullValue().flush();
    Assert.assertEquals("null", json.toString());
  }

  @Test
  public void testEmptyArray() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .endArray()
        .flush();
    Assert.assertEquals("[]", json.toString());
  }

  @Test
  public void testArrayWithStringValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value("")
        .endArray()
        .flush();
    Assert.assertEquals("[\"\"]", json.toString());
  }

  @Test
  public void testArrayWithNumberValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value(0)
        .endArray()
        .flush();
    Assert.assertEquals("[0]", json.toString());
  }

  @Test
  public void testArrayWithBooleanValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .value(false)
        .endArray()
        .flush();
    Assert.assertEquals("[false]", json.toString());
  }

  @Test
  public void testArrayWithNullValue() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .nullValue()
        .endArray()
        .flush();
    Assert.assertEquals("[null]", json.toString());
  }

  @Test
  public void testArrayWithArray() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .startArray()
        .endArray()
        .endArray()
        .flush();
    Assert.assertEquals("[[]]", json.toString());
  }

  @Test
  public void testArrayWithNestedArrays() {
    StringWriter json = new StringWriter();
    JsonWriter w = newJsonWriter(json);
    for (int i=0; i<32; i++) {
      w.startArray();
    }
    for (int i=0; i<32; i++) {
      w.endArray();
    }
    w.flush();
    Assert.assertEquals("[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]", json.toString());
  }

  @Test
  public void testArrayWithObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startArray()
        .startObject()
        .endObject()
        .endArray()
        .flush();
    Assert.assertEquals("[{}]", json.toString());
  }

  @Test
  public void testArrayWithMixedValues() {
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
    Assert.assertEquals("[\"abc\",123,3.141592653589793,true,false,[],{},null]", json.toString());
  }

  @Test
  public void testObjectIsEmpty() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .endObject()
        .flush();
    Assert.assertEquals("{}", json.toString());
  }

  @Test
  public void testSimpleObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .property("a", "b")
        .endObject()
        .flush();
    Assert.assertEquals("{\"a\":\"b\"}", json.toString());
  }

  @Test
  public void testObject() {
    StringWriter json = new StringWriter();
    newJsonWriter(json).startObject()
        .property("a", "xyz")
        .property("b", 123L)
        .property("c", Math.PI)
        .property("d", true)
        .property("e", false)
        .nullValue("f")
        .endObject()
        .flush();
    Assert.assertEquals("{\"a\":\"xyz\",\"b\":123,\"c\":3.141592653589793,\"d\":true,\"e\":false,\"f\":null}", json.toString());
  }

  @Test
  public void testObjectWithObjectField() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .startObject("abc")
        .endObject()
        .endObject()
        .flush();
    Assert.assertEquals("{\"abc\":{}}", json.toString());
  }

  @Test
  public void testObjectWithArrayField() {
    StringWriter json = new StringWriter();
    newJsonWriter(json)
        .startObject()
        .startArray("abc")
        .endArray()
        .endObject()
        .flush();
    Assert.assertEquals("{\"abc\":[]}", json.toString());
  }

  @Test
  public void testObjectWithNames() {
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
    Assert.assertEquals("{\"a\":1,\"b\":2,\"c\":3}", json.toString());
  }

}
