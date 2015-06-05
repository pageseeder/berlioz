/*
 * This file is part of the URI Template library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * A test class for the <code>TokenBase</code> class.
 * <p>
 * Note: this class uses a basic implementation of the abstract <code>TokenBase</code> for testing.
 *
 * @author Christophe Lauret
 * @version 31 December 2008
 */
public final class TokenBaseTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> expression.
   */
  @Test
  public void testNew_Null() {
    boolean nullThrown = false;
    try {
      new TokenImpl(null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assert.assertTrue(nullThrown);
    }
  }

  /**
   * Test the <code>equal</code> method.
   */
  @Test
  public void testEquals() {
    TokenImpl x = new TokenImpl("t");
    TokenImpl y = new TokenImpl("t");
    TokenImpl z = new TokenImpl("T");
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * The most basic implementation of a Token, simply for testing.
   */
  static class TokenImpl extends TokenBase {
    public TokenImpl(String exp) {
      super(exp);
    }

    @Override
    public String expand(Parameters variables) {
      return "";
    }

    @Override
    public boolean resolve(String expanded, Map<Variable, Object> values) {
      return false;
    }
  }
}
