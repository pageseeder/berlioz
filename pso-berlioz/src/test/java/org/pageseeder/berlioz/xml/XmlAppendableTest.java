package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

final class XmlAppendableTest {

  // text(String) ---------------------------------------------------------------------------------

  @Test
  void testTextEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a<>&\"'😀").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>a&lt;&gt;&amp;\"'&#x1f600;</x>");
  }

  @Test
  void testTextEscapingWithCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] text = "xx😀<&yy".toCharArray();
    xml.openElement("x").text(text, 2, 4).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&#x1f600;&lt;&amp;</x>");
  }

  @Test
  void testTextNullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").text((String) null));
  }

  // text(long) / text(double) / text(char) -------------------------------------------------------

  @Test
  void testTextLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(42L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  void testTextLongNegative() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(-1L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>-1</x>");
  }

  @Test
  void testTextDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(3.14).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>3.14</x>");
  }

  @Test
  void testTextCharPlain() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('a').closeElement();
    Assertions.assertEquals(xml.toString(), "<x>a</x>");
  }

  @Test
  void testTextCharEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('<').closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&lt;</x>");
  }

  // xml(char[], int, int) ------------------------------------------------------------------------

  @Test
  void testXmlCharArray() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "<y/>".toCharArray();
    xml.openElement("x").xml(raw, 0, raw.length).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  @Test
  void testXmlCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "xx<y/>zz".toCharArray();
    xml.openElement("x").xml(raw, 2, 4).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  // attribute(String, *) -------------------------------------------------------------------------

  @Test
  void testAttributeEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("a", "<&\"'😀").closeElement();
    Assertions.assertEquals(xml.toString(), "<x a=\"&lt;&amp;&quot;&#39;&#x1f600;\"/>");
  }

  @Test
  void testAttributeLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 42L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x n=\"42\"/>");
  }

  @Test
  void testAttributeDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 3.14).closeElement();
    Assertions.assertEquals(xml.toString(), "<x n=\"3.14\"/>");
  }

  @Test
  void testAttributeBooleanTrue() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", true).closeElement();
    Assertions.assertEquals(xml.toString(), "<x flag=\"true\"/>");
  }

  @Test
  void testAttributeBooleanFalse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", false).closeElement();
    Assertions.assertEquals(xml.toString(), "<x flag=\"false\"/>");
  }

  @Test
  void testAttributeNullNameThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").attribute(null, "v"));
  }

  @Test
  void testAttributeNullValueThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").attribute("n", (String) null));
  }

  // attributes(Map) ------------------------------------------------------------------------------

  @Test
  void testAttributesMap() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put("a", "1");
    attrs.put("b", "two");
    xml.openElement("x").attributes(attrs).closeElement();
    Assertions.assertEquals(xml.toString(), "<x a=\"1\" b=\"two\"/>");
  }

  // comment() ------------------------------------------------------------------------------------

  @Test
  void testComment() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.comment("hello");
    Assertions.assertEquals(xml.toString(), "<!-- hello -->");
  }

  @Test
  void testCommentWithDoubleDashThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new XmlStringBuilder().comment("bad--comment"));
  }

  // cdata() --------------------------------------------------------------------------------------

  @Test
  void testCdata() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").cdata("safe text").closeElement();
    Assertions.assertEquals(xml.toString(), "<x><![CDATA[safe text]]></x>");
  }

  @Test
  void testCdataWithEndSequenceThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new XmlStringBuilder().cdata("bad]]>data"));
  }

  // processingInstruction() ----------------------------------------------------------------------

  @Test
  void testProcessingInstruction() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "data");
    Assertions.assertEquals(xml.toString(), "<?target data?>");
  }

  @Test
  void testProcessingInstructionEmptyData() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "");
    Assertions.assertEquals(xml.toString(), "<?target?>");
  }

  @Test
  void testProcessingInstructionNullTargetThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().processingInstruction(null, "data"));
  }

  // declaration() --------------------------------------------------------------------------------

  @Test
  void testDeclaration() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.declaration();
    Assertions.assertEquals(xml.toString(), "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
  }

  @Test
  void testDeclarationAfterUse() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("x");
    xml.declaration();
    });
  }

  // emptyElement() -------------------------------------------------------------------------------

  @Test
  void testEmptyElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.emptyElement("br");
    Assertions.assertEquals(xml.toString(), "<br/>");
  }

  // element() shortcuts --------------------------------------------------------------------------

  @Test
  void testElementString() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "hello");
    Assertions.assertEquals(xml.toString(), "<x>hello</x>");
  }

  @Test
  void testElementStringEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "a<b");
    Assertions.assertEquals(xml.toString(), "<x>a&lt;b</x>");
  }

  @Test
  void testElementLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 42L);
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  void testElementDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 3.5d);
    Assertions.assertEquals(xml.toString(), "<x>3.5</x>");
  }

  // asText() / asXml() ---------------------------------------------------------------------------

  @Test
  void testNullContentIsIgnored() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.asText(null).asXml((Object) null).comment(null).cdata(null);
    Assertions.assertEquals(xml.toString(), "");
  }

  @Test
  void testAsTextNonNull() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText(42).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  void testAsTextEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText("<b>").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&lt;b&gt;</x>");
  }

  @Test
  void testAsXmlObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asXml("<y/>").closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  @Test
  void testAsXmlWritableObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    XmlWritable writable = writer -> writer.emptyElement("y");
    xml.openElement("x").asXml((Object) writable).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  // Non-XML character filtering ------------------------------------------------------------------

  @Test
  void testNonXmlControlCharIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("ab").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  @Test
  void testOrphanedHighSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uD800b").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  @Test
  void testOrphanedLowSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uDC00b").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  // close() / flush() ----------------------------------------------------------------------------

  @Test
  void testCloseSucceedsWhenNoOpenElements() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.close();
    Assertions.assertEquals(xml.toString(), "<x/>");
  }

  @Test
  void testFlushIsNoOpOnStringBuilder() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.flush();
    Assertions.assertEquals(xml.toString(), "<x/>");
  }

  @Test
  void testCloseElementWithNoOpenElementLeavesWriterUsable() {
    XmlStringBuilder xml = new XmlStringBuilder();
    try {
      xml.closeElement();
      Assertions.fail("Expected IllegalCloseElementException");
    } catch (IllegalCloseElementException e) {
      // writer must still be functional
    }
    xml.openElement("x").closeElement();
    Assertions.assertEquals(xml.toString(), "<x/>");
  }

  // xml(String) ----------------------------------------------------------------------------------

  @Test
  void testXmlNullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").xml((String) null));
  }

  // emptyElement() indentation -------------------------------------------------------------------

  @Test
  void testEmptyElementRespectsParentHasChildrenFalse() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", false).emptyElement("br").closeElement();
    Assertions.assertEquals(xml.toString(), "<parent><br/></parent>");
  }

  @Test
  void testEmptyElementIndentsInsideParentWithChildren() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", true).emptyElement("br").closeElement();
    Assertions.assertEquals(xml.toString(), "<parent>\n  <br/>\n</parent>");
  }

  // withIndent ------------------------------------------------------------------------------------

  @Test
  void testWithIndentAfterUse() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    xml.withIndent("  ");
    });
  }

  @Test
  void testIndentedNestedOutput() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("root", true)
        .openElement("leaf")
        .text("hi")
        .closeElement()
        .closeElement();
    Assertions.assertEquals(xml.toString(), "<root>\n  <leaf>hi</leaf>\n</root>");
  }

  @Test
  void testCommentWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.comment("note");
    Assertions.assertEquals(xml.toString(), "<!-- note -->\n");
  }

  @Test
  void testDeclarationWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.declaration();
    Assertions.assertEquals(xml.toString(), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
  }
}
