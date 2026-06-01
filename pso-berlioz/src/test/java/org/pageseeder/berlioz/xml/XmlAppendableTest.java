package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

final class XmlAppendableTest {

  // text(String) ---------------------------------------------------------------------------------

  @ParameterizedTest
  @MethodSource("textStringCases")
  void testTextString(String input, String expected) {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(input).closeElement();
    Assertions.assertEquals(expected, xml.toString());
  }

  static Stream<Arguments> textStringCases() {
    return Stream.of(
        Arguments.of("a<>&\"'\u0001\uD83D\uDE00", "<x>a&lt;&gt;&amp;\"'&#x1f600;</x>"),
        Arguments.of("a\u0001b",    "<x>ab</x>"),
        Arguments.of("a\uD800b",    "<x>ab</x>"),
        Arguments.of("a\uDC00b",    "<x>ab</x>")
    );
  }

  @Test
  void testTextEscapingWithCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] text = "xx😀<&yy".toCharArray();
    xml.openElement("x").text(text, 2, 4).closeElement();
    Assertions.assertEquals("<x>&#x1f600;&lt;&amp;</x>", xml.toString());
  }

  @Test
  void testTextNullThrows() {
    XmlAppendable<StringBuilder> xml = new XmlStringBuilder().openElement("x");
    Assertions.assertThrows(NullPointerException.class, () -> xml.text(null));
  }

  // text(long) / text(double) / text(char) -------------------------------------------------------

  @Test
  void testTextLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(42L).closeElement();
    Assertions.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  void testTextLongNegative() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(-1L).closeElement();
    Assertions.assertEquals("<x>-1</x>", xml.toString());
  }

  @Test
  void testTextDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(3.14).closeElement();
    Assertions.assertEquals("<x>3.14</x>", xml.toString());
  }

  @Test
  void testTextCharPlain() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('a').closeElement();
    Assertions.assertEquals("<x>a</x>", xml.toString());
  }

  @Test
  void testTextCharEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('<').closeElement();
    Assertions.assertEquals("<x>&lt;</x>", xml.toString());
  }

  // xml(char[], int, int) ------------------------------------------------------------------------

  @Test
  void testXmlCharArray() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "<y/>".toCharArray();
    xml.openElement("x").xml(raw, 0, raw.length).closeElement();
    Assertions.assertEquals("<x><y/></x>", xml.toString());
  }

  @Test
  void testXmlCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "xx<y/>zz".toCharArray();
    xml.openElement("x").xml(raw, 2, 4).closeElement();
    Assertions.assertEquals("<x><y/></x>", xml.toString());
  }

  // attribute(String, *) -------------------------------------------------------------------------

  @Test
  void testAttributeEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("a", "<&\"'😀").closeElement();
    Assertions.assertEquals("<x a=\"&lt;&amp;&quot;&#39;&#x1f600;\"/>", xml.toString());
  }

  @Test
  void testAttributeLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 42L).closeElement();
    Assertions.assertEquals("<x n=\"42\"/>", xml.toString());
  }

  @Test
  void testAttributeDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 3.14).closeElement();
    Assertions.assertEquals("<x n=\"3.14\"/>", xml.toString());
  }

  @Test
  void testAttributeBooleanTrue() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", true).closeElement();
    Assertions.assertEquals("<x flag=\"true\"/>", xml.toString());
  }

  @Test
  void testAttributeBooleanFalse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", false).closeElement();
    Assertions.assertEquals("<x flag=\"false\"/>", xml.toString());
  }

  @Test
  void testAttributeNullNameThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    Assertions.assertThrows(NullPointerException.class, () -> xml.attribute(null, "v"));
  }

  @Test
  void testAttributeNullValueThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    Assertions.assertThrows(NullPointerException.class, () -> xml.attribute("n", null));
  }

  // attributes(Map) ------------------------------------------------------------------------------

  @Test
  void testAttributesMap() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put("a", "1");
    attrs.put("b", "two");
    xml.openElement("x").attributes(attrs).closeElement();
    Assertions.assertEquals("<x a=\"1\" b=\"two\"/>", xml.toString());
  }

  // comment() ------------------------------------------------------------------------------------

  @Test
  void testComment() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.comment("hello");
    Assertions.assertEquals("<!-- hello -->", xml.toString());
  }

  @Test
  void testCommentWithDoubleDashThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Assertions.assertThrows(IllegalArgumentException.class, () -> xml.comment("bad--comment"));
  }

  // cdata() --------------------------------------------------------------------------------------

  @Test
  void testCdata() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").cdata("safe text").closeElement();
    Assertions.assertEquals("<x><![CDATA[safe text]]></x>", xml.toString());
  }

  @Test
  void testCdataWithEndSequenceThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Assertions.assertThrows(IllegalArgumentException.class, () -> xml.cdata("bad]]>data"));
  }

  // processingInstruction() ----------------------------------------------------------------------

  @Test
  void testProcessingInstruction() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "data");
    Assertions.assertEquals("<?target data?>", xml.toString());
  }

  @Test
  void testProcessingInstructionEmptyData() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "");
    Assertions.assertEquals("<?target?>", xml.toString());
  }

  @Test
  void testProcessingInstructionNullTargetThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Assertions.assertThrows(NullPointerException.class, () -> xml.processingInstruction(null, "data"));
  }

  // declaration() --------------------------------------------------------------------------------

  @Test
  void testDeclaration() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.declaration();
    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>", xml.toString());
  }

  @Test
  void testDeclarationAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("x");
    Assertions.assertThrows(IllegalStateException.class, xml::declaration);
  }

  // emptyElement() -------------------------------------------------------------------------------

  @Test
  void testEmptyElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.emptyElement("br");
    Assertions.assertEquals("<br/>", xml.toString());
  }

  // element() shortcuts --------------------------------------------------------------------------

  @Test
  void testElementString() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "hello");
    Assertions.assertEquals("<x>hello</x>", xml.toString());
  }

  @Test
  void testElementStringEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "a<b");
    Assertions.assertEquals("<x>a&lt;b</x>", xml.toString());
  }

  @Test
  void testElementLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 42L);
    Assertions.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  void testElementDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 3.5d);
    Assertions.assertEquals("<x>3.5</x>", xml.toString());
  }

  // asText() / asXml() ---------------------------------------------------------------------------

  @Test
  void testNullContentIsIgnored() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.asText(null).asXml((Object) null).comment(null).cdata(null);
    Assertions.assertEquals("", xml.toString());
  }

  @Test
  void testAsTextNonNull() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText(42).closeElement();
    Assertions.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  void testAsTextEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText("<b>").closeElement();
    Assertions.assertEquals("<x>&lt;b&gt;</x>", xml.toString());
  }

  @Test
  void testAsXmlObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asXml("<y/>").closeElement();
    Assertions.assertEquals("<x><y/></x>", xml.toString());
  }

  @Test
  void testAsXmlWritableObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    XmlWritable writable = writer -> writer.emptyElement("y");
    xml.openElement("x").asXml((Object) writable).closeElement();
    Assertions.assertEquals("<x><y/></x>", xml.toString());
  }

  // close() / flush() ----------------------------------------------------------------------------

  @Test
  void testCloseSucceedsWhenNoOpenElements() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.close();
    Assertions.assertEquals("<x/>", xml.toString());
  }

  @Test
  void testFlushIsNoOpOnStringBuilder() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.flush();
    Assertions.assertEquals("<x/>", xml.toString());
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
    Assertions.assertEquals("<x/>", xml.toString());
  }

  // xml(String) ----------------------------------------------------------------------------------

  @Test
  void testXmlNullThrows() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    Assertions.assertThrows(NullPointerException.class, () -> xml.xml(null));
  }

  // emptyElement() indentation -------------------------------------------------------------------

  @Test
  void testEmptyElementRespectsParentHasChildrenFalse() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", false).emptyElement("br").closeElement();
    Assertions.assertEquals("<parent><br/></parent>", xml.toString());
  }

  @Test
  void testEmptyElementIndentsInsideParentWithChildren() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", true).emptyElement("br").closeElement();
    Assertions.assertEquals("<parent>\n  <br/>\n</parent>", xml.toString());
  }

  // withIndent ------------------------------------------------------------------------------------

  @Test
  void testWithIndentAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    Assertions.assertThrows(IllegalStateException.class, () -> xml.withIndent("  "));
  }

  @Test
  void testIndentedNestedOutput() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("root", true)
        .openElement("leaf")
        .text("hi")
        .closeElement()
        .closeElement();
    Assertions.assertEquals("<root>\n  <leaf>hi</leaf>\n</root>", xml.toString());
  }

  @Test
  void testCommentWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.comment("note");
    Assertions.assertEquals("<!-- note -->\n", xml.toString());
  }

  @Test
  void testDeclarationWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.declaration();
    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n", xml.toString());
  }
}
