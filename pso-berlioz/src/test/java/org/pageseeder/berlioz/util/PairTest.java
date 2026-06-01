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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class PairTest {

  @Test
  public void testAccessors() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);

    Assertions.assertEquals(pair.first(), "alpha");
    Assertions.assertEquals(Integer.valueOf(1), pair.second());
  }

  @Test
  public void testEquals() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);
    Pair<String, Integer> same = new Pair<>("alpha", 1);
    Pair<String, Integer> differentFirst = new Pair<>("beta", 1);
    Pair<String, Integer> differentSecond = new Pair<>("alpha", 2);

    Assertions.assertEquals(pair, same);
    Assertions.assertEquals(pair.hashCode(), same.hashCode());
    Assertions.assertNotEquals(pair, differentFirst);
    Assertions.assertNotEquals(pair, differentSecond);
  }

  @Test
  public void testEqualsWithNullValues() {
    Pair<String, Integer> pair = new Pair<>(null, null);
    Pair<String, Integer> same = new Pair<>(null, null);
    Pair<String, Integer> differentFirst = new Pair<>("alpha", null);
    Pair<String, Integer> differentSecond = new Pair<>(null, 1);

    Assertions.assertEquals(pair, same);
    Assertions.assertEquals(pair.hashCode(), same.hashCode());
    Assertions.assertNotEquals(pair, differentFirst);
    Assertions.assertNotEquals(pair, differentSecond);
  }

  @Test
  public void testNotEqualsOtherTypes() {
    Pair<String, Integer> pair = new Pair<>("alpha", 1);

    Assertions.assertNotEquals(pair, null);
    Assertions.assertNotEquals(pair, "alpha");
  }
}
