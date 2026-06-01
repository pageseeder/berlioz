package org.pageseeder.berlioz.xml;

import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

public final class XmlAppendableTest {

  // text(String) ---------------------------------------------------------------------------------

  @Test
  public void testTextEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a<>&\"'😀").closeElement();
    Assert.assertEquals("<x>a&lt;&gt;&amp;\"'&#x1f600;</x>", xml.toString());
  }

  @Test
  public void testTextEscapingWithCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] text = "xx😀<&yy".toCharArray();
    xml.openElement("x").text(text, 2, 4).closeElement();
    Assert.assertEquals("<x>&#x1f600;&lt;&amp;</x>", xml.toString());
  }

  @Test(expected = NullPointerException.class)
  public void testTextNullThrows() {
    new XmlStringBuilder().openElement("x").text((String) null);
  }

  // text(long) / text(double) / text(char) -------------------------------------------------------

  @Test
  public void testTextLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(42L).closeElement();
    Assert.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  public void testTextLongNegative() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(-1L).closeElement();
    Assert.assertEquals("<x>-1</x>", xml.toString());
  }

  @Test
  public void testTextDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text(3.14).closeElement();
    Assert.assertEquals("<x>3.14</x>", xml.toString());
  }

  @Test
  public void testTextCharPlain() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('a').closeElement();
    Assert.assertEquals("<x>a</x>", xml.toString());
  }

  @Test
  public void testTextCharEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text('<').closeElement();
    Assert.assertEquals("<x>&lt;</x>", xml.toString());
  }

  // xml(char[], int, int) ------------------------------------------------------------------------

  @Test
  public void testXmlCharArray() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "<y/>".toCharArray();
    xml.openElement("x").xml(raw, 0, raw.length).closeElement();
    Assert.assertEquals("<x><y/></x>", xml.toString());
  }

  @Test
  public void testXmlCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] raw = "xx<y/>zz".toCharArray();
    xml.openElement("x").xml(raw, 2, 4).closeElement();
    Assert.assertEquals("<x><y/></x>", xml.toString());
  }

  // attribute(String, *) -------------------------------------------------------------------------

  @Test
  public void testAttributeEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("a", "<&\"'😀").closeElement();
    Assert.assertEquals("<x a=\"&lt;&amp;&quot;&#39;&#x1f600;\"/>", xml.toString());
  }

  @Test
  public void testAttributeLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 42L).closeElement();
    Assert.assertEquals("<x n=\"42\"/>", xml.toString());
  }

  @Test
  public void testAttributeDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("n", 3.14).closeElement();
    Assert.assertEquals("<x n=\"3.14\"/>", xml.toString());
  }

  @Test
  public void testAttributeBooleanTrue() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", true).closeElement();
    Assert.assertEquals("<x flag=\"true\"/>", xml.toString());
  }

  @Test
  public void testAttributeBooleanFalse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("flag", false).closeElement();
    Assert.assertEquals("<x flag=\"false\"/>", xml.toString());
  }

  @Test(expected = NullPointerException.class)
  public void testAttributeNullNameThrows() {
    new XmlStringBuilder().openElement("x").attribute(null, "v");
  }

  @Test(expected = NullPointerException.class)
  public void testAttributeNullValueThrows() {
    new XmlStringBuilder().openElement("x").attribute("n", (String) null);
  }

  // attributes(Map) ------------------------------------------------------------------------------

  @Test
  public void testAttributesMap() {
    XmlStringBuilder xml = new XmlStringBuilder();
    Map<String, String> attrs = new LinkedHashMap<>();
    attrs.put("a", "1");
    attrs.put("b", "two");
    xml.openElement("x").attributes(attrs).closeElement();
    Assert.assertEquals("<x a=\"1\" b=\"two\"/>", xml.toString());
  }

  // comment() ------------------------------------------------------------------------------------

  @Test
  public void testComment() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.comment("hello");
    Assert.assertEquals("<!-- hello -->", xml.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCommentWithDoubleDashThrows() {
    new XmlStringBuilder().comment("bad--comment");
  }

  // cdata() --------------------------------------------------------------------------------------

  @Test
  public void testCdata() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").cdata("safe text").closeElement();
    Assert.assertEquals("<x><![CDATA[safe text]]></x>", xml.toString());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCdataWithEndSequenceThrows() {
    new XmlStringBuilder().cdata("bad]]>data");
  }

  // processingInstruction() ----------------------------------------------------------------------

  @Test
  public void testProcessingInstruction() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "data");
    Assert.assertEquals("<?target data?>", xml.toString());
  }

  @Test
  public void testProcessingInstructionEmptyData() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.processingInstruction("target", "");
    Assert.assertEquals("<?target?>", xml.toString());
  }

  @Test(expected = NullPointerException.class)
  public void testProcessingInstructionNullTargetThrows() {
    new XmlStringBuilder().processingInstruction(null, "data");
  }

  // declaration() --------------------------------------------------------------------------------

  @Test
  public void testDeclaration() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.declaration();
    Assert.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>", xml.toString());
  }

  @Test(expected = IllegalStateException.class)
  public void testDeclarationAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("x");
    xml.declaration();
  }

  // emptyElement() -------------------------------------------------------------------------------

  @Test
  public void testEmptyElement() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.emptyElement("br");
    Assert.assertEquals("<br/>", xml.toString());
  }

  // element() shortcuts --------------------------------------------------------------------------

  @Test
  public void testElementString() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "hello");
    Assert.assertEquals("<x>hello</x>", xml.toString());
  }

  @Test
  public void testElementStringEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", "a<b");
    Assert.assertEquals("<x>a&lt;b</x>", xml.toString());
  }

  @Test
  public void testElementLong() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 42L);
    Assert.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  public void testElementDouble() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.element("x", 3.5d);
    Assert.assertEquals("<x>3.5</x>", xml.toString());
  }

  // asText() / asXml() ---------------------------------------------------------------------------

  @Test
  public void testNullContentIsIgnored() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.asText(null).asXml((Object) null).comment(null).cdata(null);
    Assert.assertEquals("", xml.toString());
  }

  @Test
  public void testAsTextNonNull() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText(42).closeElement();
    Assert.assertEquals("<x>42</x>", xml.toString());
  }

  @Test
  public void testAsTextEscapesContent() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asText("<b>").closeElement();
    Assert.assertEquals("<x>&lt;b&gt;</x>", xml.toString());
  }

  @Test
  public void testAsXmlObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").asXml("<y/>").closeElement();
    Assert.assertEquals("<x><y/></x>", xml.toString());
  }

  @Test
  public void testAsXmlWritableObject() {
    XmlStringBuilder xml = new XmlStringBuilder();
    XmlWritable writable = writer -> writer.emptyElement("y");
    xml.openElement("x").asXml((Object) writable).closeElement();
    Assert.assertEquals("<x><y/></x>", xml.toString());
  }

  // Non-XML character filtering ------------------------------------------------------------------

  @Test
  public void testNonXmlControlCharIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("ab").closeElement();
    Assert.assertEquals("<x>ab</x>", xml.toString());
  }

  @Test
  public void testOrphanedHighSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uD800b").closeElement();
    Assert.assertEquals("<x>ab</x>", xml.toString());
  }

  @Test
  public void testOrphanedLowSurrogateIsStripped() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a\uDC00b").closeElement();
    Assert.assertEquals("<x>ab</x>", xml.toString());
  }

  // close() / flush() ----------------------------------------------------------------------------

  @Test
  public void testCloseSucceedsWhenNoOpenElements() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.close();
  }

  @Test
  public void testFlushIsNoOpOnStringBuilder() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").closeElement();
    xml.flush();
  }

  @Test
  public void testCloseElementWithNoOpenElementLeavesWriterUsable() {
    XmlStringBuilder xml = new XmlStringBuilder();
    try {
      xml.closeElement();
      Assert.fail("Expected IllegalCloseElementException");
    } catch (IllegalCloseElementException e) {
      // writer must still be functional
    }
    xml.openElement("x").closeElement();
    Assert.assertEquals("<x/>", xml.toString());
  }

  // xml(String) ----------------------------------------------------------------------------------

  @Test(expected = NullPointerException.class)
  public void testXmlNullThrows() {
    new XmlStringBuilder().openElement("x").xml((String) null);
  }

  // emptyElement() indentation -------------------------------------------------------------------

  @Test
  public void testEmptyElementRespectsParentHasChildrenFalse() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", false).emptyElement("br").closeElement();
    Assert.assertEquals("<parent><br/></parent>", xml.toString());
  }

  @Test
  public void testEmptyElementIndentsInsideParentWithChildren() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("parent", true).emptyElement("br").closeElement();
    Assert.assertEquals("<parent>\n  <br/>\n</parent>", xml.toString());
  }

  // withIndent ------------------------------------------------------------------------------------

  @Test(expected = IllegalStateException.class)
  public void testWithIndentAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    xml.withIndent("  ");
  }

  @Test
  public void testIndentedNestedOutput() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.openElement("root", true)
        .openElement("leaf")
        .text("hi")
        .closeElement()
        .closeElement();
    Assert.assertEquals("<root>\n  <leaf>hi</leaf>\n</root>", xml.toString());
  }

  @Test
  public void testCommentWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.comment("note");
    Assert.assertEquals("<!-- note -->\n", xml.toString());
  }

  @Test
  public void testDeclarationWithIndentAddsNewline() {
    XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
    xml.declaration();
    Assert.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n", xml.toString());
  }
}
