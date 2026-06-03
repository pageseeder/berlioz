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
package org.pageseeder.berlioz.config;

import org.pageseeder.berlioz.xml.BerliozErrorHandler;
import org.pageseeder.berlioz.xml.Xml;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

/**
 * A base class to handle configuration files consistently.
 *
 * @param <T> The type of object config the handler should create.
 *
 * @version 0.13.0
 * @since 0.12.4
 */
abstract class ConfigLoader<T> extends DefaultHandler {

  /**
   * Config item being generated from the handler.
   *
   * <p>This might not be the actual config but the objects held by the config.
   */
  protected T config;

  /**
   * Create a new handler initialized with the specified config item.
   *
   * @param config Config item to create (must not be null)
   */
  ConfigLoader(T config) {
    this.config = Objects.requireNonNull(config, "Config items must be specified");
  }

  /**
   * Returns the config item generated.
   *
   * <p>NB, typically called after the parsing.
   *
   * @return The config item being generated from the handler.
   */
  T getConfig() {
    return this.config;
  }

  /**
   * Utility class to generate the config item from a file.
   *
   * @param handler The handler to use.
   * @param file The XML input stream to parse.
   * @param <X> The type of config object that the handler generates
   *
   * @throws ConfigException If an error occurred when reading the file
   */
  public static <X> X parse(ConfigHandler<X> handler, File file) throws ConfigException {
    if (file.length() > 1_000_000)
      throw new ConfigException("Configuration files must not exceed 1MB", new IllegalArgumentException());
    try (InputStream in = Files.newInputStream(file.toPath())) {
      return parse(handler, in);
    } catch (IOException ex) {
      throw new ConfigException("Unable to open config file", ex);
    }
  }

  /**
   * Reads the config from the input stream.
   *
   * @param in The XML input stream to parse.
   *
   * @throws ConfigException If an error occurred when reading from the input stream.
   */
  @SuppressWarnings("HttpUrlsUsage")
  public static <X> X parse(ConfigHandler<X> handler, InputStream in) throws ConfigException {
    byte[] bytes;
    try {
      bytes = in.readAllBytes();
    } catch (IOException ex) {
      throw new ConfigException(ex.getMessage(), ex);
    }
    try {
      // Get safe SAX parser factory
      SAXParser parser = Xml.newSafeParser(false);

      // Look for doctype declarations
      int start = find(bytes, "<!DOCTYPE ".getBytes(StandardCharsets.UTF_8));
      if (start != -1) {
        int end = find(bytes, ">".getBytes(StandardCharsets.UTF_8)[0], start+10)+1;
        if (end > start) {
          LoggerFactory.getLogger(ConfigLoader.class).warn("Doctype declaration found in config file");
          String doctype = new String(Arrays.copyOfRange(bytes, start, end), StandardCharsets.UTF_8);
          // We remove the doctype from our config
          if (doctype.contains("-//Berlioz") || doctype.contains("//Weborganic")) {
            int doctypeLength = end - start;
            byte[] clean = new byte[bytes.length - doctypeLength];
            System.arraycopy(bytes, 0, clean, 0, start);
            System.arraycopy(bytes, end, clean, start, bytes.length - end);
            bytes = clean;
          }
        }
      }

      // Validate
      if (handler.getSchema() != null) {
        validate(bytes, handler.getSchema());
      }

      XMLReader reader = parser.getXMLReader();
      // Secure reader to prevent XXE
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      // This may not be strictly required as DTDs shouldn't be allowed at all, per previous line.
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      // configure the reader
      reader.setContentHandler(handler);
      reader.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      // parse
      reader.parse(new InputSource(new ByteArrayInputStream(bytes)));
      return handler.getConfig();
    } catch (ParserConfigurationException ex) {
      throw new ConfigException("Could not configure SAX parser.", ex);
    } catch (SAXException | IOException ex) {
      throw new ConfigException("Error while parsing: "+ex.getMessage(), ex);
    }
  }

  private static void validate(byte[] bytes, String schemaName) throws ConfigException, IOException {
    try (InputStream is = new ByteArrayInputStream(bytes)) {
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      InputStream inputStream = ConfigLoader.class.getResourceAsStream("/schema/"+schemaName+".xsd");
      Source schemaFile = new StreamSource(inputStream);
      Schema schema = factory.newSchema(schemaFile);
      Validator validator =  schema.newValidator();
      validator.validate(new StreamSource(is));
    } catch (SAXException ex) {
      throw new ConfigException("Error during validation", ex);
    }
  }

  private static int find(byte[] source, byte[] pattern) {
    for (int i=0; i < source.length; i++) {
      if (match(source, pattern, i)) return i;
    }
    return -1;
  }

  private static boolean match(byte[] source, byte[] pattern, int at) {
    if (source[at] == pattern[0] && at < (source.length - pattern.length)) {
      for (int i=1; i < pattern.length; i++) {
        if (source[at+i] != pattern[i]) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private static int find(byte[] source, byte b, int from) {
    for (int i=from; i < source.length; i++) {
      if (source[i] == b) return i;
    }
    return -1;
  }

  abstract static class ConfigHandler<T> extends DefaultHandler {

    abstract String getSchema();

    /**
     * Returns the config item generated.
     *
     * <p>NB, typically called after the parsing.
     *
     * @return The config item being generated from the handler.
     */
    abstract T getConfig();
  }

}
