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

import org.junit.Assert;
import org.junit.Test;

public final class ParameterTest {

  @Test
  public void testName() {
    Parameter p = new Parameter("myParam", "value");
    Assert.assertEquals("myParam", p.name());
  }

  @Test(expected = NullPointerException.class)
  public void testName_null() {
    new Parameter(null, "value");
  }

  @Test
  public void testValue_literal() {
    Parameter p = new Parameter("x", "hello");
    Assert.assertEquals("hello", p.value());
  }

  @Test
  public void testValue_template() {
    Parameter p = new Parameter("x", "{var}");
    Assert.assertEquals("{var}", p.value());
  }

  @Test
  public void testValueWithMap_literal() {
    Parameter p = new Parameter("x", "hello");
    Map<String, String> map = Collections.singletonMap("x", "ignored");
    Assert.assertEquals("hello", p.value(map));
  }

  @Test
  public void testValueWithMap_variableResolved() {
    Parameter p = new Parameter("x", "{var}");
    Map<String, String> map = Collections.singletonMap("var", "world");
    Assert.assertEquals("world", p.value(map));
  }

  @Test
  public void testValueWithMap_variableMissing() {
    Parameter p = new Parameter("x", "{var}");
    Assert.assertEquals("", p.value(Collections.emptyMap()));
  }

  @Test
  public void testValueWithMap_variableWithDefault() {
    Parameter p = new Parameter("x", "{var=fallback}");
    Assert.assertEquals("fallback", p.value(Collections.emptyMap()));
    Assert.assertEquals("actual", p.value(Collections.singletonMap("var", "actual")));
  }

  @Test
  public void testValueWithMap_multipleVariables() {
    Parameter p = new Parameter("x", "{a}-{b}");
    Map<String, String> map = new HashMap<>();
    map.put("a", "hello");
    map.put("b", "world");
    Assert.assertEquals("hello-world", p.value(map));
  }
}
