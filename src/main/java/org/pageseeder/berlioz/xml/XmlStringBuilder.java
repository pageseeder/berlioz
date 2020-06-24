package org.pageseeder.berlioz.xml;

/**
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public final class XmlStringBuilder extends XmlAppendable<StringBuilder> {

  private XmlStringBuilder(StringBuilder xml, String indent) {
    super(xml, indent);
  }

  public XmlStringBuilder() {
    super(new StringBuilder());
  }

  @Override
  public XmlStringBuilder withIndent(String chars) {
    return new XmlStringBuilder(this._xml, chars);
  }

  @Override
  public String toString() {
    return this._xml.toString();
  };
}
