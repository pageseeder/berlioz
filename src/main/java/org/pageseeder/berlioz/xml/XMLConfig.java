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

import org.pageseeder.berlioz.config.GlobalConfig;
import org.pageseeder.xmlwriter.XMLWritable;
import org.pageseeder.xmlwriter.XMLWriter;

import java.io.*;
import java.util.Map;

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
 * @version Berlioz 0.12.4
 * @since Berlioz 0.9.7
 *
 * @deprecated Use org.pageseeder.berlioz.config.GlobalConfig instead.
 */
@Deprecated
public final class XMLConfig implements Serializable, XMLWritable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20230223L;

  private final GlobalConfig _config;

  /**
   * Creates an empty property list with no default values.
   */
  public XMLConfig() {
    this._config = new GlobalConfig();
  }

  /**
   * Creates an empty property list.
   *
   * @param properties The initial properties for this config.
   */
  public XMLConfig(Map<String, String> properties) {
    this._config = new GlobalConfig(properties);
  }

  private XMLConfig(GlobalConfig config) {
    this._config = config;
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
    GlobalConfig config = GlobalConfig.newInstance(file);
    return new XMLConfig(config);
  }

  /**
   * Returns the properties as a map.
   *
   * <p>The object returned <i>is</i> the actual map instance of this class.
   *
   * @return the properties as a map.
   */
  public Map<String, String> properties() {
    return this._config.properties();
  }

  /**
   * Reads a XML property list from the input stream.
   *
   * @param in The XML input stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public synchronized void load(InputStream in) throws IOException {
    this._config.load(in);
  }

  /**
   * Saves the XML properties to the specified stream as UTF-8.
   *
   * @param out The XML output stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public void save(OutputStream out) throws IOException {
    this._config.save(out);
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    this._config.toXML(xml);
  }

}
