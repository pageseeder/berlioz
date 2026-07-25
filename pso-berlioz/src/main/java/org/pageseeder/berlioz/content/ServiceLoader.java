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
package org.pageseeder.berlioz.content;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.SAXParser;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.xml.BerliozEntityResolver;
import org.pageseeder.berlioz.xml.SAXErrorCollector;
import org.pageseeder.berlioz.xml.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @author Christophe Lauret
 *
 * @version 0.14.1
 * @since 0.6
 */
@SuppressWarnings("java:S6548")
public enum ServiceLoader {

  INSTANCE;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceLoader.class);

  /**
   * The file filter to
   */
  private static final FilenameFilter FILE_FILTER = (dir, name) -> name.startsWith("services!") && name.endsWith(".xml");

  /**
   * Maps content generators URL patterns to their content generator instance.
   */
  private final ServiceRegistry services = new ServiceRegistry();

  /**
   * Indicates whether the boolean value was loaded.
   */
  private volatile boolean loaded = false;

  /**
   * Warnings collected while parsing the services configuration files during the last successful
   * {@link #load()} or {@link #load(File)} call (e.g. a service that could not be registered
   * because it was misconfigured). Empty if the last load reported no warnings.
   */
  private List<CollectedError<SAXParseException>> lastWarnings = List.of();

  /**
   * @return The service loader
   */
  public static ServiceLoader getInstance() {
    return INSTANCE;
  }

  /**
   * Returns the default service registry (mapped to "services.xml").
   *
   * @return the default service registry (mapped to "services.xml").
   */
  public ServiceRegistry getDefaultRegistry() {
    return this.services;
  }

  /**
   * Returns the warnings collected while parsing the services configuration during the last
   * successful load.
   *
   * <p>These do not prevent Berlioz from starting, but generally indicate that one or more
   * services were not registered (e.g. a service configured for direct output whose generator
   * supports no output format). This is the only place besides the logs where such issues can be
   * discovered.
   *
   * @return the warnings from the last load, or an empty list if there were none.
   *
   * @since 0.14.1
   */
  public synchronized List<CollectedError<SAXParseException>> getLastLoadWarnings() {
    return this.lastWarnings;
  }

  /**
   * Update the patterns based on the current generators.
   *
   * @return true is the services were reloaded.
   *
   * @throws BerliozException Should something unexpected happen.
   *
   * @since 0.8.2
   */
  public synchronized boolean loadIfRequired() throws BerliozException {
    if (this.loaded) return false;
    load();
    this.loaded = true;
    return true;
  }

  /**
   * Loads the content access file from all services files.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public synchronized void load() throws BerliozException {
    List<File> files = listServiceFiles();
    List<CollectedError<SAXParseException>> warnings = new ArrayList<>();
    for (File f : files) {
      warnings.addAll(loadFile(f));
    }
    this.lastWarnings = List.copyOf(warnings);
  }

  /**
   * Returns the list of services files to load from the config folder
   * of the repository.
   *
   * <p>This list includes the main file <code>services.xml</code> as well as
   * any file starting with <code>services!</code> and ending in <code>.xml</code>.
   *
   * <p>If it exists, the main file is always returned first. There is no
   * guaranteed ordering for the other services files.
   *
   * <p><b>Root element matters:</b> the main file must use <code>&lt;service-config&gt;</code> as
   * its root element; loading it clears the registry before its own services are registered, since
   * it is always loaded first. Group override files must instead use a bare
   * <code>&lt;services group="..."&gt;</code> as their root element (no
   * <code>&lt;service-config&gt;</code> wrapper) so that loading them adds to the registry rather
   * than clearing services already registered by the main file or by other override files loaded
   * before them. See {@link HandlingDispatcher#getHandler} for how the root element selects between
   * the two.
   *
   * @return the list of services files.
   */
  public List<File> listServiceFiles() {
    File config = GlobalSettings.getConfig();
    if (config == null) return List.of();
    File xml = new File(config, "services.xml");
    File[] subs = config.listFiles(FILE_FILTER);
    List<File> files;

    // `services.xml` file and/or at least one module
    if (subs != null && subs.length > 0) {
      files = new ArrayList<>(subs.length+1);
      if (xml.exists()) {
        files.add(xml);
      }
      Collections.addAll(files, subs);
    }

    // Single `services.xml` file
    else if (xml.exists()) {
      files = Collections.singletonList(xml);
    }

    // No services file at all!
    else {
      files = Collections.emptyList();
    }
    return files;
  }

  /**
   * Loads the content access file.
   *
   * @param xml    The XML file to load.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public synchronized void load(File xml) throws BerliozException {
    this.lastWarnings = loadFile(xml);
  }

  /**
   * Loads one content access file and returns any warnings collected while parsing it.
   *
   * @param xml The XML file to load.
   *
   * @return the warnings collected while loading the file.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  private List<CollectedError<SAXParseException>> loadFile(File xml) throws BerliozException {
    Objects.requireNonNull(xml, "The service configuration file is null! That's it I give up.");
    // Okay, let's start
    SAXParser parser = Xml.safeParser(true);
    SAXErrorCollector collector = new SAXErrorCollector(LOGGER);
    if (GlobalSettings.has(BerliozOption.XML_PARSE_STRICT)) {
      collector.setErrorFlag(Level.WARNING);
    }
    BerliozErrorID id = null;
    // Load the services
    try {
      XMLReader reader = parser.getXMLReader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, this.services);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(collector);
      LOGGER.info("Parsing {}", xml.toURI());
      reader.parse(new InputSource(xml.toURI().toString()));
      // if the error threshold was reached, throw an error!
      if (collector.hasError()) {
        id = BerliozErrorID.SERVICES_INVALID;
        throw new SAXException(collector.getErrors().size()+" error(s) reported by the XML parser.");
      }
    } catch (SAXException ex) {
      if (id == null) {
        id = BerliozErrorID.SERVICES_MALFORMED;
      }
      LOGGER.error("An SAX error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new CompoundBerliozException("Unable to parse services configuration file.", ex, id, collector);
    } catch (IOException ex) {
      LOGGER.error("An I/O error occurred while reading XML service configuration: {}", ex.getMessage());
      throw new BerliozException("Unable to read services configuration file.", ex, BerliozErrorID.SERVICES_NOT_FOUND);
    }
    this.services.touch();
    return List.copyOf(collector.getErrors());
  }

  /**
   * Update the patterns based on the current generators.
   */
  public synchronized void clear() {
    LOGGER.info("Clearing content manager");
    this.services.clear();
    this.loaded = false;
    this.lastWarnings = List.of();
  }

  // Inner class to determine which handler to use --------------------------------------------------

  /**
   * A content handler to determine which handler implementation should be used to parse the
   * web access configuration.
   */
  private static final class HandlingDispatcher extends DefaultHandler implements ContentHandler {

    /**
     * Registry for the services to load.
     */
    private final ServiceRegistry registry;

    /**
     * The reader in use.
     */
    private final XMLReader reader;

    /**
     * The document locator for use when reporting the location of errors and warnings.
     */
    private @Nullable Locator locator;

    /**
     * Create a new version sniffer for the specified XML reader.
     *
     * @param reader   The XML Reader in use.
     * @param registry The service registry.
     */
    public HandlingDispatcher(XMLReader reader, ServiceRegistry registry) {
      this.reader = reader;
      this.registry = registry;
    }

    @Override
    public void setDocumentLocator(@Nullable Locator locator) {
      this.locator = locator;
    }

    /**
     * Once the first element is matched, the reader is assigned the appropriate handler.
     * {@inheritDoc}
     *
     * @throws SAXException if the file being parsed is not a service configuration.
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      // Identify the handler to use
      ContentHandler handler = getHandler(localName, atts);
      Locator loc = this.locator;
      if (loc != null)
        handler.setDocumentLocator(loc);
      // re-trigger events on handler to ensure proper initialization
      handler.startDocument();
      handler.startElement(uri, localName, qName, atts);
      this.reader.setContentHandler(handler);
    }

    /**
     * Returns the content handler to use based on the element and its attributes
     *
     * @param name The name of the element (local)
     * @param atts The attributes attached to the element.
     *
     * @return The corresponding handler
     *
     * @throws SAXException if the file being parsed is not a service configuration.
     */
    private ContentHandler getHandler(String name, Attributes atts) throws SAXException {
      SAXErrorCollector collector = getErrorCollector(this.reader);

      // Service configuration: the main services.xml file. ServicesHandler10 clears the registry
      // when it sees this root element, so only the main file (always loaded first) should use it.
      if ("service-config".equals(name)) {
        String version = atts.getValue("version");

        // Version 1.0
        if ("1.0".equals(version)) {
          LOGGER.info("Service configuration 1.0 detected");
          return new ServicesHandler10(this.registry, collector);

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this.registry, collector);
        }

      // A group override file (services!<group>.xml): a bare <services> root, no <service-config>
      // wrapper, so ServicesHandler10 registers into the existing registry instead of clearing it.
      } else if ("services".equals(name)) {

        LOGGER.info("Services group using 1.0");
        return new ServicesHandler10(this.registry, collector);

      // Definitely not supported
      } else {
        LOGGER.error("Unable to determine Berlioz configuration");
        SAXParseException fatal = new SAXParseException("Not a valid Berlioz service configuration!", this.locator);
        collector.fatalError(fatal);
        // Just in case it wasn't thrown
        throw fatal;
      }
    }

    private SAXErrorCollector getErrorCollector(XMLReader reader) {
      ErrorHandler collector = reader.getErrorHandler();
      if (!(collector instanceof SAXErrorCollector)) throw new IllegalStateException("Expected SAX error collector for reader!");
      return (SAXErrorCollector)collector;
    }
  }

}
