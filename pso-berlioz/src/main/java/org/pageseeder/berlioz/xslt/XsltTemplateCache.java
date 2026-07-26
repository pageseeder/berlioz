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
package org.pageseeder.berlioz.xslt;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.util.SHA256;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the compilation and caching of XSLT templates for a single stylesheet location.
 *
 * <p>The caching behavior is controlled by the {@code berlioz.xslt.cache} property
 * (see {@link BerliozOption#XSLT_CACHE} and {@link XsltCacheMode}). Filesystem stylesheets are
 * mutable: {@code auto} mode monitors them for changes and {@code no} recompiles on every access.
 * Classpath stylesheets are treated as immutable — {@code auto} and {@code manual} both reuse the
 * compiled templates without ever scanning the filesystem; only {@code no} recompiles them.
 *
 * <p>A single static cache is shared across all instances. Use {@link #clearCache()} to
 * invalidate this instance's entry, or {@link #clearAllCache()} to flush everything.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.13.1
 */
public final class XsltTemplateCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(XsltTemplateCache.class);

  /**
   * How long (ms) to wait between filesystem staleness checks in AUTO mode.
   */
  private static final long AUTO_CHECK_INTERVAL_MS = 500L;

  /**
   * Sentinel returned by {@link #compile(URL)} when loading fails or the URL is {@code null}.
   * Test with {@link #isIdentity(Templates)}.
   */
  private static final Templates IDENTITY_TEMPLATES = new Templates() {
    @Override
    public Transformer newTransformer() throws TransformerConfigurationException {
      return newTransformerFactory().newTransformer();
    }
    @Override
    public Properties getOutputProperties() {
      return new Properties();
    }
  };

  /**
   * Shared cache: maps each stylesheet location's cache key to its compiled entry.
   */
  private static final Map<String, CachedEntry> CACHE = new ConcurrentHashMap<>();

  // Instance state
  // ----------------------------------------------------------------------------------------------

  private final StylesheetLocation location;
  private final @Nullable URL fallback;
  private @Nullable String etag;

  /**
   * Creates a cache for the given stylesheet path.
   *
   * @param templatesPath the path to the main XSLT stylesheet.
   * @param fallback      a fallback URL used when the main path does not exist (optional).
   */
  public XsltTemplateCache(Path templatesPath, @Nullable URL fallback) {
    this(StylesheetLocation.forFile(Objects.requireNonNull(templatesPath, "templatesPath is required")), fallback);
  }

  /**
   * Creates a cache for the given stylesheet location.
   *
   * @param location the location of the main XSLT stylesheet.
   * @param fallback a fallback URL used when the main location does not exist (optional).
   *
   * @since 0.14.2
   */
  public XsltTemplateCache(StylesheetLocation location, @Nullable URL fallback) {
    this.location = Objects.requireNonNull(location, "location is required");
    this.fallback = fallback;
    this.etag = computeEtag(location, fallback);
  }

  // Public API
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the compiled templates, consulting and updating the shared cache according to
   * the current {@link XsltCacheMode}.
   *
   * @return the compiled templates.
   * @throws TransformerException if the stylesheet cannot be compiled.
   */
  public synchronized Templates getTemplates() throws TransformerException {
    if (this.location.isIdentity()) return IDENTITY_TEMPLATES;

    XsltCacheMode mode = XsltCacheMode.from(GlobalSettings.get(BerliozOption.XSLT_CACHE));
    String key = this.location.cacheKey();

    if (mode == XsltCacheMode.NO) {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching disabled]", this.location.logicalPath());
      return compile();
    }

    CachedEntry cached = CACHE.get(key);
    if (cached != null) {
      boolean stale = this.location.isMutable() && mode == XsltCacheMode.AUTO && isStale(this.location.path(), cached);
      if (!stale) return cached.templates;
      LOGGER.info("XSLT stylesheet '{}' changed, reloading", this.location.logicalPath());
      CACHE.remove(key);
    } else {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching {}]", this.location.logicalPath(), mode);
    }

    Templates templates = compile();
    long maxLastModified = this.location.kind() == StylesheetSourceKind.FILESYSTEM
        ? maxLastModified(Objects.requireNonNull(this.location.path()).getParent()) : 0L;
    CACHE.put(key, new CachedEntry(templates, maxLastModified));
    return templates;
  }

  /**
   * Returns the ETag for the current state of the stylesheet.
   *
   * @return the ETag, or {@code null} if it cannot be computed.
   */
  public @Nullable String getEtag() {
    return this.etag;
  }

  /**
   * Returns the location of the main XSLT stylesheet.
   *
   * @return the stylesheet location.
   *
   * @since 0.14.2
   */
  public StylesheetLocation location() {
    return this.location;
  }

  /**
   * Returns the path to the main XSLT stylesheet.
   *
   * @return the templates path.
   *
   * @throws UnsupportedOperationException if this cache's location is not a filesystem path;
   *         use {@link #location()} instead.
   */
  public Path templatesPath() {
    Path path = this.location.path();
    if (path == null) {
      throw new UnsupportedOperationException("Stylesheet location is not a filesystem path: " + this.location);
    }
    return path;
  }

  /**
   * Removes the cached entry for this instance's stylesheet.
   */
  public synchronized void clearCache() {
    LOGGER.debug("Clearing XSLT cache for '{}'.", this.location.logicalPath());
    CACHE.remove(this.location.cacheKey());
  }

  /**
   * Removes all cached XSLT template entries.
   */
  public static synchronized void clearAllCache() {
    LOGGER.debug("Clearing all XSLT cache entries.");
    CACHE.clear();
  }

  /**
   * Compiles a stylesheet from the given URL without caching.
   *
   * <p>Returns the {@linkplain #isIdentity identity templates} if {@code url} is {@code null}
   * or if loading fails.
   *
   * @param url the URL of the stylesheet to compile.
   * @return the compiled templates, never {@code null}.
   */
  public static Templates compile(@Nullable URL url) {
    if (url == null) return IDENTITY_TEMPLATES;
    try {
      return compile(url, XsltErrorSensitivity.FATAL);
    } catch (IOException | TransformerException ex) {
      LOGGER.warn("Unable to compile templates from URL: {}", url, ex);
      return IDENTITY_TEMPLATES;
    }
  }

  /**
   * Returns {@code true} if the given templates are the identity (no-op) templates returned
   * when {@link #compile(URL)} fails or receives a {@code null} argument.
   *
   * @param templates the templates to test.
   * @return {@code true} if they are the identity templates.
   */
  public static boolean isIdentity(Templates templates) {
    return templates == IDENTITY_TEMPLATES;
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Compiles the stylesheet at {@link #location} and updates {@link #etag}.
   */
  private Templates compile() throws TransformerException {
    long t0 = System.currentTimeMillis();
    Templates templates = this.location.kind() == StylesheetSourceKind.FILESYSTEM
        ? toTemplatesFromFile(Objects.requireNonNull(this.location.path()), this.fallback)
        : toTemplatesFromClasspath(this.location, this.fallback);
    LOGGER.debug("Templates compiled in {}ms", System.currentTimeMillis() - t0);
    this.etag = computeEtag(this.location, this.fallback);
    return templates;
  }

  /**
   * Returns whether the cached entry is stale. Debounces the filesystem scan to at most once
   * per {@link #AUTO_CHECK_INTERVAL_MS} by updating {@code checkedAt} on every call.
   */
  private static boolean isStale(@Nullable Path p, CachedEntry entry) {
    if (p == null) return false;
    long now = System.currentTimeMillis();
    if (now - entry.checkedAt < AUTO_CHECK_INTERVAL_MS) return false;
    entry.checkedAt = now;
    return maxLastModified(p.getParent()) != entry.maxLastModified;
  }

  /**
   * Returns the highest last-modified time (ms) of any regular file under {@code dir},
   * or {@code 0} if the directory is {@code null} or cannot be read.
   */
  private static long maxLastModified(@Nullable Path dir) {
    if (dir == null) return 0L;
    try (Stream<Path> stream = Files.walk(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .mapToLong(f -> {
            try { return Files.getLastModifiedTime(f).toMillis(); }
            catch (IOException e) { return 0L; }
          })
          .max()
          .orElse(0L);
    } catch (IOException ex) {
      LOGGER.warn("Unable to scan template directory {}", dir, ex);
      return 0L;
    }
  }

  /**
   * Computes the ETag for the stylesheet location, dispatching on its kind.
   */
  private static @Nullable String computeEtag(StylesheetLocation location, @Nullable URL fallback) {
    if (location.isIdentity()) return null;
    return location.kind() == StylesheetSourceKind.FILESYSTEM
        ? computeFileEtag(Objects.requireNonNull(location.path()), fallback)
        : computeClasspathEtag(location, fallback);
  }

  /**
   * Computes the ETag for the template directory by hashing the path, size, and last-modified
   * of every file. Falls back to hashing the fallback URL string if the main file is absent.
   */
  private static @Nullable String computeFileEtag(Path templates, @Nullable URL fallback) {
    if (!Files.exists(templates)) {
      if (fallback != null) return SHA256.hash(fallback.toString());
      LOGGER.error("Unable to find XSLT stylesheet '{}'.", templates.getFileName());
      LOGGER.error("Create a stylesheet at the path below:");
      LOGGER.error("{}", templates);
      return null;
    }
    Path parent = templates.getParent();
    if (parent == null) return null;
    StringBuilder b = new StringBuilder();
    try (Stream<Path> stream = Files.walk(parent)) {
      List<Path> files = stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
      for (Path f : files) {
        b.append(SHA256.hash(f, false));
      }
    } catch (IOException ex) {
      LOGGER.warn("Error thrown while trying to calculate template etag", ex);
      return null;
    }
    return SHA256.hash(b.toString());
  }

  /**
   * Computes the ETag for a classpath stylesheet location.
   *
   * <p>For a resource inside a JAR, hashes the JAR file's own identity metadata (path, size,
   * last-modified) so that a rebuilt artifact — including changes to templates it imports/includes
   * — invalidates the ETag without reading every entry. For an exploded/development classpath
   * resource (a plain {@code file:} URL, no enclosing JAR), hashes the resource's own content;
   * no directory is scanned.
   */
  private static @Nullable String computeClasspathEtag(StylesheetLocation location, @Nullable URL fallback) {
    URL url = location.url();
    if (url == null) {
      if (fallback != null) return SHA256.hash(fallback.toString());
      LOGGER.error("Unable to find classpath XSLT stylesheet '{}'.", location.logicalPath());
      return null;
    }
    try {
      Path jarFile = jarFileOf(url);
      if (jarFile != null) return SHA256.hash(jarFile, false);
      try (InputStream in = url.openStream()) {
        return SHA256.hash(in);
      }
    } catch (IOException ex) {
      LOGGER.warn("Error thrown while trying to calculate classpath template etag", ex);
      return null;
    }
  }

  /**
   * Returns the path of the JAR file backing a {@code jar:} URL, or {@code null} if {@code url}
   * does not use the {@code jar} protocol.
   */
  private static @Nullable Path jarFileOf(URL url) {
    if (!"jar".equals(url.getProtocol())) return null;
    try {
      JarURLConnection connection = (JarURLConnection) url.openConnection();
      return Path.of(connection.getJarFile().getName());
    } catch (IOException | ClassCastException ex) {
      return null;
    }
  }

  /**
   * Compiles a stylesheet from {@code stylepath}, using {@code fallback} if the file is absent.
   */
  private static Templates toTemplatesFromFile(Path stylepath, @Nullable URL fallback) throws TransformerException {
    try (InputStream in = Files.newInputStream(stylepath)) {
      Source source = new StreamSource(in);
      source.setSystemId(stylepath.toUri().toString());
      TransformerFactory factory = newTransformerFactory();
      XsltErrorCollector listener = new XsltErrorCollector(LOGGER);
      factory.setErrorListener(listener);
      return newTemplates(factory, source, listener);
    } catch (NoSuchFileException ex) {
      if (fallback != null) {
        LOGGER.warn("Unable to find template file: {} — using fallback {}", stylepath, fallback);
        XsltErrorSensitivity sensitivity = XsltErrorSensitivity.from(
            GlobalSettings.get(BerliozOption.XSLT_SENSITIVITY));
        try {
          return compile(fallback, sensitivity);
        } catch (IOException ex2) {
          throw new TransformerConfigurationException("Unable to read fallback stylesheet: " + fallback, ex2);
        }
      }
      LOGGER.warn("Unable to find template file: {}", stylepath);
      throw new TransformerConfigurationException(
          "Unable to find stylesheet: " + StylesheetLocation.forFile(stylepath).logicalPath(), ex);
    } catch (IOException ex) {
      throw new TransformerConfigurationException(
          "Unable to read stylesheet: " + StylesheetLocation.forFile(stylepath).logicalPath(), ex);
    }
  }

  /**
   * Compiles a stylesheet from a classpath location, using {@code fallback} if the resource
   * could not be resolved.
   */
  private static Templates toTemplatesFromClasspath(StylesheetLocation location, @Nullable URL fallback)
      throws TransformerException {
    URL url = location.url();
    if (url != null) {
      try (InputStream in = url.openStream()) {
        Source source = new StreamSource(in);
        source.setSystemId(url.toString());
        TransformerFactory factory = newTransformerFactory();
        XsltErrorCollector listener = new XsltErrorCollector(LOGGER);
        factory.setErrorListener(listener);
        return newTemplates(factory, source, listener);
      } catch (IOException ex) {
        throw new TransformerConfigurationException("Unable to read stylesheet: " + location.logicalPath(), ex);
      }
    }
    if (fallback != null) {
      LOGGER.warn("Unable to find classpath template: {} — using fallback {}", location.logicalPath(), fallback);
      XsltErrorSensitivity sensitivity = XsltErrorSensitivity.from(
          GlobalSettings.get(BerliozOption.XSLT_SENSITIVITY));
      try {
        return compile(fallback, sensitivity);
      } catch (IOException ex2) {
        throw new TransformerConfigurationException("Unable to read fallback stylesheet: " + fallback, ex2);
      }
    }
    LOGGER.warn("Unable to find classpath template: {}", location.logicalPath());
    throw new TransformerConfigurationException("Unable to find stylesheet: " + location.logicalPath(),
        new FileNotFoundException(location.logicalPath()));
  }

  private static Templates newTemplates(TransformerFactory factory, Source source, XsltErrorCollector listener)
      throws TransformerException {
    try {
      Templates templates = factory.newTemplates(source);
      listener.throwIfThresholdReached();
      return templates;
    } catch (TransformerException ex) {
      throw new XsltExceptionWrapper(ex, listener);
    }
  }

  private static Templates compile(URL url, XsltErrorSensitivity sensitivity)
      throws IOException, TransformerException {
    try (InputStream in = url.openStream()) {
      Source source = new StreamSource(in);
      source.setSystemId(url.toString());
      TransformerFactory factory = newTransformerFactory();
      XsltErrorCollector listener = new XsltErrorCollector(LOGGER, sensitivity);
      factory.setErrorListener(listener);
      return newTemplates(factory, source, listener);
    }
  }

  @SuppressWarnings("java:S2755") // access is restricted to file/jar and a constrained resolver below
  static TransformerFactory newTransformerFactory() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file,jar");
    factory.setURIResolver(new SecureXsltUriResolver(StylesheetLocation.resolveApplicationClassLoader()));
    return factory;
  }

  // Inner types
  // ----------------------------------------------------------------------------------------------

  /**
   * Holds a compiled {@link Templates} alongside the metadata used by AUTO mode for staleness
   * detection without reading file content.
   */
  private static final class CachedEntry {

    final Templates templates;

    /** Highest last-modified time (ms) across the template directory at compile time. */
    final long maxLastModified;

    /** Timestamp of the last staleness check; volatile for cross-instance visibility. */
    volatile long checkedAt;

    CachedEntry(Templates templates, long maxLastModified) {
      this.templates = templates;
      this.maxLastModified = maxLastModified;
      this.checkedAt = System.currentTimeMillis();
    }
  }

}
