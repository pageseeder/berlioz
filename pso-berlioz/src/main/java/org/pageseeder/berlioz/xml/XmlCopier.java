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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Copy the parsed XML to the specified {@link XmlWriter}.
 *
 * <p>The source is parsed into an in-memory buffer first; the target {@link XmlWriter} is only
 * touched once parsing has completed successfully. This guarantees that a malformed source never
 * leaves the target writer with a dangling open element — on error, the target instead receives a
 * single {@code <copy-error>} element, same as a successful copy produces a single well-formed
 * document.
 *
 * <p>Two API shapes are provided:
 * <ul>
 *   <li>{@link #copy(File, XmlWriter)} / {@link #copy(Reader, XmlWriter)} — throw
 *       {@link XmlParseException} (or {@link UncheckedIOException} if the file cannot be found)
 *       so the caller decides how to handle a bad source.</li>
 *   <li>{@link #copyTo(File, XmlWriter)} / {@link #copyTo(Reader, XmlWriter)} — never throw;
 *       errors are reported inline as a {@code <copy-error>} element and {@code false} is
 *       returned. The element always carries a {@code reason} ({@code not-found} or
 *       {@code parsing}) and a {@code filename} when copying from a {@link File}; the
 *       {@code message}/{@code line}/{@code column} attributes are only added when
 *       {@code includeDetails} is {@code true}.</li>
 * </ul>
 *
 * <p>This class also implements the {@link LexicalHandler} interface, so that comments are copied
 * when the underlying parser supports it (see {@link Xml#parse}).
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class XmlCopier extends DefaultHandler implements ContentHandler, LexicalHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlCopier.class);

  /**
   * Where the XML should be copied to.
   */
  private final XmlWriter to;

  /**
   * The prefix mapping to add to the next <i>startElement</i> event.
   */
  private final Map<String, String> mapping = new HashMap<>();

  /**
   * Creates a new XmlCopier wrapping the specified XML writer.
   *
   * @param xml The XML writer to use.
   */
  private XmlCopier(XmlWriter xml) {
    this.to = xml;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) {
    this.to.openElement(qName);
    for (int i = 0; i < atts.getLength(); i++) {
      String name = atts.getQName(i);
      String value = atts.getValue(i);
      if (name != null && value != null) {
        this.to.attribute(name, value);
      }
    }
    // Put the prefix mapping was reported BEFORE the startElement was reported...
    if (!this.mapping.isEmpty()) {
      for (Entry<String, String> e : this.mapping.entrySet()) {
        boolean hasPrefix = !e.getKey().isEmpty();
        this.to.attribute("xmlns"+(hasPrefix? ":"+ e.getKey() : e.getKey()), e.getValue());
      }
      this.mapping.clear();
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    this.to.text(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    this.to.closeElement();
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) {
    this.mapping.put(prefix, uri);
  }

  @Override
  public void processingInstruction(String target, @Nullable String data) {
    this.to.processingInstruction(target, data != null ? data : "");
  }

  // Lexical Handler =============================================================================

  /**
   * Copy the comment to the output.
   * {@inheritDoc}
   */
  @Override
  public void comment(char[] ch, int start, int length) {
    this.to.comment(String.copyValueOf(ch, start, length));
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void startCDATA() {
    // No-op
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void endCDATA() {
    // No-op
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void startDTD(String name, @Nullable String publicId, @Nullable String systemId) {
    // No-op
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void endDTD() {
    // No-op
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void startEntity(String name) {
    // No-op
  }

  /**
   * Does nothing.
   * {@inheritDoc}
   */
  @Override
  public void endEntity(String name) {
    // No-op
  }

  // Throwing API
  // ----------------------------------------------------------------------------------------------

  /**
   * Copy the specified File to the given XML Writer.
   *
   * @param file The file.
   * @param xml  The XML writer.
   *
   * @throws UncheckedIOException if the file does not exist.
   * @throws XmlParseException    if the file could not be parsed.
   */
  public static void copy(File file, XmlWriter xml) {
    if (!file.exists()) {
      throw new UncheckedIOException(new FileNotFoundException(file.toString()));
    }
    XmlStringBuilder buffer = new XmlStringBuilder();
    try {
      // disallowDoctype=false: unlike Xml.parse(File, boolean), a copy is not resolved for
      // routing/configuration, so a DOCTYPE (e.g. Berlioz's own services.xml convention) is fine.
      Xml.parse(new XmlCopier(buffer), new InputSource(file.toURI().toString()), false, false);
    } catch (BerliozException ex) {
      throw toParseException(ex);
    }
    xml.xml(buffer.toString());
  }

  /**
   * Copy the specified Reader to the given XML Writer.
   *
   * @param reader The reader over the XML to read.
   * @param xml    The XML writer.
   *
   * @throws XmlParseException if the source could not be parsed.
   */
  public static void copy(Reader reader, XmlWriter xml) {
    XmlStringBuilder buffer = new XmlStringBuilder();
    try {
      Xml.parse(new XmlCopier(buffer), new InputSource(reader), false, false);
    } catch (BerliozException ex) {
      throw toParseException(ex);
    }
    xml.xml(buffer.toString());
  }

  // Non-throwing (auto-resolving) API
  // ----------------------------------------------------------------------------------------------

  /**
   * Copy the specified File to the given XML Writer.
   *
   * <p>Any error is reported as a {@code <copy-error>} element on the XML writer, without the
   * {@code message}/{@code line}/{@code column} attributes.
   *
   * @param file The file.
   * @param xml  The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(File file, XmlWriter xml) {
    return copyTo(file, xml, false);
  }

  /**
   * Copy the specified File to the given XML Writer.
   *
   * <p>This method does not perform any caching, generators better handle caching externally.
   *
   * @param file           The file.
   * @param xml            The XML writer.
   * @param includeDetails Whether to include the {@code message}/{@code line}/{@code column}
   *                       attributes on parse failure. Callers should only pass {@code true} when
   *                       the caller's own diagnostic verbosity setting allows it.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(File file, XmlWriter xml, boolean includeDetails) {
    try {
      copy(file, xml);
      return true;
    } catch (UncheckedIOException ex) {
      LOGGER.warn("Could not find {}", file.toURI());
      writeNotFound(xml, file.getName());
      return false;
    } catch (XmlParseException ex) {
      LOGGER.warn("An error was reported by the parser while parsing {}", file.toURI());
      handleError(xml, ex, includeDetails, file.getName());
      return false;
    }
  }

  /**
   * Copy the specified Reader to the given XML Writer.
   *
   * <p>Any error is reported as a {@code <copy-error>} element on the XML writer, without the
   * {@code message}/{@code line}/{@code column} attributes.
   *
   * @param reader The reader over the XML to read.
   * @param xml    The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(Reader reader, XmlWriter xml) {
    return copyTo(reader, xml, false);
  }

  /**
   * Copy the specified Reader to the given XML Writer.
   *
   * <p>This method does not perform any caching or validation.
   *
   * @param reader         The reader over the XML to read.
   * @param xml            The XML writer.
   * @param includeDetails Whether to include the {@code message}/{@code line}/{@code column}
   *                       attributes on parse failure. Callers should only pass {@code true} when
   *                       the caller's own diagnostic verbosity setting allows it.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(Reader reader, XmlWriter xml, boolean includeDetails) {
    try {
      copy(reader, xml);
      return true;
    } catch (XmlParseException ex) {
      LOGGER.warn("An error was reported by the parser while parsing reader");
      handleError(xml, ex, includeDetails, null);
      return false;
    }
  }

  // private helpers
  // --------------------------------------------------------------------------------------------------------

  private static XmlParseException toParseException(BerliozException ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof SAXParseException) {
      SAXParseException sax = (SAXParseException) cause;
      return new XmlParseException(ex.getMessage(), ex, sax.getLineNumber(), sax.getColumnNumber());
    }
    return new XmlParseException(ex.getMessage(), ex);
  }

  private static void writeNotFound(XmlWriter xml, String filename) {
    xml.openElement("copy-error");
    xml.attribute("reason", "not-found");
    xml.attribute("filename", filename);
    xml.closeElement();
  }

  private static void handleError(XmlWriter xml, XmlParseException ex, boolean includeDetails, @Nullable String filename) {
    LOGGER.warn("Error details:", ex);
    xml.openElement("copy-error");
    xml.attribute("reason", "parsing");
    if (filename != null) {
      xml.attribute("filename", filename);
    }
    if (includeDetails) {
      String m = ex.getMessage();
      xml.attribute("message", m != null ? m : "(No message)");
      if (ex.hasLocation()) {
        xml.attribute("line", ex.getLine());
        xml.attribute("column", ex.getColumn());
      }
    }
    xml.closeElement();
  }

}
