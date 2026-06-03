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

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ParameterBuilderTest {

  // --- asInt -----------------------------------------------------------------

  @Test
  void asInt_validValue_required() {
    int result = builder("page", "5").asInt().required();
    Assertions.assertEquals(5, result);
  }

  @Test
  void asInt_validValue_defaultValue() {
    int result = builder("page", "3").asInt().defaultValue(1);
    Assertions.assertEquals(3, result);
  }

  @Test
  void asInt_absent_defaultValue() {
    int result = builder("page", null).asInt().defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void asInt_absent_required_throws() {
    TypedParameter<?> p = builder("page", null).asInt();
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  @Test
  void asInt_invalid_required_throws() {
    TypedParameter<?> p = builder("page", "abc").asInt();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
    Assertions.assertEquals("abc", ex.getParameterValue());
  }

  @Test
  void asInt_invalid_defaultValue_returnsDefault() {
    int result = builder("page", "abc").asInt().defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void asInt_invalid_nullable_throws() {
    TypedParameter<?> p = builder("page", "abc").asInt();
    Assertions.assertThrows(InvalidParameterException.class, p::nullable);
  }

  @Test
  void asInt_absent_nullable_returnsNull() {
    Integer result = builder("page", null).asInt().nullable();
    Assertions.assertNull(result);
  }

  @Test
  void asInt_valid_orDefault_returnsValue() {
    int result = builder("page", "7").asInt().orDefault(1);
    Assertions.assertEquals(7, result);
  }

  @Test
  void asInt_absent_orDefault_throws() {
    TypedParameter<Integer> p = builder("page", null).asInt();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.orDefault(1));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asInt_invalid_orDefault_returnsDefault() {
    int result = builder("page", "abc").asInt().orDefault(1);
    Assertions.assertEquals(1, result);
  }

  // --- asLong ----------------------------------------------------------------

  @Test
  void asLong_validValue() {
    long result = builder("id", "9999999999").asLong().required();
    Assertions.assertEquals(9999999999L, result);
  }

  @Test
  void asLong_invalid_throws() {
    TypedParameter<?> p = builder("id", "notanumber").asLong();
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  @Test
  void asLong_valid_orDefault_returnsValue() {
    long result = builder("id", "9999999999").asLong().orDefault(0L);
    Assertions.assertEquals(9999999999L, result);
  }

  @Test
  void asLong_absent_orDefault_throws() {
    TypedParameter<Long> p = builder("id", null).asLong();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.orDefault(0L));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asLong_invalid_orDefault_returnsDefault() {
    long result = builder("id", "notanumber").asLong().orDefault(-1L);
    Assertions.assertEquals(-1L, result);
  }

  // --- asBoolean -------------------------------------------------------------

  @Test
  void asBoolean_true() {
    Assertions.assertTrue(builder("flag", "true").asBoolean().required());
  }

  @Test
  void asBoolean_false() {
    Assertions.assertFalse(builder("flag", "false").asBoolean().required());
  }

  @Test
  void asBoolean_caseInsensitive() {
    Assertions.assertTrue(builder("flag", "TRUE").asBoolean().required());
    Assertions.assertFalse(builder("flag", "False").asBoolean().required());
  }

  @Test
  void asBoolean_invalid_throws() {
    TypedParameter<?> p = builder("flag", "yes").asBoolean();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void asBoolean_valid_orDefault_returnsValue() {
    boolean result = builder("flag", "true").asBoolean().orDefault(false);
    Assertions.assertTrue(result);
  }

  @Test
  void asBoolean_absent_orDefault_throws() {
    TypedParameter<Boolean> p = builder("flag", null).asBoolean();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.orDefault(false));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asBoolean_invalid_orDefault_returnsDefault() {
    boolean result = builder("flag", "yes").asBoolean().orDefault(false);
    Assertions.assertFalse(result);
  }

  // --- asLocalDate -----------------------------------------------------------

  @Test
  void asLocalDate_validIso() {
    LocalDate result = builder("from", "2024-03-15").asLocalDate().required();
    Assertions.assertEquals(LocalDate.of(2024, 3, 15), result);
  }

  @Test
  void asLocalDate_invalid_throws() {
    TypedParameter<?> p = builder("from", "15-03-2024").asLocalDate();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void asLocalDate_absent_defaultValue() {
    LocalDate def = LocalDate.of(2024, 1, 1);
    LocalDate result = builder("from", null).asLocalDate().defaultValue(def);
    Assertions.assertEquals(def, result);
  }

  @Test
  void asLocalDate_valid_orDefault_returnsValue() {
    LocalDate def = LocalDate.of(2024, 1, 1);
    LocalDate result = builder("from", "2024-03-15").asLocalDate().orDefault(def);
    Assertions.assertEquals(LocalDate.of(2024, 3, 15), result);
  }

  @Test
  void asLocalDate_absent_orDefault_throws() {
    LocalDate def = LocalDate.of(2024, 1, 1);
    TypedParameter<LocalDate> p = builder("from", null).asLocalDate();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.orDefault(def));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asLocalDate_invalid_orDefault_returnsDefault() {
    LocalDate def = LocalDate.of(2024, 1, 1);
    LocalDate result = builder("from", "15-03-2024").asLocalDate().orDefault(def);
    Assertions.assertEquals(def, result);
  }

  // --- asString --------------------------------------------------------------

  @Test
  void asString_presentValue() {
    String result = builder("q", "hello").asString().required();
    Assertions.assertEquals("hello", result);
  }

  @Test
  void asString_absent_required_throws() {
    TypedParameter<?> p = builder("q", null).asString();
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  // --- oneOf -----------------------------------------------------------------

  @Test
  void oneOf_validValue() {
    String result = builder("sort", "date").oneOf("name", "date", "title").defaultValue("name");
    Assertions.assertEquals("date", result);
  }

  @Test
  void oneOf_invalidValue_throws() {
    TypedParameter<?> p = builder("sort", "score").oneOf("name", "date", "title");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
    Assertions.assertEquals("score", ex.getParameterValue());
  }

  @Test
  void oneOf_absent_defaultValue() {
    String result = builder("sort", null).oneOf("name", "date", "title").defaultValue("name");
    Assertions.assertEquals("name", result);
  }

  @Test
  void oneOf_absent_required_throws() {
    TypedParameter<?> p = builder("sort", null).oneOf("name", "date");
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  @Test
  void oneOf_valid_orDefault_returnsValue() {
    String result = builder("sort", "date").oneOf("name", "date", "title").orDefault("name");
    Assertions.assertEquals("date", result);
  }

  @Test
  void oneOf_absent_orDefault_throws() {
    TypedParameter<String> p = builder("sort", null).oneOf("name", "date", "title");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.orDefault("name"));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void oneOf_invalid_orDefault_returnsDefault() {
    String result = builder("sort", "score").oneOf("name", "date", "title").orDefault("name");
    Assertions.assertEquals("name", result);
  }

  // --- helpers ---------------------------------------------------------------

  private static ParameterBuilder builder(String name, String value) {
    return new ParameterBuilder(name, value);
  }

}
