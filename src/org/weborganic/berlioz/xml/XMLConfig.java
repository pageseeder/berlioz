/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A simpler version of the XML config file to improve readability of Berlioz configuration.
 *
 * @author Christophe Lauret
 * @version 6 October 2012
 */
public final class XMLConfig implements Serializable {

  // TODO Javadoc

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20120123256100001L;

  /**
   * List of properties to load.
   */
  private final Map<String, String> _properties;

  /**
   * Creates an empty property list with no default values.
   */
  public XMLConfig(Map<String, String> properties) {
    this._properties = properties;
  }

  /**
   * Reads a XML property list from the input stream.
   *
   * @param inStream The XML input stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public synchronized void load(InputStream inStream) throws IOException {
    try {
      // use the SAX parser factory to ensure validation
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      // get the parser
      XMLReader reader = factory.newSAXParser().getXMLReader();
      // configure the reader
      Handler handler = new Handler(this._properties);
      reader.setContentHandler(handler);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      // parse
      reader.parse(new InputSource(inStream));
    } catch (ParserConfigurationException ex) {
      throw new IOException("Could not configure SAX parser.");
    } catch (SAXException ex) {
      throw new IOException("Error while parsing: "+ex.getMessage());
    }
  }

// a handler for the properties file in XML ----------------------------------------------------

  /**
   * Parses the file as XML.
   *
   * @author Christophe Lauret (Weborganic)
   * @version 6 October 2012
   */
  private static final class Handler extends DefaultHandler {

    /**
     * The properties to load.
     */
    private final Map<String, String> _properties;

    /**
     * Keeps track of the nodes.
     */
    private Stack<String> nodes = null;

    /**
     * Creates a new handler.
     *
     * @param properties The properties to load.
     *
     * @throws NullPointerException If the properties are <code>null</code>.
     */
    public Handler(Map<String, String> properties) {
      if (properties == null) throw new NullPointerException("Properties must be specified.");
      this._properties = properties;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      if (this.nodes != null) {
        this.nodes.push(localName);
        int attCount = atts.getLength();
        if (attCount > 0) {
          String prefix = getPrefix();
          for (int i = 0; i < attCount; i++) {
            String name = atts.getLocalName(i);
            String value = atts.getValue(i);
            this._properties.put(prefix+name, value);
          }
        }
      } else {
        this.nodes = new Stack<String>();
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      if (this.nodes != null) {
        if (this.nodes.size() > 0)
          this.nodes.pop();
        else
          this.nodes = null;
      }
    }

    /**
     * @return the prefix from the current stack of nodes.
     */
    private String getPrefix() {
      StringBuilder prefix = new StringBuilder();
      for (String node : this.nodes) {
        prefix.append(node).append('.');
      }
      return prefix.toString();
    }
  }

}
