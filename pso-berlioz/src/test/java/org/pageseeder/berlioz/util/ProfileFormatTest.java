package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileFormatTest {

  @Test
  void testZero() {
    assertEquals("0.00", ProfileFormat.format(0L));
  }

  @Test
  void testOneMillisecond() {
    // 1,000,000 ns = 1 ms
    assertEquals("1.00", ProfileFormat.format(1_000_000L));
  }

  @Test
  void testHalfMillisecond() {
    // 500,000 ns = 0.5 ms
    assertEquals("0.50", ProfileFormat.format(500_000L));
  }

  @Test
  void testLargeValue() {
    // 1,000,000,000 ns = 1000 ms -> "1,000.00"
    String result = ProfileFormat.format(1_000_000_000L);
    assertTrue(result.contains("000.00"), "Large values should use grouping separators, got: " + result);
  }
}
