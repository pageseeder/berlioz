/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * Copy the parsed XML to the specified XML writer.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 19 July 2010
 */
public final class XMLCopy extends DefaultHandler implements ContentHandler {

  /**
   * Logger the extractor.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLCopy.class);

  /**
   * Where the XML should be copied to. 
   */
  private final XMLWriter to;

  /**
   * The prefix mapping to add to the next startElement event in case the prefix mapping is reported before.
   */
  private Map<String, String> mapping = new HashMap<String, String>();

  /**
   * Creates a new XMLExtractor wrapping the specified XML writer.
   *
   * @param xml The XML writer to use.
   */
  public XMLCopy(XMLWriter xml) {
    this.to = xml;
  }

  /**
   * {@inheritDoc}
   */
  @Override public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      this.to.openElement(qName);
      for (int i = 0; i < atts.getLength(); i++) {
        this.to.attribute(atts.getQName(i), atts.getValue(i));
      }
      // in case the prefix mapping was reported BEFORE the startElement was reported...
      if (!mapping.isEmpty()) {
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

  /**
   * {@inheritDoc}
   */
  @Override public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      this.to.writeText(ch, start, length);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      this.to.closeElement();
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override public void startPrefixMapping(String prefix, String uri) throws SAXException {
    boolean hasPrefix = prefix != null && prefix.length() > 0;
    try {
      this.to.attribute("xmlns"+(hasPrefix? ":"+ prefix : prefix), uri);
//    this.recipient.setPrefixMapping(prefix, uri);
    } catch (IllegalArgumentException ex) {
      this.mapping.put((hasPrefix? prefix : ""), uri);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void endPrefixMapping(String prefix) throws SAXException {
    // TODO ???
  }

  /**
   * {@inheritDoc}
   */
  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    // TODO ???
  }

  /**
   * {@inheritDoc}
   */
  @Override public void processingInstruction(String target, String data) throws SAXException {
    try {
      this.to.writePI(target, data);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  // Static helper ===============================================================================

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
   * @throws IOException should an error occur when writing the XML.
   */
  public static void copyTo(File file, XMLWriter xml) throws IOException {
    // load
    if (file.exists()) {
      try {
        // writers to use
        StringWriter writer = new StringWriter();
        XMLWriter internal = new XMLWriterImpl(writer);

        // copy the data
        XMLUtils.parse(new XMLCopy(internal), file, false);
        internal.flush();
        String parsed = writer.toString();

        // write to XML writer
        xml.writeXML(parsed);

      // an error was reported by the parser
      } catch (BerliozException ex) {
        LOGGER.warn("An error was reported by the parser while parsing {}", file.toURI());
        LOGGER.warn("Error details:", ex);
        xml.openElement("no-data");
        xml.attribute("error", "parsing");
        xml.attribute("details", ex.getMessage());
        xml.closeElement();
      }
    // the file does not exist
    } else {
      LOGGER.warn("Could not find {}", file.toURI());
      xml.openElement("no-data");
      xml.attribute("error", "file-not-found");
      xml.closeElement();
    }
  }

  /**
   * This method is kept for backward compatibility - the cache parameter has no effect. 
   * 
   * <p>Identical to {@link #copyTo(File, XMLWriter)}.
   * 
   * @deprecated Use {@link #copyTo(File, XMLWriter)} instead.
   * 
   * @param file  The file.
   * @param xml   The XML writer.
   * @param cache Ignored.
   * 
   * @throws IOException should an error occur when writing the XML.
   */
  @Deprecated public static void copyTo(File file, XMLWriter xml, boolean cache) throws IOException {
    LOGGER.warn("Called deprecated method copyTo(File, XMLWriter, boolean) - use copyTo(File, XMLWriter) instead");
    copyTo(file, xml);
  }
}
