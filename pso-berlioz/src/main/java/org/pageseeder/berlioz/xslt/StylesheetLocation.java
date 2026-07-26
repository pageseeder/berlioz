/*
 * Copyright 2026 Allette Systems (Australia)
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

/**
 * Identifies where an XSLT stylesheet is resolved from: the identity transform, a filesystem
 * path beneath <code>WEB-INF</code>, or a <code>classpath:</code>/<code>resource:</code> resource.
 *
 * <p>This is the common abstraction shared by {@link org.pageseeder.berlioz.servlet.BerliozConfig},
 * {@link org.pageseeder.berlioz.servlet.XsltTransformer}, and {@link XsltTemplateCache} so that
 * stylesheet resolution, caching, and diagnostics agree on a single notion of "where this
 * stylesheet comes from" and "what is safe to report about it".
 *
 * <p>{@link #logicalPath()} is safe to use in logs and exception messages: for a filesystem path
 * it is the path relative to (or truncated at) <code>WEB-INF</code>; for a classpath resource it is
 * the canonical <code>classpath:</code> reference, never a raw JAR URL or absolute deployment path.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
public final class StylesheetLocation {

  /** The configuration value meaning "no transformation". */
  public static final String IDENTITY = "IDENTITY";

  private static final String CLASSPATH_PREFIX = "classpath:";

  private static final String RESOURCE_PREFIX = "resource:";

  private final StylesheetSourceKind kind;

  private final @Nullable Path path;

  private final @Nullable URL url;

  private final String logicalPath;

  private final boolean mutable;

  private final String cacheKey;

  private StylesheetLocation(StylesheetSourceKind kind, @Nullable Path path, @Nullable URL url,
      String logicalPath, boolean mutable, String cacheKey) {
    this.kind = kind;
    this.path = path;
    this.url = url;
    this.logicalPath = logicalPath;
    this.mutable = mutable;
    this.cacheKey = cacheKey;
  }

  /**
   * @return the identity location: no stylesheet is applied.
   */
  public static StylesheetLocation identity() {
    return new StylesheetLocation(StylesheetSourceKind.IDENTITY, null, null, IDENTITY, false, IDENTITY);
  }

  /**
   * Creates a location for a stylesheet beneath the application's <code>WEB-INF</code> directory.
   *
   * @param path the stylesheet path, expected to be absolute and already contained within the
   *             application's private folder.
   *
   * @return the corresponding location.
   */
  public static StylesheetLocation forFile(Path path) {
    Objects.requireNonNull(path, "path is required");
    Path absolute = path.toAbsolutePath().normalize();
    return new StylesheetLocation(StylesheetSourceKind.FILESYSTEM, path, null,
        toWebPath(absolute.toString()), true, absolute.toString());
  }

  /**
   * Creates a location for a <code>classpath:</code>/<code>resource:</code> stylesheet.
   *
   * @param url         the resolved resource URL, or {@code null} if the resource could not be
   *                     found — resolution failure is reported lazily, at compile time, so it can
   *                     go through the same fallback/error pipeline as a missing filesystem file.
   * @param logicalPath the safe, canonical <code>classpath:</code> reference (never a raw JAR URL).
   *
   * @return the corresponding location.
   */
  public static StylesheetLocation forClasspath(@Nullable URL url, String logicalPath) {
    Objects.requireNonNull(logicalPath, "logicalPath is required");
    String key = url != null ? url.toExternalForm() : logicalPath;
    return new StylesheetLocation(StylesheetSourceKind.CLASSPATH, null, url, logicalPath, false, key);
  }

  /**
   * @return the kind of location this stylesheet was resolved from.
   */
  public StylesheetSourceKind kind() {
    return this.kind;
  }

  /**
   * @return {@code true} if this location is the identity transform.
   */
  public boolean isIdentity() {
    return this.kind == StylesheetSourceKind.IDENTITY;
  }

  /**
   * @return the filesystem path, only present when {@link #kind()} is {@link StylesheetSourceKind#FILESYSTEM}.
   */
  public @Nullable Path path() {
    return this.path;
  }

  /**
   * @return the resource URL, only meaningful when {@link #kind()} is {@link StylesheetSourceKind#CLASSPATH};
   *         {@code null} if the classpath resource could not be resolved.
   */
  public @Nullable URL url() {
    return this.url;
  }

  /**
   * @return a safe, human-readable identification of this location, suitable for logs and
   *         exception messages (never a raw JAR URL or absolute deployment path).
   */
  public String logicalPath() {
    return this.logicalPath;
  }

  /**
   * @return {@code true} if the underlying resource can change while the application is running.
   *         Filesystem stylesheets are mutable; classpath stylesheets are treated as immutable for
   *         the lifetime of the classloader that resolved them.
   */
  public boolean isMutable() {
    return this.mutable;
  }

  /**
   * @return a stable key distinguishing this location from any other, suitable for use in a
   *         shared cache. Distinguishes identically named classpath resources contributed by
   *         different JARs.
   */
  public String cacheKey() {
    return this.cacheKey;
  }

  /**
   * @return this location as a URL — the resource URL for a classpath location, or the file's own
   *         URL for a filesystem location — or {@code null} for the identity location, an
   *         unresolved classpath location, or a filesystem path that cannot be converted to a URL.
   */
  public @Nullable URL toUrl() {
    switch (this.kind) {
      case CLASSPATH:
        return this.url;
      case FILESYSTEM:
        try {
          return Objects.requireNonNull(this.path).toUri().toURL();
        } catch (MalformedURLException ex) {
          return null;
        }
      default:
        return null;
    }
  }

  /**
   * Resolves the classloader to use for classpath stylesheet resolution: the current thread's
   * context classloader, falling back to the classloader that loaded Berlioz itself if
   * unavailable. Mirrors the convention used for classpath service discovery.
   *
   * @return the resolved application classloader.
   */
  public static ClassLoader resolveApplicationClassLoader() {
    ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    return contextLoader != null ? contextLoader : StylesheetLocation.class.getClassLoader();
  }

  /**
   * Extracts the resource path from a <code>classpath:</code> or <code>resource:</code> value,
   * normalizing away a single optional leading slash so that both <code>classpath:/a/b.xsl</code>
   * and <code>classpath:a/b.xsl</code> resolve identically.
   *
   * @param value the configured stylesheet value.
   *
   * @return the normalized resource path, or {@code null} if {@code value} does not use either prefix.
   */
  public static @Nullable String extractClasspathReference(String value) {
    if (value.startsWith(CLASSPATH_PREFIX)) return normalizeLeadingSlash(value.substring(CLASSPATH_PREFIX.length()));
    if (value.startsWith(RESOURCE_PREFIX)) return normalizeLeadingSlash(value.substring(RESOURCE_PREFIX.length()));
    return null;
  }

  private static String normalizeLeadingSlash(String path) {
    return path.startsWith("/") ? path.substring(1) : path;
  }

  @Override
  public String toString() {
    return this.logicalPath;
  }

  /**
   * Strips everything up to and including a leading <code>WEB-INF</code> segment so filesystem
   * paths reported in logs/exceptions don't expose the absolute deployment directory.
   */
  private static String toWebPath(String s) {
    String from = "WEB-INF";
    int x = s.indexOf(from);
    return x != -1 ? s.substring(x + from.length()).replace('\\', '/') : s.replace('\\', '/');
  }

}
