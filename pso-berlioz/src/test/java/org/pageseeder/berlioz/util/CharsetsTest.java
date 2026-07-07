package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CharsetsTest {

  @Test
  void testLength_emptyString() {
    assertEquals(0, Charsets.length("", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_asciiInUtf8() {
    assertEquals(5, Charsets.length("Hello", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_asciiInIso88591() {
    assertEquals(5, Charsets.length("Hello", StandardCharsets.ISO_8859_1));
  }

  @Test
  void testLength_multibyte_utf8() {
    // "é" is 2 bytes in UTF-8
    assertEquals(2, Charsets.length("é", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_multibyte_utf16() {
    // UTF-16 uses 2 bytes per BMP char + 2 byte BOM = 4 bytes for "Hi"
    int len = Charsets.length("Hi", StandardCharsets.UTF_16);
    assertTrue(len > 2, "UTF-16 should use more bytes than ASCII chars");
  }

  @Test
  void testLength_surrogatePair_utf8() {
    // A surrogate pair (e.g. an emoji) encodes to 4 bytes in UTF-8
    assertEquals(4, Charsets.length("😀", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_unpairedSurrogate_utf8() {
    assertEquals(-1, Charsets.length("\uD83D", StandardCharsets.UTF_8));
  }

  @Test
  void testLength_nullContentThrows() {
    assertThrows(NullPointerException.class,
        () -> Charsets.length(null, StandardCharsets.UTF_8));
  }

  @Test
  void testLength_nullCharsetThrows() {
    assertThrows(NullPointerException.class,
        () -> Charsets.length("hello", (Charset) null));
  }
}
