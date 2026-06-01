package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XMLContentTest {

  @Test
  void testContent() {
    XMLContent c = new XMLContent("<root/>");
    assertEquals("<root/>", c.content().toString());
  }

  @Test
  void testGetMediaType() {
    assertEquals("application/xml", new XMLContent("").getMediaType());
  }

  @Test
  void testGetEncoding() {
    assertEquals("utf-8", new XMLContent("").getEncoding());
  }

  @Test
  void testImplementsBerliozOutput() {
    assertInstanceOf(BerliozOutput.class, new XMLContent("data"));
  }

  @Test
  void testEmptyContent() {
    XMLContent c = new XMLContent("");
    assertEquals("", c.content().toString());
  }
}
