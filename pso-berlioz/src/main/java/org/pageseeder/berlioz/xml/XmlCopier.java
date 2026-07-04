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
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Copy the parsed XML to the specified {@link XmlWriter}.
 *
 * <p>The source is parsed into an in-memory buffer first; the target {@link XmlWriter} is only
 * touched once parsing has completed successfully. This guarantees that a malformed source never
 * leaves the target writer with a dangling open element — on error, the target instead receives a
 * single {@code <no-data>} element, same as a successful copy produces a single well-formed
 * document.
 *
 * <p>This class also implements the {@link LexicalHandler} interface, so that comments can be copied if the
 * {@link XMLReader} reader supports the {@value #LEXICAL_HANDLER_PROPERTY} property.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class XmlCopier extends DefaultHandler implements ContentHandler, LexicalHandler {

  /**
   * Logger the extractor.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XmlCopier.class);

  /**
   * The LexicalHandler property.
   */
  @SuppressWarnings("HttpUrlsUsage")
  private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

  /**
   * Whether comments are supported (optimistically assumes they are).
   */
  private static volatile boolean supportsComments = true;

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

  // Static helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Copy the specified File to the given XML Writer.
   *
   * <p>Any error is reported as XML on the XML writer.
   *
   * <p>This method does not perform any caching, generators better handle caching externally.
   *
   * @param file The file.
   * @param xml  The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(File file, XmlWriter xml) {
    if (!file.exists()) {
      LOGGER.warn("Could not find {}", file.toURI());
      xml.openElement("no-data");
      xml.attribute("error", "file-not-found");
      xml.closeElement();
      return false;
    }
    XmlStringBuilder buffer = new XmlStringBuilder();
    try {
      parse(new XmlCopier(buffer), new InputSource(file.toURI().toString()));
      xml.xml(buffer.toString());
      return true;
    } catch (BerliozException ex) {
      LOGGER.warn("An error was reported by the parser while parsing {}", file.toURI());
      handleError(xml, ex);
      return false;
    }
  }

  /**
   * Copy the specified Reader to the given XML Writer.
   *
   * <p>Any error is reported as XML on the XML writer. This method does not perform any caching
   * or validation.
   *
   * @param reader The reader over the XML to read.
   * @param xml    The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   */
  public static boolean copyTo(Reader reader, XmlWriter xml) {
    XmlStringBuilder buffer = new XmlStringBuilder();
    try {
      parse(new XmlCopier(buffer), new InputSource(reader));
      xml.xml(buffer.toString());
      return true;
    } catch (BerliozException ex) {
      LOGGER.warn("An error was reported by the parser while parsing reader");
      handleError(xml, ex);
      return false;
    }
  }

  // private parsing methods
  // --------------------------------------------------------------------------------------------------------

  /**
   * Parses the specified file using the given handler.
   *
   * @param copier The XmlCopier instance.
   * @param source The input source to copy
   *
   * @throws BerliozException Should something unexpected happen.
   */
  private static void parse(XmlCopier copier, InputSource source) throws BerliozException {
    SAXParser parser = Xml.safeParser(false);
    try {
      // get the reader
      XMLReader xmlreader = parser.getXMLReader();
      // configure the reader
      xmlreader.setContentHandler(copier);
      trySettingLexicalHandler(xmlreader, copier);
      xmlreader.setEntityResolver(BerliozEntityResolver.getInstance());
      xmlreader.setErrorHandler(BerliozErrorHandler.getInstance());
      xmlreader.parse(source);
    } catch (SAXException ex) {
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (IOException ex) {
      LOGGER.error("Could not read file.", ex);
      throw new BerliozException("Could not read file.", ex);
    }
  }

  /**
   * Try to set the lexical handler property to copy comments.
   *
   * <p>If the property is not supported, a warning is logged and no further attempts will be made.
   *
   * @param xmlreader the XML reader.
   * @param copier    the XML copy handler.
   */
  private static void trySettingLexicalHandler(XMLReader xmlreader, XmlCopier copier) {
    if (supportsComments) {
      try {
        xmlreader.setProperty(LEXICAL_HANDLER_PROPERTY, copier);
      } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
        supportsComments = false;
        LOGGER.warn("Unable to copy comments", ex);
      }
    }
  }

  private static void handleError(XmlWriter xml, Exception ex) {
    String m = ex.getMessage();
    Throwable cause = ex.getCause();
    LOGGER.warn("Error details:", ex);
    xml.openElement("no-data");
    xml.attribute("error", "parsing");
    xml.attribute("details", m != null? m : "(No message)");
    if (cause instanceof SAXParseException) {
      SAXParseException sax = (SAXParseException)cause;
      xml.attribute("line", sax.getLineNumber());
      xml.attribute("column", sax.getColumnNumber());
    }
    xml.closeElement();
  }
}
