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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XMLUtilsTest {

  @TempDir
  Path tempDir;

  @Test
  void testGetParser_nonValidating() throws BerliozException {
    SAXParser parser = XMLUtils.getParser(false);
    assertNotNull(parser);
    assertFalse(parser.isValidating());
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
    XMLUtils.parse(handler, new StringReader("<root><child/></root>"), false);
    assertEquals(List.of("root", "child"), elements);
  }

  @Test
  void testParse_reader_invalidXml_throwsBerliozException() {
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class,
        () -> XMLUtils.parse(handler, new StringReader("<unclosed"), false));
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
    XMLUtils.parse(handler, file.toFile(), false);
    assertEquals(List.of("root", "item"), elements);
  }

  @Test
  void testParse_file_notFound_throwsBerliozException() {
    File missing = new File(tempDir.toFile(), "missing.xml");
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class, () -> XMLUtils.parse(handler, missing, false));
  }

  @Test
  void testParse_file_directory_throwsBerliozException() {
    DefaultHandler handler = new DefaultHandler();
    assertThrows(BerliozException.class,
        () -> XMLUtils.parse(handler, tempDir.toFile(), false));
  }
}
