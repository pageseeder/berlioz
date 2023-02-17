package org.pageseeder.berlioz.util;

import org.junit.Assert;
import org.junit.Test;

public final class VersionsTest {

  @Test(expected = NullPointerException.class)
  public void testCompareNullA() {
    Versions.compare(null, "1");
  }

  @Test(expected = NullPointerException.class)
  public void testCompareNullB() {
    Versions.compare("1", null);
  }

  @Test
  public void testCompareEmpty() {
    assertLatestIsA("1", "");
    assertLatestIsB("", "1");
  }

  @Test
  public void testCompareZeroDot() {
    assertLatestIsA("2", "1");
    assertLatestIsB("1", "2");
    assertEquivalent("1", "1");
  }

  @Test
  public void testCompareOneDot() {
    assertLatestIsA("2.0", "1.0");
    assertLatestIsB("1.0", "2.0");
    assertEquivalent("1.0", "1.0");
    assertLatestIsA("1.1", "1.0");
    assertLatestIsB("1.0", "1.1");
  }

  @Test
  public void testCompareTwoDots() {
    assertLatestIsA("1.0.1", "1.0.0");
    assertLatestIsB("1.0.0", "1.0.1");
    assertLatestIsA("1.1.0", "1.0.1");
    assertLatestIsB("1.0.1", "1.1.0");
    assertLatestIsA("1.1.1", "1.1.0");
    assertLatestIsB("1.1.0", "1.1.1");
  }

  @Test
  public void testCompareAlpha() {
    assertLatestIsA("1.0.beta", "1.0.alpha");
    assertLatestIsB("1.0.alpha", "1.0.beta");
    assertLatestIsA("1.0-alpha2", "1.0-alpha1");
    assertLatestIsB("1.0-alpha1", "1.0-alpha2");
  }

  private static void assertLatestIsA(String a, String b) {
    int compare = Versions.compare(a, b);
    Assert.assertTrue("A '"+a+"' is newer than B '"+b+"'", compare > 0);
  }

  private static void assertLatestIsB(String a, String b) {
    int compare = Versions.compare(a, b);
    Assert.assertTrue("B '"+b+"' is newer than A '"+a+"'", compare < 0);
  }

  private static void assertEquivalent(String a, String b) {
    int compare = Versions.compare(a, b);
    Assert.assertEquals("A and B are equivalent", 0, compare);
  }
}
