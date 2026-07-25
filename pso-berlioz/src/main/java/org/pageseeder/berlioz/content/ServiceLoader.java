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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.SAXParser;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.Pair;
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
 * @version 0.14.2
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
   *
   * <p>This is the stable object returned by {@link #getDefaultRegistry()}; its identity never
   * changes across reloads (so references retained by e.g. {@code BerliozServlet} stay valid), but
   * its internal state is atomically replaced wholesale by {@link ServiceRegistry#replaceWith}
   * once a candidate aggregate registry has been fully built and validated.
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
   * Discovers, parses and publishes the complete service configuration: every classpath
   * {@code META-INF/berlioz/services.xml} resource followed by the filesystem {@code services.xml}
   * and {@code services!*.xml} modules (see {@link #discoverSources()}).
   *
   * <p>Each source is parsed in isolation (see {@link #parseSource(ServiceSource)}) so that one
   * source's {@code <service-config>} root cannot erase another source's contributions, then all
   * sources' registrations are merged in source order (see {@link #mergeRegistrations(List)}):
   * a later declaration for the same HTTP method and URI pattern replaces an earlier one, and the
   * replacement is reported as a warning naming both origins. Different methods sharing the same
   * pattern never conflict.
   *
   * <p>The resulting aggregate candidate is only published — replacing the live registry
   * atomically and updating {@link #getLastLoadWarnings()} — once every source has parsed without
   * a fatal error. If any source fails, this method throws and the previously published registry,
   * version, and warnings are left completely untouched.
   *
   * @throws BerliozException Should any source fail to parse.
   */
  public synchronized void load() throws BerliozException {
    List<ServiceSource> sources = discoverSources();
    List<CollectedError<SAXParseException>> warnings = new ArrayList<>();
    List<List<ServiceRegistration>> perSourceRegistrations = new ArrayList<>(sources.size());
    for (ServiceSource source : sources) {
      ParsedSource parsed = parseSource(source);
      warnings.addAll(parsed.warnings);
      perSourceRegistrations.add(parsed.registrations);
    }
    MergeResult merged = mergeRegistrations(perSourceRegistrations);
    warnings.addAll(merged.warnings);
    ServiceRegistry candidate = new ServiceRegistry();
    for (ServiceRegistration registration : merged.registrations) {
      candidate.register(registration.service(), registration.pattern(), registration.method());
    }
    this.services.replaceWith(candidate);
    this.lastWarnings = List.copyOf(warnings);
  }

  /**
   * Returns the list of services files to load from the config folder
   * of the repository.
   *
   * <p>This list includes the main file <code>services.xml</code> as well as
   * any file starting with <code>services!</code> and ending in <code>.xml</code>.
   *
   * <p>If it exists, the main file is always returned first. The other service files are returned
   * in lexical filename order.
   *
   * <p><b>Root element matters:</b> the main file must use <code>&lt;service-config&gt;</code> as
   * its root element; group override files must instead use a bare
   * <code>&lt;services group="..."&gt;</code> as their root element (no
   * <code>&lt;service-config&gt;</code> wrapper). Since each file is now parsed into its own
   * isolated registry (see {@link #parseSource(ServiceSource)}) before being merged into the
   * aggregate candidate, this distinction no longer controls whether a file erases another file's
   * contributions — it did in previous versions, when files were parsed directly into one shared
   * registry — but it is retained for clarity and forward compatibility. See
   * {@link HandlingDispatcher#getHandler} for how the root element selects between the two.
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
      Arrays.sort(subs, Comparator.comparing(File::getName));
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
   * Discovers all service configuration sources, classpath sources first and filesystem sources
   * second, so that application-owned filesystem declarations are parsed — and take precedence —
   * last.
   *
   * <p>Note: unlike {@link #load()}, this method only discovers and describes sources; it does not
   * parse or register them.
   *
   * @return the discovered sources, classpath sources first.
   *
   * @since 0.14.2
   */
  public List<ServiceSource> discoverSources() {
    List<ServiceSource> sources = new ArrayList<>(discoverClasspathSources(resolveApplicationClassLoader()));
    for (File file : listServiceFiles()) {
      sources.add(ServiceSource.filesystem(file, GlobalSettings.getConfig()));
    }
    return List.copyOf(sources);
  }

  /**
   * Discovers {@code META-INF/berlioz/services.xml} resources visible to the given classloader.
   *
   * <p>Identical resource URLs (e.g. the same classpath entry contributed twice) are returned only
   * once. The result is sorted by {@link ServiceSource#orderingKey()} so that discovery order is
   * stable across repeated calls regardless of classloader enumeration order.
   *
   * @param classLoader the classloader to enumerate resources from.
   *
   * @return the discovered classpath sources, deduplicated and deterministically ordered.
   *
   * @since 0.14.2
   */
  List<ServiceSource> discoverClasspathSources(ClassLoader classLoader) {
    Objects.requireNonNull(classLoader, "classLoader is required");
    Map<String, ServiceSource> byUrl = new LinkedHashMap<>();
    try {
      for (URL url : Collections.list(classLoader.getResources(ServiceOrigin.CLASSPATH_RESOURCE_PATH))) {
        byUrl.putIfAbsent(url.toExternalForm(), ServiceSource.classpath(url));
      }
    } catch (IOException ex) {
      LOGGER.warn("Unable to enumerate classpath service configuration resources: {}", ex.getMessage());
    }
    List<ServiceSource> sources = new ArrayList<>(byUrl.values());
    sources.sort(Comparator.comparing(ServiceSource::orderingKey));
    return List.copyOf(sources);
  }

  /**
   * Resolves the classloader to use for classpath discovery and generator instantiation: the
   * current thread's context classloader, falling back to the classloader that loaded Berlioz
   * itself if unavailable.
   *
   * @return the resolved application classloader.
   *
   * @since 0.14.2
   */
  static ClassLoader resolveApplicationClassLoader() {
    ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    return contextLoader != null ? contextLoader : ServiceLoader.class.getClassLoader();
  }

  /**
   * Loads a single, ad hoc service configuration file, replacing the complete live registry with
   * this file's content alone.
   *
   * <p>The file is parsed in isolation first (see {@link #parseSource(ServiceSource)}); the live
   * registry and {@link #getLastLoadWarnings()} are only replaced once parsing succeeds, so a
   * malformed file leaves the previous registry and warnings completely untouched.
   *
   * @param xml The XML file to load.
   *
   * @throws BerliozException Should something unexpected happen.
   */
  public synchronized void load(File xml) throws BerliozException {
    Objects.requireNonNull(xml, "The service configuration file is null! That's it I give up.");
    ParsedSource parsed = parseSource(ServiceSource.filesystem(xml, GlobalSettings.getConfig()));
    ServiceRegistry candidate = new ServiceRegistry();
    for (ServiceRegistration registration : parsed.registrations) {
      candidate.register(registration.service(), registration.pattern(), registration.method());
    }
    this.services.replaceWith(candidate);
    this.lastWarnings = parsed.warnings;
  }

  /**
   * Parses one service configuration source into a fresh, isolated {@link ServiceRegistry} — never
   * the live/shared one — so that a source using a {@code <service-config>} root (expected of every
   * classpath JAR) cannot erase another source's contributions when sources are later merged.
   *
   * @param source The service configuration source.
   *
   * @return the warnings collected while parsing the source, and the registrations it declared
   *         (each tagged with the source's origin).
   *
   * @throws BerliozException Should something unexpected happen.
   */
  private ParsedSource parseSource(ServiceSource source) throws BerliozException {
    // Okay, let's start
    SAXParser parser = Xml.safeParser(true);
    SAXErrorCollector collector = new SAXErrorCollector(LOGGER);
    if (GlobalSettings.has(BerliozOption.XML_PARSE_STRICT)) {
      collector.setErrorFlag(Level.WARNING);
    }
    BerliozErrorID id = null;
    ServiceRegistry isolated = new ServiceRegistry();
    // Load the services
    try {
      XMLReader reader = parser.getXMLReader();
      ClassLoader classLoader = resolveApplicationClassLoader();
      HandlingDispatcher dispatcher = new HandlingDispatcher(reader, isolated, classLoader);
      reader.setContentHandler(dispatcher);
      reader.setEntityResolver(BerliozEntityResolver.getInstance());
      reader.setErrorHandler(collector);
      LOGGER.info("Parsing {}", source.origin());
      reader.parse(new InputSource(source.url().toExternalForm()));
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
    List<ServiceRegistration> registrations = isolated.toRegistrations(source.origin());
    return new ParsedSource(List.copyOf(collector.getErrors()), registrations);
  }

  /**
   * Merges the registrations extracted from each parsed source into a single aggregate candidate,
   * in source order.
   *
   * <p>Registrations are keyed by HTTP method and URI pattern: when two sources declare the same
   * key, the later source's registration replaces the earlier one, and a warning is recorded
   * naming the HTTP method, the URI pattern, the replaced service and its origin, and the
   * replacing service and its origin. Different methods sharing the same URI pattern are
   * independent registrations and never conflict.
   *
   * @param perSourceRegistrations the registrations extracted from each source, in source order.
   *
   * @return the merged, conflict-free registrations and the warnings describing any replacements.
   */
  private static MergeResult mergeRegistrations(List<List<ServiceRegistration>> perSourceRegistrations) {
    Map<Pair<HttpMethod, String>, ServiceRegistration> byKey = new LinkedHashMap<>();
    List<CollectedError<SAXParseException>> warnings = new ArrayList<>();
    for (List<ServiceRegistration> sourceRegistrations : perSourceRegistrations) {
      for (ServiceRegistration registration : sourceRegistrations) {
        Pair<HttpMethod, String> key = new Pair<>(registration.method(), registration.pattern().toString());
        ServiceRegistration previous = byKey.put(key, registration);
        if (previous != null) {
          String message = conflictWarning(previous, registration);
          LOGGER.warn(message);
          warnings.add(new CollectedError<>(Level.WARNING, new SAXParseException(message, null)));
        }
      }
    }
    return new MergeResult(List.copyOf(byKey.values()), List.copyOf(warnings));
  }

  /**
   * Builds the warning message reported when a registration replaces another one previously
   * registered for the same HTTP method and URI pattern, naming both origins.
   *
   * @param previous the registration being replaced.
   * @param next     the replacing registration.
   *
   * @return the warning message.
   */
  private static String conflictWarning(ServiceRegistration previous, ServiceRegistration next) {
    return next.service() + " from " + next.origin() + " overrides " + previous.service() + " from "
        + previous.origin() + " for " + next.method() + " " + next.pattern()
        + " - " + previous.service() + " will no longer be reachable";
  }

  /**
   * Marks the service configuration for reload without changing the currently published registry.
   *
   * <p>The next call to {@link #loadIfRequired()} builds and validates a complete candidate before
   * replacing the live registry. If that load fails, the last successful registry, version, and
   * warnings remain available.
   *
   * @since 0.14.2
   */
  public synchronized void requestReload() {
    LOGGER.info("Marking content manager for reload");
    this.loaded = false;
  }

  /**
   * Clears the currently published registry and marks the service configuration for reload.
   *
   * <p>This method retains its destructive public semantics. Use {@link #requestReload()} when the
   * live registry must remain available until a replacement has been loaded successfully.
   */
  public synchronized void clear() {
    LOGGER.info("Clearing content manager");
    this.services.clear();
    this.loaded = false;
    this.lastWarnings = List.of();
  }

  /**
   * The result of parsing a single {@link ServiceSource} in isolation: the warnings collected
   * while parsing it, and the registrations it declared (each tagged with the source's origin).
   */
  private static final class ParsedSource {

    private final List<CollectedError<SAXParseException>> warnings;

    private final List<ServiceRegistration> registrations;

    ParsedSource(List<CollectedError<SAXParseException>> warnings, List<ServiceRegistration> registrations) {
      this.warnings = warnings;
      this.registrations = registrations;
    }
  }

  /**
   * The result of merging every source's registrations into a single aggregate candidate: the
   * conflict-free registrations to publish, and the warnings describing any replacements.
   */
  private static final class MergeResult {

    private final List<ServiceRegistration> registrations;

    private final List<CollectedError<SAXParseException>> warnings;

    MergeResult(List<ServiceRegistration> registrations, List<CollectedError<SAXParseException>> warnings) {
      this.registrations = registrations;
      this.warnings = warnings;
    }
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
     * The classloader used to instantiate generators declared by the parsed source.
     */
    private final ClassLoader classLoader;

    /**
     * The document locator for use when reporting the location of errors and warnings.
     */
    private @Nullable Locator locator;

    /**
     * Create a new version sniffer for the specified XML reader.
     *
     * @param reader      The XML Reader in use.
     * @param registry    The service registry.
     * @param classLoader The classloader used to instantiate generators.
     */
    public HandlingDispatcher(XMLReader reader, ServiceRegistry registry, ClassLoader classLoader) {
      this.reader = reader;
      this.registry = registry;
      this.classLoader = classLoader;
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

      // Service configuration: the main services.xml file, or any classpath source (each parsed
      // into its own isolated registry, so clearing it here only clears that empty isolated
      // registry, never another source's contributions).
      if ("service-config".equals(name)) {
        String version = atts.getValue("version");

        // Version 1.0
        if ("1.0".equals(version)) {
          LOGGER.info("Service configuration 1.0 detected");
          return new ServicesHandler10(this.registry, collector, this.classLoader);

        // Unknown version (assume 1.0)
        } else {
          LOGGER.info("Service configuration version unavailable, assuming 1.0");
          return new ServicesHandler10(this.registry, collector, this.classLoader);
        }

      // A group override file (services!<group>.xml): a bare <services> root, no <service-config>
      // wrapper, so ServicesHandler10 registers into the existing registry instead of clearing it.
      } else if ("services".equals(name)) {

        LOGGER.info("Services group using 1.0");
        return new ServicesHandler10(this.registry, collector, this.classLoader);

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
