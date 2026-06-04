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

import java.io.IOException;
import java.io.InputStream;
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
 * Manages the compilation and caching of XSLT templates for a single stylesheet path.
 *
 * <p>The caching behavior is controlled by the {@code berlioz.xslt.cache} property
 * (see {@link BerliozOption#XSLT_CACHE} and {@link XsltCacheMode}).
 *
 * <p>A single static cache is shared across all instances. Use {@link #clearCache()} to
 * invalidate this instance's entry, or {@link #clearAllCache()} to flush everything.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
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
   * Shared cache: maps each stylesheet path to its compiled entry.
   */
  private static final Map<Path, CachedEntry> CACHE = new ConcurrentHashMap<>();

  // Instance state
  // ----------------------------------------------------------------------------------------------

  private final Path templatesPath;
  private final @Nullable URL fallback;
  private @Nullable String etag;

  /**
   * Creates a cache for the given stylesheet path.
   *
   * @param templatesPath the path to the main XSLT stylesheet.
   * @param fallback      a fallback URL used when the main path does not exist (optional).
   */
  public XsltTemplateCache(Path templatesPath, @Nullable URL fallback) {
    this.templatesPath = Objects.requireNonNull(templatesPath, "templatesPath is required");
    this.fallback = fallback;
    this.etag = computeEtag(templatesPath, fallback);
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
    XsltCacheMode mode = XsltCacheMode.from(GlobalSettings.get(BerliozOption.XSLT_CACHE));
    String stylesheet = toWebPath(this.templatesPath.toAbsolutePath().toString());

    if (mode == XsltCacheMode.NO) {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching disabled]", stylesheet);
      return compile();
    }

    CachedEntry cached = CACHE.get(this.templatesPath);
    if (cached != null) {
      if (mode == XsltCacheMode.MANUAL || !isStale(this.templatesPath, cached)) {
        return cached.templates;
      }
      LOGGER.info("XSLT stylesheet '{}' changed, reloading", stylesheet);
      CACHE.remove(this.templatesPath);
    } else {
      LOGGER.info("Loading XSLT stylesheet '{}' [caching {}]", stylesheet, mode);
    }

    Templates templates = compile();
    CACHE.put(this.templatesPath, new CachedEntry(templates, maxLastModified(this.templatesPath.getParent())));
    return templates;
  }

  /**
   * Returns the ETag for the current state of the stylesheet directory.
   *
   * @return the ETag, or {@code null} if it cannot be computed.
   */
  public @Nullable String getEtag() {
    return this.etag;
  }

  /**
   * Returns the path to the main XSLT stylesheet.
   *
   * @return the templates path.
   */
  public Path templatesPath() {
    return this.templatesPath;
  }

  /**
   * Removes the cached entry for this instance's stylesheet.
   */
  public synchronized void clearCache() {
    LOGGER.debug("Clearing XSLT cache for '{}'.", this.templatesPath.getFileName());
    CACHE.remove(this.templatesPath);
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
    try (InputStream in = url.openStream()) {
      Source source = new StreamSource(in);
      source.setSystemId(url.toString());
      return newTransformerFactory().newTemplates(source);
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
   * Compiles the stylesheet at {@link #templatesPath} and updates {@link #etag}.
   */
  private Templates compile() throws TransformerException {
    long t0 = System.currentTimeMillis();
    Templates templates = toTemplates(this.templatesPath, this.fallback);
    LOGGER.debug("Templates compiled in {}ms", System.currentTimeMillis() - t0);
    this.etag = computeEtag(this.templatesPath, this.fallback);
    return templates;
  }

  /**
   * Returns whether the cached entry is stale. Debounces the filesystem scan to at most once
   * per {@link #AUTO_CHECK_INTERVAL_MS} by updating {@code checkedAt} on every call.
   */
  private static boolean isStale(Path p, CachedEntry entry) {
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
   * Computes the ETag for the template directory by hashing the path, size, and last-modified
   * of every file. Falls back to hashing the fallback URL string if the main file is absent.
   */
  private static @Nullable String computeEtag(Path templates, @Nullable URL fallback) {
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
   * Compiles a stylesheet from {@code stylepath}, using {@code fallback} if the file is absent.
   */
  private static Templates toTemplates(Path stylepath, @Nullable URL fallback) throws TransformerException {
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
        return compile(fallback);
      }
      LOGGER.warn("Unable to find template file: {}", stylepath);
      throw new TransformerConfigurationException("Unable to find stylesheet: " + toWebPath(stylepath.toString()), ex);
    } catch (IOException ex) {
      throw new TransformerConfigurationException("Unable to read stylesheet: " + toWebPath(stylepath.toString()), ex);
    }
  }

  private static Templates newTemplates(TransformerFactory factory, Source source, XsltErrorCollector listener)
      throws TransformerException {
    try {
      return factory.newTemplates(source);
    } catch (TransformerConfigurationException ex) {
      throw new XsltExceptionWrapper(ex, listener);
    }
  }

  @SuppressWarnings("java:S2755") // file-only access is required for xsl:import/include
  static TransformerFactory newTransformerFactory() throws TransformerConfigurationException {
    TransformerFactory factory = TransformerFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file");
    return factory;
  }

  private static String toWebPath(String s) {
    String from = "WEB-INF";
    int x = s.indexOf(from);
    return x != -1 ? s.substring(x + from.length()).replace('\\', '/') : s.replace('\\', '/');
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
