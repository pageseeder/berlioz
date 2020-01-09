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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.LoggerFactory;
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
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.7
 */
public final class XMLConfig implements Serializable, XMLWritable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20120123256100001L;

  /**
   * Check that it is a valid attribute name in XML.
   *
   * NB: We disallow ':' to avoid issues with namespaces.
   */
  private final static Pattern VALID_XML_NAME = Pattern.compile("[a-zA-Z_][-a-zA-Z0-9_.]*");

  /**
   * Logger.
   */
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(XMLConfig.class);

  /**
   * List of properties to load.
   */
  private final Map<String, String> _properties;

  /**
   * Creates an empty property list with no default values.
   */
  public XMLConfig() {
    this._properties = new HashMap<>();
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
   * @param in The XML input stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public synchronized void load(InputStream in) throws IOException {
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
      reader.parse(new InputSource(in));
    } catch (ParserConfigurationException ex) {
      throw new IOException("Could not configure SAX parser.");
    } catch (SAXException ex) {
      throw new IOException("Error while parsing: "+ex.getMessage());
    }
  }

  /**
   * Saves the XML properties to the specified stream as UTF-8.
   *
   * @param out The XML output stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public void save(OutputStream out) throws IOException {
    try (OutputStreamWriter w = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
      XMLWriter xml = new XMLWriterImpl(w, true);
      toXML(xml);
    }
  }

//a handler for the properties file in XML ----------------------------------------------------

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    SortedMap<String, String> sorted = new TreeMap<>(this._properties);
    xml.openElement("global", true);
    toXML(xml, sorted);
    xml.closeElement();
  }

  /**
   * Recursive function
   *
   * @param xml The XML writer
   * @param map The map to process.
   *
   * @throws IOException If thrown by the XML Writer.
   */
  private static void toXML(XMLWriter xml, SortedMap<String, String> map) throws IOException {
    attributes(xml, map);
    for (String node : nodes(map)) {
      if (VALID_XML_NAME.matcher(node).matches()) {
        xml.openElement(node, true);
        toXML(xml, sub(map, node));
        xml.closeElement();
      } else {
        LOGGER.warn("Unable to write this element as xml (invalid name): {}", node);
      }
    }
  }

  /**
   * Writes attributes for the current node onto the XML including only properties
   * without a '.'
   *
   * @param xml The XML writer
   * @param map The map to process.
   *
   * @throws IOException If thrown by the XML Writer.
   */
  private static void attributes(XMLWriter xml, Map<String, String> map) throws IOException {
    for (Entry<String, String> x : map.entrySet()) {
      String property = x.getKey();
      if (property.indexOf('.') < 0) {
        if (VALID_XML_NAME.matcher(property).matches()) {
          xml.attribute(property, x.getValue());
        } else {
          LOGGER.warn("Unable to write this attribute as xml (invalid name): {}", property);
        }
      }
    }
  }

  /**
   * Returns the set of notes from the map.
   *
   * <p>A node is the prefix of a property where the property is <code>[node].[name]</code>
   *
   * @param map The map to process.
   *
   * @return A set of nodes from the map
   */
  private static SortedSet<String> nodes(Map<String, String> map) {
    SortedSet<String> nodes = new TreeSet<>();
    for (String property : map.keySet()) {
      int dot = property.indexOf('.');
      if (dot >= 0) {
        nodes.add(property.substring(0, dot));
      }
    }
    return nodes;
  }

  /**
   * Returns the subset of the map with the node prefix removed from the key.
   *
   * <p>A node is the prefix of a property where the property is <code>[node].[name]</code>
   *
   * @param map  The map to process.
   * @param node The node to use for prefixing.
   *
   * @return A set of nodes from the map
   *
   * @throws IOException If thrown by the XML Writer.
   */
  private static SortedMap<String, String> sub(SortedMap<String, String> map, String node) {
    String prefix = node+".";
    SortedMap<String, String> sub = new TreeMap<>();
    for (Entry<String, String> e : map.entrySet()) {
      String property = e.getKey();
      if (property.startsWith(prefix)) {
        sub.put(property.substring(prefix.length()), e.getValue());
      }
    }
    return sub;
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
    private @Nullable Stack<String> nodes = null;

    /**
     * Creates a new handler.
     *
     * @param properties The properties to load.
     *
     * @throws NullPointerException If the properties are <code>null</code>.
     */
    public Handler(Map<String, String> properties) {
      this._properties = Objects.requireNonNull(properties, "Properties must be specified.");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      Stack<String> nodes = this.nodes;
      if (nodes != null) {
        nodes.push(localName);
        int attCount = atts.getLength();
        if (attCount > 0) {
          String prefix = getPrefix(nodes);
          for (int i = 0; i < attCount; i++) {
            String name = atts.getLocalName(i);
            String value = atts.getValue(i);
            if (value != null) {
              this._properties.put(prefix+name, value);
            }
          }
        }
      } else {
        this.nodes = new Stack<>();
        int attCount = atts.getLength();
        if (attCount > 0) {
          for (int i = 0; i < attCount; i++) {
            String name = atts.getLocalName(i);
            String value = atts.getValue(i);
            if (name != null && value != null) {
              this._properties.put(name, value);
            }
          }
        }
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
      Stack<String> nodes = this.nodes;
      if (nodes != null) {
        if (nodes.size() > 0) {
          nodes.pop();
        } else {
          this.nodes = null;
        }
      }
    }

    /**
     * @return the prefix from the current stack of nodes.
     */
    private static String getPrefix(Stack<String> nodes) {
      StringBuilder prefix = new StringBuilder();
      for (String node : nodes) {
        prefix.append(node).append('.');
      }
      return prefix.toString();
    }
  }

}
