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
package org.pageseeder.berlioz.util;

import org.junit.Assert;
import org.junit.Test;

public final class PairTest {

  @Test
  public void testAccessors() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);

    Assert.assertEquals("alpha", pair.first());
    Assert.assertEquals(Integer.valueOf(1), pair.second());
  }

  @Test
  public void testEquals() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);
    Pair<String, Integer> same = new Pair<>("alpha", 1);
    Pair<String, Integer> differentFirst = new Pair<>("beta", 1);
    Pair<String, Integer> differentSecond = new Pair<>("alpha", 2);

    Assert.assertEquals(pair, same);
    Assert.assertEquals(pair.hashCode(), same.hashCode());
    Assert.assertNotEquals(pair, differentFirst);
    Assert.assertNotEquals(pair, differentSecond);
  }

  @Test
  public void testEqualsWithNullValues() {
    Pair<String, Integer> pair = new Pair<>(null, null);
    Pair<String, Integer> same = new Pair<>(null, null);
    Pair<String, Integer> differentFirst = new Pair<>("alpha", null);
    Pair<String, Integer> differentSecond = new Pair<>(null, 1);

    Assert.assertEquals(pair, same);
    Assert.assertEquals(pair.hashCode(), same.hashCode());
    Assert.assertNotEquals(pair, differentFirst);
    Assert.assertNotEquals(pair, differentSecond);
  }

  @Test
  public void testNotEqualsOtherTypes() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);

    Assert.assertNotEquals(pair, null);
    Assert.assertNotEquals(pair, "alpha");
  }
}
