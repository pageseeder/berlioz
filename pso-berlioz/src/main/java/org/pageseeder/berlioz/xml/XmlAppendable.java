/*
 * Copyright 2010-2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.xml;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * A base implementation for XML writers.
 *
 * <p>Provides methods to generate well-formed XML data easily. wrapping a writer.
 *
 * <p>This version only supports utf-8 encoding, if writing to a file make sure that the
 * encoding of the file output stream is "utf-8".
 *
 * <p>The recommended implementation is to use a <code>BufferedWriter</code> to write.
 *
 * <pre>
 *  Writer writer =
 *     new BufferedWriter(new OutputStreamWriter(new FileOutputStream("foo.out"),"utf-8"));
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.0
 */
public class XmlAppendable<T extends Appendable> implements XmlWriter {

  /**
   * The root node.
   */
  private static final Element ROOT = new Element("", true);

  /**
   * Where the XML data goes.
   */
  private final T xml;

  /**
   * Encoding of the output xml.
   */
  private static final String ENCODING = "utf-8";

  /**
   * Indicates whether the xml should be indented or not.
   *
   * <p>The default is <code>true</code> (indented).
   *
   * <p>The indentation is 2 white-spaces.
   */
  private final boolean indent;

  /**
   * The default indentation spaces used.
   */
  private final @Nullable String indentChars;

  /**
   * A stack of elements to close the elements automatically.
   */
  private final List<Element> elements = new ArrayList<>();

  /**
   * Level of the depth of the xml document currently produced.
   *
   * <p>This attribute changes depending on the state of the instance.
   */
  private int depth = 0;

  /**
   * Flag to indicate that the element open tag is not finished yet.
   */
  private boolean isNude = false;

  /**
   * Indicates whether anything has been appended by this writer.
   */
  private boolean used = false;

  // constructors -------------------------------------------------------------------------

  /**
   * <p>Creates a new XML writer.
   *
   * @param xml  Where this writer should write the XML data.
   * @param indentChars  Set the indentation flag.
   *
   * @throws NullPointerException If the writer is <code>null</code>.
   */
  protected XmlAppendable(T xml, @Nullable String indentChars) throws NullPointerException {
    this.xml = Objects.requireNonNull(xml, "XmlWriter cannot use a null writer.");
    this.indent = indentChars != null;
    this.indentChars = indentChars;
    this.elements.add(ROOT);
  }

  /**
   * <p>Creates a new XML writer.
   *
   * @param xml  Where this writer should write the XML data.
   *
   * @throws NullPointerException If the writer is <code>null</code>.
   */
  public XmlAppendable(T xml) throws NullPointerException {
    this(xml, null);
  }

  /**
   * Sets the string to use for indentation.
   *
   * <p>The string must be only composed of valid spaces characters.
   *
   * <p>If the string is <code>null</code> then the indentation is turned off.
   *
   * @see Character#isSpaceChar(char)
   *
   * @param spaces The indentation string to use.
   *
   * @throws IllegalArgumentException If the indent string is not made of spaces.
   * @throws IllegalStateException    If the writer has already been used.
   */
  public XmlAppendable<T> withIndent(@Nullable String spaces) {
    checkCanSetIndent(spaces);
    return new XmlAppendable<>(this.xml, spaces);
  }

  // Write text methods
  // ----------------------------------------------------------------------------------------------

  @Override
  public final XmlAppendable<T> text(String text) {
    Objects.requireNonNull(text, "Text must not be null.");
    deNude();
    appendText(text, 0, text.length());
    return this;
  }

  @Override
  public final XmlAppendable<T> text(long number) {
    deNude();
    append(Long.toString(number));
    return this;
  }

  @Override
  public final XmlAppendable<T> text(double number) {
    deNude();
    append(Double.toString(number));
    return this;
  }

  @Override
  public final XmlAppendable<T> text(char[] text, int off, int len) {
    Objects.requireNonNull(text, "Text must not be null.");
    deNude();
    appendText(CharBuffer.wrap(text, off, len), 0, len);
    return this;
  }

  @Override
  public final XmlAppendable<T> text(char c) {
    deNude();
    appendText(Character.toString(c), 0, 1);
    return this;
  }

