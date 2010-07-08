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
import org.weborganic.berlioz.xml.BerliozEntityResolver;
import org.weborganic.berlioz.xml.BerliozErrorHandler;
import org.weborganic.berlioz.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A utility class to provide access to the content of generators.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 11 December 2009
 */
public final class ContentManager {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ContentManager.class);

  /**
   * Maps content generators URL patterns to their content generator instance.
   */
  private static final ServiceRegistry REGISTRY = new ServiceRegistry();

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
   * Returns the content generator instance corresponding to the specified
   * path information.
   * 
   * @param pathInfo The path information to access this generator.
   * 
   * @return The corresponding task instance.
   */
  public static MatchingService getInstance(String pathInfo) {
    if (pathInfo == null) return null;
    // load the generator if not loaded yet
    if (!loaded) {
      try {
        load();
        loaded = true;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return REGISTRY.get(pathInfo);
  }

  /**
   * Loads the content access file.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void load() throws BerliozException {
    File repository = GlobalSettings.getRepository();
    File xml = new File(new File(repository, "config"), "access.xml");
    if (!xml.exists())
      xml = new File(new File(repository, "config"), "services.xml");
    if (!xml.exists())
      return;
    load (xml);
  }

  /**
   * Loads the content access file.
   * 
   * @throws BerliozException Should something unexpected happen.
   */
  public static void load(File xml) throws BerliozException {
    if (xml == null || !xml.exists())
      throw new IllegalArgumentException("The generators configuration could not be found in "+xml);
    SAXParser parser = XMLUtils.getParser(true);
    // Load the generators
    try {
      XMLReader reader = parser.getXMLReader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, REGISTRY);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(BerliozErrorHandler.getInstance());
      LOGGER.info("parsing "+xml.toURI().toString());
      reader.parse(new InputSource(xml.toURI().toString()));
    } catch (SAXException ex) {
      LOGGER.error("An SAX error occurred while reading XML configuration of generatores", ex);
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (IOException ex) {
      LOGGER.error("An I/O error occurred while reading XML configuration of generatores", ex);
      throw new BerliozException("Could not read file.", ex);
    }
  }

  /**
   * Update the patterns based on the current generators.
   */
  public synchronized static void clear() {
    REGISTRY.clear();
    loaded = false;
  }

  // Inner class to determine which handler to use --------------------------------------------------

  /**
   * A content handler to determine which handler implementation should be used to parse the 
   * web access configuration.
   * 
   * @author Christophe Lauret
   * @version 26 November 2009
   */
  private static final class HandlingDispatcher extends DefaultHandler implements ContentHandler {

    /**
     * Maps path infos to generator instances.
     */
    private final ServiceRegistry _registry;

    /**
     * The reader in use.
     */
    private final XMLReader _reader;

    /**
     * Create a new version sniffer for the specified XML reader.
     * 
     * @param reader     The XML Reader in use.
     * @param generators The generators to produce.
     */
    public HandlingDispatcher(XMLReader reader, ServiceRegistry registry) {
      this._reader = reader;
      this._registry = registry;
    }

    /**
     * Once the first element is matched, the reader is assigned the appropriate handler.
     * 
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      // Identify the handler to use
      ContentHandler handler = this.getHandler(localName, atts);
      if (handler != null) {
        try {
          // re-trigger start element event to ensure proper initialisation
          handler.startElement(uri, localName, qName, atts);
          this._reader.setContentHandler(handler);
        } catch (SAXException ex) {
          LOGGER.warn("Generated by wrapped handler", ex);
        }
      }
    }

    /**
     * Returns the content handler to use based on the element and its attributes
     * 
     * @param name The name of the element (local)
     * @param atts The attributes attached to the element.
     * 
     * @return The corresponding handler
     */
    private ContentHandler getHandler(String name, Attributes atts) {
      // Service configuration
      if ("service-config".equals(name)) {
        String version = atts.getValue("version");

        // Version 1.0
        if ("1.0".equals(version)) {
          LOGGER.info("Service configuration 1.0 detected");
          return new ServicesHandler10(this._registry);

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this._registry);
        }

      // Definitely not supported 
      } else {
        LOGGER.error("Unable to determine Berlioz configuration");
        return null;
      }
    }

  }

}
