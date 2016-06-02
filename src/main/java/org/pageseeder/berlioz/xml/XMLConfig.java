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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
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
 * <p>The XML is not required to validate a specific schema but will be parsed according to the
 * following rules:
 * <ul>
 *   <li>Top level element is ignored;</li>
 *   <li>Each attribute will be used to create a property;</li>
 *   <li>The property name is the concatenation of all ancestor element names and attribute, separated by a dot;</li>
 *   <li>The property value is the attribute value;</li>
 *   <li>If the same property is declared multiple times, the latest value is used.</li>
 * </ul>
 *
 * <p>For example, the following XML:
 * <pre>{@code
 *  <global>
 *    <myapp test="true" id="123"/>
 *  </global>
 * }</pre>
 *
 * <p>Will be read as:
 * <pre>{@code
 *  myapp.test=true
 *  myapp.id=123
 * }</pre>
 *
 * <p>Note: all property values are internally stored as strings.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.9 - 10 October 2012
 * @since Berlioz 0.9.7
 */
public final class XMLConfig implements Serializable {

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
  public XMLConfig() {
    this._properties = new HashMap<String, String>();
  }

  /**
   * Creates an empty property list.
   *
   * @param properties The initial properties for this config.
   */
  public XMLConfig(Map<String, String> properties) {
    this._properties = properties;
  }

  /**
   * Returns the properties as a map.
   *
   * <p>The object returned <i>is</i> the actual map instance of this class.
   *
   * @return the properties as a map.
   */
  public Map<String, String> properties() {
    return this._properties;
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

  /**
   * Creates a new instance of an XML configuration by loading the specified file.
   *
   * @param file The file to load.
   * @return The XML configuration instance with the values loaded from the file.
   *
   * @throws IOException Should any I/O error occur while reading the file.
   */
  public static XMLConfig newInstance(File file) throws IOException {
    XMLConfig config = new XMLConfig();
    try (InputStream in = new FileInputStream(file)) {
      config.load(in);
    }
    return config;
  }

// a handler for the properties file in XML ----------------------------------------------------

  /**
   * Parses the file as XML following the rules for the config.
   *
   * @author Christophe Lauret
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
        int attCount = atts.getLength();
        if (attCount > 0) {
          for (int i = 0; i < attCount; i++) {
            String name = atts.getLocalName(i);
            String value = atts.getValue(i);
            this._properties.put(name, value);
          }
        }
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      if (this.nodes != null) {
        if (this.nodes.size() > 0) {
          this.nodes.pop();
        } else {
          this.nodes = null;
        }
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
