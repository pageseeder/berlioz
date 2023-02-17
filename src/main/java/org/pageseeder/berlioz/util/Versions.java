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
package org.pageseeder.berlioz.util;

/**
 * Utility class for versions.
 *
 * @author Christophe Lauret
 * @version 0.9.26
 * @since 0.9.26
 */
public final class Versions {

  /**
   * Utility class.
   */
  private Versions() {
  }

  /**
   * Compares two versions.
   *
   * <p>This method decomposes the versions into components assuming they are separated
   * by '.', and compare them in order. If possible the comparison is numeric, otherwise it
   * is alphanumeric. The first comparison resulting in a non equal result will be the
   * result of this function.
   *
   * @param versionA The first version to compare.
   * @param versionB The second version to compare.
   *
   * @return a positive number if version A is considered greater than version B;
   *         a negative number if version B is considered greater than version A;
   *         zero if the versions are considered equivalent.
   */
  public static int compare(String versionA, String versionB) {
    String[] atomsA = versionA.split("\\.");
    String[] atomsB = versionB.split("\\.");
    int atomCount = Math.max(atomsA.length, atomsB.length);
    for (int i = 0; i < atomCount; i++) {
      String a = atomsA.length > i? atomsA[i] : "0";
      String b = atomsB.length > i? atomsB[i] : "0";
      int compare;
      if (a.matches("[0-9]+") && b.matches("[0-9]+")) {
        try {
          Long al = Long.valueOf(a);
          Long bl = Long.valueOf(b);
          compare = al.compareTo(bl);
        } catch (NumberFormatException ex) {
          compare = a.compareTo(b);
        }
      } else {
        compare = a.compareTo(b);
      }
      if (compare != 0) return compare;
    }
    return 0;
  }

}
