/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.IOException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Extract XML data from the file and write directly onto the specified XML writer.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public final class XMLExtractor extends DefaultHandler {

  /**
   * The XML produced by the extractor. 
   */
  private final XMLWriter recipient;

  /**
   * Creates a new XMLExtractor wrapping the specified XML writer.
   *
   * @param xml The XML writer to use.
   */
  public XMLExtractor(XMLWriter xml) {
    this.recipient = xml;
  }

  /**
   * {@inheritDoc}
   */
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      this.recipient.openElement(qName);
      for (int i = 0; i < atts.getLength(); i++) {
        this.recipient.attribute(atts.getQName(i), atts.getValue(i));
      }
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void characters(char[] ch, int start, int length) throws SAXException {
    try {
      this.recipient.writeText(ch, start, length);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      this.recipient.closeElement();
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

}
