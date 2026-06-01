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
package org.pageseeder.berlioz.json;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonTest {

  @Test
  void testCamelifyEmpty() {
    Assertions.assertEquals("", Json.camelify(""));
  }

  @Test
  void testCamelifyNoDash() {
    Assertions.assertEquals("hello", Json.camelify("hello"));
  }

  @Test
  void testCamelifyOneDash() {
    Assertions.assertEquals("helloWorld", Json.camelify("hello-world"));
  }

  @Test
  void testCamelifyMultipleDashes() {
    Assertions.assertEquals("helloWorldFoo", Json.camelify("hello-world-foo"));
  }

  @Test
  void testCamelifyTrailingDash() {
    Assertions.assertEquals("hello-", Json.camelify("hello-"));
  }

  @Test
  void testCamelifyLeadingDash() {
    Assertions.assertEquals("Hello", Json.camelify("-hello"));
  }

  @Test
  void testCamelifyConsecutiveDashes() {
    Assertions.assertEquals("hello-world", Json.camelify("hello--world"));
  }

  @Test
  void testProviderNameIsKnown() {
    String name = Json.providerName();
    Assertions.assertNotNull(name);
    Assertions.assertNotEquals("UNKNOWN", name);
  }

  @Test
  void testProviderNameIsStable() {
    Assertions.assertEquals(Json.providerName(), Json.providerName());
  }

}
