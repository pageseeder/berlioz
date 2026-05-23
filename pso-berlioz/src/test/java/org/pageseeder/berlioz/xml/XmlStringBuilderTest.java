package org.pageseeder.berlioz.xml;

import org.junit.Assert;
import org.junit.Test;

public final class XmlStringBuilderTest {

  @Test
  public void testWithIndentKeepsConcreteType() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");

    xml.openElement("root", true)
        .openElement("item")
        .attribute("name", "one")
        .closeElement()
        .closeElement();

    Assert.assertEquals("<root>\n  <item name=\"one\"/>\n</root>", xml.toString());
  }

  @Test
  public void testWithIndentOff() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent(null);

    xml.openElement("root", true).emptyElement("item").closeElement();

    Assert.assertEquals("<root><item/></root>", xml.toString());
  }

  @Test
  public void testToStringReflectsSharedAppendableAfterIndent() {
    XmlStringBuilder original = new XmlStringBuilder();
    XmlStringBuilder indented = original.withIndent(" ");

    indented.openElement("root").closeElement();

    Assert.assertEquals("<root/>", original.toString());
    Assert.assertEquals("<root/>", indented.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testWithIndentRejectsNonSpaceCharacters() {
    new XmlStringBuilder().withIndent(" \t.");
  }

  @Test(expected = IllegalStateException.class)
  public void testWithIndentAfterText() {
    XmlStringBuilder xml = new XmlStringBuilder();

    xml.text("already used");
    xml.withIndent("  ");
  }
}
