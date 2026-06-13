package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;

final class XmlWriterAdapterTest {

  // Delegation -----------------------------------------------------------------------------------

  @Test
  void testOpenCloseElement() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.closeElement();
    Assertions.assertEquals("<root/>", out.toString());
  }

  @Test
  void testOpenElementWithHasChildren() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root", true);
    adapter.openElement("child", false);
    adapter.closeElement();
    adapter.closeElement();
    Assertions.assertEquals("<root><child/></root>", out.toString());
  }

  @Test
  void testElement() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.element("item", "hello");
    Assertions.assertEquals("<item>hello</item>", out.toString());
  }

  @Test
  void testEmptyElement() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.emptyElement("br");
    Assertions.assertEquals("<br/>", out.toString());
  }

  @Test
  void testAttributeString() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.attribute("id", "42");
    adapter.closeElement();
    Assertions.assertEquals("<root id=\"42\"/>", out.toString());
  }

  @Test
  void testAttributeInt() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.attribute("count", 7);
    adapter.closeElement();
    Assertions.assertEquals("<root count=\"7\"/>", out.toString());
  }

  @Test
  void testAttributeLong() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.attribute("size", 1_000_000_000L);
    adapter.closeElement();
    Assertions.assertEquals("<root size=\"1000000000\"/>", out.toString());
  }

  @Test
  void testWriteText() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeText("hello & world");
    adapter.closeElement();
    Assertions.assertEquals("<root>hello &amp; world</root>", out.toString());
  }

  @Test
  void testWriteTextNull() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeText((String) null);
    adapter.closeElement();
    Assertions.assertEquals("<root/>", out.toString());
  }

  @Test
  void testWriteTextChar() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeText('X');
    adapter.closeElement();
    Assertions.assertEquals("<root>X</root>", out.toString());
  }

  @Test
  void testWriteTextCharArray() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeText(new char[]{'a', 'b', 'c'}, 1, 2);
    adapter.closeElement();
    Assertions.assertEquals("<root>bc</root>", out.toString());
  }

  @Test
  void testWriteXML() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeXML("<child/>");
    adapter.closeElement();
    Assertions.assertEquals("<root><child/></root>", out.toString());
  }

  @Test
  void testWriteXMLNull() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeXML((String) null);
    adapter.closeElement();
    Assertions.assertEquals("<root/>", out.toString());
  }

  @Test
  void testWriteXMLCharArray() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeXML(new char[]{'<', 'x', '/', '>'}, 0, 4);
    adapter.closeElement();
    Assertions.assertEquals("<root><x/></root>", out.toString());
  }

  @Test
  void testWriteCDATA() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.writeCDATA("raw <content>");
    adapter.closeElement();
    Assertions.assertEquals("<root><![CDATA[raw <content>]]></root>", out.toString());
  }

  @Test
  void testWriteComment() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.writeComment(" hello ");
    Assertions.assertEquals("<!--  hello  -->", out.toString());
  }

  @Test
  void testWritePI() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.writePI("target", "data");
    Assertions.assertEquals("<?target data?>", out.toString());
  }

  @Test
  void testXmlDecl() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.xmlDecl();
    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>", out.toString());
  }

  // close() is a no-op ---------------------------------------------------------------------------

  @Test
  void testCloseDoesNotCloseUnderlyingWriter() throws IOException {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    adapter.openElement("root");
    adapter.close();
    // underlying writer is still open and usable
    out.closeElement();
    Assertions.assertEquals("<root/>", out.toString());
  }

  // Unsupported namespace-aware methods ----------------------------------------------------------

  @Test
  void testOpenElementWithUriThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.openElement("http://example.com/ns", "root", false));
  }

  @Test
  void testEmptyElementWithUriThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.emptyElement("http://example.com/ns", "br"));
  }

  @Test
  void testAttributeWithUriStringThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    out.openElement("root");
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.attribute("http://example.com/ns", "id", "val"));
  }

  @Test
  void testAttributeWithUriIntThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    out.openElement("root");
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.attribute("http://example.com/ns", "count", 1));
  }

  @Test
  void testAttributeWithUriLongThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    out.openElement("root");
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.attribute("http://example.com/ns", "count", 1L));
  }

  @Test
  void testSetPrefixMappingThrows() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWriter adapter = new XmlWriterAdapter(out);
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> adapter.setPrefixMapping("http://example.com/ns", "ex"));
  }

  // asXml(XMLWritable) on XmlWriter --------------------------------------------------------------

  @Test
  void testAsXmlLegacyWritable() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWritable writable = xml -> {
      xml.openElement("item");
      xml.attribute("id", "1");
      xml.writeText("value");
      xml.closeElement();
    };
    out.openElement("root").asXml(writable).closeElement();
    Assertions.assertEquals("<root><item id=\"1\">value</item></root>", out.toString());
  }

  @Test
  void testAsXmlLegacyWritableReturnsThis() {
    XmlStringBuilder out = new XmlStringBuilder();
    XMLWritable writable = xml -> xml.emptyElement("x");
    out.openElement("root");
    Assertions.assertSame(out, out.asXml(writable));
    out.closeElement();
  }

  @Test
  void testAsXmlLegacyWritableIOExceptionWrapped() {
    XmlStringBuilder out = new XmlStringBuilder();
    IOException original = new IOException("disk full");
    XMLWritable failing = xml -> { throw original; };
    out.openElement("root");
    XmlWriteFailureException ex = Assertions.assertThrows(
        XmlWriteFailureException.class, () -> out.asXml(failing));
    Assertions.assertSame(original, ex.getCause());
  }
}
