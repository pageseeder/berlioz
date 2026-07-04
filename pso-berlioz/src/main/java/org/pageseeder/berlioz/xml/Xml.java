package org.pageseeder.berlioz.xml;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.LexicalHandler;

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
import java.util.Locale;

/**
 *
 * @version 0.13.0
 * @since 0.12.0
 */
public class Xml {

  private static final Logger LOGGER = LoggerFactory.getLogger(Xml.class);

  /**
   * The LexicalHandler property name.
   */
  @SuppressWarnings("HttpUrlsUsage")
  private static final String LEXICAL_HANDLER_PROPERTY = "http://xml.org/sax/properties/lexical-handler";

  /**
   * Whether the lexical-handler property is supported (optimistically assumes it is).
   */
  private static volatile boolean supportsLexicalHandler = true;

  private Xml() {
  }

  /**
   * Indicates whether the specified bare media type identifies XML content.
   *
   * <p>A media type is considered XML if:</p>
   * <ul>
   *   <li>it is {@code application/xml} or {@code text/xml}, the canonical XML media types
   *       (RFC 7303 §4.1); or</li>
   *   <li>its subtype ends with the {@code +xml} structured syntax suffix (RFC 6839 §3.2),
   *       covering types such as {@code application/atom+xml} or {@code application/rss+xml}.</li>
   * </ul>
   *
   * <p>The comparison is case-insensitive as required by RFC 2045 §5.1.
   * Media type parameters (e.g. {@code ;charset=utf-8}) must be stripped before calling this method.</p>
   *
   * @param mediaType the bare media type to test, without parameters
   * @return {@code true} if the media type represents XML content; {@code false} for {@code null} or non-XML
   */
  public static boolean isXmlMediaType(@Nullable String mediaType) {
    if (mediaType == null) return false;
    String type = mediaType.trim().toLowerCase(Locale.ROOT);
    return "application/xml".equals(type) || "text/xml".equals(type) || type.endsWith("+xml");
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
  public static void parse(ContentHandler handler, File xml, boolean validate) throws BerliozException {
    if (xml.isDirectory())
      throw new BerliozException("Cannot parse a directory");
    URI uri = xml.toURI();
    LOGGER.info("Parsing file {}", uri);
    parse(handler, new InputSource(uri.toString()), validate, true);
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
    parse(handler, new InputSource(reader), validate, false);
  }

  /**
   * Shared parsing core: configures the safe parser, entity resolver, error handler, and
   * (opportunistically) the lexical handler, then parses the given source.
   *
   * <p>Package-private so callers within {@code org.pageseeder.berlioz.xml} that need to parse a
   * source without disallowing {@code DOCTYPE} declarations (e.g. {@link XmlCopier}, which copies
   * documents verbatim rather than resolving them for routing/configuration) can reuse the same
   * plumbing as the public {@link #parse(ContentHandler, File, boolean)} /
   * {@link #parse(ContentHandler, Reader, boolean)} methods.
   *
   * @param handler         The content handler to use.
   * @param source          The input source to parse.
   * @param validate        Whether to validate.
   * @param disallowDoctype Whether to reject any {@code DOCTYPE} declaration outright.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  @SuppressWarnings("HttpUrlsUsage")
  static void parse(ContentHandler handler, InputSource source, boolean validate, boolean disallowDoctype)
      throws BerliozException {
    SAXParser parser = safeParser(validate);
    try {
      XMLReader reader = parser.getXMLReader();
      if (disallowDoctype) {
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      }
      reader.setContentHandler(handler);
      trySettingLexicalHandler(reader, handler);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      reader.parse(source);
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
   * Opportunistically registers the handler as the lexical handler, so that comments are
   * reported to {@link ContentHandler} implementations that also implement {@link LexicalHandler}.
   *
   * <p>If the property is not supported by the underlying parser, a warning is logged once and
   * no further attempts are made.
   *
   * @param reader  the XML reader.
   * @param handler the content handler, only registered if it also implements {@link LexicalHandler}.
   */
  private static void trySettingLexicalHandler(XMLReader reader, ContentHandler handler) {
    if (supportsLexicalHandler && handler instanceof LexicalHandler) {
      try {
        reader.setProperty(LEXICAL_HANDLER_PROPERTY, handler);
      } catch (SAXNotRecognizedException | SAXNotSupportedException ex) {
        supportsLexicalHandler = false;
        LOGGER.warn("Unable to copy comments", ex);
      }
    }
  }

  /**
   * Creates a safe parser, converting checked exceptions to {@link BerliozException}.
   *
   * @param validating {@code true} to create a validating parser; {@code false} otherwise
   *
   * @return a new SAX parser instance
   *
   * @throws BerliozException if the parser could not be configured or initialised
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
