/*
 * Copyright 2020 Allette Systems (Australia)
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
package org.pageseeder.berlioz.output;

import org.junit.Test;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.pageseeder.berlioz.output.OutputWriter.ContextOption;
import static org.pageseeder.berlioz.output.OutputWriter.FieldOption;

/**
 * Tests for {@link JsonOutputAdapter}, covering all {@link OutputWriter.FieldOption} and
 * {@link OutputWriter.ContextOption} combinations.
 *
 * <p>All tests capture output via an explicit {@link StringWriter} passed to the
 * {@link JsonOutputAdapter#JsonOutputAdapter(java.io.Writer)} constructor.</p>
 */
public class JsonOutputAdapterTest {

  // ---------------------------------------------------------------------------
  // getType
  // ---------------------------------------------------------------------------

  @Test
  public void getType_returnsJson() {
    assertEquals(OutputType.JSON, new JsonOutputAdapter().getType());
  }

  // ---------------------------------------------------------------------------
  // field(String, boolean, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_boolean_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":true}", sw.toString());
  }

  @Test
  public void field_boolean_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":true}", sw.toString());
  }

  @Test
  public void field_boolean_xmlText_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("active", false, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":false}", sw.toString());
  }

  @Test
  public void field_boolean_xmlElement_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":true}", sw.toString());
  }

  @Test
  public void field_boolean_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, int, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_int_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":7}", sw.toString());
  }

  @Test
  public void field_int_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void field_int_noOption_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":7}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, long, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_long_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":42}", sw.toString());
  }

  @Test
  public void field_long_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":42}", sw.toString());
  }

  @Test
  public void field_long_xmlText_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":42}", sw.toString());
  }

  @Test
  public void field_long_xmlElement_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":42}", sw.toString());
  }

  @Test
  public void field_long_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, double, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_double_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":1.5}", sw.toString());
  }

  @Test
  public void field_double_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":1.5}", sw.toString());
  }

  @Test
  public void field_double_xmlText_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":1.5}", sw.toString());
  }

  @Test
  public void field_double_xmlElement_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":1.5}", sw.toString());
  }

  @Test
  public void field_double_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, String, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_string_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"hello\"}", sw.toString());
  }

  @Test
  public void field_string_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"hello\"}", sw.toString());
  }

  @Test
  public void field_string_xmlText_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"hello\"}", sw.toString());
  }

  @Test
  public void field_string_xmlElement_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"hello\"}", sw.toString());
  }

  @Test
  public void field_string_xmlCopy_isWrittenAsProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("content", "<b>bold</b>", FieldOption.XML_COPY);
    out.endObject();
    out.flush();
    assertEquals("{\"content\":\"<b>bold</b>\"}", sw.toString());
  }

  @Test
  public void field_string_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, String[], FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_stringArray_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[]{"a", "b", "c"}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"tags\":[\"a\",\"b\",\"c\"]}", sw.toString());
  }

  @Test
  public void field_stringArray_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[]{"a", "b"}, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void field_stringArray_empty_isWrittenAsEmptyArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[0], FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"tags\":[]}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, Iterable<String>, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_iterable_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", Arrays.asList("x", "y"), FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"tags\":[\"x\",\"y\"]}", sw.toString());
  }

  @Test
  public void field_iterable_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    List<String> tags = Arrays.asList("x", "y");
    out.field("tags", tags, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // startObject / endObject — ContextOption
  // ---------------------------------------------------------------------------

  @Test
  public void startObject_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startObject("meta", ContextOption.DEFAULT);
    out.field("version", "1.0");
    out.endObject();
    out.endObject();
    out.flush();
    assertEquals("{\"meta\":{\"version\":\"1.0\"}}", sw.toString());
  }

  @Test
  public void startObject_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startObject("meta", ContextOption.JSON_ONLY);
    out.field("version", "1.0");
    out.endObject();
    out.endObject();
    out.flush();
    assertEquals("{\"meta\":{\"version\":\"1.0\"}}", sw.toString());
  }

  @Test
  public void startObject_xmlOnly_isSuppressed() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startObject("meta", ContextOption.XML_ONLY);
    out.field("version", "1.0");
    out.endObject();
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void startObject_xmlOnly_suppressesNestedContent() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startObject("outer", ContextOption.XML_ONLY);
    out.startObject("inner");
    out.field("x", "y");
    out.endObject();
    out.endObject();
    out.field("after", "visible");
    out.endObject();
    out.flush();
    assertEquals("{\"after\":\"visible\"}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // startArray / endArray — ContextOption
  // ---------------------------------------------------------------------------

  @Test
  public void startArray_default_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.DEFAULT);
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("{\"items\":[]}", sw.toString());
  }

  @Test
  public void startArray_jsonOnly_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.JSON_ONLY);
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("{\"items\":[]}", sw.toString());
  }

  @Test
  public void startArray_xmlOnly_isSuppressed() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.XML_ONLY);
    out.startObject("item");
    out.field("id", 1L);
    out.endObject();
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // optionalField
  // ---------------------------------------------------------------------------

  @Test
  public void optionalField_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", (String) null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void optionalField_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", "hello");
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"hello\"}", sw.toString());
  }

  @Test
  public void optionalField_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", (String) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, double[], FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_doubleArray_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("values", new double[]{1.5, 2.5, 3.0}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"values\":[1.5,2.5,3.0]}", sw.toString());
  }

  @Test
  public void field_doubleArray_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("values", new double[]{1.5, 2.5}, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void field_doubleArray_empty_isWrittenAsEmptyArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("values", new double[0], FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"values\":[]}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // nullField
  // ---------------------------------------------------------------------------

  @Test
  public void nullField_default_writesNullProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name");
    out.endObject();
    out.flush();
    assertEquals("{\"name\":null}", sw.toString());
  }

  @Test
  public void nullField_xmlElement_writesNullProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":null}", sw.toString());
  }

  @Test
  public void nullField_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  public void nullField_jsonOnly_writesNullProperty() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{\"name\":null}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // camelCase field name conversion
  // ---------------------------------------------------------------------------

  @Test
  public void field_hyphenatedName_isCamelCased() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("my-field", "value");
    out.endObject();
    out.flush();
    assertEquals("{\"myField\":\"value\"}", sw.toString());
  }

  @Test
  public void field_plainName_isUnchanged() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "value");
    out.endObject();
    out.flush();
    assertEquals("{\"name\":\"value\"}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // Multiple fields
  // ---------------------------------------------------------------------------

  @Test
  public void multipleFields_allWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("a", "x");
    out.field("b", 1L);
    out.field("c", true);
    out.endObject();
    out.flush();
    assertEquals("{\"a\":\"x\",\"b\":1,\"c\":true}", sw.toString());
  }

}
