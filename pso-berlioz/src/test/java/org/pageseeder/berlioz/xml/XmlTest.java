package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.SAXParser;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class XmlTest {

  @Test
  void testNewWriter_returnsNonNull() {
    XmlWriter writer = Xml.newWriter(new StringWriter());
    assertNotNull(writer);
  }

  @Test
  void testNewWriter_isXmlAppendable() {
    XmlWriter writer = Xml.newWriter(new StringWriter());
    assertInstanceOf(XmlAppendable.class, writer);
  }

  @Test
  void testNewSafeParser_returnsNonNull() throws Exception {
    SAXParser parser = Xml.newSafeParser();
    assertNotNull(parser);
  }

  @Test
  void testNewSafeParser_notValidating() throws Exception {
    SAXParser parser = Xml.newSafeParser(false);
    assertFalse(parser.isValidating());
  }

  @Test
  void testNewSafeParser_namespaceAware() throws Exception {
    SAXParser parser = Xml.newSafeParser(false);
    assertTrue(parser.isNamespaceAware());
  }

  @Test
  void testNewSafeParser_secureProcessing() {
    // should not throw even with security features enabled
    assertDoesNotThrow(() -> Xml.newSafeParser(false));
  }
}
