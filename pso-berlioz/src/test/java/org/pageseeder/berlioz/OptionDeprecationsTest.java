package org.pageseeder.berlioz;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class OptionDeprecationsTest {

  private static final String CONTROL_NETWORK_PROPERTY = BerliozOption.CONTROL_NETWORK.property();

  @Test
  void testCheckAll_controlNetworkLoopback_warnsOnce() {
    String output = captureStderr(() ->
        OptionDeprecations.checkAll(Map.of(CONTROL_NETWORK_PROPERTY, "loopback")));
    assertEquals(1, countOccurrences(output, CONTROL_NETWORK_PROPERTY));
  }

  @Test
  void testCheckAll_controlNetworkLan_warnsOnce() {
    String output = captureStderr(() ->
        OptionDeprecations.checkAll(Map.of(CONTROL_NETWORK_PROPERTY, "lan")));
    assertEquals(1, countOccurrences(output, CONTROL_NETWORK_PROPERTY));
  }

  @Test
  void testCheckAll_controlNetworkOff_noWarning() {
    String output = captureStderr(() ->
        OptionDeprecations.checkAll(Map.of(CONTROL_NETWORK_PROPERTY, "off")));
    assertFalse(output.contains(CONTROL_NETWORK_PROPERTY));
  }

  @Test
  void testCheckAll_controlNetworkNotConfigured_noWarning() {
    String output = captureStderr(() -> OptionDeprecations.checkAll(Map.of()));
    assertFalse(output.contains(CONTROL_NETWORK_PROPERTY));
  }

  private static int countOccurrences(String haystack, String needle) {
    Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(haystack);
    int count = 0;
    while (matcher.find()) count++;
    return count;
  }

  private static String captureStderr(Runnable action) {
    PrintStream original = System.err;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    System.setErr(new PrintStream(buffer, true, StandardCharsets.UTF_8));
    try {
      action.run();
    } finally {
      System.setErr(original);
    }
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
