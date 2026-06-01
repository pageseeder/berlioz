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

import org.junit.Assert;
import org.junit.Test;

public class JsonTest {

  @Test
  public void testCamelifyEmpty() {
    Assert.assertEquals("", Json.camelify(""));
  }

  @Test
  public void testCamelifyNoDash() {
    Assert.assertEquals("hello", Json.camelify("hello"));
  }

  @Test
  public void testCamelifyOneDash() {
    Assert.assertEquals("helloWorld", Json.camelify("hello-world"));
  }

  @Test
  public void testCamelifyMultipleDashes() {
    Assert.assertEquals("helloWorldFoo", Json.camelify("hello-world-foo"));
  }

  @Test
  public void testCamelifyTrailingDash() {
    Assert.assertEquals("hello-", Json.camelify("hello-"));
  }

  @Test
  public void testCamelifyLeadingDash() {
    Assert.assertEquals("Hello", Json.camelify("-hello"));
  }

  @Test
  public void testCamelifyConsecutiveDashes() {
    Assert.assertEquals("hello-world", Json.camelify("hello--world"));
  }

  @Test
  public void testProviderNameIsKnown() {
    String name = Json.providerName();
    Assert.assertNotNull(name);
    Assert.assertNotEquals("UNKNOWN", name);
  }

  @Test
  public void testProviderNameIsStable() {
    Assert.assertEquals(Json.providerName(), Json.providerName());
  }

}
