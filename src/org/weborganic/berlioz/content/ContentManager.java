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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.SAXParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.http.HttpMethod;
import org.weborganic.berlioz.util.CollectedError;
import org.weborganic.berlioz.util.CollectedError.Level;
import org.weborganic.berlioz.xml.BerliozEntityResolver;
import org.weborganic.berlioz.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
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
 * @version 23 June 2011
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
   * Returns the content generator instance corresponding to the specified
   * path information.
   * 
   * @param path   The path information to access this generator.
   * @param method The HTTP method for this service.
   * 
   * @return The corresponding task instance.
   */
  public static MatchingService getService(String path, HttpMethod method) {
    if (path == null || method == null) return null;
    loadIfRequired();
    return SERVICES.get(path, method);
  }

  /**
   * Returns the list of methods allowed for this URL.
   * 
   * @param path   The path information to access this generator.
   * 
   * @return The corresponding task instance.
   */
  @Beta public static List<String> allows(String path) {
    if (path == null) return Collections.emptyList();
    loadIfRequired();
    return SERVICES.allows(path);
  }

  /**
   * Update the patterns based on the current generators.
   */
  private static synchronized void loadIfRequired() {
    if (!loaded) {
      try {
        load();
        loaded = true;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
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
    if (!xml.exists())
      return;
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
    if (xml == null || !xml.exists())
      throw new IllegalArgumentException("The generators configuration could not be found in "+xml);
    SAXParser parser = XMLUtils.getParser(true);
    ErrorCollector collector = new ErrorCollector();
    // Load the generators
    try {
      XMLReader reader = parser.getXMLReader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, SERVICES);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(collector);
      LOGGER.info("parsing "+xml.toURI().toString());
      reader.parse(new InputSource(xml.toURI().toString()));
    } catch (SAXException ex) {
      LOGGER.error("An SAX error occurred while reading XML configuration of generators");
      throw new BerliozException("Could not parse file. " + ex.getMessage(), ex);
    } catch (IOException ex) {
      LOGGER.error("An I/O error occurred while reading XML configuration of generators");
      throw new BerliozException("Could not read file.", ex);
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
     * The document locator for use when reporting errors and warnings.
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
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) {
      // Identify the handler to use
      ContentHandler handler = this.getHandler(localName, atts);
      if (handler != null) {
        try {
          // re-trigger start element event to ensure proper initialisation
          handler.setDocumentLocator(this._locator);
          handler.startDocument();
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
          return new ServicesHandler10(this._registry, this._reader.getErrorHandler());

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this._registry, this._reader.getErrorHandler());
        }

      // Definitely not supported 
      } else {
        LOGGER.error("Unable to determine Berlioz configuration");
        return null;
      }
    }

  }

  /**
   * 
   * @author Christophe Lauret (Weborganic)
   * @version 28 June 2011
   */
  private static final class ErrorCollector implements ErrorHandler {

    /**
     * The collected errors.
     */
    private List<CollectedError<SAXParseException>> errors = new ArrayList<CollectedError<SAXParseException>>(); 

    /**
     * Creates a new Berlioz error handler.
     */
    private ErrorCollector() {
    }

    /**
     * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
     * 
     * @param exception A SAX parse fatal reported by the SAX parser.
     * 
     * @throws SAXParseException Identical to the parameter.
     */
    public void fatalError(SAXParseException exception) throws SAXParseException {
      this.errors.add(new CollectedError<SAXParseException>(Level.FATAL, exception));
      LOGGER.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
      throw exception;
    }

    /**
     * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
     * 
     * @param exception A SAX parse error reported by the SAX parser.
     * 
     * @throws SAXParseException Identical to the parameter.
     */
    public void error(SAXParseException exception) throws SAXParseException {
      this.errors.add(new CollectedError<SAXParseException>(Level.ERROR, exception));
      LOGGER.error("{} (line: {})", exception.getMessage(), exception.getLineNumber());
      throw exception;
    }

    /**
     * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
     * 
     * @param exception A SAX parse warning reported by the SAX parser.
     */
    public void warning(SAXParseException exception) {
      this.errors.add(new CollectedError<SAXParseException>(Level.WARNING, exception));
      LOGGER.warn("{} (line: {})", exception.getMessage(), exception.getLineNumber());
    }

  }

}