  /**
   * Writes the string value of an object.
   *
   * <p>Does nothing if the object is <code>null</code>.
   *
   * @param o The object that should be written as text.
   */
  public XmlAppendable<T> asText(@Nullable Object o) {
    if (o != null) {
      this.text(o.toString());
    }
    return this;
  }

  /**
   * Writes the string value of an object.
   *
   * <p>Does nothing if the object is <code>null</code>.
   *
   * @param o The object that should be written as text.
   */
  public XmlAppendable<T> asXml(@Nullable Object o) {
    if (o instanceof XmlWritable) {
      return asXml((XmlWritable)o);
    }
    if (o != null) {
      this.xml(o.toString());
    }
    return this;
  }

  // Write XML methods
  // ----------------------------------------------------------------------------------------------

  @Override
  public final XmlAppendable<T> xml(String xml) {
    deNude();
    append(xml);
    return this;
  }

  @Override
  public final XmlAppendable<T> xml(char[] xml, int off, int len) {
    Objects.requireNonNull(xml, "XML must not be null.");
    deNude();
    append(CharBuffer.wrap(xml, off, len));
    return this;
  }

  @Override
  public XmlAppendable<T> asXml(XmlWritable object) {
    object.toXml(this);
    return this;
  }

  // Processing Instructions, CDATA sections and comments
  // ----------------------------------------------------------------------------------------------


  @Override
  public final void declaration() {
    if (this.used) {
      throw new IllegalStateException("Cannot write XML declaration after other content.");
    }
    append("<?xml version=\"1.0\" encoding=\""+ENCODING +"\"?>");
    if (this.indent) {
      append('\n');
    }
  }

