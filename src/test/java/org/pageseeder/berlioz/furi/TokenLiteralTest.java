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

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * A test class for the <code>TokenLiteral</code>.
 *
 * @author Christophe Lauret
 * @version 30 December 2008
 */
public final class TokenLiteralTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> expression.
   */
  @Test
  public void testNew_Null() {
    boolean nullThrown = false;
    try {
      new TokenLiteral(null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assert.assertTrue(nullThrown);
    }
  }

  /**
   * Test the <code>equals</code> method.
   */
  @Test
  public void testEquals() {
    TokenLiteral x = new TokenLiteral("t");
    TokenLiteral y = new TokenLiteral("t");
    TokenLiteral z = new TokenLiteral("T");
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatch() {
    assertMatchItsef("abc");
    assertMatchItsef("123");
    assertMatchItsef("/\\|");
    assertMatchItsef("[]{}()");
    assertMatchItsef(".,;:<>'\"!");
    assertMatchItsef("\u2014"); // m dash
    assertMatchItsef("http://pageseeder.com/user/clauret/home");
  }

  /**
   * Test the <code>pattern</code> method.
   */
  @Test
  public void testPattern() {
    assertPatternIsOK("abc");
    assertPatternIsOK("123");
    assertPatternIsOK("/\\|");
    assertPatternIsOK("[]{}()");
    assertPatternIsOK(".,;:<>'\"!");
    assertPatternIsOK("\u2014"); // m dash
    assertPatternIsOK("http://pageseeder.com/user/clauret/home");
  }

  // private helpers
  // --------------------------------------------------------------------------

  /**
   * Asserts that the string of matches itself.
   */
  private void assertMatchItsef(String s) {
    TokenLiteral t = new TokenLiteral(s);
    Assert.assertTrue(t.match(s));
  }

  /**
   * Asserts that the pattern is valid and working for a given string.
   */
  private void assertPatternIsOK(String s) {
    TokenLiteral t = new TokenLiteral(s);
    Pattern p = t.pattern();
    Assert.assertNotNull(p);
    Assert.assertTrue(p.matcher(s).matches());
  }
}
