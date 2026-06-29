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
import org.pageseeder.berlioz.error.InvalidParameterException;

final class ParameterSpecTest {

  enum Status { ACTIVE, INACTIVE, PENDING }

  // Specs defined as constants — mirrors real-world usage
  private static final ParameterSpec<Integer> PAGE =
      ParameterSpec.of("page", b -> b.asInt().clamp(1, 10000).defaultValue(1));

  private static final ParameterSpec<Status> STATUS =
      ParameterSpec.of("status", b -> b.asEnum(Status.class).optional(Status.ACTIVE));

  private static final ParameterSpec<String> REQUIRED_SLUG =
      ParameterSpec.of("slug", b -> b.asString().required());

  // --- name() ----------------------------------------------------------------

  @Test
  void name_returnsParameterName() {
    Assertions.assertEquals("page", PAGE.name());
    Assertions.assertEquals("status", STATUS.name());
  }

  // --- int spec (clamp + defaultValue) ---------------------------------------

  @Test
  void intSpec_validInRange_returnsValue() {
    int result = resolve(PAGE, "5");
    Assertions.assertEquals(5, result);
  }

  @Test
  void intSpec_absent_returnsDefault() {
    int result = resolve(PAGE, null);
    Assertions.assertEquals(1, result);
  }

  @Test
  void intSpec_belowMin_clampsToMin() {
    int result = resolve(PAGE, "0");
    Assertions.assertEquals(1, result);
  }

  @Test
  void intSpec_aboveMax_clampsToMax() {
    int result = resolve(PAGE, "99999");
    Assertions.assertEquals(10000, result);
  }

  @Test
  void intSpec_invalidFormat_returnsDefault() {
    int result = resolve(PAGE, "abc");
    Assertions.assertEquals(1, result);
  }

  // --- enum spec (orDefault) -------------------------------------------------

  @Test
  void enumSpec_validValue_returnsConstant() {
    Status result = resolve(STATUS, "ACTIVE");
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void enumSpec_absent_returnsDefault() {
    Status result = resolve(STATUS, null);
    Assertions.assertEquals(Status.ACTIVE, result);
  }

  @Test
  void enumSpec_invalidValue_throws() {
    Assertions.assertThrows(InvalidParameterException.class, () -> resolve(STATUS, "UNKNOWN"));
  }

  @Test
  void enumSpec_allValues_resolveCorrectly() {
    Assertions.assertEquals(Status.INACTIVE, resolve(STATUS, "INACTIVE"));
    Assertions.assertEquals(Status.PENDING,  resolve(STATUS, "PENDING"));
  }

  // --- required spec ---------------------------------------------------------

  @Test
  void requiredSpec_present_returnsValue() {
    String result = resolve(REQUIRED_SLUG, "hello-world");
    Assertions.assertEquals("hello-world", result);
  }

  @Test
  void requiredSpec_absent_throws() {
    Assertions.assertThrows(InvalidParameterException.class, () -> resolve(REQUIRED_SLUG, null));
  }

  // --- of() null guards ------------------------------------------------------

  @Test
  void of_nullName_throws() {
    Assertions.assertThrows(NullPointerException.class,
        () -> ParameterSpec.of(null, b -> b.asString().optional()));
  }

  @Test
  void of_nullResolver_throws() {
    Assertions.assertThrows(NullPointerException.class,
        () -> ParameterSpec.of("x", null));
  }

  // --- helpers ---------------------------------------------------------------

  private static <T> T resolve(ParameterSpec<T> spec, String rawValue) {
    return spec.resolve(new ParameterBuilder(spec.name(), rawValue));
  }

}
