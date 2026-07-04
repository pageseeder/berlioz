package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlCopierTest {

  @TempDir
  Path tempDir;

  @Test
  void testCopyTo_file_success() throws IOException {
    Path file = tempDir.resolve("source.xml");
    Files.writeString(file, "<root><child attr=\"val\">text</child></root>");

    XmlStringBuilder xml = new XmlStringBuilder();
    boolean ok = XmlCopier.copyTo(file.toFile(), xml);

    assertTrue(ok);
    String out = xml.toString();
    assertTrue(out.contains("<root>"), "Should contain root element");
    assertTrue(out.contains("<child"), "Should contain child element");
    assertTrue(out.contains("text"), "Should contain text content");
  }

  @Test
  void testCopyTo_file_notFound_writesErrorElement() {
    File missing = new File(tempDir.toFile(), "missing.xml");
    XmlStringBuilder xml = new XmlStringBuilder();

    boolean ok = XmlCopier.copyTo(missing, xml);

    assertFalse(ok);
    String out = xml.toString();
    assertTrue(out.contains("no-data"), "Should write <no-data> element");
    assertTrue(out.contains("file-not-found"), "Should indicate file-not-found error");
  }

  @Test
  void testCopyTo_file_invalidXml_writesErrorElement() throws IOException {
    Path file = tempDir.resolve("bad.xml");
    Files.writeString(file, "<unclosed");
    XmlStringBuilder xml = new XmlStringBuilder();

    boolean ok = XmlCopier.copyTo(file.toFile(), xml);

    assertFalse(ok);
    String out = xml.toString();
    assertTrue(out.contains("no-data"), "Should write <no-data> on parse error");
  }

  @Test
  void testCopyTo_reader_success() {
    StringReader reader = new StringReader("<doc><p>hello</p></doc>");
    XmlStringBuilder xml = new XmlStringBuilder();

    boolean ok = XmlCopier.copyTo(reader, xml);

    assertTrue(ok);
    String out = xml.toString();
    assertTrue(out.contains("<doc>"), "Should contain doc element");
    assertTrue(out.contains("hello"), "Should contain text");
  }

  @Test
  void testCopyTo_reader_invalidXml_writesErrorElement() {
    StringReader reader = new StringReader("not xml at all!!!");
    XmlStringBuilder xml = new XmlStringBuilder();

    boolean ok = XmlCopier.copyTo(reader, xml);

    assertFalse(ok);
    assertTrue(xml.toString().contains("no-data"), "Should write <no-data> on parse error");
  }

  @Test
  void testCopyTo_preservesAttributes() throws IOException {
    Path file = tempDir.resolve("attrs.xml");
    Files.writeString(file, "<root id=\"123\" name=\"test\"/>");
    XmlStringBuilder xml = new XmlStringBuilder();

    XmlCopier.copyTo(file.toFile(), xml);

    String out = xml.toString();
    assertTrue(out.contains("id=\"123\""), "Should preserve id attribute");
    assertTrue(out.contains("name=\"test\""), "Should preserve name attribute");
  }

  /**
   * A malformed source that opens several elements successfully before failing must not leave
   * any of them dangling on the destination writer: the destination should only ever see the
   * complete document, or the {@code <no-data>} error stub, never a partial fragment.
   */
  @Test
  void testCopyTo_invalidXml_doesNotLeavePartialElementsOnDestination() throws IOException {
    Path file = tempDir.resolve("partial.xml");
    Files.writeString(file, "<root><a>1</a><b>2</b>");

    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("wrapper", true);
    boolean ok = XmlCopier.copyTo(file.toFile(), xml);
    xml.closeElement();

    assertFalse(ok);
    // No exception thrown closing the writer means no element was left dangling open
    assertDoesNotThrow(xml::close);

    String out = xml.toString();
    assertTrue(out.contains("no-data"), "Should write <no-data> on parse error");
    assertFalse(out.contains("<root>"), "Malformed source content must not leak into destination");
    assertFalse(out.contains("<a>"), "Malformed source content must not leak into destination");
  }
}
