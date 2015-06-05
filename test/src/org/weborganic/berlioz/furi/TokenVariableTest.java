/*
 * This file is part of the URI Template library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import org.junit.Assert;
import org.junit.Test;

/**
 * A test class for the <code>TokenVariable</code>.
 *
 * @author Christophe Lauret
 * @version 30 December 2008
 */
public final class TokenVariableTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> expression.
   */
  @Test
  public void testNew_Null() {
    boolean nullThrown = false;
    try {
      new TokenVariable((Variable) null);
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
    Variable v = new Variable("v");
    Variable w = new Variable("w");
    TokenVariable x = new TokenVariable(v);
    TokenVariable y = new TokenVariable(v);
    TokenVariable z = new TokenVariable(w);
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatch() {
    TokenVariable v = new TokenVariable("X");
    // should match unreserved characters
    Assert.assertTrue(v.match("abcxyz"));
    Assert.assertTrue(v.match("ABCXYZ"));
    Assert.assertTrue(v.match("0123456789"));
    Assert.assertTrue(v.match("_"));
    Assert.assertTrue(v.match("-"));
    Assert.assertTrue(v.match("."));
    Assert.assertTrue(v.match("%45"));
    // should not match reserved characters in ASCII range
    Assert.assertFalse(v.match("%"));
    Assert.assertFalse(v.match("/"));
    Assert.assertFalse(v.match("*"));
    Assert.assertFalse(v.match("*"));
    // should not match reserved characters outside ASCII range
    Assert.assertFalse(v.match("\u00e9"));
  }
}
