package org.pageseeder.berlioz.http;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class HttpAcceptHeaderTest {

  @Test
  public void testGet_NullOrEmpty() {
    Assert.assertTrue(HttpAcceptHeader.get(null).isEmpty());
    Assert.assertTrue(HttpAcceptHeader.get("").isEmpty());
  }

  @Test
  public void testGet_ParseQualityValues() {
    Map<String, Float> accept = HttpAcceptHeader.get("text/html,application/xml;q=0.9,*/*;q=0.1");
    Assert.assertEquals(Float.valueOf(1.0f), accept.get("text/html"));
    Assert.assertEquals(Float.valueOf(0.9f), accept.get("application/xml"));
    Assert.assertEquals(Float.valueOf(0.1f), accept.get("*/*"));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGet_ReturnsUnmodifiableMap() {
    HttpAcceptHeader.get("text/html").put("application/json", 1.0f);
  }

  @Test
  public void testAccepts_ExactValue() {
    Assert.assertTrue(HttpAcceptHeader.accepts("text/html", "text/html"));
    Assert.assertFalse(HttpAcceptHeader.accepts("text/html;q=0", "text/html"));
  }

  @Test
  public void testAccepts_Wildcards() {
    Assert.assertTrue(HttpAcceptHeader.accepts("*/*;q=0.5", "image/png"));
    Assert.assertTrue(HttpAcceptHeader.accepts("text/*;q=0.5", "text/plain"));
    Assert.assertTrue(HttpAcceptHeader.accepts("*;q=0.5", "br"));
    Assert.assertFalse(HttpAcceptHeader.accepts("text/*;q=0", "text/plain"));
  }

  @Test
  public void testAccepts_Map() {
    Map<String, Float> accept = HttpAcceptHeader.get("application/json;q=0.7");
    Assert.assertTrue(HttpAcceptHeader.accepts(accept, "application/json"));
    Assert.assertFalse(HttpAcceptHeader.accepts(accept, "application/xml"));
  }

}
