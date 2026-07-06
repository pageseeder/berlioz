package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class GenericEntityInfoTest {

  @Test
  void testConstructor_long() {
    GenericEntityInfo info = new GenericEntityInfo(1000L, "text/html", "etag1");
    assertEquals(1000L, info.getLastModified());
    assertEquals("text/html", info.getMimeType());
    assertEquals("etag1", info.getETag());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testConstructor_date() {
    Date date = new Date(5000L);
    GenericEntityInfo info = new GenericEntityInfo(date, "application/json", "abc");
    assertEquals(5000L, info.getLastModified());
    assertEquals("application/json", info.getMimeType());
    assertEquals("abc", info.getETag());
  }

  @Test
  void testNullETag() {
    GenericEntityInfo info = new GenericEntityInfo(0L, "text/plain", null);
    assertNull(info.getETag());
  }
}
