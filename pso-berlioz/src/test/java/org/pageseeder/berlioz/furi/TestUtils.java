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
  public static void assertEqualsContract(Object x, Object y, Object z) {
    // reflexive
    Assertions.assertEquals(x, x);
    Assertions.assertEquals(y, y);
    Assertions.assertEquals(z, z);
    // symmetric
    Assertions.assertEquals(x, y);
    Assertions.assertEquals(y, x);
    Assertions.assertNotEquals(x, z);
    Assertions.assertNotEquals(z, x);
    // consistent hashcode
    Assertions.assertEquals(x.hashCode(), x.hashCode());
    Assertions.assertEquals(y.hashCode(), y.hashCode());
    Assertions.assertNotEquals(x.hashCode(), z.hashCode());
    Assertions.assertNotEquals(y.hashCode(), z.hashCode());
    // null is false
    Assertions.assertNotEquals(null, x);
    Assertions.assertNotEquals(null, z);
    // different object is false
    Assertions.assertNotEquals(false, x);
  }

}
