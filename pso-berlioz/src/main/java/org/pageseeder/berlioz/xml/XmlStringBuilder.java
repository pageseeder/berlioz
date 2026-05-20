package org.pageseeder.berlioz.xml;

import org.eclipse.jdt.annotation.Nullable;

/**
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public final class XmlStringBuilder extends XmlAppendable<StringBuilder> {

  private XmlStringBuilder(StringBuilder xml, @Nullable String indent) {
    super(xml, indent);
  }

  public XmlStringBuilder() {
    super(new StringBuilder());
  }

  @Override
  public XmlStringBuilder withIndent(@Nullable String chars) {
    return new XmlStringBuilder(this._xml, chars);
  }

  @Override
  public String toString() {
    return this._xml.toString();
  };
}
