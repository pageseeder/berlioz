/*
 * Copyright 2015 Allette Systems (Australia)
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
package org.pageseeder.berlioz.furi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * A test class for the <code>TokenVariable</code>.
 *
 * @author Christophe Lauret
 * @version 0.9.33
 * @since 0.9.33
 */
final class TokenVariableTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> expression.
   */
  @Test
  void testNew_Null() {
    boolean nullThrown = false;
    try {
      new TokenVariable((Variable) null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assertions.assertTrue(nullThrown);
    }
  }

  /**
   * Test the <code>equals</code> method.
   */
  @Test
  void testEquals() {
    Variable v = new Variable("v");
    Variable w = new Variable("w");
    TokenVariable x = new TokenVariable(v);
    TokenVariable y = new TokenVariable(v);
    TokenVariable z = new TokenVariable(w);
    TestUtils.assertEqualsContract(x, y, z);
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  void testMatch() {
    TokenVariable v = new TokenVariable("X");
    // should match unreserved characters
    Assertions.assertTrue(v.match("abcxyz"));
    Assertions.assertTrue(v.match("ABCXYZ"));
    Assertions.assertTrue(v.match("0123456789"));
    Assertions.assertTrue(v.match("_"));
    Assertions.assertTrue(v.match("-"));
    Assertions.assertTrue(v.match("."));
    Assertions.assertTrue(v.match("%45"));
    // should not match reserved characters in ASCII range
    Assertions.assertFalse(v.match("%"));
    Assertions.assertFalse(v.match("/"));
    Assertions.assertFalse(v.match("*"));
    Assertions.assertFalse(v.match("*"));
    // should not match reserved characters outside ASCII range
    Assertions.assertFalse(v.match("\u00e9"));
  }
}
