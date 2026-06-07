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

final class ResponseTest {

  @Test
  void testHeader_validHeader() {
    Response response = Response.ok()
        .header("Content-Location", "/articles/42")
        .header("WWW-Authenticate", "Bearer realm=\"api\"");

    Assertions.assertEquals("/articles/42", response.headers().get("Content-Location"));
    Assertions.assertEquals("Bearer realm=\"api\"", response.headers().get("WWW-Authenticate"));
  }

  @Test
  void testHeader_replacesExistingHeader() {
    Response response = Response.ok()
        .header("X-Test", "one")
        .header("X-Test", "two");

    Assertions.assertEquals(1, response.headers().size());
    Assertions.assertEquals("two", response.headers().get("X-Test"));
  }

  @Test
  void testHeader_rejectsInvalidNames() {
    String[] names = {
        "",
        "Bad Header",
        "Bad:Header",
        "Bad/Header",
        "Bad(Header)",
        "Bad\rHeader",
        "Bad\nHeader",
        "Bad\u007fHeader",
        "Bad\u00e9Header"
    };

    for (String name : names) {
      Response response = Response.ok();
      Assertions.assertThrows(IllegalArgumentException.class, () -> response.header(name, "value"), name);
    }
  }

  @Test
  void testHeader_rejectsInvalidValues() {
    String[] values = {
        "one\r\nSet-Cookie: session=attacker",
        "one\nX-Injected: yes",
        "one\rX-Injected: yes",
        "one\u0000two",
        "one\u001ftwo",
        "one\u007ftwo"
    };

    for (String value : values) {
      Response response = Response.ok();
      Assertions.assertThrows(IllegalArgumentException.class, () -> response.header("X-Test", value), value);
    }
  }

  @Test
  void testHeader_allowsSpaceAndTabInValues() {
    Response response = Response.ok().header("X-Test", "one two\tthree");

    Assertions.assertEquals("one two\tthree", response.headers().get("X-Test"));
  }

}
