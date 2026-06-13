/*
 * Copyright 2026 Allette Systems (Australia)
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

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.pageseeder.berlioz.output.OutputWriter.FieldOption;

/**
 * Tests for the default methods on the {@link OutputWriter} interface.
 *
 * <p>Concrete adapter behaviour (all {@link OutputWriter.FieldOption} and
 * {@link OutputWriter.ContextOption} combinations) is covered by
 * {@link XmlOutputAdapterTest} and {@link JsonOutputAdapterTest}.
 * This class fills the remaining gaps:
 * {@code isXml()}/{@code isJson()} predicates, {@code write()}/{@code writeIfPresent()},
 * all typed {@code optionalField} overloads, and the
 * {@code field(long[])}, {@code field(boolean[])}, and {@code field(int[])} array variants.</p>
 */
class OutputWriterTest {

  // ---------------------------------------------------------------------------
  // isXml / isJson predicates (default methods on OutputWriter)
  // ---------------------------------------------------------------------------

  @Test
  void xmlAdapter_isXml_true() {
    assertTrue(new XmlOutputAdapter().isXml());
  }

  @Test
  void xmlAdapter_isJson_false() {
    assertFalse(new XmlOutputAdapter().isJson());
  }

  @Test
  void jsonAdapter_isJson_true() {
    assertTrue(new JsonOutputAdapter().isJson());
  }

  @Test
  void jsonAdapter_isXml_false() {
    assertFalse(new JsonOutputAdapter().isXml());
  }

  // ---------------------------------------------------------------------------
  // write(OutputWritable)
  // ---------------------------------------------------------------------------

