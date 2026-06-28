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
package org.pageseeder.berlioz.content;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;

final class ProblemDetailsTest {

  // --- of() ---

  @Test
  void testOf_setsStatus() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertEquals(404, p.status());
  }

  @Test
  void testOf_optionalFieldsAreNull() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertNull(p.type());
    Assertions.assertNull(p.title());
    Assertions.assertNull(p.detail());
    Assertions.assertNull(p.instance());
  }

  @Test
  void testOf_extensionsAreEmpty() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertTrue(p.extensions().isEmpty());
  }

  @Test
  void testOf_nullStatusThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> ProblemDetails.of(null));
  }

  // --- of(int) ---

  @Test
  void testOfInt_setsStatus() {
    Assertions.assertEquals(429, ProblemDetails.of(429).status());
  }

  @Test
  void testOfInt_boundaries() {
    Assertions.assertEquals(100, ProblemDetails.of(100).status());
    Assertions.assertEquals(599, ProblemDetails.of(599).status());
  }

  @Test
  void testOfInt_belowRangeThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ProblemDetails.of(99));
  }

  @Test
  void testOfInt_aboveRangeThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> ProblemDetails.of(600));
  }

  // --- type() ---

  @Test
  void testType_setsType() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND)
        .type("https://example.com/problems/not-found");
    Assertions.assertEquals("https://example.com/problems/not-found", p.type());
  }

  @Test
  void testType_returnsNewInstance() {
    ProblemDetails original = ProblemDetails.of(ContentStatus.NOT_FOUND);
    ProblemDetails updated = original.type("https://example.com/problems/not-found");
    Assertions.assertNotSame(original, updated);
    Assertions.assertNull(original.type());
  }

  @Test
  void testType_preservesOtherFields() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .title("Bad input")
        .type("https://example.com/problems/bad-input");
    Assertions.assertEquals(400, p.status());
    Assertions.assertEquals("Bad input", p.title());
  }

  @Test
  void testType_nullThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertThrows(NullPointerException.class, () -> p.type(null));
  }

  // --- title() ---

  @Test
  void testTitle_setsTitle() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND).title("Resource not found");
    Assertions.assertEquals("Resource not found", p.title());
  }

  @Test
  void testTitle_returnsNewInstance() {
    ProblemDetails original = ProblemDetails.of(ContentStatus.NOT_FOUND);
    ProblemDetails updated = original.title("Resource not found");
    Assertions.assertNotSame(original, updated);
    Assertions.assertNull(original.title());
  }

  @Test
  void testTitle_nullThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertThrows(NullPointerException.class, () -> p.title(null));
  }

  // --- detail() ---

  @Test
  void testDetail_setsDetail() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND)
        .detail("The article with id 42 does not exist.");
    Assertions.assertEquals("The article with id 42 does not exist.", p.detail());
  }

  @Test
  void testDetail_returnsNewInstance() {
    ProblemDetails original = ProblemDetails.of(ContentStatus.NOT_FOUND);
    ProblemDetails updated = original.detail("The article with id 42 does not exist.");
    Assertions.assertNotSame(original, updated);
    Assertions.assertNull(original.detail());
  }

  @Test
  void testDetail_nullThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertThrows(NullPointerException.class, () -> p.detail(null));
  }

  // --- instance() ---

  @Test
  void testInstance_setsInstance() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND)
        .instance("/articles/42");
    Assertions.assertEquals("/articles/42", p.instance());
  }

  @Test
  void testInstance_returnsNewInstance() {
    ProblemDetails original = ProblemDetails.of(ContentStatus.NOT_FOUND);
    ProblemDetails updated = original.instance("/articles/42");
    Assertions.assertNotSame(original, updated);
    Assertions.assertNull(original.instance());
  }

  @Test
  void testInstance_nullThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertThrows(NullPointerException.class, () -> p.instance(null));
  }

  // --- extension() ---

  @Test
  void testExtension_addsEntry() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("errors", List.of("field 'name' is required"));
    Assertions.assertEquals(1, p.extensions().size());
    Assertions.assertEquals(List.of("field 'name' is required"), p.extensions().get("errors"));
  }

  @Test
  void testExtension_accumulatesMultipleEntries() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("errors", List.of("field 'name' is required"))
        .extension("traceId", "abc-123");
    Assertions.assertEquals(2, p.extensions().size());
    Assertions.assertEquals("abc-123", p.extensions().get("traceId"));
  }

  @Test
  void testExtension_preservesInsertionOrder() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("first", 1)
        .extension("second", 2)
        .extension("third", 3);
    Assertions.assertIterableEquals(List.of("first", "second", "third"), p.extensions().keySet());
  }

  @Test
  void testExtension_returnsNewInstance() {
    ProblemDetails original = ProblemDetails.of(ContentStatus.BAD_REQUEST);
    ProblemDetails updated = original.extension("errors", List.of());
    Assertions.assertNotSame(original, updated);
    Assertions.assertTrue(original.extensions().isEmpty());
  }

  @Test
  void testExtension_mapIsUnmodifiable() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("errors", List.of());
    Map<String, Object> extensions = p.extensions();
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> extensions.put("extra", "value"));
  }

  @Test
  void testExtension_nullNameThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST);
    Assertions.assertThrows(NullPointerException.class, () -> p.extension(null, "value"));
  }

  @Test
  void testExtension_nullValueThrows() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.BAD_REQUEST);
    Assertions.assertThrows(NullPointerException.class, () -> p.extension("errors", null));
  }

  // --- immutability / chaining ---

  @Test
  void testChaining_fullObject() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .type("https://example.com/problems/internal")
        .title("Internal Server Error")
        .detail("An unexpected condition was encountered.")
        .instance("/log/events/789")
        .extension("traceId", "xyz-456");

    Assertions.assertEquals(500, p.status());
    Assertions.assertEquals("https://example.com/problems/internal", p.type());
    Assertions.assertEquals("Internal Server Error", p.title());
    Assertions.assertEquals("An unexpected condition was encountered.", p.detail());
    Assertions.assertEquals("/log/events/789", p.instance());
    Assertions.assertEquals("xyz-456", p.extensions().get("traceId"));
  }

  @Test
  void testChaining_doesNotMutatePriorInstance() {
    ProblemDetails base = ProblemDetails.of(ContentStatus.NOT_FOUND).title("Not Found");
    ProblemDetails withDetail = base.detail("The resource was not found.");

    Assertions.assertNull(base.detail());
    Assertions.assertEquals("The resource was not found.", withDetail.detail());
    Assertions.assertEquals("Not Found", withDetail.title());
  }

  // --- toJson() ---

  @Test
  void testToJson_minimalStatus() {
    String json = ProblemDetails.of(ContentStatus.NOT_FOUND).toJson();
    Assertions.assertTrue(json.contains("\"status\":404"));
    Assertions.assertFalse(json.contains("\"type\""));
    Assertions.assertFalse(json.contains("\"title\""));
    Assertions.assertFalse(json.contains("\"detail\""));
    Assertions.assertFalse(json.contains("\"instance\""));
  }

  @Test
  void testToJson_allStandardFields() {
    String json = ProblemDetails.of(ContentStatus.NOT_FOUND)
        .type("https://example.com/not-found")
        .title("Not Found")
        .detail("Resource missing.")
        .instance("/items/42")
        .toJson();
    Assertions.assertTrue(json.contains("\"type\":\"https://example.com/not-found\""));
    Assertions.assertTrue(json.contains("\"status\":404"));
    Assertions.assertTrue(json.contains("\"title\":\"Not Found\""));
    Assertions.assertTrue(json.contains("\"detail\":\"Resource missing.\""));
    Assertions.assertTrue(json.contains("\"instance\":\"/items/42\""));
  }

  @Test
  void testToJson_extensionString() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("trace", "abc123").toJson();
    Assertions.assertTrue(json.contains("\"trace\":\"abc123\""));
  }

  @Test
  void testToJson_extensionLong() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("count", 42L).toJson();
    Assertions.assertTrue(json.contains("\"count\":42"));
  }

  @Test
  void testToJson_extensionInteger() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("page", 3).toJson();
    Assertions.assertTrue(json.contains("\"page\":3"));
  }

  @Test
  void testToJson_extensionDouble() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("ratio", 0.5d).toJson();
    Assertions.assertTrue(json.contains("\"ratio\":0.5"));
  }

  @Test
  void testToJson_extensionBoolean() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("retryable", true).toJson();
    Assertions.assertTrue(json.contains("\"retryable\":true"));
  }

  @Test
  void testToJson_extensionList() {
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("errors", List.of("name required", "email invalid")).toJson();
    Assertions.assertTrue(json.contains("\"errors\":["));
    Assertions.assertTrue(json.contains("name required"));
    Assertions.assertTrue(json.contains("email invalid"));
  }

  @Test
  void testToJson_extensionFallback() {
    // ContentStatus is an enum — not String/Long/Integer/Double/Boolean/List, so falls through to toString()
    String json = ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("origin", ContentStatus.BAD_REQUEST).toJson();
    Assertions.assertTrue(json.contains("\"origin\":"));
  }

  // --- toXml(XmlWriter) ---

  @Test
  void testToXml_minimalStatus() {
    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.NOT_FOUND).toXml(out);
    String xml = out.toString();
    Assertions.assertTrue(xml.contains("<problem>"));
    Assertions.assertTrue(xml.contains("<status>404</status>"));
    Assertions.assertFalse(xml.contains("<type>"));
    Assertions.assertFalse(xml.contains("<title>"));
  }

  @Test
  void testToXml_allStandardFields() {
    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.NOT_FOUND)
        .type("https://example.com/not-found")
        .title("Not Found")
        .detail("Resource missing.")
        .instance("/items/42")
        .toXml(out);
    String xml = out.toString();
    Assertions.assertTrue(xml.contains("<type>https://example.com/not-found</type>"));
    Assertions.assertTrue(xml.contains("<status>404</status>"));
    Assertions.assertTrue(xml.contains("<title>Not Found</title>"));
    Assertions.assertTrue(xml.contains("<detail>Resource missing.</detail>"));
    Assertions.assertTrue(xml.contains("<instance>/items/42</instance>"));
  }

  @Test
  void testToXml_extensionScalar() {
    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("trace", "abc123").toXml(out);
    Assertions.assertTrue(out.toString().contains("<trace>abc123</trace>"));
  }

  @Test
  void testToXml_extensionList() {
    XmlStringBuilder out = new XmlStringBuilder();
    ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("error", List.of("name required", "email invalid")).toXml(out);
    String xml = out.toString();
    Assertions.assertTrue(xml.contains("<error>name required</error>"));
    Assertions.assertTrue(xml.contains("<error>email invalid</error>"));
  }

  // --- writeTo(OutputWriter) ---

  @Test
  void testWriteTo_xmlAdapter_minimalStatus() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    ProblemDetails.of(ContentStatus.NOT_FOUND).writeTo(out);
    out.flush();
    String xml = sw.toString();
    Assertions.assertTrue(xml.contains("<status>404</status>"));
    Assertions.assertFalse(xml.contains("<type>"));
  }

  @Test
  void testWriteTo_xmlAdapter_allStandardFields() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    ProblemDetails.of(ContentStatus.NOT_FOUND)
        .type("https://example.com/not-found")
        .title("Not Found")
        .detail("Missing.")
        .instance("/x/42")
        .writeTo(out);
    out.flush();
    String xml = sw.toString();
    Assertions.assertTrue(xml.contains("<type>https://example.com/not-found</type>"));
    Assertions.assertTrue(xml.contains("<title>Not Found</title>"));
    Assertions.assertTrue(xml.contains("<detail>Missing.</detail>"));
    Assertions.assertTrue(xml.contains("<instance>/x/42</instance>"));
  }

  @Test
  void testWriteTo_xmlAdapter_extensionTypes() {
    StringWriter sw = new StringWriter();
    XmlOutputAdapter out = new XmlOutputAdapter(sw);
    ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .extension("strField", "hello")
        .extension("longField", 10L)
        .extension("intField", 5)
        .extension("dblField", 1.5d)
        .extension("boolField", false)
        .extension("listField", List.of("x", "y"))
        .extension("otherField", ContentStatus.BAD_REQUEST)
        .writeTo(out);
    out.flush();
    String xml = sw.toString();
    Assertions.assertTrue(xml.contains("strField"));
    Assertions.assertTrue(xml.contains("longField"));
    Assertions.assertTrue(xml.contains("intField"));
    Assertions.assertTrue(xml.contains("dblField"));
    Assertions.assertTrue(xml.contains("boolField"));
    Assertions.assertTrue(xml.contains("listField"));
    Assertions.assertTrue(xml.contains("otherField"));
  }

  @Test
  void testWriteTo_jsonAdapter_minimalStatus() {
    StringWriter sw = new StringWriter();
    JsonOutputAdapter out = new JsonOutputAdapter(sw);
    ProblemDetails.of(ContentStatus.NOT_FOUND).writeTo(out);
    out.flush();
    Assertions.assertTrue(sw.toString().contains("\"status\":404"));
  }

}
