package org.pageseeder.berlioz.http;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpAcceptHeaderTest {

  @Test
  void testGet_NullOrEmpty() {
    Assertions.assertTrue(HttpAcceptHeader.get(null).isEmpty());
    Assertions.assertTrue(HttpAcceptHeader.get("").isEmpty());
  }

  @Test
  void testGet_ParseQualityValues() {
    Map<String, Float> accept = HttpAcceptHeader.get("text/html,application/xml;q=0.9,*/*;q=0.1");
    Assertions.assertEquals(Float.valueOf(1.0f), accept.get("text/html"));
    Assertions.assertEquals(Float.valueOf(0.9f), accept.get("application/xml"));
    Assertions.assertEquals(Float.valueOf(0.1f), accept.get("*/*"));
  }

  @Test
  void testGet_ReturnsUnmodifiableMap() {
    Assertions.assertThrows(UnsupportedOperationException.class, () -> HttpAcceptHeader.get("text/html").put("application/json", 1.0f));
  }

  @Test
  void testAccepts_ExactValue() {
    Assertions.assertTrue(HttpAcceptHeader.accepts("text/html", "text/html"));
    Assertions.assertFalse(HttpAcceptHeader.accepts("text/html;q=0", "text/html"));
  }

  @Test
  void testAccepts_Wildcards() {
    Assertions.assertTrue(HttpAcceptHeader.accepts("*/*;q=0.5", "image/png"));
    Assertions.assertTrue(HttpAcceptHeader.accepts("text/*;q=0.5", "text/plain"));
    Assertions.assertTrue(HttpAcceptHeader.accepts("*;q=0.5", "br"));
    Assertions.assertFalse(HttpAcceptHeader.accepts("text/*;q=0", "text/plain"));
  }

  @Test
  void testAccepts_Map() {
    Map<String, Float> accept = HttpAcceptHeader.get("application/json;q=0.7");
    Assertions.assertTrue(HttpAcceptHeader.accepts(accept, "application/json"));
    Assertions.assertFalse(HttpAcceptHeader.accepts(accept, "application/xml"));
  }

}
