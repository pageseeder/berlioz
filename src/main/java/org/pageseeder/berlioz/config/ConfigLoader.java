package org.pageseeder.berlioz.config;

import org.pageseeder.berlioz.xml.BerliozEntityResolver;
import org.pageseeder.berlioz.xml.BerliozErrorHandler;
import org.pageseeder.berlioz.xml.Xml;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * A base class to handle configuration files consistently.
 *
 * @param <T> The type of object config the handler should create.
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.12.4
 */
abstract class ConfigLoader<T> extends DefaultHandler {

  /**
   * Config item being generated from the handler.
   *
   * <p>This might not be the actual config but the objects held by the config.
   */
  protected T config;

  /**
   * Create a new handler initialised with the specified config item.
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
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public static <X> X parse(ConfigHandler<X> handler, File file) throws IOException {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      return parse(handler, in);
    }
  }

  /**
   * Reads the config from the input stream.
   *
   * @param in The XML input stream to parse.
   *
   * @throws IOException If an error occurred when reading from the input stream.
   */
  public static <X> X parse(ConfigHandler<X> handler, InputStream in) throws IOException {
    try {
      // Get safe SAX parser factory to ensure validation
      SAXParser parser = Xml.newSafeParser();
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
      reader.parse(new InputSource(in));
      return handler.getConfig();
    } catch (ParserConfigurationException ex) {
      throw new IOException("Could not configure SAX parser.");
    } catch (SAXException ex) {
      throw new IOException("Error while parsing: "+ex.getMessage());
    }
  }

  static abstract class ConfigHandler<T> extends DefaultHandler {

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
