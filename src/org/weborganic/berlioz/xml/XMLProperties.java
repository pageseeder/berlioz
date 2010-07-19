/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.topologi.diffx.xml.XMLWritable;
import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * An XML version of the <code>Properties</code> class.
 *
 * @author Christophe Lauret (Allette Systems)
 * @version 1 August 2006
 */
public final class XMLProperties extends Properties implements XMLWritable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20060123256100001L;

  /**
   * Creates an empty property list with no default values.
   */
  public XMLProperties() {
    super(null);
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
      Handler handler = new Handler(this);
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

  /**
   * Stores these XML properties in an XML file.
   *
   * @param out An output stream.
   *
   * @param header A description of the property list.
   *
   * @throws IOException if writing this property list to the specified
   *             output stream throws an <tt>IOException</tt>.
   *
   * @throws ClassCastException If this <code>Properties</code> object
   *             contains any keys or values that are not <code>Strings</code>.
   *
   * @throws NullPointerException If <code>out</code> is null.
   */
  public synchronized void store(OutputStream out, String header)
      throws IOException, NullPointerException, ClassCastException {
    // create the writer
    BufferedWriter awriter = new BufferedWriter(new OutputStreamWriter(out, "utf-8"));
    XMLWriterImpl xml = new XMLWriterImpl(awriter, true);
    // write the header if one is required
    if (header != null)
      xml.writeComment(header);
    xml.writeComment(new Date().toString());
    toXML(xml);
    xml.close();
  }

  /**
   *
   * @param xml The XML writer receiving data.
   *
   * @throws IOException Should an error occur with the XML writer.
   */
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("properties", true);
    xml.openElement("root", true);
    nodeToXML("", xml);
    xml.closeElement();
    xml.closeElement();
  }

  /**
   * Recursive XML method that prints the XML for each node.
   *
   * @param prefix The prefix for the node.
   * @param xml    The XML writer receiving data.
   *
   * @throws IOException Should an error occur with the XML writer.
   */
  private void nodeToXML(String prefix, XMLWriter xml) throws IOException {
    final HashSet nodes = new HashSet();
    // get all the entries
    xml.openElement("map", true);
    for (Enumeration e = keys(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      if (key.startsWith(prefix) && key.length() > prefix.length()) {
        String suffix = (prefix.length() > 0)? key.substring(prefix.length()) : key;
        // identify and serialise entries now
        boolean isEntry = suffix.indexOf(".") < 0;
        if (isEntry) {
          xml.openElement("entry", false);
          xml.attribute("key", suffix);
          xml.attribute("value", getProperty(key));
          xml.closeElement();
        // identify the nodes to process recursively later
        } else {
          String name = suffix.substring(0, suffix.indexOf("."));
          nodes.add(name);
        }
      }
    }
    xml.closeElement();
    // process each node
    for (Iterator i = nodes.iterator(); i.hasNext();) {
      String name = (String)i.next();
      xml.openElement("node", true);
      xml.attribute("name", name);
      String nodePrefix = ((prefix.length() > 0)? prefix : "") + name+".";
      nodeToXML(nodePrefix, xml);
      xml.closeElement();
    }
  }

// a handler for the properties file in XML ----------------------------------------------------

  /**
   * Parses the properties file as XML.
   *
   * @author Christophe Lauret (Weborganic)
   * @version 9 October 2009
   */
  private static final class Handler extends DefaultHandler {

    /**
     * The properties to load.
     */
    private final Properties _properties;

    /**
     * The prefix used for the entries.
     */
    private StringBuffer prefix = new StringBuffer();

    /**
     * Creates a new handler.
     * 
     * @param properties The properties to load.
     * 
     * @throws IllegalArgumentException If the properties are <code>null</code>.
     */
    public Handler(Properties properties) throws IllegalArgumentException {
      if (properties == null) throw new IllegalArgumentException("Properties must be specified.");
      this._properties = properties;
    }

    /**
     * {@inheritDoc}
     */
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      if ("node".equals(localName)) {
        this.prefix.append(atts.getValue("name")).append(".");
      } else if ("entry".equals(localName)) {
        String key = this.prefix.toString()+atts.getValue("key");
        this._properties.setProperty(key, atts.getValue("value"));
      }
    }

    /**
     * {@inheritDoc}
     */
    public void endElement(String uri, String localName, String qName) {
      if ("node".equals(localName)) {
        this.prefix.setLength(this.prefix.length() - 1);
        this.prefix.setLength(this.prefix.lastIndexOf(".")+1);
      }
    }
  }

}