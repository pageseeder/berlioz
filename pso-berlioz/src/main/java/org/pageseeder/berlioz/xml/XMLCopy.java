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

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;
import org.pageseeder.xmlwriter.XMLWriter;
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
 * Copy the parsed XML to the specified XML writer.
 *
 * <p>This class also implements the {@link LexicalHandler} interface, so that comments can be copied if the
 * {@link XMLReader} reader supports the {@value #LEXICAL_HANDLER_PROPERTY} property.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public final class XMLCopy extends DefaultHandler implements ContentHandler, LexicalHandler {

  /**
   * Logger the extractor.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLCopy.class);

  /**
   * The LexicalHandler property.
   */
  private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

  /**
   * Whether comments are supported (optimistically assumes they are).
   */
  private static volatile boolean supportsComments = true;

  /**
   * Where the XML should be copied to.
   */
  private final XMLWriter to;

  /**
   * The prefix mapping to add to the next <i>startElement</i> event.
   */
  private final Map<String, String> mapping = new HashMap<>();

  /**
   * Creates a new XMLExtractor wrapping the specified XML writer.
   *
   * @param xml The XML writer to use.
   */
  public XMLCopy(XMLWriter xml) {
    this.to = xml;
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      this.to.openElement(qName);
      for (int i = 0; i < atts.getLength(); i++) {
        String name = atts.getQName(i);
        String value = atts.getValue(i);
        if (name != null &&  value != null) {
          this.to.attribute(name, value);
        }
      }
      // Put the prefix mapping was reported BEFORE the startElement was reported...
      if (!this.mapping.isEmpty()) {
        for (Entry<String, String> e : this.mapping.entrySet()) {
          boolean hasPrefix = e.getKey() != null && e.getKey().length() > 0;
          this.to.attribute("xmlns"+(hasPrefix? ":"+ e.getKey() : e.getKey()), e.getValue());
        }
        this.mapping.clear();
      }
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      this.to.writeText(ch, start, length);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      this.to.closeElement();
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) {
    boolean hasPrefix = prefix != null && prefix.length() > 0;
    this.mapping.put((hasPrefix? prefix : ""), uri);
  }

  @Override
  public void processingInstruction(String target, @Nullable String data) throws SAXException {
    try {
      this.to.writePI(target, data);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  // Lexical Handler =============================================================================

  /**
   * Copy the comment to the output.
   *
   * {@inheritDoc}
   */
  @Override
  public void comment(char[] ch, int start, int length) throws SAXException {
    try {
      this.to.writeComment(String.copyValueOf(ch, start, length));
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void startCDATA() {
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void endCDATA() {
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void startDTD(String name, @Nullable String publicId, @Nullable String systemId) {
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void endDTD() {
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void startEntity(String name) {
  }

  /**
   * Does nothing.
   *
   * {@inheritDoc}
   */
  @Override
  public void endEntity(String name) {
  }

  // Static helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Copy the specified File to the given XML Writer.
   *
   * <p>Any error is reported as XML on the XML writer.
   *
   * <p>This method does not perform any caching, caching is better handled externally by
   * generators.
   *
   * @param file The file.
   * @param xml  The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   *
   * @throws IOException should an error occur when writing the XML.
   */
  public static boolean copyTo(File file, XMLWriter xml) throws IOException {
    boolean ok = false;
    // load
    if (file.exists()) {
      try {
        // writers to use
        XMLStringWriter copy = new XMLStringWriter(NamespaceAware.No);

        // copy the data
        parse(new XMLCopy(copy), new InputSource(file.toURI().toString()));
        copy.flush();
        String parsed = copy.toString();

        // write to XML writer
        xml.writeXML(parsed);
        ok = true;

      // an error was reported by the parser
      } catch (BerliozException ex) {
        LOGGER.warn("An error was reported by the parser while parsing {}", file.toURI());
        handleError(xml, ex);
      }
    // the file does not exist
    } else {
      LOGGER.warn("Could not find {}", file.toURI());
      xml.openElement("no-data");
      xml.attribute("error", "file-not-found");
      xml.closeElement();
    }
    return ok;
  }

  /**
   * Copy the specified File to the given XML Writer.
   *
   * <p>Any error is reported as XML on the XML writer. This method does not perform any caching
   * or validation.
   *
   * @param reader The reader over the XML to read.
   * @param xml    The XML writer.
   *
   * @return <code>true</code> if the copy was done successfully;
   *         <code>false</code> otherwise.
   *
   * @throws IOException should an error occur when writing the XML.
   */
  public static boolean copyTo(Reader reader, XMLWriter xml) throws IOException {
    boolean ok = false;
    // load
    try {
      // writers to use
      XMLStringWriter copy = new XMLStringWriter(NamespaceAware.No);

      // copy the data
      parse(new XMLCopy(copy), new InputSource(reader));
      copy.flush();
      String parsed = copy.toString();

      // write to XML writer
      xml.writeXML(parsed);
      ok = true;

    // an error was reported by the parser
    } catch (BerliozException ex) {
      LOGGER.warn("An error was reported by the parser while parsing reader");
      handleError(xml, ex);
    }
    return ok;
  }

  // private parsing methods
  // --------------------------------------------------------------------------------------------------------

  /**
   * Parses the specified file using the given handler.
   *
   * @param copier The XML Copy instance.
   * @param source The input source to copy
   *
   * @throws BerliozException Should something unexpected happen.
   */
  private static void parse(XMLCopy copier, InputSource source) throws BerliozException {
    SAXParser parser = XMLUtils.getParser(false);
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
   * Try set the lexical handler property in order to copy comments.
   *
   * <p>If the property is not supported, a warning is logged and no further attempts will be made.
   *
   * @param xmlreader the XML reader.
   * @param copier    the XML copy handler.
   */
  private static void trySettingLexicalHandler(XMLReader xmlreader, XMLCopy copier) {
    if (supportsComments) {
      try {
        xmlreader.setProperty(LEXICAL_HANDLER_PROPERTY, copier);
      } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
        supportsComments = false;
        LOGGER.warn("Unable to copy comments", ex);
      }
    }
  }

  private static void handleError(XMLWriter xml, Exception ex) throws IOException {
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
