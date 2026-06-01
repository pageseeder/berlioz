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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ParameterTest {

  @Test
  void testName() {
    Parameter p = new Parameter("myParam", "value");
    Assertions.assertEquals(p.name(), "myParam");
  }

  @Test
  void testName_null() {
    Assertions.assertThrows(NullPointerException.class, () -> new Parameter(null, "value"));
  }

  @Test
  void testValue_literal() {
    Parameter p = new Parameter("x", "hello");
    Assertions.assertEquals(p.value(), "hello");
  }

  @Test
  void testValue_template() {
    Parameter p = new Parameter("x", "{var}");
    Assertions.assertEquals(p.value(), "{var}");
  }

  @Test
  void testValueWithMap_literal() {
    Parameter p = new Parameter("x", "hello");
    Map<String, String> map = Collections.singletonMap("x", "ignored");
    Assertions.assertEquals(p.value(map), "hello");
  }

  @Test
  void testValueWithMap_variableResolved() {
    Parameter p = new Parameter("x", "{var}");
    Map<String, String> map = Collections.singletonMap("var", "world");
    Assertions.assertEquals(p.value(map), "world");
  }

  @Test
  void testValueWithMap_variableMissing() {
    Parameter p = new Parameter("x", "{var}");
    Assertions.assertEquals(p.value(Collections.emptyMap()), "");
  }

  @Test
  void testValueWithMap_variableWithDefault() {
    Parameter p = new Parameter("x", "{var=fallback}");
    Assertions.assertEquals(p.value(Collections.emptyMap()), "fallback");
    Assertions.assertEquals(p.value(Collections.singletonMap("var", "actual")), "actual");
  }

  @Test
  void testValueWithMap_multipleVariables() {
    Parameter p = new Parameter("x", "{a}-{b}");
    Map<String, String> map = new HashMap<>();
    map.put("a", "hello");
    map.put("b", "world");
    Assertions.assertEquals(p.value(map), "hello-world");
  }
}