  @Test
  void write_delegatesToWritable() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    OutputWritable writable = writer -> writer.startObject("item").field("id", 1L).endObject();
    out.startObject("root");
    out.write(writable);
    out.endObject();
    out.flush();
    assertEquals("{\"item\":{\"id\":1}}", sw.toString());
  }

  @Test
  void write_returnsThis() {
    JsonOutputAdapter out = new JsonOutputAdapter(new StringWriter());
    out.startObject("root");
    assertSame(out, out.write(writer -> writer.startObject("x").endObject()));
    out.endObject();
  }

  // ---------------------------------------------------------------------------
  // writeIfPresent(OutputWritable)
  // ---------------------------------------------------------------------------

  @Test
  void writeIfPresent_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.writeIfPresent(null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void writeIfPresent_nonNull_delegatesToWritable() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    OutputWritable writable = writer -> writer.field("ok", true);
    out.startObject("root");
    out.writeIfPresent(writable);
    out.endObject();
    out.flush();
    assertEquals("{\"ok\":true}", sw.toString());
  }

  @Test
  void writeIfPresent_returnsThis() {
    JsonOutputAdapter out = new JsonOutputAdapter(new StringWriter());
    out.startObject("root");
    assertSame(out, out.writeIfPresent(null));
    out.endObject();
  }

  // ---------------------------------------------------------------------------
  // optionalField(String, Long) / optionalField(String, Long, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void optionalField_Long_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", (Long) null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Long_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", 42L);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":42}", sw.toString());
  }

  @Test
  void optionalField_Long_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", (Long) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Long_withOption_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", 7L, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":7}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // optionalField(String, Integer) / optionalField(String, Integer, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void optionalField_Integer_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", (Integer) null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Integer_nonNull_isWrittenAsLong() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", 5);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":5}", sw.toString());
  }

  @Test
  void optionalField_Integer_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", (Integer) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Integer_withOption_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("count", 3, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"count\":3}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // optionalField(String, Double) / optionalField(String, Double, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void optionalField_Double_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("ratio", (Double) null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Double_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("ratio", 1.5);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":1.5}", sw.toString());
  }

  @Test
  void optionalField_Double_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("ratio", (Double) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Double_withOption_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("ratio", 2.5, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"ratio\":2.5}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // optionalField(String, Boolean) / optionalField(String, Boolean, FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void optionalField_Boolean_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("active", (Boolean) null);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Boolean_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("active", Boolean.TRUE);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":true}", sw.toString());
  }

  @Test
  void optionalField_Boolean_withOption_null_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("active", (Boolean) null, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void optionalField_Boolean_withOption_nonNull_isWritten() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.optionalField("active", Boolean.FALSE, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"active\":false}", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, int[], FieldOption) — delegates to field(long[], FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void field_intArray_json_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new int[]{1, 2, 3}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"ids\":[1,2,3]}", sw.toString());
  }

  @Test
  void field_intArray_json_shorthand_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new int[]{4, 5});
    out.endObject();
    out.flush();
    assertEquals("{\"ids\":[4,5]}", sw.toString());
  }

  @Test
  void field_intArray_xml_default_writesCommaSeparatedAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new int[]{1, 2, 3}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root ids=\"1,2,3\"/>", sw.toString());
  }

  @Test
  void field_intArray_xml_xmlElement_writesOneElementPerValue() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new int[]{10, 20}, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><ids>10</ids><ids>20</ids></root>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // field(String, long[], FieldOption) — JSON (not in JsonOutputAdapterTest)
  // ---------------------------------------------------------------------------

  @Test
  void field_longArray_json_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new long[]{10L, 20L, 30L}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"ids\":[10,20,30]}", sw.toString());
  }

  @Test
  void field_longArray_json_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new long[]{1L, 2L}, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void field_longArray_xml_default_writesCommaSeparatedAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new long[]{1L, 2L, 3L}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root ids=\"1,2,3\"/>", sw.toString());
  }

  @Test
  void field_longArray_xml_xmlElement_writesOneElementPerValue() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("ids", new long[]{100L, 200L}, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><ids>100</ids><ids>200</ids></root>", sw.toString());
  }

  // ---------------------------------------------------------------------------
  // write(Iterable<OutputWritable>)
  // ---------------------------------------------------------------------------

  @Test
  void write_iterable_delegatesToEachWritable() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    List<OutputWritable> items = List.of(
        w -> w.startObject("item").field("id", 1L).endObject(),
        w -> w.startObject("item").field("id", 2L).endObject()
    );
    out.startArray("items").write(items).endArray();
    out.flush();
    assertEquals("[{\"id\":1},{\"id\":2}]", sw.toString());
  }

  @Test
  void write_iterable_empty_writesNothing() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startArray("items").write(List.<OutputWritable>of()).endArray();
    out.flush();
    assertEquals("[]", sw.toString());
  }

  @Test
  void write_iterable_returnsThis() {
    JsonOutputAdapter out = new JsonOutputAdapter(new StringWriter());
    out.startArray("items");
    assertSame(out, out.write(List.<OutputWritable>of()));
    out.endArray();
  }

  // ---------------------------------------------------------------------------
  // field(String, boolean[], FieldOption)
  // ---------------------------------------------------------------------------

  @Test
  void field_booleanArray_json_default_isWrittenAsArray() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("flags", new boolean[]{true, false, true}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("{\"flags\":[true,false,true]}", sw.toString());
  }

  @Test
  void field_booleanArray_json_xmlOnly_isSkipped() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    out.startObject("root");
    out.field("flags", new boolean[]{true, false}, FieldOption.XML_ONLY);
    out.endObject();
    out.flush();
    assertEquals("{}", sw.toString());
  }

  @Test
  void field_booleanArray_xml_default_writesCommaSeparatedAttribute() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("flags", new boolean[]{true, false}, FieldOption.DEFAULT);
    out.endObject();
    out.flush();
    assertEquals("<root flags=\"true,false\"/>", sw.toString());
  }

  @Test
  void field_booleanArray_xml_xmlElement_writesOneElementPerValue() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    out.startObject("root");
    out.field("flags", new boolean[]{true, false}, FieldOption.XML_ELEMENT);
    out.endObject();
    out.flush();
    assertEquals("<root><flags>true</flags><flags>false</flags></root>", sw.toString());
  }

}
