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

import java.util.List;
import java.util.Map;

final class ProblemDetailsTest {

  // --- of() ---

  @Test
  void testOf_setsStatus() {
    ProblemDetails p = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Assertions.assertEquals(ContentStatus.NOT_FOUND, p.status());
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
    Assertions.assertEquals(ContentStatus.BAD_REQUEST, p.status());
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

    Assertions.assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, p.status());
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

}
