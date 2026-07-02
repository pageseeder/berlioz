package org.pageseeder.berlioz.xml;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class XmlWriterTest {

  @Test
  void testWriteDocumentThroughInterface() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;
    Map<String, String> attributes = new LinkedHashMap<>();
    attributes.put("a", "1<&\"");
    attributes.put("b", "two");

    xml.declaration();
    xml.processingInstruction("xml-stylesheet", "href=\"style.xsl\"");
    xml.comment(" comment ");
    xml.openElement("root", true).attributes(attributes);
    xml.element("text", "A&B<C>");
    xml.element("long", 42L);
    xml.element("double", 3.5d);
    xml.emptyElement("empty");
    xml.openElement("raw").xml("<inside/>").closeElement();
    xml.cdata("safe <raw> & text");
    xml.closeElement();
    xml.close();

    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<?xml-stylesheet href=\"style.xsl\"?>"
        + "<!--  comment  -->"
        + "<root a=\"1&lt;&amp;&quot;\" b=\"two\">"
        + "<text>A&amp;B&lt;C&gt;</text>"
        + "<long>42</long>"
        + "<double>3.5</double>"
        + "<empty/>"
        + "<raw><inside/></raw>"
        + "<![CDATA[safe <raw> & text]]>"
        + "</root>",
        out.toString());
  }

  @Test
  void testProcessingInstructionWithoutData() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", null);
    Assertions.assertEquals("<?target?>", xml.toString());
  }

  @Test
  void testProcessingInstructionRejectsTerminator() {
    XmlWriter xml = new XmlStringBuilder();
    Assertions.assertThrows(IllegalArgumentException.class, () ->
        xml.processingInstruction("target", "bad?>data")
    );
  }

  @Test
  void testAttributeAfterElementContent() {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root").text("content");
    Assertions.assertThrows(IllegalStateException.class, () ->
        xml.attribute("late", "true"));
  }

  @Test
  void testCloseElementWithoutOpenElement() {
    XmlWriter xml = new XmlStringBuilder();
    Assertions.assertThrows(IllegalCloseElementException.class, xml::closeElement);
  }

  @Test
  void testCloseWriterWithUnclosedElement() {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root");
    Assertions.assertThrows(UnclosedElementException.class, xml::close);
  }

  // asXml(XmlWritable) / asXml(Iterable) -------------------------------------------------------

  @Test
  void testAsXmlSingleWritable() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;
    XmlWritable writable = w -> w.emptyElement("item");
    xml.openElement("root").asXml(writable).closeElement();
    Assertions.assertEquals("<root><item/></root>", out.toString());
  }

  @Test
  void testAsXmlSingleWritableReturnsThis() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWritable writable = w -> w.emptyElement("x");
    out.openElement("root");
    Assertions.assertSame(out, out.asXml(writable));
    out.closeElement();
  }

  @Test
  void testAsXmlIterableWritables() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;
    List<XmlWritable> items = List.of(
        w -> w.element("a", "1"),
        w -> w.element("b", "2")
    );
    xml.openElement("root").asXml(items).closeElement();
    Assertions.assertEquals("<root><a>1</a><b>2</b></root>", out.toString());
  }

  @Test
  void testAsXmlIterableEmpty() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;
    xml.openElement("root").asXml(List.<XmlWritable>of()).closeElement();
    Assertions.assertEquals("<root/>", out.toString());
  }

  @Test
  void testAsXmlIterableReturnsThis() {
    XmlStringBuilder out = new XmlStringBuilder();
    out.openElement("root");
    Assertions.assertSame(out, out.asXml(List.<XmlWritable>of()));
    out.closeElement();
  }
}
