package org.pageseeder.berlioz.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class XmlWriterTest {

  @Test
  public void testWriteDocumentThroughInterface() {
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

    Assertions.assertEquals(out.toString(), "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<?xml-stylesheet href=\"style.xsl\"?>"
            + "<!--  comment  -->"
            + "<root a=\"1&lt;&amp;&quot;\" b=\"two\">"
            + "<text>A&amp;B&lt;C&gt;</text>"
            + "<long>42</long>"
            + "<double>3.5</double>"
            + "<empty/>"
            + "<raw><inside/></raw>"
            + "<![CDATA[safe <raw> & text]]>"
            + "</root>");
  }

  @Test
  public void testProcessingInstructionWithoutData() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;

    xml.processingInstruction("target", null);

    Assertions.assertEquals(out.toString(), "<?target?>");
  }

  @Test
  public void testProcessingInstructionRejectsTerminator() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
    XmlWriter xml = new XmlStringBuilder();
    xml.processingInstruction("target", "bad?>data");
    });
  }

  @Test
  public void testAttributeAfterElementContent() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root").text("content").attribute("late", "true");
    });
  }

  @Test
  public void testCloseElementWithoutOpenElement() {
    Assertions.assertThrows(IllegalCloseElementException.class, () -> {
    XmlWriter xml = new XmlStringBuilder();
    xml.closeElement();
    });
  }

  @Test
  public void testCloseWriterWithUnclosedElement() {
    Assertions.assertThrows(UnclosedElementException.class, () -> {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root");
    xml.close();
    });
  }
}
