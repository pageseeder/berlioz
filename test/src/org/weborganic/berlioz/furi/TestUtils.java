/*
 * This file is part of the URI Template library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import org.junit.Assert;

/**
 * Utility classes for tests.
 *
 * @author Christophe Lauret
 * @version 30 December 2008
 */
class TestUtils {

  private TestUtils() {
  }

  /**
   * Indicates whether a class satisfies the basic requirements of the <code>equals</code> method
   * contract.
   *
   * @param x An instance of the class to test.
   * @param y An instance of the class to test equal to the first parameter.
   * @param z An instance of the class to test NOT equal to the first parameter.
   */
  public static void satisfyEqualsContract(Object x, Object y, Object z) {
    // reflexive
    Assert.assertTrue(x.equals(x));
    Assert.assertTrue(y.equals(y));
    Assert.assertTrue(z.equals(z));
    // symmetric
    Assert.assertTrue(x.equals(y));
    Assert.assertTrue(y.equals(x));
    Assert.assertFalse(x.equals(z));
    Assert.assertFalse(z.equals(x));
    // consistent hashcode
    Assert.assertEquals(x.hashCode(), x.hashCode());
    Assert.assertEquals(y.hashCode(), y.hashCode());
    Assert.assertTrue(x.hashCode() != z.hashCode());
    Assert.assertTrue(y.hashCode() != z.hashCode());
    // null is false
    Assert.assertFalse(x.equals(null));
    Assert.assertFalse(z.equals(null));
    // different object is false;
    Assert.assertFalse(x.equals(new Object()));
  }

}
