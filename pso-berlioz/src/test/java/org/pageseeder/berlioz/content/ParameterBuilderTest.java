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
import java.time.Month;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.error.InvalidParameterException;

final class ParameterBuilderTest {

  enum Status { ACTIVE, INACTIVE, PENDING }

  static final class UserId {
    final int value;
    UserId(int value) { this.value = value; }
    static UserId parse(String s) { return new UserId(Integer.parseInt(s)); }
    @Override public boolean equals(Object o) { return o instanceof UserId && ((UserId) o).value == value; }
    @Override public int hashCode() { return value; }
  }

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
  void asInt_invalid_optional_throws() {
    TypedParameter<?> p = builder("page", "abc").asInt();
    Assertions.assertThrows(InvalidParameterException.class, p::optional);
  }

  @Test
  void asInt_absent_optional_returnsNull() {
    Integer result = builder("page", null).asInt().optional();
    Assertions.assertNull(result);
  }

  @Test
  void asInt_valid_required_def_returnsValue() {
    int result = builder("page", "7").asInt().required(1);
    Assertions.assertEquals(7, result);
  }

  @Test
  void asInt_absent_required_def_throws() {
    TypedParameter<Integer> p = builder("page", null).asInt();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(1));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asInt_invalid_required_def_returnsDefault() {
    int result = builder("page", "abc").asInt().required(1);
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
  void asLong_valid_required_def_returnsValue() {
    long result = builder("id", "9999999999").asLong().required(0L);
    Assertions.assertEquals(9999999999L, result);
  }

  @Test
  void asLong_absent_required_def_throws() {
    TypedParameter<Long> p = builder("id", null).asLong();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(0L));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asLong_invalid_required_def_returnsDefault() {
    long result = builder("id", "notanumber").asLong().required(-1L);
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
  void asBoolean_valid_required_def_returnsValue() {
    boolean result = builder("flag", "true").asBoolean().required(false);
    Assertions.assertTrue(result);
  }

  @Test
  void asBoolean_absent_required_def_throws() {
    TypedParameter<Boolean> p = builder("flag", null).asBoolean();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(false));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asBoolean_invalid_required_def_returnsDefault() {
    boolean result = builder("flag", "yes").asBoolean().required(false);
    Assertions.assertFalse(result);
  }

  // --- asLocalDate -----------------------------------------------------------

  @Test
  void asLocalDate_validIso() {
    LocalDate result = builder("from", "2024-03-15").asLocalDate().required();
    Assertions.assertEquals(LocalDate.of(2024, Month.MARCH, 15), result);
  }

  @Test
  void asLocalDate_invalid_throws() {
    TypedParameter<?> p = builder("from", "15-03-2024").asLocalDate();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void asLocalDate_absent_defaultValue() {
    LocalDate def = LocalDate.of(2024, Month.JANUARY, 1);
    LocalDate result = builder("from", null).asLocalDate().defaultValue(def);
    Assertions.assertEquals(def, result);
  }

  @Test
  void asLocalDate_valid_required_def_returnsValue() {
    LocalDate def = LocalDate.of(2024, Month.JANUARY, 1);
    LocalDate result = builder("from", "2024-03-15").asLocalDate().required(def);
    Assertions.assertEquals(LocalDate.of(2024, Month.MARCH, 15), result);
  }

  @Test
  void asLocalDate_absent_required_def_throws() {
    LocalDate def = LocalDate.of(2024, Month.JANUARY, 1);
    TypedParameter<LocalDate> p = builder("from", null).asLocalDate();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(def));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asLocalDate_invalid_required_def_returnsDefault() {
    LocalDate def = LocalDate.of(2024, Month.JANUARY, 1);
    LocalDate result = builder("from", "15-03-2024").asLocalDate().required(def);
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
  void oneOf_valid_required_def_returnsValue() {
    String result = builder("sort", "date").oneOf("name", "date", "title").required("name");
    Assertions.assertEquals("date", result);
  }

  @Test
  void oneOf_absent_required_def_throws() {
    TypedParameter<String> p = builder("sort", null).oneOf("name", "date", "title");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required("name"));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void oneOf_invalid_required_def_returnsDefault() {
    String result = builder("sort", "score").oneOf("name", "date", "title").required("name");
    Assertions.assertEquals("name", result);
  }

  // --- clamp -----------------------------------------------------------------

  @Test
  void clamp_validInRange_returnsValue() {
    int result = builder("page", "5").asInt().clamp(1, 1000).required();
    Assertions.assertEquals(5, result);
  }

  @Test
  void clamp_validBelowMin_returnsMin() {
    int result = builder("page", "0").asInt().clamp(1, 1000).required();
    Assertions.assertEquals(1, result);
  }

  @Test
  void clamp_validAboveMax_returnsMax() {
    int result = builder("page", "9999").asInt().clamp(1, 1000).required();
    Assertions.assertEquals(1000, result);
  }

  @Test
  void clamp_absent_passesThroughForTerminal() {
    int result = builder("page", null).asInt().clamp(1, 1000).defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void clamp_invalidFormat_passesThroughForTerminal() {
    int result = builder("page", "abc").asInt().clamp(1, 1000).defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void clamp_validWithLong_clampsCorrectly() {
    long result = builder("id", "5000000000").asLong().clamp(1L, 9999999999L).required();
    Assertions.assertEquals(5000000000L, result);
  }

  // --- inRange ---------------------------------------------------------------

  @Test
  void inRange_validInRange_returnsValue() {
    int result = builder("page", "5").asInt().inRange(1, 1000).required();
    Assertions.assertEquals(5, result);
  }

  @Test
  void inRange_validBelowMin_required_throws() {
    TypedParameter<Integer> p = builder("page", "0").asInt().inRange(1, 1000);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
  }

  @Test
  void inRange_validAboveMax_required_throws() {
    TypedParameter<Integer> p = builder("page", "9999").asInt().inRange(1, 1000);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
  }

  @Test
  void inRange_outOfRange_defaultValue_returnsDefault() {
    int result = builder("page", "9999").asInt().inRange(1, 1000).defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void inRange_absent_passesThroughForTerminal() {
    int result = builder("page", null).asInt().inRange(1, 1000).defaultValue(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void inRange_invalidFormat_passesThroughForTerminal() {
    TypedParameter<Integer> p = builder("page", "abc").asInt().inRange(1, 1000);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void inRange_outOfRange_required_def_returnsDefault() {
    int result = builder("page", "9999").asInt().inRange(1, 1000).required(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void inRange_absent_required_def_throws() {
    TypedParameter<Integer> p = builder("page", null).asInt().inRange(1, 1000);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(1));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  // --- asEnum (no mapper) ----------------------------------------------------

  @Test
  void asEnum_exactMatch_returnsConstant() {
    Status result = builder("status", "ACTIVE").asEnum(Status.class).required();
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void asEnum_lowercase_throws() {
    TypedParameter<Status> p = builder("status", "active").asEnum(Status.class);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
  }

  @Test
  void asEnum_unknown_throws() {
    TypedParameter<Status> p = builder("status", "UNKNOWN").asEnum(Status.class);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
    Assertions.assertEquals("UNKNOWN", ex.getParameterValue());
  }

  @Test
  void asEnum_absent_required_throws() {
    TypedParameter<Status> p = builder("status", null).asEnum(Status.class);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void asEnum_absent_defaultValue_returnsDefault() {
    Status result = builder("status", null).asEnum(Status.class).defaultValue(Status.ACTIVE);
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void asEnum_invalid_defaultValue_returnsDefault() {
    Status result = builder("status", "unknown").asEnum(Status.class).defaultValue(Status.ACTIVE);
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  // --- asEnum (with nameMapper) ----------------------------------------------

  @Test
  void asEnum_nameMapper_lowercase_accepted() {
    Status result = builder("status", "active").asEnum(Status.class, String::toLowerCase).required();
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void asEnum_nameMapper_uppercase_rejected() {
    TypedParameter<Status> p = builder("status", "ACTIVE").asEnum(Status.class, String::toLowerCase);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
  }

  @Test
  void asEnum_nameMapper_mixedCase_rejected() {
    TypedParameter<Status> p = builder("status", "Active").asEnum(Status.class, String::toLowerCase);
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  @Test
  void asEnum_nameMapper_errorListsNormalisedNames() {
    TypedParameter<Status> p = builder("status", "ACTIVE").asEnum(Status.class, String::toLowerCase);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    String message = ex.getMessage();
    Assertions.assertTrue(message.contains("active"),    "should list normalised name 'active'");
    Assertions.assertTrue(message.contains("inactive"),  "should list normalised name 'inactive'");
    Assertions.assertTrue(message.contains("pending"),   "should list normalised name 'pending'");
    Assertions.assertFalse(message.contains("INACTIVE"), "should not list Java constant name 'INACTIVE'");
    Assertions.assertFalse(message.contains("PENDING"),  "should not list Java constant name 'PENDING'");
  }

  @Test
  void asEnum_nameMapper_absent_defaultValue_returnsDefault() {
    Status result = builder("status", null).asEnum(Status.class, String::toLowerCase).defaultValue(Status.ACTIVE);
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void asEnum_nameMapper_invalid_required_def_returnsDefault() {
    Status result = builder("status", "ACTIVE").asEnum(Status.class, String::toLowerCase).required(Status.INACTIVE);
    Assertions.assertEquals(Status.INACTIVE, result);
  }

  @Test
  void asEnum_nameMapper_absent_required_def_throws() {
    TypedParameter<Status> p = builder("status", null).asEnum(Status.class, String::toLowerCase);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.required(Status.ACTIVE));
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  // --- optional(def) ---------------------------------------------------------

  @Test
  void optional_def_valid_returnsValue() {
    int result = builder("page", "7").asInt().optional(1);
    Assertions.assertEquals(7, result);
  }

  @Test
  void optional_def_absent_returnsDefault() {
    int result = builder("page", null).asInt().optional(1);
    Assertions.assertEquals(1, result);
  }

  @Test
  void optional_def_invalid_throws() {
    TypedParameter<Integer> p = builder("page", "abc").asInt();
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.optional(1));
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void optional_def_outOfRange_throws() {
    TypedParameter<Integer> p = builder("page", "9999").asInt().inRange(1, 1000);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.optional(1));
    Assertions.assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
  }

  @Test
  void optional_def_notAllowed_throws() {
    TypedParameter<Status> p = builder("status", "UNKNOWN").asEnum(Status.class);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, () -> p.optional(Status.ACTIVE));
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
  }

  @Test
  void optional_def_enum_absent_returnsDefault() {
    Status result = builder("status", null).asEnum(Status.class).optional(Status.ACTIVE);
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  // --- as(Class, Function) ---------------------------------------------------

  @Test
  void as_class_valid_returnsValue() {
    UserId result = builder("id", "42").as(UserId.class, UserId::parse).required();
    Assertions.assertEquals(new UserId(42), result);
  }

  @Test
  void as_class_absent_optional_returnsNull() {
    UserId result = builder("id", null).as(UserId.class, UserId::parse).optional();
    Assertions.assertNull(result);
  }

  @Test
  void as_class_absent_required_throws() {
    TypedParameter<UserId> p = builder("id", null).as(UserId.class, UserId::parse);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void as_class_invalid_required_throws() {
    TypedParameter<UserId> p = builder("id", "abc").as(UserId.class, UserId::parse);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
    Assertions.assertEquals("abc", ex.getParameterValue());
  }

  @Test
  void as_class_invalid_usesSimpleNameInMessage() {
    TypedParameter<UserId> p = builder("id", "abc").as(UserId.class, UserId::parse);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertTrue(ex.getMessage().contains("UserId"), "error message should contain class simple name");
  }

  @Test
  void as_class_invalid_defaultValue_returnsDefault() {
    UserId result = builder("id", "abc").as(UserId.class, UserId::parse).defaultValue(new UserId(0));
    Assertions.assertEquals(new UserId(0), result);
  }

  // --- as(Function, String) --------------------------------------------------

  @Test
  void as_typeName_valid_returnsValue() {
    UserId result = builder("id", "7").as(UserId::parse, "user identifier").required();
    Assertions.assertEquals(new UserId(7), result);
  }

  @Test
  void as_typeName_absent_optional_returnsNull() {
    UserId result = builder("id", null).as(UserId::parse, "user identifier").optional();
    Assertions.assertNull(result);
  }

  @Test
  void as_typeName_invalid_required_throws() {
    TypedParameter<UserId> p = builder("id", "abc").as(UserId::parse, "user identifier");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
    Assertions.assertEquals("abc", ex.getParameterValue());
  }

  @Test
  void as_typeName_invalid_usesProvidedNameInMessage() {
    TypedParameter<UserId> p = builder("id", "abc").as(UserId::parse, "user identifier");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertTrue(ex.getMessage().contains("user identifier"), "error message should contain provided type name");
  }

  @Test
  void as_typeName_invalid_defaultValue_returnsDefault() {
    UserId result = builder("id", "abc").as(UserId::parse, "user identifier").defaultValue(new UserId(0));
    Assertions.assertEquals(new UserId(0), result);
  }

  // --- matching --------------------------------------------------------------

  @Test
  void matching_validSatisfiesPredicate_returnsValue() {
    int result = builder("count", "4").asInt().matching(n -> n % 2 == 0, "must be even").required();
    Assertions.assertEquals(4, result);
  }

  @Test
  void matching_validFailsPredicate_required_throws() {
    TypedParameter<Integer> p = builder("count", "3").asInt().matching(n -> n % 2 == 0, "must be even");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
    Assertions.assertEquals("3", ex.getParameterValue());
  }

  @Test
  void matching_validFailsPredicate_defaultValue_returnsDefault() {
    int result = builder("count", "3").asInt().matching(n -> n % 2 == 0, "must be even").defaultValue(0);
    Assertions.assertEquals(0, result);
  }

  @Test
  void matching_absent_passesThroughForTerminal() {
    int result = builder("count", null).asInt().matching(n -> n % 2 == 0, "must be even").defaultValue(0);
    Assertions.assertEquals(0, result);
  }

  @Test
  void matching_invalidFormat_passesThroughForTerminal() {
    TypedParameter<Integer> p = builder("count", "abc").asInt().matching(n -> n % 2 == 0, "must be even");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void matching_onString_satisfiesPredicate() {
    String result = builder("sku", "SKU-001").asString().matching(s -> s.startsWith("SKU-"), "must start with SKU-").required();
    Assertions.assertEquals("SKU-001", result);
  }

  @Test
  void matching_onString_failsPredicate_throws() {
    TypedParameter<String> p = builder("sku", "ABC-001").asString().matching(s -> s.startsWith("SKU-"), "must start with SKU-");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
  }

  // --- matchingRegex ---------------------------------------------------------

  @Test
  void matchingRegex_string_valid_returnsValue() {
    String result = builder("slug", "hello-world").matchingRegex("[a-z0-9-]+").required();
    Assertions.assertEquals("hello-world", result);
  }

  @Test
  void matchingRegex_string_invalid_required_throws() {
    TypedParameter<String> p = builder("slug", "Hello World").matchingRegex("[a-z0-9-]+");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
    Assertions.assertEquals("Hello World", ex.getParameterValue());
  }

  @Test
  void matchingRegex_string_invalid_defaultValue_returnsDefault() {
    String result = builder("slug", "Hello World").matchingRegex("[a-z0-9-]+").defaultValue("fallback");
    Assertions.assertEquals("fallback", result);
  }

  @Test
  void matchingRegex_string_absent_required_throws() {
    TypedParameter<String> p = builder("slug", null).matchingRegex("[a-z0-9-]+");
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
  }

  @Test
  void matchingRegex_string_absent_defaultValue_returnsDefault() {
    String result = builder("slug", null).matchingRegex("[a-z0-9-]+").defaultValue("fallback");
    Assertions.assertEquals("fallback", result);
  }

  @Test
  void matchingRegex_pattern_valid_returnsValue() {
    Pattern digits = Pattern.compile("\\d{4}");
    String result = builder("code", "1234").matchingRegex(digits).required();
    Assertions.assertEquals("1234", result);
  }

  @Test
  void matchingRegex_pattern_invalid_throws() {
    Pattern digits = Pattern.compile("\\d{4}");
    TypedParameter<String> p = builder("code", "12X4").matchingRegex(digits);
    InvalidParameterException ex = Assertions.assertThrows(InvalidParameterException.class, p::required);
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
  }

  @Test
  void matchingRegex_doesNotMatchPartially() {
    TypedParameter<String> p = builder("slug", "abc!").matchingRegex("[a-z]+");
    Assertions.assertThrows(InvalidParameterException.class, p::required);
  }

  // --- helpers ---------------------------------------------------------------

  private static ParameterBuilder builder(String name, String value) {
    return new ParameterBuilder(name, value);
  }

}
