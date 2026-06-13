/*
 * Copyright 2015 Allette Systems (Australia)
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
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.IOException;
import java.util.Objects;

/**
 * Adapts a {@link XmlWriter} to the legacy {@link XMLWriter} interface.
 *
 * <p>Use this class when code expects a legacy {@link XMLWriter} but you have a {@link XmlWriter},
 * for example when passing to {@link org.pageseeder.xmlwriter.XMLWritable#toXML(XMLWriter)}.
 * {@link XmlWriteFailureException} thrown by the underlying writer is unwrapped back to
 * {@link IOException} to satisfy the {@link XMLWriter} contract.
 *
 * <p>Namespace-aware methods ({@code openElement(String, String, boolean)},
 * {@code emptyElement(String, String)}, {@code attribute(String, String, ...)},
 * {@code setPrefixMapping(String, String)}) throw {@link UnsupportedOperationException}
 * because {@link XmlWriter} does not model namespaces through separate URI parameters.
 *
 * <p>{@link #close()} is intentionally a no-op: the lifetime of the wrapped
 * {@link XmlWriter} is managed by the caller, not by this adapter.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.3
 * @since 0.13.3
 */
public final class XmlWriterAdapter implements XMLWriter {

  private final XmlWriter xml;

  /**
   * Creates an adapter wrapping the given {@link XmlWriter}.
   *
   * @param xml the writer to wrap; must not be {@code null}
   */
  public XmlWriterAdapter(XmlWriter xml) {
    this.xml = Objects.requireNonNull(xml, "xml");
  }

  @Override
  public void xmlDecl() throws IOException {
    try { this.xml.declaration(); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void setIndentChars(@Nullable String spaces) {
    // XmlWriter does not expose indentation control after construction
  }

  @Override
  public void writeText(char c) throws IOException {
    try { this.xml.text(c); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeText(@Nullable String text) throws IOException {
    if (text == null) return;
    try { this.xml.text(text); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeText(char[] text, int off, int len) throws IOException {
    try { this.xml.text(text, off, len); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeCDATA(String data) throws IOException {
    try { this.xml.cdata(data); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeXML(@Nullable String text) throws IOException {
    if (text == null) return;
    try { this.xml.xml(text); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeXML(char[] text, int off, int len) throws IOException {
    try { this.xml.xml(text, off, len); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writeComment(String comment) throws IOException {
    try { this.xml.comment(comment); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void writePI(String target, String data) throws IOException {
    try { this.xml.processingInstruction(target, data); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void openElement(String name) throws IOException {
    try { this.xml.openElement(name); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void openElement(String name, boolean hasChildren) throws IOException {
    try { this.xml.openElement(name, hasChildren); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  /**
   * @throws UnsupportedOperationException always — namespace-aware open is not supported
   */
  @Override
  public void openElement(String uri, String name, boolean hasChildren) {
    throw new UnsupportedOperationException("Namespace-aware openElement is not supported by XmlWriterAdapter");
  }

  @Override
  public void closeElement() throws IOException {
    try { this.xml.closeElement(); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void element(String name, String text) throws IOException {
    try { this.xml.element(name, text); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void emptyElement(String element) throws IOException {
    try { this.xml.emptyElement(element); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  /**
   * @throws UnsupportedOperationException always — namespace-aware emptyElement is not supported
   */
  @Override
  public void emptyElement(String uri, String element) {
    throw new UnsupportedOperationException("Namespace-aware emptyElement is not supported by XmlWriterAdapter");
  }

  @Override
  public void attribute(String name, String value) throws IOException {
    try { this.xml.attribute(name, value); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void attribute(String name, int value) throws IOException {
    try { this.xml.attribute(name, value); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  @Override
  public void attribute(String name, long value) throws IOException {
    try { this.xml.attribute(name, value); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  /**
   * @throws UnsupportedOperationException always — namespace-aware attribute is not supported
   */
  @Override
  public void attribute(String uri, String name, String value) {
    throw new UnsupportedOperationException("Namespace-aware attribute is not supported by XmlWriterAdapter");
  }

  /**
   * @throws UnsupportedOperationException always — namespace-aware attribute is not supported
   */
  @Override
  public void attribute(String uri, String name, int value) {
    throw new UnsupportedOperationException("Namespace-aware attribute is not supported by XmlWriterAdapter");
  }

  /**
   * @throws UnsupportedOperationException always — namespace-aware attribute is not supported
   */
  @Override
  public void attribute(String uri, String name, long value) {
    throw new UnsupportedOperationException("Namespace-aware attribute is not supported by XmlWriterAdapter");
  }

  /**
   * @throws UnsupportedOperationException always — namespace prefix mapping is not supported
   */
  @Override
  public void setPrefixMapping(String uri, String prefix) {
    throw new UnsupportedOperationException("Namespace prefix mapping is not supported by XmlWriterAdapter");
  }

  @Override
  public void flush() throws IOException {
    try { this.xml.flush(); } catch (XmlWriteFailureException e) { throw cause(e); }
  }

  /**
   * Does nothing — the underlying {@link XmlWriter} is managed by the caller.
   */
  @Override
  public void close() {
    // intentional no-op
  }

  private static IOException cause(XmlWriteFailureException e) {
    Throwable cause = e.getCause();
    return cause instanceof IOException ? (IOException) cause : new IOException(e);
  }
}
