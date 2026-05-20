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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.pageseeder.berlioz.BerliozException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * A utility class to help with some simple XML operations.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.6
 */
public final class XMLUtils {

  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLUtils.class);

  /**
   * Prevents creation of instances.
   */
  private XMLUtils() {
  }

  /**
   * Parses the specified file using the given handler.
   *
   * @param handler The content handler to use.
   * @param xml     The XML file to parse.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, File xml) throws BerliozException {
    parse(handler, xml, true);
  }

  /**
   * Parses the specified file using the given handler.
   *
   * @param handler  The content handler to use.
   * @param reader   The reader over the XML to parse.
   * @param validate whether to validate or not.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, Reader reader, boolean validate) throws BerliozException {
    SAXParser parser = getParser(validate);
    try {
      // get the reader
      XMLReader xmlreader = parser.getXMLReader();
      // configure the reader
      xmlreader.setContentHandler(handler);
      xmlreader.setEntityResolver(BerliozEntityResolver.getInstance());
      xmlreader.setErrorHandler(BerliozErrorHandler.getInstance());
      xmlreader.parse(new InputSource(reader));
    } catch (SAXException ex) {
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (IOException ex) {
      throw new BerliozException("Could not read file.", ex);
    }
  }

  /**
   * Parses the specified file using the given handler.
   *
   * @param handler  The content handler to use.
   * @param xml      The XML file to parse.
   * @param validate whether to validate or not.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, File xml, boolean validate) throws BerliozException {
    SAXParser parser = getParser(validate);
    try {
      // get the reader
      XMLReader reader = parser.getXMLReader();
      // Secure reader to prevent XXE
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      // This may not be strictly required as DTDs shouldn't be allowed at all, per previous line.
      reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
      reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      // configure the reader
      reader.setContentHandler(handler);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      // parse
      if (xml.isDirectory())
       throw new BerliozException("Cannot parse a directory");
      LOGGER.info("Parsing file {}", xml.toURI());
      reader.parse(new InputSource(xml.toURI().toString()));
    } catch (SAXException ex) {
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (FileNotFoundException ex) {
      LOGGER.warn("Attempted to parse file which cannot be found", ex);
      throw new BerliozException("Could not find file.", ex);
    } catch (IOException ex) {
      LOGGER.warn("Unable to parse file", ex);
      throw new BerliozException("Could not read file.", ex);
    }
  }

  /**
   * Returns the requested SAX Parser factory.
   *
   * @param validating <code>true</code> for the validating factory;
   *                   <code>false</code> for the non-validating factory;
   * @return the SAX parser factory to use.
   *
   * @throws BerliozException If one of the features is not recognised or supported by the factory.
   */
  public static SAXParser getParser(boolean validating) throws BerliozException {
    SAXParser parser;
    try {
//      // use the SAX parser factory to ensure validation
//      SAXParserFactory factory = SAXParserFactory.newInstance();
//      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
//      factory.setValidating(validating);
//      factory.setNamespaceAware(true);
//      factory.setXIncludeAware(false);
//      // also specify the features
//      factory.setFeature("http://xml.org/sax/features/validation", validating);
//      factory.setFeature("http://xml.org/sax/features/namespaces", true);
//      factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
      // get a new parser
      parser = Xml.newSafeParser(validating);
    } catch (ParserConfigurationException ex) {
      throw new BerliozException("Could not configure SAX parser.", ex);
    } catch (SAXException ex) {
      throw new BerliozException("Could not setup SAX parser factory: " + ex.getMessage(), ex);
    }
    return parser;
  }

}
