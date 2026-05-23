package org.pageseeder.berlioz.xml;

import org.junit.Assert;
import org.junit.Test;

public final class XmlAppendableTest {

  @Test
  public void testTextEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").text("a<>&\"'\u0001\uD83D\uDE00").closeElement();
    Assert.assertEquals("<x>a&lt;&gt;&amp;\"'&#x1f600;</x>", xml.toString());
  }

  @Test
  public void testTextEscapingWithCharArrayOffset() {
    XmlStringBuilder xml = new XmlStringBuilder();
    char[] text = "xx\uD83D\uDE00<&yy".toCharArray();
    xml.openElement("x").text(text, 2, 4).closeElement();
    Assert.assertEquals("<x>&#x1f600;&lt;&amp;</x>", xml.toString());
  }

  @Test
  public void testAttributeEscaping() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x").attribute("a", "<&\"'\u0001\uD83D\uDE00").closeElement();
    Assert.assertEquals("<x a=\"&lt;&amp;&quot;&#39;&#x1f600;\"/>", xml.toString());
  }

  @Test
  public void testNullContentIsIgnored() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.asText(null).asXml((Object)null).comment(null).cdata(null);
    Assert.assertEquals("", xml.toString());
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
    xml.openElement("x").asXml((Object)writable).closeElement();
    Assert.assertEquals("<x><y/></x>", xml.toString());
  }

  @Test(expected = IllegalStateException.class)
  public void testWithIndentAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.openElement("x");
    xml.withIndent("  ");
  }

  @Test(expected = IllegalStateException.class)
  public void testDeclarationAfterUse() {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.text("x");
    xml.declaration();
  }
}
