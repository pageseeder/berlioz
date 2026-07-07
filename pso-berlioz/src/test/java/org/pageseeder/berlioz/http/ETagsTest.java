package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ETagsTest {

  // ---------------------------------------------------------------------------
  // isCompressible
  // ---------------------------------------------------------------------------

  @Test
  void testIsCompressible_Null() {
    Assertions.assertFalse(ETags.isCompressible(null));
  }

  @Test
  void testIsCompressible_TextTypes() {
    Assertions.assertTrue(ETags.isCompressible("text/html"));
    Assertions.assertTrue(ETags.isCompressible("text/plain"));
    Assertions.assertTrue(ETags.isCompressible("text/css"));
    Assertions.assertTrue(ETags.isCompressible("text/javascript"));
  }

  @Test
  void testIsCompressible_XmlTypes() {
    Assertions.assertTrue(ETags.isCompressible("application/xml"));
    Assertions.assertTrue(ETags.isCompressible("application/xhtml+xml"));
    Assertions.assertTrue(ETags.isCompressible("image/svg+xml"));
  }

  @Test
  void testIsCompressible_JsonTypes() {
    Assertions.assertTrue(ETags.isCompressible("application/json"));
    Assertions.assertTrue(ETags.isCompressible("application/geo+json"));
  }

  @Test
  void testIsCompressible_JavascriptTypes() {
    Assertions.assertTrue(ETags.isCompressible("application/javascript"));
    Assertions.assertTrue(ETags.isCompressible("application/x-javascript"));
  }

  @Test
  void testIsCompressible_BinaryTypes() {
    Assertions.assertFalse(ETags.isCompressible("image/png"));
    Assertions.assertFalse(ETags.isCompressible("image/jpeg"));
    Assertions.assertFalse(ETags.isCompressible("application/pdf"));
    Assertions.assertFalse(ETags.isCompressible("application/octet-stream"));
  }

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
