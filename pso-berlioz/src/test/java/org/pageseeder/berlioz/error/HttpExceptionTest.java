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
package org.pageseeder.berlioz.error;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class HttpExceptionTest {

  @Test
  void headers_defaultEmpty() {
    HttpException ex = new HttpException("busy", 503) {};
    assertTrue(ex.headers().isEmpty());
  }

  @Test
  void header_setsValueRetrievableViaHeaders() {
    HttpException ex = new HttpException("busy", 503) {}.header("Retry-After", "30");
    assertEquals("30", ex.headers().get("Retry-After"));
  }

  @Test
  void header_returnsSameInstanceForChaining() {
    HttpException ex = new HttpException("busy", 503) {};
    HttpException chained = ex.header("Retry-After", "30");
    assertSame(ex, chained);
  }

  @Test
  void header_sameNameTwice_lastCallWins() {
    HttpException ex = new HttpException("busy", 503) {}
        .header("Retry-After", "30")
        .header("Retry-After", "60");
    assertEquals("60", ex.headers().get("Retry-After"));
    assertEquals(1, ex.headers().size());
  }

  @Test
  void header_multipleNames_allPresent() {
    HttpException ex = new HttpException("busy", 503) {}
        .header("Retry-After", "30")
        .header("X-Rate-Limit-Reset", "1700000000");
    assertEquals(Map.of("Retry-After", "30", "X-Rate-Limit-Reset", "1700000000"), ex.headers());
  }

  @Test
  void header_invalidName_throwsIllegalArgumentException() {
    HttpException ex = new HttpException("busy", 503) {};
    assertThrows(IllegalArgumentException.class, () -> ex.header("Invalid Name", "30"));
  }

  @Test
  void header_invalidValue_throwsIllegalArgumentException() {
    HttpException ex = new HttpException("busy", 503) {};
    assertThrows(IllegalArgumentException.class, () -> ex.header("Retry-After", "30\r\nX-Injected: true"));
  }

  @Test
  void header_nullName_throwsNullPointerException() {
    HttpException ex = new HttpException("busy", 503) {};
    assertThrows(NullPointerException.class, () -> ex.header(null, "30"));
  }

  @Test
  void header_nullValue_throwsNullPointerException() {
    HttpException ex = new HttpException("busy", 503) {};
    assertThrows(NullPointerException.class, () -> ex.header("Retry-After", null));
  }

  @Test
  void headers_returnsUnmodifiableView() {
    HttpException ex = new HttpException("busy", 503) {}.header("Retry-After", "30");
    assertThrows(UnsupportedOperationException.class, () -> ex.headers().put("X", "y"));
  }

  // findIn ---------------------------------------------------------------------------------------

  @Test
  void findIn_directMatch_returnsSameInstance() {
    HttpException ex = new HttpException("busy", 503) {};
    assertSame(ex, HttpException.findIn(ex));
  }

  @Test
  void findIn_wrappedInCause_findsOriginal() {
    HttpException ex = new HttpException("busy", 503) {}.header("Retry-After", "30");
    Exception wrapper = new Exception("wrapped", ex);
    assertSame(ex, HttpException.findIn(wrapper));
  }

  @Test
  void findIn_noHttpExceptionInChain_returnsNull() {
    Exception plain = new RuntimeException("boom", new IllegalStateException("cause"));
    assertNull(HttpException.findIn(plain));
  }

  @Test
  void findIn_null_returnsNull() {
    assertNull(HttpException.findIn(null));
  }

}
