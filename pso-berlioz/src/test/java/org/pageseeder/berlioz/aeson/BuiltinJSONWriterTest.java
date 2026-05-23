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
package org.pageseeder.berlioz.aeson;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;


/**
 * An implementation of a JSON Writer backed by
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class BuiltinJSONWriterTest {

  @Test
  public void testEmptyArray() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray().end();
    Assert.assertEquals("[]", json.toString());
  }

  @Test
  public void testSimpleArray() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray().value("a").end();
    Assert.assertEquals("[\"a\"]", json.toString());
  }

  @Test
  public void testArray() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray().value("abc").value(123L).value(Math.PI).value(true).value(false).writeNull().end();
    Assert.assertEquals("[\"abc\",123,3.141592653589793,true,false,null]", json.toString());
  }

  @Test
  public void testArrayArray() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray().startArray().end().end();
    Assert.assertEquals("[[]]", json.toString());
  }

  @Test
  public void testEmptyObject() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject().end();
    Assert.assertEquals("{}", json.toString());
  }

  @Test
  public void testSimpleObject() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject().property("a", "b").end();
    Assert.assertEquals("{\"a\":\"b\"}", json.toString());
  }

  @Test
  public void testObject() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject()
    .property("a","xyz")
    .property("b",123L)
    .property("c", Math.PI)
    .property("d", true)
    .property("e", false)
    .writeNull("f")
    .end();
    Assert.assertEquals("{\"a\":\"xyz\",\"b\":123,\"c\":3.141592653589793,\"d\":true,\"e\":false,\"f\":null}", json.toString());
  }

  @Test
  public void testObjectObject() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject().startObject("abc").end().end();
    Assert.assertEquals("{\"abc\":{}}", json.toString());
  }

  // Nested container as non-first item (regression for maybeAppendComma bug)

  @Test
  public void testObjectNestedObjectAfterProperty() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject()
        .property("a", "1")
        .startObject("inner")
            .property("x", "y")
        .end()
    .end();
    Assert.assertEquals("{\"a\":\"1\",\"inner\":{\"x\":\"y\"}}", json.toString());
  }

  @Test
  public void testObjectNestedArrayAfterProperty() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject()
        .property("a", "1")
        .startArray("items")
            .value("x")
            .value("y")
        .end()
        .property("b", "2")
    .end();
    Assert.assertEquals("{\"a\":\"1\",\"items\":[\"x\",\"y\"],\"b\":\"2\"}", json.toString());
  }

  @Test
  public void testArrayNestedArrayAfterValue() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray()
        .value("a")
        .startArray()
            .value("b")
        .end()
    .end();
    Assert.assertEquals("[\"a\",[\"b\"]]", json.toString());
  }

  @Test
  public void testArrayNestedObjectAfterValue() {
    StringWriter json = new StringWriter();
    newJSON(json).startArray()
        .value(1L)
        .startObject()
            .property("k", "v")
        .end()
    .end();
    Assert.assertEquals("[1,{\"k\":\"v\"}]", json.toString());
  }

  @Test
  public void testMultipleSequentialNestedObjects() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject()
        .startObject("a").property("x", "1").end()
        .startObject("b").property("y", "2").end()
    .end();
    Assert.assertEquals("{\"a\":{\"x\":\"1\"},\"b\":{\"y\":\"2\"}}", json.toString());
  }

  @Test
  public void testPropertyAfterNestedObject() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject()
        .startObject("inner").end()
        .property("after", "value")
    .end();
    Assert.assertEquals("{\"inner\":{},\"after\":\"value\"}", json.toString());
  }

  @Test
  public void testDeepNesting() {
    StringWriter json = new StringWriter();
    BuiltinJSONWriter w = newJSON(json);
    int depth = 40;
    for (int i = 0; i < depth; i++) w.startArray();
    for (int i = 0; i < depth; i++) w.end();
    String expected = "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
                      "]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]";
    Assert.assertEquals(expected, json.toString());
  }

  // NaN and Infinity are not valid JSON

  @Test(expected = IllegalArgumentException.class)
  public void testValueNaNThrows() {
    newJSON(new StringWriter()).value(Double.NaN);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValuePositiveInfinityThrows() {
    newJSON(new StringWriter()).value(Double.POSITIVE_INFINITY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValueNegativeInfinityThrows() {
    newJSON(new StringWriter()).value(Double.NEGATIVE_INFINITY);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyNaNThrows() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject().property("x", Double.NaN).end();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyInfinityThrows() {
    StringWriter json = new StringWriter();
    newJSON(json).startObject().property("x", Double.POSITIVE_INFINITY).end();
  }

  @Test
  public void testNumber() {
    StringWriter json = new StringWriter();
    newJSON(json).value(Long.MAX_VALUE);
    Assert.assertEquals("9223372036854775807", json.toString());
    json = new StringWriter();
    newJSON(json).value(Long.MIN_VALUE);
    Assert.assertEquals("-9223372036854775808", json.toString());
    json = new StringWriter();
    newJSON(json).value(Double.MAX_VALUE);
    Assert.assertEquals("1.7976931348623157E308", json.toString());
    json = new StringWriter();
    newJSON(json).value(Double.MIN_VALUE);
    Assert.assertEquals("4.9E-324", json.toString());
  }

  @Test
  public void testString() {
    StringWriter json = new StringWriter();
    newJSON(json).value("Café\n \"Test\" \t\\");
    Assert.assertEquals("\"Café\\n \\\"Test\\\" \\t\\\\\"", json.toString());
  }

  // appendJSONString - named escapes (RFC 8259 section 7)

  @Test
  public void testStringEmpty() {
    StringWriter json = new StringWriter();
    newJSON(json).value("");
    Assert.assertEquals("\"\"", json.toString());
  }

  @Test
  public void testStringQuote() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\"");
    Assert.assertEquals("\"\\\"\"", json.toString());
  }

  @Test
  public void testStringBackslash() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\\");
    Assert.assertEquals("\"\\\\\"", json.toString());
  }

  @Test
  public void testStringNewline() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\n");
    Assert.assertEquals("\"\\n\"", json.toString());
  }

  @Test
  public void testStringCarriageReturn() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\r");
    Assert.assertEquals("\"\\r\"", json.toString());
  }

  @Test
  public void testStringTab() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\t");
    Assert.assertEquals("\"\\t\"", json.toString());
  }

  @Test
  public void testStringBackspace() {
    // U+0008 uses the short \b named escape
    StringWriter json = new StringWriter();
    newJSON(json).value("\b");
    Assert.assertEquals("\"\\b\"", json.toString());
  }

  @Test
  public void testStringFormFeed() {
    // U+000C uses the short \f named escape
    StringWriter json = new StringWriter();
    newJSON(json).value("\f");
    Assert.assertEquals("\"\\f\"", json.toString());
  }

  // appendJSONString - control character hex escape (U+0000-U+001F, except the 7 named ones)

  @Test
  public void testStringControlNull() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\u0000");
    Assert.assertEquals("\"\\u0000\"", json.toString());
  }

  @Test
  public void testStringControlUnit() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\u0001");
    Assert.assertEquals("\"\\u0001\"", json.toString());
  }

  @Test
  public void testStringControlEsc() {
    // U+001B (ESC) - uses hex form, not a named escape
    StringWriter json = new StringWriter();
    newJSON(json).value("\u001b");
    Assert.assertEquals("\"\\u001b\"", json.toString());
  }

  @Test
  public void testStringControlMaxControl() {
    // U+001F - last control character before the printable range
    StringWriter json = new StringWriter();
    newJSON(json).value("\u001f");
    Assert.assertEquals("\"\\u001f\"", json.toString());
  }

  // appendJSONString - segment flush positions (exercises the bulk-copy path)

  @Test
  public void testStringSpecialAtStart() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\"hello");
    Assert.assertEquals("\"\\\"hello\"", json.toString());
  }

  @Test
  public void testStringSpecialAtEnd() {
    StringWriter json = new StringWriter();
    newJSON(json).value("hello\"");
    Assert.assertEquals("\"hello\\\"\"", json.toString());
  }

  @Test
  public void testStringSpecialInMiddle() {
    StringWriter json = new StringWriter();
    newJSON(json).value("hello\nworld");
    Assert.assertEquals("\"hello\\nworld\"", json.toString());
  }

  @Test
  public void testStringBackslashAtStart() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\\hello");
    Assert.assertEquals("\"\\\\hello\"", json.toString());
  }

  @Test
  public void testStringOnlySpecialChars() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\"\\");
    Assert.assertEquals("\"\\\"\\\\\"", json.toString());
  }

  @Test
  public void testStringConsecutiveSpecialChars() {
    StringWriter json = new StringWriter();
    newJSON(json).value("\n\r\t");
    Assert.assertEquals("\"\\n\\r\\t\"", json.toString());
  }

  // appendJSONString - bulk clean-run copy and non-ASCII passthrough

  @Test
  public void testStringLongClean() {
    StringWriter json = new StringWriter();
    String s = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    newJSON(json).value(s);
    Assert.assertEquals("\"" + s + "\"", json.toString());
  }

  @Test
  public void testStringNonAsciiPassthrough() {
    // Characters above U+001F that are not " or \ pass through unchanged
    StringWriter json = new StringWriter();
    newJSON(json).value("café 中文");
    Assert.assertEquals("\"café 中文\"", json.toString());
  }

  @Test
  public void testBoolean() {
    StringWriter json = new StringWriter();
    newJSON(json).value(true);
    Assert.assertEquals("true", json.toString());
    json = new StringWriter();
    newJSON(json).value(false);
    Assert.assertEquals("false", json.toString());
  }

  @Test
  public void testNull() {
    StringWriter json = new StringWriter();
    newJSON(json).writeNull();
    Assert.assertEquals("null", json.toString());
  }

  private BuiltinJSONWriter newJSON(StringWriter json) {
    return new BuiltinJSONWriter(new PrintWriter(json));
  }

}
