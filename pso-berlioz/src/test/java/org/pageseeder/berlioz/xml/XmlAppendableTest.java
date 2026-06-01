package org.pageseeder.berlioz.xml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public final class XmlAppendableTest {

  // text(String) ---------------------------------------------------------------------------------

  @Test
  public void testTextEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a<>&\"'😀").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>a&lt;&gt;&amp;\"'&#x1f600;</x>");
  }

  @Test
  public void testTextEscapingWithCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] text = "xx😀<&yy".toCharArray();
    xml.openElement("x").text(text, 2, 4).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&#x1f600;&lt;&amp;</x>");
  }

  @Test
  public void testTextNullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").text((String) null));
  }

  // text(long) / text(double) / text(char) -------------------------------------------------------

  @Test
  public void testTextLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(42L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  public void testTextLongNegative() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(-1L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>-1</x>");
  }

  @Test
  public void testTextDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(3.14).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>3.14</x>");
  }

  @Test
  public void testTextCharPlain() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('a').closeElement();
    Assertions.assertEquals(xml.toString(), "<x>a</x>");
  }

  @Test
  public void testTextCharEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('<').closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&lt;</x>");
  }

  // xml(char[], int, int) ------------------------------------------------------------------------

  @Test
  public void testXmlCharArray() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "<y/>".toCharArray();
    xml.openElement("x").xml(raw, 0, raw.length).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  @Test
  public void testXmlCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "xx<y/>zz".toCharArray();
    xml.openElement("x").xml(raw, 2, 4).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  // attribute(String, *) -------------------------------------------------------------------------

  @Test
  public void testAttributeEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("a", "<&\"'😀").closeElement();
    Assertions.assertEquals(xml.toString(), "<x a=\"&lt;&amp;&quot;&#39;&#x1f600;\"/>");
  }

  @Test
  public void testAttributeLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 42L).closeElement();
    Assertions.assertEquals(xml.toString(), "<x n=\"42\"/>");
  }

  @Test
  public void testAttributeDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 3.14).closeElement();
    Assertions.assertEquals(xml.toString(), "<x n=\"3.14\"/>");
  }

  @Test
  public void testAttributeBooleanTrue() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", true).closeElement();
    Assertions.assertEquals(xml.toString(), "<x flag=\"true\"/>");
  }

  @Test
  public void testAttributeBooleanFalse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", false).closeElement();
    Assertions.assertEquals(xml.toString(), "<x flag=\"false\"/>");
  }

  @Test
  public void testAttributeNullNameThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").attribute(null, "v"));
  }

  @Test
  public void testAttributeNullValueThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").attribute("n", (String) null));
  }

  // attributes(Map) ------------------------------------------------------------------------------

  @Test
  public void testAttributesMap() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put("a", "1");
    attrs.put("b", "two");
    xml.openElement("x").attributes(attrs).closeElement();
    Assertions.assertEquals(xml.toString(), "<x a=\"1\" b=\"two\"/>");
  }

  // comment() ------------------------------------------------------------------------------------

  @Test
  public void testComment() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.comment("hello");
    Assertions.assertEquals(xml.toString(), "<!-- hello -->");
  }

  @Test
  public void testCommentWithDoubleDashThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new XmlStringBuilder().comment("bad--comment"));
  }

  // cdata() --------------------------------------------------------------------------------------

  @Test
  public void testCdata() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").cdata("safe text").closeElement();
    Assertions.assertEquals(xml.toString(), "<x><![CDATA[safe text]]></x>");
  }

  @Test
  public void testCdataWithEndSequenceThrows() {
    Assertions.assertThrows(IllegalArgumentException.class, () -> new XmlStringBuilder().cdata("bad]]>data"));
  }

  // processingInstruction() ----------------------------------------------------------------------

  @Test
  public void testProcessingInstruction() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "data");
    Assertions.assertEquals(xml.toString(), "<?target data?>");
  }

  @Test
  public void testProcessingInstructionEmptyData() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "");
    Assertions.assertEquals(xml.toString(), "<?target?>");
  }

  @Test
  public void testProcessingInstructionNullTargetThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().processingInstruction(null, "data"));
  }

  // declaration() --------------------------------------------------------------------------------

  @Test
  public void testDeclaration() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.declaration();
    Assertions.assertEquals(xml.toString(), "<?xml version=\"1.0\" encoding=\"utf-8\"?>");
  }

  @Test
  public void testDeclarationAfterUse() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("x");
    xml.declaration();
    });
  }

  // emptyElement() -------------------------------------------------------------------------------

  @Test
  public void testEmptyElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.emptyElement("br");
    Assertions.assertEquals(xml.toString(), "<br/>");
  }

  // element() shortcuts --------------------------------------------------------------------------

  @Test
  public void testElementString() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "hello");
    Assertions.assertEquals(xml.toString(), "<x>hello</x>");
  }

  @Test
  public void testElementStringEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "a<b");
    Assertions.assertEquals(xml.toString(), "<x>a&lt;b</x>");
  }

  @Test
  public void testElementLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 42L);
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  public void testElementDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 3.5d);
    Assertions.assertEquals(xml.toString(), "<x>3.5</x>");
  }

  // asText() / asXml() ---------------------------------------------------------------------------

  @Test
  public void testNullContentIsIgnored() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.asText(null).asXml((Object) null).comment(null).cdata(null);
    Assertions.assertEquals(xml.toString(), "");
  }

  @Test
  public void testAsTextNonNull() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText(42).closeElement();
    Assertions.assertEquals(xml.toString(), "<x>42</x>");
  }

  @Test
  public void testAsTextEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText("<b>").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>&lt;b&gt;</x>");
  }

  @Test
  public void testAsXmlObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asXml("<y/>").closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  @Test
  public void testAsXmlWritableObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    XmlWritable writable = writer -> writer.emptyElement("y");
    xml.openElement("x").asXml((Object) writable).closeElement();
    Assertions.assertEquals(xml.toString(), "<x><y/></x>");
  }

  // Non-XML character filtering ------------------------------------------------------------------

  @Test
  public void testNonXmlControlCharIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("ab").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  @Test
  public void testOrphanedHighSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uD800b").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  @Test
  public void testOrphanedLowSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uDC00b").closeElement();
    Assertions.assertEquals(xml.toString(), "<x>ab</x>");
  }

  // close() / flush() ----------------------------------------------------------------------------

  @Test
  public void testCloseSucceedsWhenNoOpenElements() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.close();
    Assertions.assertEquals(xml.toString(), "<x/>");
  }

  @Test
  public void testFlushIsNoOpOnStringBuilder() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.flush();
    Assertions.assertEquals(xml.toString(), "<x/>");
  }

  @Test
  public void testCloseElementWithNoOpenElementLeavesWriterUsable() {
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
  public void testXmlNullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> new XmlStringBuilder().openElement("x").xml((String) null));
  }

  // emptyElement() indentation -------------------------------------------------------------------

  @Test
  public void testEmptyElementRespectsParentHasChildrenFalse() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", false).emptyElement("br").closeElement();
    Assertions.assertEquals(xml.toString(), "<parent><br/></parent>");
  }

  @Test
  public void testEmptyElementIndentsInsideParentWithChildren() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", true).emptyElement("br").closeElement();
    Assertions.assertEquals(xml.toString(), "<parent>\n  <br/>\n</parent>");
  }

  // withIndent ------------------------------------------------------------------------------------

  @Test
  public void testWithIndentAfterUse() {
    Assertions.assertThrows(IllegalStateException.class, () -> {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    xml.withIndent("  ");
    });
  }

  @Test
  public void testIndentedNestedOutput() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("root", true)
        .openElement("leaf")
        .text("hi")
        .closeElement()
        .closeElement();
    Assertions.assertEquals(xml.toString(), "<root>\n  <leaf>hi</leaf>\n</root>");
  }

  @Test
  public void testCommentWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.comment("note");
    Assertions.assertEquals(xml.toString(), "<!-- note -->\n");
  }

  @Test
  public void testDeclarationWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.declaration();
    Assertions.assertEquals(xml.toString(), "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
  }
}
