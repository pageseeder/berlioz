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

import org.eclipse.jdt.annotation.Nullable;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
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
 * @version Berlioz 0.12.0
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
  final T _xml;

  /**
   * Encoding of the output xml.
   */
  final String _encoding = "utf-8";

  /**
   * Indicates whether the xml should be indented or not.
   *
   * <p>The default is <code>true</code> (indented).
   *
   * <p>The indentation is 2 white-spaces.
   */
  boolean indent;

  /**
   * The default indentation spaces used.
   */
  private final @Nullable String _indentChars;

  /**
   * A stack of elements to close the elements automatically.
   */
  private final List<Element> _elements = new ArrayList<>();

  /**
   * Level of the depth of the xml document currently produced.
   *
   * <p>This attribute changes depending on the state of the instance.
   */
  int depth = 0;

  /**
   * Flag to indicate that the element open tag is not finished yet.
   */
  boolean isNude = false;

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
    this._xml = Objects.requireNonNull(xml, "XmlWriter cannot use a null writer.");
    this.indent = indentChars != null;
    this._indentChars = indentChars;
    this._elements.add(ROOT);
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
  public XmlAppendable<T> withIndent(String spaces) {
    // check that this is a valid indentation string
    if (spaces != null) {
      for (int i = 0; i < spaces.length(); i++) {
        if (!Character.isSpaceChar(spaces.charAt(i)))
          throw new IllegalArgumentException("Not a valid indentation string.");
      }
    }
    return new XmlAppendable<>(this._xml, spaces);
  }

  // Write text methods
  // ----------------------------------------------------------------------------------------------

  @Override
  public final XmlAppendable<T> text(String text) {
    return text(text.toCharArray(), 0, text.length());
  }

  @Override
  public final XmlAppendable<T> text(long number) {
    deNude();
    try {
      this._xml.append(Long.toString(number));
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> text(double number) {
    deNude();
    try {
      this._xml.append(Double.toString(number));
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> text(char[] text, int off, int len) {
    deNude();
    try {
      char c = ' ';
      for (int i = off; i < off+len; i++) {
        c = text[i];
        // '<' always replace with '&lt;'
        if (c == '<') {
          this._xml.append("&lt;");
        } else if (c == '>') {
          this._xml.append("&gt;");
        } else if (c == '&') {
          this._xml.append("&amp;");
        } else if (c == '\n' || c == '\r' || c == '\t') {
          this._xml.append(c);
        } else if (c < 0x20 || c >= 0x7F && c < 0xA0) {
          // Do nothing
        } else if (c >= 0xD800 && c <= 0xDFFF) {
          int codePoint = Character.codePointAt(text, i, len);
          i += Character.charCount(codePoint) - 1;
          this._xml.append("&#x");
          this._xml.append(Integer.toHexString(codePoint));
          this._xml.append(";");
        } else {
          this._xml.append(c);
        }
      }
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> text(char c) {
    deNude();
    try {
      // '<' must always be escaped
      if (c == '<') {
        this._xml.append("&lt;");
      } else if (c == '>') {
        this._xml.append("&gt;");
      } else if (c == '&') {
        this._xml.append("&amp;");
      } else if (c == '\n' || c == '\r' || c == '\t') {
        this._xml.append(c);
      } else if (c < 0x20 || c >= 0x7F && c < 0xA0) {
        // Do nothing
      } else {
        this._xml.append(c);
      }
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
    return this;
  }

  /**
   * Writes the string value of an object.
   *
   * <p>Does nothing if the object is <code>null</code>.
   *
   * @param o The object that should be written as text.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  public XmlAppendable<T> asText(@Nullable Object o) {
    this.text(o != null ? o.toString() : "null");
    return this;
  }

  /**
   * Writes the string value of an object.
   *
   * <p>Does nothing if the object is <code>null</code>.
   *
   * @param o The object that should be written as text.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  public XmlAppendable<T> asXml(@Nullable Object o) {
    // FIXME
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
    deNude();
    for (int i=off; i<off+len; i++) {
      append(xml[i]);
    }
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
    append("<?xml version=\"1.0\" encoding=\""+this._encoding+"\"?>");
    if (this.indent) {
      append('\n');
    }
  }

  @Override
  public final XmlAppendable<T> comment(String comment) throws IllegalArgumentException {
    if (comment.indexOf("--") >= 0)
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
    deNude();
    try {
      this._xml.append("<?");
      this._xml.append(target);
      this._xml.append(' ');
      this._xml.append(data);
      this._xml.append("?>");
      if (this.indent) {
        this._xml.append('\n');
      }
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public final XmlAppendable<T> cdata(String data) {
    final String end = "]]>";
    if (data.indexOf(end) >= 0)
      throw new IllegalArgumentException("CDATA sections must not contain \']]>\'");
    deNude();
    try {
      this._xml.append("<![CDATA[");
      this._xml.append(data);
      this._xml.append(end);
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
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
    try {
      this._xml.append(' ');
      this._xml.append(name);
      this._xml.append('=');
      this._xml.append('"');
      appendAttrValue(value.toCharArray(), 0, value.length());
      this._xml.append('"');
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
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

  private final XmlAppendable<T> appendRawAttr(String name, String value) {
    if (!this.isNude) throw new IllegalStateException("Cannot write attribute: too late!");
    try {
      this._xml.append(' ');
      this._xml.append(name);
      this._xml.append('=');
      this._xml.append('"');
      this._xml.append(value);
      this._xml.append('"');
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
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
   *
   * @throws IOException If thrown by the wrapped writer.
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
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  @Override
  public XmlAppendable<T> openElement(String name, boolean hasChildren) {
    deNude();
    if (peekElement().hasChildren) {
      indent();
    }
    this._elements.add(new Element(name, hasChildren));
    append('<');
    append(name);
    this.isNude = true;
    this.depth++;
    return this;
  }

  /**
   * Write the end element tag.
   *
   * @throws IOException If thrown by the wrapped writer.
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
   *
   * @throws IOException If thrown by the wrapped writer.
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
    return this._elements.get(this._elements.size() - 1);
  }

  /**
   * Removes the last element in the list.
   *
   * @return The current element.
   */
  private Element popElement() {
    return this._elements.remove(this._elements.size() - 1);
  }

  /**
   * Close the writer.
   *
   * @throws IOException If thrown by the wrapped writer.
   * @throws UnclosedElementException If an element has been left open.
   */
  @Override
  public void close() throws UnclosedElementException {
    Element open = peekElement();
    if (open != ROOT)
      throw new UnclosedElementException(open.name);
    if (this._xml instanceof Closeable) {
      try {
        ((Closeable)this._xml).close();
      } catch (IOException ex) {
        throw new XmlWriteFailureException(ex);
      }
    }
  }

  @Override
  public void flush() {
    if (this._xml instanceof Flushable) {
      try {
        ((Flushable)this._xml).flush();
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

  /**
   * Insert the correct amount of space characters depending on the depth and if
   * the <code>indent</code> flag is set to <code>true</code>.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  void indent() {
    String spaces = this._indentChars;
    if (this.indent && spaces != null) {
      for (int i = 0; i < this.depth; i++) {
        append(spaces);
      }
    }
  }

  private void appendAttrValue(char[] ch, int off, int len) {
    char c;
    try {
      for (int i = off; i < off+len; i++) {
        c = ch[i];
        // '<' always replace with '&lt;'
        if      (c == '<') {
          this._xml.append("&lt;");
        } else if (c == '&') {
          this._xml.append("&amp;");
        } else if (c == '"') {
          this._xml.append("&quot;");
        } else if (c == '\'') {
          this._xml.append("&#39;");
        } else if (c == '\n' || c == '\r' || c == '\t') {
          this._xml.append(c);
        } else if (c < 0x20 || c >= 0x7F && c < 0xA0) {
          // Do nothing
        } else if (c >= 0xD800 && c <= 0xDFFF) {
          int codePoint = Character.codePointAt(ch, i, len);
          i += Character.charCount(codePoint) - 1;
          this._xml.append("&#x");
          this._xml.append(Integer.toHexString(codePoint));
          this._xml.append(";");
        } else {
          this._xml.append(c);
        }
      }
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
  }

  private final Appendable append(CharSequence csq) throws XmlWriteFailureException {
    try {
      return this._xml.append(csq);
    } catch (IOException ex) {
      throw new XmlWriteFailureException(ex);
    }
  }

  private final Appendable append(char c) throws XmlWriteFailureException {
    try {
      return this._xml.append(c);
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
