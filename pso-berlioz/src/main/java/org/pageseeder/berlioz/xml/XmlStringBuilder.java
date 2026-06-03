package org.pageseeder.berlioz.xml;

import org.jspecify.annotations.Nullable;

/**
 * An {@link XmlAppendable} that writes XML into an in-memory {@link StringBuilder}.
 *
 * <p>Use this class when the complete XML document needs to be captured as a {@link String},
 * for example to pass XML markup to another API or to build test fixtures. The accumulated
 * content is retrieved via {@link #toString()}.
 *
 * <pre>{@code
 * XmlStringBuilder xml = new XmlStringBuilder().withIndent("  ");
 * xml.openElement("root", true)
 *    .element("child", "value")
 *    .closeElement();
 * String result = xml.toString();
 * }</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.12.0
 */
public final class XmlStringBuilder extends XmlAppendable<StringBuilder> {

  /**
   * Creates a new builder backed by the given {@link StringBuilder}, with the specified indentation.
   *
   * @param xml    the backing buffer to write XML into
   * @param indent the indentation string per nesting level, or {@code null} to disable indentation
   */
  private XmlStringBuilder(StringBuilder xml, @Nullable String indent) {
    super(xml, indent);
  }

  /**
   * Creates a new builder backed by a fresh {@link StringBuilder}, with indentation disabled.
   */
  public XmlStringBuilder() {
    super(new StringBuilder());
  }

  /**
   * Returns a new builder sharing the same backing buffer but with the given indentation string.
   *
   * @param chars the indentation string per nesting level, or {@code null} to disable indentation
   * @return a new {@code XmlStringBuilder} configured with the given indentation
   * @throws IllegalArgumentException If {@code chars} contains a non-space character
   * @throws IllegalStateException    If this builder has already produced output
   */
  @Override
  public XmlStringBuilder withIndent(@Nullable String chars) {
    checkCanSetIndent(chars);
    return new XmlStringBuilder(appendable(), chars);
  }

  /**
   * Returns the XML content accumulated so far as a string.
   *
   * @return the accumulated XML markup
   */
  @Override
  public String toString() {
    return appendable().toString();
  }
}
