package org.pageseeder.berlioz.xml;

import org.jspecify.annotations.Nullable;

/**
 *
 * @version Berlioz 0.13.0
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
    checkCanSetIndent(chars);
    return new XmlStringBuilder(appendable(), chars);
  }

  @Override
  public String toString() {
    return appendable().toString();
  }
}
