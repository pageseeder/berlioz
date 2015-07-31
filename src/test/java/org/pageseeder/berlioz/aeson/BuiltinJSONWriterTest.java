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
    newJSON(json).value("Caf\u00e9\n \"Test\" \t\\");
    Assert.assertEquals("\"Caf\u00e9\\n \\\"Test\\\" \\t\\\\\"", json.toString());
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
