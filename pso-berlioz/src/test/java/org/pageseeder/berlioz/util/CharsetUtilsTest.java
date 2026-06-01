package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CharsetUtilsTest {

  @Test
  void testLength_emptyString() {
    assertEquals(0, CharsetUtils.length("", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_asciiInUtf8() {
    assertEquals(5, CharsetUtils.length("Hello", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_asciiInIso88591() {
    assertEquals(5, CharsetUtils.length("Hello", StandardCharsets.ISO_8859_1));
  }

  @Test
  void testLength_multibyte_utf8() {
    // "é" is 2 bytes in UTF-8
    assertEquals(2, CharsetUtils.length("é", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_multibyte_utf16() {
    // UTF-16 uses 2 bytes per BMP char + 2 byte BOM = 4 bytes for "Hi"
    int len = CharsetUtils.length("Hi", StandardCharsets.UTF_16);
    assertTrue(len > 2, "UTF-16 should use more bytes than ASCII chars");
  }

  @Test
  void testLength_nullContentThrows() {
    assertThrows(NullPointerException.class,
        () -> CharsetUtils.length(null, StandardCharsets.UTF_8));
  }

  @Test
  void testLength_nullCharsetThrows() {
    assertThrows(NullPointerException.class,
        () -> CharsetUtils.length("hello", (Charset) null));
  }
}
