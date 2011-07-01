/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.SAXParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.util.BerliozInternal;
import org.weborganic.berlioz.util.CompoundBerliozException;
import org.weborganic.berlioz.xml.BerliozEntityResolver;
import org.weborganic.berlioz.xml.SAXErrorCollector;
import org.weborganic.berlioz.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility class to provide access to the content of generators.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 1 July 2011
 */
public final class ContentManager {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentManager.class);

  /**
   * Maps content generators URL patterns to their content generator instance.
   */
  private static final ServiceRegistry SERVICES = new ServiceRegistry();

  /**
   * Indicates whether the boolean value was loaded. 
   */
  private static transient boolean loaded = false;

  /**
   * Prevents creation of instances. 
   */
  private ContentManager() {
    // no public constructors for utility classes
  }

  /**
   * Returns the default service registry (mapped to "services.xml").
   * @return the default service registry (mapped to "services.xml").
   */
  public static ServiceRegistry getDefaultRegistry() {
    return SERVICES;
  }

  /**
   * Update the patterns based on the current generators.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static synchronized void loadIfRequired() throws BerliozException{
    if (!loaded) {
      load();
      loaded = true;
    }
  }

  /**
   * Loads the content access file.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void load() throws BerliozException {
    File repository = GlobalSettings.getRepository();
    File xml = new File(new File(repository, "config"), "services.xml");
    load(xml);
  }

  /**
   * Loads the content access file.
   * 
   * @param xml The XML file to load.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void load(File xml) throws BerliozException {
    if (xml == null) 
      throw new NullPointerException("The service configuration file is null! That's it I give up.");
    // OK Let's start
    SAXParser parser = XMLUtils.getParser(true);
    SAXErrorCollector collector = new SAXErrorCollector(LOGGER);
    BerliozInternal id = null;
    // Load the services
    try {
      XMLReader reader = parser.getXMLReader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, SERVICES);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(collector);
      LOGGER.info("Parsing "+xml.toURI().toString());
      reader.parse(new InputSource(xml.toURI().toString()));
      // if the error threshold was reached, throw an error!
      if (collector.hasError()) {
        id = BerliozInternal.SERVICES_INVALID;
        throw new SAXException(collector.getErrors().size()+" error(s) reported by the XML parser.");
      }
    } catch (SAXException ex) {
      if (id == null) id = BerliozInternal.SERVICES_MALFORMED;
      LOGGER.error("An SAX error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new CompoundBerliozException("Unable to parse services configuration file.", ex, id, collector);
    } catch (IOException ex) {
      LOGGER.error("An I/O error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new BerliozException("Unable to read services configuration file.", ex, BerliozInternal.SERVICES_NOT_FOUND);
    }
  }

  /**
   * Update the patterns based on the current generators.
   */
  public static synchronized void clear() {
    LOGGER.info("Clearing content manager");
    SERVICES.clear();
    loaded = false;
  }

  // Inner class to determine which handler to use --------------------------------------------------

  /**
   * A content handler to determine which handler implementation should be used to parse the 
   * web access configuration.
   * 
   * @author Christophe Lauret
   * @version 29 June 2011
   */
  private static final class HandlingDispatcher extends DefaultHandler implements ContentHandler {

    /**
     * Registry for the services to load.
     */
    private final ServiceRegistry _registry;

    /**
     * The reader in use.
     */
    private final XMLReader _reader;

    /**
     * The document locator for use when reporting the location of errors and warnings.
     */
    private Locator _locator;

    /**
     * Create a new version sniffer for the specified XML reader.
     * 
     * @param reader   The XML Reader in use.
     * @param registry The service registry.
     */
    public HandlingDispatcher(XMLReader reader, ServiceRegistry registry) {
      this._reader = reader;
      this._registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDocumentLocator(Locator locator) {
      this._locator = locator;
    }

    /**
     * Once the first element is matched, the reader is assigned the appropriate handler.
     * 
     * {@inheritDoc}
     * 
     * @throws SAXException if the the file being parsed is not a service configuration.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      // Identify the handler to use
      ContentHandler handler = getHandler(localName, atts);
      handler.setDocumentLocator(this._locator);
      // re-trigger events on handler to ensure proper initialisation
      handler.startDocument();
      handler.startElement(uri, localName, qName, atts);
      this._reader.setContentHandler(handler);
    }

    /**
     * Returns the content handler to use based on the element and its attributes
     * 
     * @param name The name of the element (local)
     * @param atts The attributes attached to the element.
     * 
     * @return The corresponding handler
     * 
     * @throws SAXException if the the file being parsed is not a service configuration.
     */
    private ContentHandler getHandler(String name, Attributes atts) throws SAXException {
      SAXErrorCollector collector = (SAXErrorCollector)this._reader.getErrorHandler();
      // Service configuration
      if ("service-config".equals(name)) {
        String version = atts.getValue("version");

        // Version 1.0
        if ("1.0".equals(version)) {
          LOGGER.info("Service configuration 1.0 detected");
          return new ServicesHandler10(this._registry, collector);

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this._registry, collector);
        }

      // Definitely not supported 
      } else {
        LOGGER.error("Unable to determine Berlioz configuration");
        SAXParseException fatal = new SAXParseException("Not a valid Berlioz service configuration!", this._locator);
        collector.fatalError(fatal);
        // Just in case it wasn't thrown
        throw fatal;
      }
    }

  }

}
