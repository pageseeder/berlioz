package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class VersionsTest {

  @Test
  void testCompareNullA() {
    Assertions.assertThrows(NullPointerException.class, () -> Versions.compare(null, "1"));
  }

  @Test
  void testCompareNullB() {
    Assertions.assertThrows(NullPointerException.class, () -> Versions.compare("1", null));
  }

  @Test
  void testCompareEmpty() {
    assertLatestIsA("1", "");
    assertLatestIsB("", "1");
  }

  @Test
  void testCompareZeroDot() {
    assertLatestIsA("2", "1");
    assertLatestIsB("1", "2");
    assertEquivalent("1", "1");
  }

  @Test
  void testCompareOneDot() {
    assertLatestIsA("2.0", "1.0");
    assertLatestIsB("1.0", "2.0");
    assertEquivalent("1.0", "1.0");
    assertLatestIsA("1.1", "1.0");
    assertLatestIsB("1.0", "1.1");
  }

  @Test
  void testCompareTwoDots() {
    assertLatestIsA("1.0.1", "1.0.0");
    assertLatestIsB("1.0.0", "1.0.1");
    assertLatestIsA("1.1.0", "1.0.1");
    assertLatestIsB("1.0.1", "1.1.0");
    assertLatestIsA("1.1.1", "1.1.0");
    assertLatestIsB("1.1.0", "1.1.1");
  }

  @Test
  void testCompareAlpha() {
    assertLatestIsA("1.0.beta", "1.0.alpha");
    assertLatestIsB("1.0.alpha", "1.0.beta");
    assertLatestIsA("1.0-alpha2", "1.0-alpha1");
    assertLatestIsB("1.0-alpha1", "1.0-alpha2");
  }

  private static void assertLatestIsA(String a, String b) {
    int compare = Versions.compare(a, b);
    Assertions.assertTrue(compare > 0, "A '"+a+"' is newer than B '"+b+"'");
  }

  private static void assertLatestIsB(String a, String b) {
    int compare = Versions.compare(a, b);
    Assertions.assertTrue(compare < 0, "B '"+b+"' is newer than A '"+a+"'");
  }

  private static void assertEquivalent(String a, String b) {
    int compare = Versions.compare(a, b);
    Assertions.assertEquals(0, compare, "A and B are equivalent");
  }
}
