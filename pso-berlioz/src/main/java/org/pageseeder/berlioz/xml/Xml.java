package org.pageseeder.berlioz.xml;

import org.pageseeder.berlioz.BerliozException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;

/**
 *
 * @version 0.13.0
 * @since 0.12.0
 */
public class Xml {

  private static final Logger LOGGER = LoggerFactory.getLogger(Xml.class);

  private Xml() {
  }

  /**
   * Always return an XML Writer.
   *
   * @param writer The writer receiving the XML output.
   *
   * @return The corresponding XML writer to use.
   */
  public static XmlWriter newWriter(Writer writer) {
    return new XmlAppendable<>(writer);
  }

  /**
   * @return A SAX Parser
   *
   * @throws ParserConfigurationException if a parser cannot be created.
   * @throws SAXException if a parser feature cannot be configured.
   */
  public static SAXParser newSafeParser() throws ParserConfigurationException, SAXException {
    return newSafeParser(false);
  }

  /**
   * @param validating Whether the parser should validate.
   * @return A SAX Parser
   *
   * @throws ParserConfigurationException if a parser cannot be created.
   * @throws SAXException if a parser feature cannot be configured.
   */
  @SuppressWarnings("HttpUrlsUsage")
  public static SAXParser newSafeParser(boolean validating) throws ParserConfigurationException, SAXException {
    // use the SAX parser factory to ensure validation
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(validating);
    factory.setNamespaceAware(true);
    factory.setXIncludeAware(false);
    // also specify the features
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://xml.org/sax/features/validation", validating);
    factory.setFeature("http://xml.org/sax/features/namespaces", true);
    factory.setFeature("http://xml.org/sax/features/namespace-prefixes", false);
    // get a new parser
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    return factory.newSAXParser();
  }

  /**
   * Parses the specified file using the given handler (with validation enabled).
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
   * @param xml      The XML file to parse.
   * @param validate Whether to validate.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  @SuppressWarnings("HttpUrlsUsage")
  public static void parse(ContentHandler handler, File xml, boolean validate) throws BerliozException {
    if (xml.isDirectory())
      throw new BerliozException("Cannot parse a directory");
    SAXParser parser = safeParser(validate);
    try {
      XMLReader reader = parser.getXMLReader();
      reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      reader.setContentHandler(handler);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      URI uri = xml.toURI();
      LOGGER.info("Parsing file {}", uri);
      reader.parse(new InputSource(uri.toString()));
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
   * Parses the specified reader using the given handler.
   *
   * @param handler  The content handler to use.
   * @param reader   The reader over the XML to parse.
   * @param validate Whether to validate.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public static void parse(ContentHandler handler, Reader reader, boolean validate) throws BerliozException {
    SAXParser parser = safeParser(validate);
    try {
      XMLReader xmlreader = parser.getXMLReader();
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
   * Creates a safe parser, converting checked exceptions to {@link BerliozException}.
   */
  public static SAXParser safeParser(boolean validating) throws BerliozException {
    try {
      return newSafeParser(validating);
    } catch (ParserConfigurationException ex) {
      throw new BerliozException("Could not configure SAX parser.", ex);
    } catch (SAXException ex) {
      throw new BerliozException("Could not setup SAX parser factory: " + ex.getMessage(), ex);
    }
  }

}
