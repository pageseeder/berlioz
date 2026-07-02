package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class XmlContentTest {

  @Test
  void testContent() {
    XmlContent c = new XmlContent("<root/>");
    assertEquals("<root/>", c.content().toString());
  }

  @Test
  void testGetMediaType() {
    assertEquals("application/xml", new XmlContent("").getMediaType());
  }

  @Test
  void testGetEncoding() {
    assertEquals("UTF-8", new XmlContent("").getEncoding());
  }

  @Test
  void testImplementsBerliozOutput() {
    assertInstanceOf(BerliozOutput.class, new XmlContent("data"));
  }

  @Test
  void testEmptyContent() {
    XmlContent c = new XmlContent("");
    assertEquals("", c.content().toString());
  }
}
