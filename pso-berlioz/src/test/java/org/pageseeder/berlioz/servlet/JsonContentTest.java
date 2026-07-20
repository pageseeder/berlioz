package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonContentTest {

  @Test
  void testContent() {
    JsonContent c = new JsonContent("{}");
    assertEquals("{}", c.content().toString());
  }

  @Test
  void testGetMediaType() {
    assertEquals("application/json", new JsonContent("").getMediaType());
  }

  @Test
  void testGetMediaTypeOverride() {
    assertEquals("application/problem+json", new JsonContent("", "application/problem+json").getMediaType());
  }

  @Test
  void testGetEncoding() {
    assertEquals("UTF-8", new JsonContent("").getEncoding());
  }

  @Test
  void testImplementsBerliozOutput() {
    assertInstanceOf(BerliozOutput.class, new JsonContent("data"));
  }

  @Test
  void testEmptyContent() {
    JsonContent c = new JsonContent("");
    assertEquals("", c.content().toString());
  }
}
