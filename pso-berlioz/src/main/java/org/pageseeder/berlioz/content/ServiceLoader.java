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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
 * @version 0.14.5
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
   * Maps content generators URL patterns to their content generator instance.
   *
   * <p>This is the stable object returned by {@link #getDefaultRegistry()}; its identity never
   * changes across reloads (so references retained by e.g. {@code BerliozServlet} stay valid), but
   * its internal state is atomically replaced wholesale by {@link ServiceRegistry#replaceWith}
   * once a candidate aggregate registry has been fully built and validated.
   */
  private final ServiceRegistry services = new ServiceRegistry();

  /**
   * Whether the current load request has completed successfully.
   */
  private boolean loaded = false;

  private boolean published = false;

  private @Nullable BerliozException lastFailure;

  private long retryAfterNanos;

  /**
   * The sources of the last successful load.
   */
  private List<ServiceSource> loadedSources = List.of();

  /**
   * The source being parsed, so that a failure can be attributed to it.
   */
  private @Nullable ServiceSource currentSource;

  private @Nullable Instant loadedAt;

  private @Nullable Instant failedAt;

  private @Nullable Instant retryAt;

  private ServiceLoadDiagnostics.@Nullable Failure failureDetails;

  static final long RETRY_DELAY_NANOS = TimeUnit.SECONDS.toNanos(30);

  /**
   * Safe diagnostic status; exception details are confined to server logs.
   * @return pending, ready, startup-failed, or reload-failed
   * @since 0.14.3
   */
  public synchronized String getLoadStatus() {
    if (this.lastFailure != null) return this.published ? "reload-failed" : "startup-failed";
    return this.loaded ? "ready" : "pending";
  }

  /**
   * Returns a consistent snapshot of the load state: status, timestamps, the sources and warnings
   * of the last successful load, and the cause of the last failed attempt.
   *
   * @return the current load diagnostics.
   *
   * @since 0.14.5
   */
  public synchronized ServiceLoadDiagnostics getLoadDiagnostics() {
    return new ServiceLoadDiagnostics(getLoadStatus(), this.loadedAt, this.failedAt,
        this.lastFailure != null ? this.retryAt : null, this.loadedSources,
        this.services.warnings(), this.failureDetails);
  }

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
    return this.services.warnings();
  }

  /**
   * Update the patterns based on the current generators.
   *
   * <p>A failed reload keeps serving the last valid registry. Failed attempts are retried on
   * demand after 30 seconds; {@link #requestReload()} permits an immediate retry.
   *
   * @return true if a new registry was published, false if already loaded or retaining the old one.
   *
   * @throws BerliozException if loading fails before any valid registry has been published.
   *
   * @since 0.8.2
   */
  public synchronized boolean loadIfRequired() throws BerliozException {
    return loadIfRequired(System.nanoTime());
  }

  // Monotonic time supplied separately for deterministic retry tests.
  synchronized boolean loadIfRequired(long now) throws BerliozException {
    if (this.loaded) return false;
    if (this.lastFailure != null && now - this.retryAfterNanos < 0) {
      if (!this.published) throw this.lastFailure;
      return false;
    }
    try {
      load();
      return true;
    } catch (BerliozException | RuntimeException ex) {
      // Third-party classloaders and parser/generator initialization can fail unchecked too.
      // Keep them inside the same retry boundary; JVM Errors deliberately propagate.
      BerliozException failure = ex instanceof BerliozException ? (BerliozException) ex
          : new BerliozException("Unexpected failure loading service configuration", ex);
      this.lastFailure = failure;
      this.retryAfterNanos = now + RETRY_DELAY_NANOS;
      recordFailure(failure);
      LOGGER.error(this.published
          ? "Service reload rejected; continuing with the last valid registry. Retry in 30 seconds."
          : "Service startup failed; no registry published. Retry in 30 seconds.", ex);
      if (!this.published) throw failure;
      return false;
    }
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
   * @throws BerliozException if discovery or parsing of any source fails.
   */
  public synchronized void load() throws BerliozException {
    List<ServiceSource> sources = discoverSources();
    List<CollectedError<SAXParseException>> warnings = new ArrayList<>();
    List<List<ServiceRegistration>> perSourceRegistrations = new ArrayList<>(sources.size());
    for (ServiceSource source : sources) {
      this.currentSource = source;
      ParsedSource parsed = parseSource(source);
      warnings.addAll(parsed.warnings);
      perSourceRegistrations.add(parsed.registrations);
    }
    this.currentSource = null;
    MergeResult merged = mergeRegistrations(perSourceRegistrations);
    warnings.addAll(merged.warnings);
    ServiceRegistry candidate = new ServiceRegistry();
    for (ServiceRegistration registration : merged.registrations) {
      candidate.register(registration.service(), registration.pattern(), registration.method());
    }
    this.services.replaceWith(candidate, merged.registrations, warnings);
    loadedSuccessfully(sources);
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
   * @throws UncheckedIOException if the configuration directory cannot be enumerated.
   */
  public List<File> listServiceFiles() {
    File config = GlobalSettings.getConfig();
    if (config == null) return List.of();
    Path directory = config.toPath();
    // An absent optional configuration directory supports classpath-only applications.
    // Unknown/inaccessible paths must be opened so the I/O failure is not treated as absence.
    if (Files.notExists(directory)) return List.of();
    try (Stream<Path> entries = Files.list(directory)) {
      return entries.filter(path -> {
        String name = path.getFileName().toString();
        return name.equals("services.xml") || (name.startsWith("services!") && name.endsWith(".xml"));
      }).map(Path::toFile)
          .sorted(Comparator.comparing((File file) -> !file.getName().equals("services.xml"))
              .thenComparing(File::getName))
          .collect(Collectors.toList());
    } catch (IOException ex) {
      throw new UncheckedIOException("Unable to enumerate filesystem service configuration", ex);
    }
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
   * @throws BerliozException if any source discovery fails.
   *
   * @since 0.14.3
   */
  public List<ServiceSource> discoverSources() throws BerliozException {
    List<ServiceSource> sources = new ArrayList<>(discoverClasspathSources(resolveApplicationClassLoader()));
    try {
      for (File file : listServiceFiles()) {
        sources.add(ServiceSource.filesystem(file, GlobalSettings.getConfig()));
      }
    } catch (UncheckedIOException | SecurityException ex) {
      throw new BerliozException("Unable to discover filesystem service configuration", ex,
          BerliozErrorID.SERVICES_NOT_FOUND);
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
   * @throws BerliozException if classpath enumeration fails.
   *
   * @since 0.14.3
   */
  List<ServiceSource> discoverClasspathSources(ClassLoader classLoader) throws BerliozException {
    Objects.requireNonNull(classLoader, "classLoader is required");
    Map<String, ServiceSource> byUrl = new LinkedHashMap<>();
    try {
      for (URL url : Collections.list(classLoader.getResources(ServiceOrigin.CLASSPATH_RESOURCE_PATH))) {
        byUrl.putIfAbsent(url.toExternalForm(), ServiceSource.classpath(url));
      }
    } catch (IOException | UncheckedIOException | SecurityException ex) {
      throw new BerliozException("Unable to discover classpath service configuration", ex,
          BerliozErrorID.SERVICES_NOT_FOUND);
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
   * @since 0.14.3
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
    ServiceSource source = ServiceSource.filesystem(xml, GlobalSettings.getConfig());
    ParsedSource parsed = parseSource(source);
    ServiceRegistry candidate = new ServiceRegistry();
    for (ServiceRegistration registration : parsed.registrations) {
      candidate.register(registration.service(), registration.pattern(), registration.method());
    }
    this.services.replaceWith(candidate, parsed.registrations, parsed.warnings);
    loadedSuccessfully(List.of(source));
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
        ServiceRegistration previous = byKey.get(key);
        byKey.put(key, previous != null ? registration.replacing(previous) : registration);
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
   * @since 0.14.3
   */
  public synchronized void requestReload() {
    LOGGER.info("Marking content manager for reload");
    this.loaded = false;
    this.retryAfterNanos = System.nanoTime();
    this.retryAt = Instant.now();
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
    this.published = false;
    this.lastFailure = null;
    this.retryAfterNanos = 0;
    this.loadedSources = List.of();
    this.currentSource = null;
    this.loadedAt = null;
    this.failedAt = null;
    this.retryAt = null;
    this.failureDetails = null;
  }

  private void loadedSuccessfully(List<ServiceSource> sources) {
    if (this.lastFailure != null) LOGGER.info("Service configuration recovered; new registry published.");
    this.published = true;
    this.loaded = true;
    this.lastFailure = null;
    this.retryAfterNanos = 0;
    this.loadedSources = List.copyOf(sources);
    this.loadedAt = Instant.now();
    this.failedAt = null;
    this.retryAt = null;
    this.failureDetails = null;
  }

  /**
   * Records the cause of a failed attempt, attributed to the source being parsed if any.
   *
   * <p>The message of a {@code BerliozException} thrown by this class is always generic; the
   * detail is only taken from a parse error, never from an I/O error, which may name a path.
   */
  private void recordFailure(BerliozException ex) {
    ServiceSource source = this.currentSource;
    this.currentSource = null;
    Instant now = Instant.now();
    this.failedAt = now;
    this.retryAt = now.plusNanos(RETRY_DELAY_NANOS);
    SAXParseException parseError = findParseError(ex);
    String message = ex.getMessage();
    this.failureDetails = new ServiceLoadDiagnostics.Failure(source != null ? source.origin().displayName() : null,
        parseError != null ? parseError.getLineNumber() : -1,
        message != null ? message : "Unable to load services configuration.",
        parseError != null ? parseError.getMessage() : null);
  }

  /**
   * Returns the first parse error reported by the parser, falling back to the cause chain.
   */
  private static @Nullable SAXParseException findParseError(BerliozException ex) {
    if (ex instanceof CompoundBerliozException) {
      for (CollectedError<Throwable> error : ((CompoundBerliozException) ex).getCollector().getErrors()) {
        if (error.level() != Level.WARNING && error.error() instanceof SAXParseException) {
          return (SAXParseException) error.error();
        }
      }
    }
    for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
      if (cause instanceof SAXParseException) return (SAXParseException) cause;
    }
    return null;
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
