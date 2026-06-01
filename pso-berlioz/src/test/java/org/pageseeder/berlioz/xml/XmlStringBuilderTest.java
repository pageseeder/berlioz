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

    Assertions.assertEquals(xml.toString(), "<root>\n  <item name=\"one\"/>\n</root>");
  }

  @Test
  void testWithIndentOff() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent(null);

    xml.openElement("root", true).emptyElement("item").closeElement();

    Assertions.assertEquals(xml.toString(), "<root><item/></root>");
  }

  @Test
  void testToStringReflectsSharedAppendableAfterIndent() {
    XmlStringBuilder original = new XmlStringBuilder();
    XmlStringBuilder indented = original.withIndent(" ");

    indented.openElement("root").closeElement();

    Assertions.assertEquals(original.toString(), "<root/>");
    Assertions.assertEquals(indented.toString(), "<root/>");
  }

  @Test
  void testWithIndentRejectsNonSpaceCharacters() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new XmlStringBuilder().withIndent(" \t."));
  }

  @Test
  void testWithIndentAfterText() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("already used");
    xml.withIndent("  ");
    });
  }
}
