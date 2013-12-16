/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

/**
 * Utility class for versions.
 *
 * @author Christophe Lauret
 * @version 0.9.26 - 16 December 2013
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
      int compare = 0;
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
