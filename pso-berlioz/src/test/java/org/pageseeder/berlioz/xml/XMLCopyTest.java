package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XMLCopyTest {

  @TempDir
  Path tempDir;

  @Test
  void testCopyTo_file_success() throws IOException {
    Path file = tempDir.resolve("source.xml");
    Files.writeString(file, "<root><child attr=\"val\">text</child></root>");

    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    boolean ok = XMLCopy.copyTo(file.toFile(), xml);

    assertTrue(ok);
    String out = xml.toString();
    assertTrue(out.contains("<root>"), "Should contain root element");
    assertTrue(out.contains("<child"), "Should contain child element");
    assertTrue(out.contains("text"), "Should contain text content");
  }

  @Test
  void testCopyTo_file_notFound_writesErrorElement() throws IOException {
    File missing = new File(tempDir.toFile(), "missing.xml");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    boolean ok = XMLCopy.copyTo(missing, xml);

    assertFalse(ok);
    String out = xml.toString();
    assertTrue(out.contains("no-data"), "Should write <no-data> element");
    assertTrue(out.contains("file-not-found"), "Should indicate file-not-found error");
  }

  @Test
  void testCopyTo_file_invalidXml_writesErrorElement() throws IOException {
    Path file = tempDir.resolve("bad.xml");
    Files.writeString(file, "<unclosed");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    boolean ok = XMLCopy.copyTo(file.toFile(), xml);

    assertFalse(ok);
    String out = xml.toString();
    assertTrue(out.contains("no-data"), "Should write <no-data> on parse error");
  }

  @Test
  void testCopyTo_reader_success() throws IOException {
    StringReader reader = new StringReader("<doc><p>hello</p></doc>");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    boolean ok = XMLCopy.copyTo(reader, xml);

    assertTrue(ok);
    String out = xml.toString();
    assertTrue(out.contains("<doc>"), "Should contain doc element");
    assertTrue(out.contains("hello"), "Should contain text");
  }

  @Test
  void testCopyTo_reader_invalidXml_writesErrorElement() throws IOException {
    StringReader reader = new StringReader("not xml at all!!!");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    boolean ok = XMLCopy.copyTo(reader, xml);

    assertFalse(ok);
    assertTrue(xml.toString().contains("no-data"), "Should write <no-data> on parse error");
  }

  @Test
  void testCopyTo_preservesAttributes() throws IOException {
    Path file = tempDir.resolve("attrs.xml");
    Files.writeString(file, "<root id=\"123\" name=\"test\"/>");
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);

    XMLCopy.copyTo(file.toFile(), xml);

    String out = xml.toString();
    assertTrue(out.contains("id=\"123\""), "Should preserve id attribute");
    assertTrue(out.contains("name=\"test\""), "Should preserve name attribute");
  }
}
