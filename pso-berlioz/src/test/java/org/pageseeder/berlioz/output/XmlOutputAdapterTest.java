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
 * Tests for {@link XmlOutputAdapter}, covering all {@link OutputWriter.FieldOption} and
 * {@link OutputWriter.ContextOption} combinations.
 *
 *
 * <p>All tests capture output via an explicit {@link StringWriter} passed to the
 * {@link XmlOutputAdapter#XmlOutputAdapter(java.io.Writer)} constructor.</p>
 */
public class XmlOutputAdapterTest {

  // ---------------------------------------------------------------------------
  // getType
  // ---------------------------------------------------------------------------

  @Test
  public void getType_returnsXml() {
    assertEquals(OutputType.XML, new XmlOutputAdapter().getType());
  }

  // ---------------------------------------------------------------------------
  // startObject / endObject — ContextOption
  // ---------------------------------------------------------------------------

  @Test
  public void startObject_default_writesElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void startObject_xmlOnly_writesElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.startObject("meta", ContextOption.XML_ONLY);
    out.field("version", "1.0");
    out.endObject();
    out.endObject();
    out.flush();
    assertEquals("<root><meta version=\"1.0\"/></root>", sw.toString());
  }

  @Test
  public void startObject_jsonOnly_suppressesWrapperElementOnly() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.startObject("meta", ContextOption.JSON_ONLY);
    out.field("version", "1.0");
    out.endObject();
    out.endObject();
    out.flush();
    assertEquals("<root version=\"1.0\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // startArray / endArray — ContextOption
  // ---------------------------------------------------------------------------

  @Test
  public void startArray_default_writesWrapperElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.DEFAULT);
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("<root><items/></root>", sw.toString());
  }

  @Test
  public void startArray_jsonOnly_suppressesWrapperElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.JSON_ONLY);
    out.startObject("item");
    out.field("id", 1L);
    out.endObject();
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("<root><item id=\"1\"/></root>", sw.toString());
  }

  @Test
  public void startArray_xmlOnly_writesWrapperElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.startArray("items", ContextOption.XML_ONLY);
    out.endArray();
    out.endObject();
    out.flush();
    assertEquals("<root><items/></root>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, boolean, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_boolean_default_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root active=\"true\"/>", sw.toString());
  }

  @Test
  public void field_boolean_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void field_boolean_xmlText_writesTextContent() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("<root>true</root>", sw.toString());
  }

  @Test
  public void field_boolean_xmlElement_writesChildElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><active>true</active></root>", sw.toString());
  }

  @Test
  public void field_boolean_xmlOnly_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("active", true, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root active=\"true\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, int, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_int_default_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root count=\"7\"/>", sw.toString());
  }

  @Test
  public void field_int_xmlElement_writesChildElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><count>7</count></root>", sw.toString());
  }

  @Test
  public void field_int_noOption_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 7);
    out.endObject();
    out.flush();
    assertEquals("<root count=\"7\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, long, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_long_default_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root count=\"42\"/>", sw.toString());
  }

  @Test
  public void field_long_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void field_long_xmlText_writesTextContent() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("<root>42</root>", sw.toString());
  }

  @Test
  public void field_long_xmlElement_writesChildElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><count>42</count></root>", sw.toString());
  }

  @Test
  public void field_long_xmlOnly_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("count", 42L, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root count=\"42\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, double, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_double_default_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root ratio=\"1.5\"/>", sw.toString());
  }

  @Test
  public void field_double_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void field_double_xmlText_writesTextContent() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("<root>1.5</root>", sw.toString());
  }

  @Test
  public void field_double_xmlElement_writesChildElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><ratio>1.5</ratio></root>", sw.toString());
  }

  @Test
  public void field_double_xmlOnly_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ratio", 1.5, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root ratio=\"1.5\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, String, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_string_default_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root name=\"hello\"/>", sw.toString());
  }

  @Test
  public void field_string_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void field_string_xmlText_writesTextContent() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_TEXT);
    out.endObject();
    out.flush();
    assertEquals("<root>hello</root>", sw.toString());
  }

  @Test
  public void field_string_xmlElement_writesChildElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><name>hello</name></root>", sw.toString());
  }

  @Test
  public void field_string_xmlCopy_writesRawXml() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("content", "<b>bold</b>", FieldOption.XML_COPY);
    out.endObject();
    out.flush();
    assertEquals("<root><b>bold</b></root>", sw.toString());
  }

  @Test
  public void field_string_xmlOnly_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("name", "hello", FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root name=\"hello\"/>", sw.toString());
  }

  @Test
  public void field_string_default_escapesSpecialChars() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("value", "a&b", FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root value=\"a&amp;b\"/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, String[], FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_stringArray_default_writesCommaSeparatedAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[]{"a", "b", "c"}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root tags=\"a,b,c\"/>", sw.toString());
  }

  @Test
  public void field_stringArray_xmlElement_writesOneElementPerValue() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[]{"a", "b"}, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><tags>a</tags><tags>b</tags></root>", sw.toString());
  }

  @Test
  public void field_stringArray_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", new String[]{"a", "b"}, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, Iterable<String>, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  public void field_iterable_default_writesCommaSeparatedAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("tags", Arrays.asList("x", "y"), FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root tags=\"x,y\"/>", sw.toString());
  }

  @Test
  public void field_iterable_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    List<String> tags = Arrays.asList("x", "y");
    out.field("tags", tags, FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // optionalField
  // ---------------------------------------------------------------------------

  @Test
  public void optionalField_null_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", (String) null);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void optionalField_nonNull_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", "hello");
    out.endObject();
    out.flush();
    assertEquals("<root name=\"hello\"/>", sw.toString());
  }

  @Test
  public void optionalField_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", (String) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void optionalField_withOption_nonNull_writesAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("name", "hello", FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><name>hello</name></root>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // nullField
  // ---------------------------------------------------------------------------

  @Test
  public void nullField_default_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name");
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  @Test
  public void nullField_xmlElement_writesSelfClosingElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><name/></root>", sw.toString());
  }

  @Test
  public void nullField_xmlOnly_writesSelfClosingElement() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root><name/></root>", sw.toString());
  }

  @Test
  public void nullField_jsonOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.nullField("name", FieldOption.JSON_ONLY);
    out.endObject();
    out.flush();
    assertEquals("<root/>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // Multiple fields
  // ---------------------------------------------------------------------------

  @Test
  public void multipleFields_allWritten() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("a", "x");
    out.field("b", 1L);
    out.field("c", true);
    out.endObject();
    out.flush();
    assertEquals("<root a=\"x\" b=\"1\" c=\"true\"/>", sw.toString());
  }

}
