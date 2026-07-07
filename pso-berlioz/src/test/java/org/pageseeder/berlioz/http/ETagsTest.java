package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ETagsTest {

  // ---------------------------------------------------------------------------
  // getETagForGZip
  // ---------------------------------------------------------------------------

  @Test
  void testGetETagForGZip_Null() {
    Assertions.assertNull(ETags.getETagForGZip(null));
  }

  @Test
  void testGetETagForGZip_Normal() {
    Assertions.assertEquals("\"abc-gzip\"", ETags.getETagForGZip("\"abc\""));
  }

  @Test
  void testGetETagForGZip_Hash() {
    Assertions.assertEquals("\"a1b2c3-gzip\"", ETags.getETagForGZip("\"a1b2c3\""));
  }

  @Test
  void testGetETagForGZip_NoQuotes() {
    // ETag with no double-quote at all (lastIndexOf returns -1): returned unchanged
    Assertions.assertEquals("abc", ETags.getETagForGZip("abc"));
  }

  // ---------------------------------------------------------------------------
  // getETagForUncompressed
  // ---------------------------------------------------------------------------

  @Test
  void testGetETagForUncompressed_Null() {
    Assertions.assertNull(ETags.getETagForUncompressed(null));
  }

  @Test
  void testGetETagForUncompressed_GZipETag() {
    Assertions.assertEquals("\"abc\"", ETags.getETagForUncompressed("\"abc-gzip\""));
  }

  @Test
  void testGetETagForUncompressed_Hash() {
    Assertions.assertEquals("\"a1b2c3\"", ETags.getETagForUncompressed("\"a1b2c3-gzip\""));
  }

  @Test
  void testGetETagForUncompressed_PlainETag() {
    // No GZip suffix: returned unchanged
    Assertions.assertEquals("\"abc\"", ETags.getETagForUncompressed("\"abc\""));
  }

  @Test
  void testGetETagForUncompressed_RoundTrip() {
    // getETagForGZip and getETagForUncompressed should be inverses of each other
    String original = "\"a1b2c3\"";
    Assertions.assertEquals(original, ETags.getETagForUncompressed(ETags.getETagForGZip(original)));
  }

}
