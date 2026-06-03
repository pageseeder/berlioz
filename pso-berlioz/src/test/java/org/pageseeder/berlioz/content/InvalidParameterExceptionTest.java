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

final class InvalidParameterExceptionTest {

  @Test
  void required_hasCorrectFields() {
    InvalidParameterException ex = InvalidParameterException.required("page");
    Assertions.assertEquals("page", ex.getParameterName());
    Assertions.assertNull(ex.getParameterValue());
    Assertions.assertEquals(InvalidParameterException.Reason.REQUIRED, ex.getReason());
    Assertions.assertTrue(ex.getMessage().contains("page"));
  }

  @Test
  void invalidFormat_hasCorrectFields() {
    InvalidParameterException ex = InvalidParameterException.invalidFormat("page", "abc", "integer");
    Assertions.assertEquals("page", ex.getParameterName());
    Assertions.assertEquals("abc", ex.getParameterValue());
    Assertions.assertEquals(InvalidParameterException.Reason.INVALID_FORMAT, ex.getReason());
    Assertions.assertTrue(ex.getMessage().contains("page"));
    Assertions.assertTrue(ex.getMessage().contains("abc"));
    Assertions.assertTrue(ex.getMessage().contains("integer"));
  }

  @Test
  void outOfRange_hasCorrectFields() {
    InvalidParameterException ex = InvalidParameterException.outOfRange("count", "0", "must be >= 1");
    Assertions.assertEquals("count", ex.getParameterName());
    Assertions.assertEquals("0", ex.getParameterValue());
    Assertions.assertEquals(InvalidParameterException.Reason.OUT_OF_RANGE, ex.getReason());
    Assertions.assertTrue(ex.getMessage().contains("must be >= 1"));
  }

  @Test
  void notAllowed_hasCorrectFields() {
    InvalidParameterException ex = InvalidParameterException.notAllowed("sort", "score", "name", "date");
    Assertions.assertEquals("sort", ex.getParameterName());
    Assertions.assertEquals("score", ex.getParameterValue());
    Assertions.assertEquals(InvalidParameterException.Reason.NOT_ALLOWED, ex.getReason());
    Assertions.assertTrue(ex.getMessage().contains("name"));
    Assertions.assertTrue(ex.getMessage().contains("date"));
  }

  @Test
  void fillInStackTrace_returnsThis() {
    InvalidParameterException ex = InvalidParameterException.required("x");
    // fillInStackTrace() is a no-op — getStackTrace() returns an empty array
    Assertions.assertEquals(0, ex.getStackTrace().length);
  }

}
