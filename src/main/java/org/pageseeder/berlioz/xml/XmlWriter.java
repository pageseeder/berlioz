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

import java.io.IOException;
import java.util.Map;

/**
 * Defines a writer for XML data.
 *
 * <p>The purpose of this interface is to provide a simple, efficient and convenient set of
 * methods for writing XML.</p>
 *
 * <p>Most implementations should wrap a writer or an output stream. Implementations can be focused
 * on performance, reliability, error reporting, etc... Generally a compromise will be required
 * to balance the performance with the simplicity of the code.</p>
 *
 * <p>The design principle has been to favour simplicity and efficiency for the most common cases
 * rather that cover the full spectrum of XML features.</p>
 *
 * <p>Here are the recommendations for implementation:</p>
 * <ul>
 *   <li>All implementations MUST escape text nodes and attributes values appropriately;</li>
 *   <li>Implementations SHOULD check correct nesting of elements to help detect and debug runtime
 *   errors;</li>
 *   <li>Implementations SHOULD NOT check that name of an element or attribute is valid, developers
 *   are expected to send the correct names since name are not expected to be variable;</li>
 *   <li>Implementations MAY check that raw XML is well-formed, the {@link #xml(String)}; and
 *   {@link #xml(char[], int, int)} methods are provided as an escape hatch in case an XML can be
 *   produced elsewhere;</li>
 *   <li>Implementations MAY copy comments or CDATA section verbatim, for the same reason;</li>
 *   <li>Implementations SHOULD NOT manage namespaces, developers are expected to manage namespaces
 *   themselves, namespaces can be declared using the {@link #attribute(String, String)} method;</li>
 * </ul>
 *
 * <p>For improved performance, the most efficient solution will generally to have an
 * implementation write on a buffered writer since the memory usage will generally be
 * restricted little more than the size of the buffer, and this will keep the I/O
 * operation to a minimum.
 *
 * <p>Other implementations might want to wrap a SAX content handler.
 *
 * <p>This interface follow the fluent-style API for easy method chaining.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public interface XmlWriter {

  // Text methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes the given character correctly for the encoding of this document.
   *
   * @param c The character to write.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter text(char c);

  /**
   * Writes the given number as a text node.
   *
   * @param text The text to write
   *
   * @throws XmlWriteFailureException If an I/O exception is thrown by the underlying writer.
   */
  XmlWriter text(long text);

  /**
   * Writes the given number as a text node.
   *
   * @param text The text to write
   *
   * @throws XmlWriteFailureException If an I/O exception is thrown by the underlying writer.
   */
  XmlWriter text(double text);

  /**
   * Writes the given text correctly for the encoding of this document.
   *
   * @param text The text to write
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter text(String text);

  /**
   * Write the given text correctly for the encoding of this document.
   *
   * @param text The text to write.
   * @param off  The offset where we should start writing the string.
   * @param len  The length of the character subarray to write.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter text(char[] text, int off, int len);

  // XML methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes the given XML data.
   *
   * <p>The text is appended as is, therefore it should be escaped properly for the
   * encoding used by the underlying stream writer.
   *
   * @param xml The raw XML content to write.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter xml(String xml);

  /**
   * Write the given XML data.
   *
   * <p>The text is appended as is, therefore it should be escaped properly for the
   * encoding used by the underlying stream writer.
   *
   * @param text The text to write.
   * @param off  The offset where we should start writing the string.
   * @param len  The length of the character subarray to write.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter xml(char[] text, int off, int len);


  XmlWriter asXml(XmlWritable object);

  // Comments, CDATA, PIs and XML declaration
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes an XML comment.
   *
   * <p>An XML comment is:<br>
   * <pre>
   *   &lt;!-- <i>comment</i> --&gt;
   * </pre>
   *
   * <p>Comments are not indented.
   *
   * <p>Does not write anything if the comment if <code>null</code>.
   *
   * @param comment The comment to be written
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter comment(String comment);

  /**
   * Writes the given text as a CDATA section.
   *
   * <p>Does nothing if the text is <code>null</code>.
   *
   * @param data The data to write inside the CDATA section.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   * @throws IllegalArgumentException If the implementation does not support CDATA nesting
   */
  XmlWriter cdata(String data);

  /**
   * Writes an XML processing instruction.
   *
   * <p>An XML processing instruction is:<br>
   * <pre>
   *   &lt;?<i>target</i> <i>data</i>?&gt;
   * </pre>
   *
   * @param target The PI's target.
   * @param data   The PI's data.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter processingInstruction(String target, @Nullable String data);

  /**
   * Writes the XML declaration.
   *
   * <p>Always:
   * <pre>
   *   &lt;?xml version="1.0" encoding="<i>encoding</i>"?&gt;
   * </pre>
   *
   * <p>It is followed by a new line character if the indentation is turned on.
   *
   * @throws IllegalStateException If this method is called after the writer has started
   *                               writing elements nodes.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  void declaration();

  // Open/close elements
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes a start element tag correctly indented.
   *
   * <p>It is the same as <code>openElement(name, false)</code>
   *
   * @see #openElement(String, boolean)
   *
   * @param name the name of the element
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter openElement(String name);

  /**
   * Writes a start element tag correctly indented.
   *
   * <p>Use the <code>hasChildren</code> parameter to specify whether this element is terminal
   * node or not, note: this affects the indenting.
   *
   * <p>The name can contain attributes and should be a valid xml name.
   *
   * @param name        The name of the element
   * @param hasChildren true if this element has children
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter openElement(String name, boolean hasChildren);

  /**
   * Close the element automatically.
   *
   * <p>The element is closed symmetrically to the
   * {@link #openElement(String, boolean)} method.
   *
   * @throws XmlWriteFailureException If an I/O exception occurs.
   */
  XmlWriter closeElement();

  // Element shortcuts
  // ----------------------------------------------------------------------------------------------

  /**
   * Opens element, inserts text node and closes.
   *
   * <p>This method should behave like:
   * <pre>
   *   this.openElement(name, false);
   *   this.text(text);
   *   this.closeElement();
   * </pre>
   *
   * @param name The name of the element.
   * @param text The text of the element.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  XmlWriter element(String name, String text);

  /**
   * Opens element, inserts text node and closes.
   *
   * <p>This method should behave like:
   * <pre>
   *   this.openElement(name, false);
   *   this.text(text);
   *   this.closeElement();
   * </pre>
   *
   * @param name The name of the element.
   * @param text The text of the element.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  XmlWriter element(String name, long text);

  /**
   * Opens element, inserts text node and closes.
   *
   * <p>This method should behave like:
   * <pre>
   *   this.openElement(name, false);
   *   this.text(text);
   *   this.closeElement();
   * </pre>
   *
   * @param name The name of the element.
   * @param text The text of the element.
   *
   * @throws IOException If thrown by the wrapped writer.
   */
  XmlWriter element(String name, double text);

  /**
   * Writes an empty element.
   *
   * @param element the name of the element
   */
  XmlWriter emptyElement(String element);

  // Attributes
  // ----------------------------------------------------------------------------------------------

  /**
   * Writes an attribute.
   *
   * @param name  The name of the attribute.
   * @param value The value of the attribute.
   *
   * @throws IllegalStateException If there is no open element or text has been written.
   */
  XmlWriter attribute(String name, String value);

  /**
   * Writes an attribute.
   *
   * <p>This method for number does not require escaping.
   *
   * @param name  The name of the attribute.
   * @param value The value of the attribute.
   *
   * @throws IllegalStateException If there is no open element or text has been written.
   */
  XmlWriter attribute(String name, long value);

  /**
   * Writes an attribute.
   *
   * <p>This method for number does not require escaping.
   *
   * @param name  The name of the attribute.
   * @param value The value of the attribute.
   *
   * @throws IllegalStateException If there is no open element or text has been written.
   */
  XmlWriter attribute(String name, double value);

  /**
   * Writes an attribute with the value <code>true</code> or <code>false</code>.
   *
   * @param name  The name of the attribute.
   * @param value The value of the attribute.
   *
   * @throws IllegalStateException If there is no open element or text has been written.
   */
  XmlWriter attribute(String name, boolean value);

  /**
   * Writes a map of attributes.
   *
   * @param map  A map of name/value pairs of the attributes.
   *
   * @throws IllegalStateException If there is no open element or text has been written.
   */
  XmlWriter attributes(Map<String, String> map);

  // Direct access to the writer
  // ----------------------------------------------------------------------------------------------

  /**
   * Flush the writer.
   */
  void flush();

  /**
   * Close the writer.
   *
   * @throws UnclosedElementException If there is still an open element.
   */
  void close();

}
