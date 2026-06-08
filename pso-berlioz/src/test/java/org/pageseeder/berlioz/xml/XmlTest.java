package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.BerliozException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmlTest {

  @TempDir
  Path tempDir;

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

  @Test
  void testParse_reader_simpleXml() throws BerliozException {
    List<String> elements = new ArrayList<>();
    DefaultHandler handler = new DefaultHandler() {
      @Override
      public void startElement(String uri, String localName, String qName, Attributes atts) {
        elements.add(localName);
      }
    };
    Xml.parse(handler, new StringReader("<root><child/></root>"), false);
    assertEquals(List.of("root", "child"), elements);
  }

  @Test
  void testParse_reader_invalidXml_throwsBerliozException() {
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class,
        () -> Xml.parse(handler, new StringReader("<unclosed"), false));
  }

  @Test
  void testParse_file_simpleXml() throws IOException, BerliozException {
    Path file = tempDir.resolve("test.xml");
    Files.writeString(file, "<root><item/></root>");
    List<String> elements = new ArrayList<>();
    DefaultHandler handler = new DefaultHandler() {
      @Override
      public void startElement(String uri, String localName, String qName, Attributes atts) {
        elements.add(localName);
      }
    };
    Xml.parse(handler, file.toFile(), false);
    assertEquals(List.of("root", "item"), elements);
  }

  @Test
  void testParse_file_notFound_throwsBerliozException() {
    File missing = new File(tempDir.toFile(), "missing.xml");
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class, () -> Xml.parse(handler, missing, false));
  }

  @Test
  void testParse_file_directory_throwsBerliozException() {
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class,
        () -> Xml.parse(handler, tempDir.toFile(), false));
  }

  // isXmlMediaType

  @Test
  void testIsXmlMediaType_null() {
    assertFalse(Xml.isXmlMediaType(null));
  }

  @Test
  void testIsXmlMediaType_applicationXml() {
    assertTrue(Xml.isXmlMediaType("application/xml"));
  }

  @Test
  void testIsXmlMediaType_textXml() {
    assertTrue(Xml.isXmlMediaType("text/xml"));
  }

  @Test
  void testIsXmlMediaType_caseInsensitive() {
    assertTrue(Xml.isXmlMediaType("Application/XML"));
  }

  @Test
  void testIsXmlMediaType_plusXmlSuffix() {
    assertTrue(Xml.isXmlMediaType("application/atom+xml"));
  }

  @Test
  void testIsXmlMediaType_rssXml() {
    assertTrue(Xml.isXmlMediaType("application/rss+xml"));
  }

  @Test
  void testIsXmlMediaType_applicationJson() {
    assertFalse(Xml.isXmlMediaType("application/json"));
  }

  @Test
  void testIsXmlMediaType_textHtml() {
    assertFalse(Xml.isXmlMediaType("text/html"));
  }

  @Test
  void testIsXmlMediaType_empty() {
    assertFalse(Xml.isXmlMediaType(""));
  }

  @Test
  void testIsXmlMediaType_parametersNotStripped() {
    // parameters are a separate concern; callers must strip them first
    assertFalse(Xml.isXmlMediaType("application/xml;charset=utf-8"));
  }
}