  @Override
  public final XmlAppendable<T> comment(@Nullable String comment) throws IllegalArgumentException {
    if (comment == null) {
      return this;
    }
    if (comment.contains("--"))
      throw new IllegalArgumentException("A comment must not contain '--'.");
    deNude();
    append("<!-- ");
    append(comment);
    append(" -->");
    if (this.indent) {
      append('\n');
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> processingInstruction(String target, @Nullable String data) {
    Objects.requireNonNull(target, "Processing instruction target must not be null.");
    if (data != null && data.contains("?>")) {
      throw new IllegalArgumentException("Processing instruction data must not contain '?>'.");
    }
    deNude();
    append("<?");
    append(target);
    if (data != null && !data.isEmpty()) {
      append(' ');
      append(data);
    }
    append("?>");
    if (this.indent) {
      append('\n');
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> cdata(@Nullable String data) {
    if (data == null) {
      return this;
    }
    final String end = "]]>";
    if (data.contains(end))
      throw new IllegalArgumentException("CDATA sections must not contain ']]>'");
    deNude();
    append("<![CDATA[");
    append(data);
    append(end);
    return this;
  }

  // Attribute methods
  // ----------------------------------------------------------------------------------------------

  @Override
  public XmlAppendable<T> attributes(Map<String, String> map) {
    for (Entry<String, String> attr : map.entrySet()) {
      attribute(attr.getKey(), attr.getValue());
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> attribute(String name, String value) {
    if (!this.isNude) throw new IllegalStateException("Cannot write attribute: too late!");
    Objects.requireNonNull(name, "Attribute name must not be null.");
    Objects.requireNonNull(value, "Attribute value must not be null.");
    append(' ');
    append(name);
    append('=');
    append('"');
    appendAttrValue(value, 0, value.length());
    append('"');
    return this;
  }

  @Override
  public final XmlAppendable<T> attribute(String name, double value) {
    return appendRawAttr(name, Double.toString(value));
  }

  @Override
  public XmlAppendable<T> attribute(String name, long value) {
    return appendRawAttr(name, Long.toString(value));
  }

  @Override
  public XmlAppendable<T> attribute(String name, boolean value) {
    return appendRawAttr(name, Boolean.toString(value));
  }

  private XmlAppendable<T> appendRawAttr(String name, String value) {
    if (!this.isNude) throw new IllegalStateException("Cannot write attribute: too late!");
    Objects.requireNonNull(name, "Attribute name must not be null.");
    append(' ');
    append(name);
    append('=');
    append('"');
    append(value);
    append('"');
    return this;
  }

  // Open/close specific elements
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes a start element tag correctly indented.
   *
   * <p>It is the same as <code>openElement(null, name, false)</code>
   *
   * @param name The name of the element
   */
  @Override
  public XmlAppendable<T> openElement(String name) {
    return openElement(name, false);
  }

  /**
   * Writes a start element tag correctly indented.
   *
   * <p>Use the <code>hasChildren</code> parameter to specify whether this element is
   * terminal node or not, which affects the indenting.
   *
   * <p>The name can contain attributes and should be a valid xml name.
   *
   * @param name        The name of the element.
   * @param hasChildren <code>true</code> if this element has children.
   */
  @Override
  public XmlAppendable<T> openElement(String name, boolean hasChildren) {
    deNude();
    if (peekElement().hasChildren) {
      indent();
    }
    this.elements.add(new Element(name, hasChildren));
    append('<');
    append(name);
    this.isNude = true;
    this.depth++;
    return this;
  }

  /**
   * Write the end element tag.
   *
   * @throws IllegalCloseElementException If there is no element to close
   */
  @Override
  public XmlAppendable<T> closeElement() throws IllegalCloseElementException {
    Element elt = popElement();
    // reaching the end of the document
    if (elt == ROOT)
      throw new IllegalCloseElementException();
    this.depth--;
    // this is an empty element
    if (this.isNude) {
      this.append('/');
      this.isNude = false;
      // the element contains text
    } else {
      if (elt.hasChildren) {
        indent();
      }
      this.append('<');
      this.append('/');
      int x = elt.name.indexOf(' ');
      if (x < 0) {
        this.append(elt.name);
      } else {
        this.append(elt.name.substring(0, x));
      }
    }
    this.append('>');
    // take care of the new line if the indentation is on
    if (this.indent) {
      Element parent = peekElement();
      if (parent.hasChildren && parent != ROOT) {
        this.append('\n');
      }
    }
    return this;
  }

  /**
   * Same as <code>emptyElement(null, element);</code>.
   *
   * <p>It is possible for the element to contain attributes,
   * however, since there is no character escaping, great care
   * must be taken not to introduce invalid characters. For
   * example:
   * <pre>
   *    &lt;<i>example test="yes"</i>/&gt;
   * </pre>
   *
   * @param element the name of the element
   */
  @Override
  public XmlAppendable<T> emptyElement(String element) {
    deNude();
    indent();
    this.append('<');
    this.append(element);
    this.append('/');
    this.append('>');
    if (this.indent) {
      Element parent = peekElement();
      if (parent.hasChildren && parent != ROOT) {
        this.append('\n');
      }
    }
    return this;
  }

  @Override
  public XmlAppendable<T> element(String name, String text) {
    return this.openElement(name).text(text).closeElement();
  }

  @Override
  public XmlAppendable<T> element(String name, long text) {
    return this.openElement(name).xml(Long.toString(text)).closeElement();
  }

  @Override
  public XmlAppendable<T> element(String name, double text) {
    return this.openElement(name).xml(Double.toString(text)).closeElement();
  }

  /**
   * Returns the last element in the list.
   *
   * @return The current element.
   */
  private Element peekElement() {
    return this.elements.get(this.elements.size() - 1);
  }

  /**
   * Removes the last element in the list.
   *
   * @return The current element.
   */
  private Element popElement() {
    return this.elements.remove(this.elements.size() - 1);
  }

  /**
   * Close the writer.
   *
   * @throws UnclosedElementException If an element has been left open.
   */
  @Override
  public void close() throws UnclosedElementException {
    Element open = peekElement();
    if (open != ROOT)
      throw new UnclosedElementException(open.name);
    if (this.xml instanceof Closeable) {
      try {
        ((Closeable)this.xml).close();
      } catch (IOException ex) {
        throw new XmlWriteFailureException(ex);
      }
    }
  }

  @Override
  public void flush() {
    if (this.xml instanceof Flushable) {
      try {
        ((Flushable)this.xml).flush();
      } catch (IOException ex) {
        throw new XmlWriteFailureException(ex);
      }
    }
  }

  // Base class and convenience methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes the end of the open element tag.
   *
   * <p>After this method is invoked it is not possible to write attributes
   * for an element.
   */
  private void deNude() {
    if (this.isNude) {
      append('>');
      if (peekElement().hasChildren && this.indent) {
        append('\n');
      }
      this.isNude = false;
    }
  }

  final T appendable() {
    return this.xml;
  }

  final void checkCanSetIndent(@Nullable String spaces) {
    if (this.used) {
      throw new IllegalStateException("The writer has already been used.");
    }
    if (spaces != null) {
      for (int i = 0; i < spaces.length(); i++) {
        if (!Character.isSpaceChar(spaces.charAt(i))) {
          throw new IllegalArgumentException("Not a valid indentation string.");
        }
      }
    }
  }

  /**
   * Insert the correct amount of space characters depending on the depth and if
   * the <code>indent</code> flag is set to <code>true</code>.
   */
  void indent() {
    String spaces = this.indentChars;
    if (this.indent && spaces != null) {
      for (int i = 0; i < this.depth; i++) {
        append(spaces);
      }
    }
  }

  private void appendAttrValue(CharSequence ch, int off, int len) {
    appendEscaped(ch, off, len, true);
  }

  private void appendText(CharSequence ch, int off, int len) {
    appendEscaped(ch, off, len, false);
  }

  private void appendEscaped(CharSequence ch, int off, int len, boolean attr) {
    int end = off + len;
    int segmentStart = off;
    for (int i = off; i < end; i++) {
      char c = ch.charAt(i);
      String replacement = null;
      if (c == '<') {
        replacement = "&lt;";
      } else if (c == '&') {
        replacement = "&amp;";
      } else if (!attr && c == '>') {
        replacement = "&gt;";
      } else if (attr && c == '"') {
        replacement = "&quot;";
      } else if (attr && c == '\'') {
        replacement = "&#39;";
      } else if (Character.isHighSurrogate(c)) {
        if (i + 1 < end && Character.isLowSurrogate(ch.charAt(i + 1))) {
          if (segmentStart < i) {
            append(ch, segmentStart, i);
          }
          append("&#x");
          append(Integer.toHexString(Character.toCodePoint(c, ch.charAt(i + 1))));
          append(';');
          i++;
          segmentStart = i + 1;
          continue;
        }
        replacement = "";
      } else if (Character.isLowSurrogate(c) || !isXmlCharacter(c)) {
        replacement = "";
      }
      if (replacement != null) {
        if (segmentStart < i) {
          append(ch, segmentStart, i);
        }
        if (!replacement.isEmpty()) {
          append(replacement);
        }
        segmentStart = i + 1;
      }
    }
    if (segmentStart < end) {
      append(ch, segmentStart, end);
    }
  }

  private static boolean isXmlCharacter(char c) {
    return c == '\n' || c == '\r' || c == '\t'
        || (c >= 0x20 && c <= 0xD7FF)
        || (c >= 0xE000 && c <= 0xFFFD);
  }

  private Appendable append(CharSequence csq) throws XmlWriteFailureException {
    try {
      this.used = true;
      return this.xml.append(csq);
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
  }

  private Appendable append(CharSequence csq, int start, int end) throws XmlWriteFailureException {
    try {
      this.used = true;
      return this.xml.append(csq, start, end);
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
  }

  private Appendable append(char c) throws XmlWriteFailureException {
    try {
      this.used = true;
      return this.xml.append(c);
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
  }

  // Inner class: Element
  // ----------------------------------------------------------------------------------------------

  /**
   * A light object to keep track of the element.
   *
   * <p>This object does not support namespaces.
   *
   * @author Christophe Lauret
   * @version 7 March 2005
   */
  private static final class Element {

    /**
     * The fully qualified name of the element.
     */
    private final String name;

    /**
     * Indicates whether the element has children.
     */
    private final boolean hasChildren;

    /**
     * Creates a new Element.
     *
     * @param name       The qualified name of the element.
     * @param hasChildren Whether the element has children.
     */
    public Element(String name, boolean hasChildren) {
      this.name = name;
      this.hasChildren = hasChildren;
    }
  }

}
