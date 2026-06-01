package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class XmlStringBuilderTest {

  @Test
  void testWithIndentKeepsConcreteType() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");

    xml.openElement("root", true)
        .openElement("item")
        .attribute("name", "one")
        .closeElement()
        .closeElement();

    Assertions.assertEquals("<root>\n  <item name=\"one\"/>\n</root>", xml.toString());
  }

  @Test
  void testWithIndentOff() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent(null);

    xml.openElement("root", true).emptyElement("item").closeElement();

    Assertions.assertEquals("<root><item/></root>", xml.toString());
  }

  @Test
  void testToStringReflectsSharedAppendableAfterIndent() {
    XmlStringBuilder original = new XmlStringBuilder();
    XmlStringBuilder indented = original.withIndent(" ");

    indented.openElement("root").closeElement();

    Assertions.assertEquals("<root/>", original.toString());
    Assertions.assertEquals("<root/>", indented.toString());
  }

  @Test
  void testWithIndentRejectsNonSpaceCharacters() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Assertions.assertThrows(IllegalArgumentException.class, () -> xml.withIndent(" \t."));
  }

  @Test
  void testWithIndentAfterText() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("already used");
    Assertions.assertThrows(IllegalStateException.class, () ->
        xml.withIndent("  ")
    );
  }
}
