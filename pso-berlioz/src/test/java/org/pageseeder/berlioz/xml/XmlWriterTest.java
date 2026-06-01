package org.pageseeder.berlioz.xml;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

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

    Assert.assertEquals(
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
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
  public void testProcessingInstructionWithoutData() {
    XmlStringBuilder out = new XmlStringBuilder();
    XmlWriter xml = out;

    xml.processingInstruction("target", null);

    Assert.assertEquals("<?target?>", out.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProcessingInstructionRejectsTerminator() {
    XmlWriter xml = new XmlStringBuilder();
    xml.processingInstruction("target", "bad?>data");
  }

  @Test(expected = IllegalStateException.class)
  public void testAttributeAfterElementContent() {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root").text("content").attribute("late", "true");
  }

  @Test(expected = IllegalCloseElementException.class)
  public void testCloseElementWithoutOpenElement() {
    XmlWriter xml = new XmlStringBuilder();
    xml.closeElement();
  }

  @Test(expected = UnclosedElementException.class)
  public void testCloseWriterWithUnclosedElement() {
    XmlWriter xml = new XmlStringBuilder();
    xml.openElement("root");
    xml.close();
  }
}
