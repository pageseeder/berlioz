/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Extract XML data from the file and write directly onto the specified XML writer.
 * 
 * @deprecated Use {@link XMLCopy} instead.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
@Deprecated public final class XMLExtractor extends DefaultHandler {

  /**
   * The XML produced by the extractor. 
   */
  private final XMLWriter recipient;

  /**
   * The prefix mapping to add to the next startElement event in case the prefix mapping is reported before.
   */
  private final Map<String, String> mapping = new HashMap<String, String>();

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
  @Override
  public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
    try {
      this.recipient.openElement(qName);
      for (int i = 0; i < atts.getLength(); i++) {
        this.recipient.attribute(atts.getQName(i), atts.getValue(i));
      }
      // in case the prefix mapping was reported BEFORE the startElement was reported...
      if (!this.mapping.isEmpty()) {
        for (Entry<String, String> e : this.mapping.entrySet()) {
          boolean hasPrefix = e.getKey() != null && e.getKey().length() > 0;
          this.recipient.attribute("xmlns"+(hasPrefix? ":"+ e.getKey() : e.getKey()), e.getValue());
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
  @Override
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
  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    try {
      this.recipient.closeElement();
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

  @Override
  public void startPrefixMapping(String prefix, String uri) throws SAXException {
    boolean hasPrefix = prefix != null && prefix.length() > 0;
    try {
      this.recipient.attribute("xmlns"+(hasPrefix? ":"+ prefix : prefix), uri);
//    this.recipient.setPrefixMapping(prefix, uri);
    } catch (IllegalArgumentException ex) {
      this.mapping.put((hasPrefix? prefix : ""), uri);
    } catch (IOException ex) {
      throw new SAXException(ex);
    }
  }

}
