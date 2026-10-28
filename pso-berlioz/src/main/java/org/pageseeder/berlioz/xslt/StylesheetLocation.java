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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.FileUtils;

/**
 * Identifies where an XSLT stylesheet is resolved from: the identity transform, a filesystem
 * path beneath <code>WEB-INF</code>, or a <code>classpath:</code> resource (the deprecated
 * <code>resource:</code> prefix is a compatibility alias for the same resolution, retained until
 * 1.0 — see {@link #extractClasspathReference(String)}).
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
 * @version 0.14.5
 * @since 0.14.3
 */
public final class StylesheetLocation {

  /** The configuration value meaning "no transformation". */
  public static final String IDENTITY = "IDENTITY";

  private static final String CLASSPATH_PREFIX = "classpath:";

  /** Deprecated compatibility alias for {@link #CLASSPATH_PREFIX}, scheduled for removal in 1.0. */
  private static final String RESOURCE_PREFIX = "resource:";

  /** The classpath folder that overlay stylesheets are packaged under. */
  private static final String OVERLAY_FOLDER = "META-INF/berlioz/";

  /** The folder beneath <code>WEB-INF</code> whose stylesheets can be supplied by an overlay. */
  private static final String XSLT_FOLDER = "xslt";

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
  public static StylesheetLocation forIdentity() {
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
    String classLoaderId = Integer.toHexString(System.identityHashCode(resolveApplicationClassLoader()));
    String key = classLoaderId + ':' + (url != null ? url.toExternalForm() : logicalPath);
    return new StylesheetLocation(StylesheetSourceKind.CLASSPATH, null, url, logicalPath, false, key);
  }

  /**
   * Selects the stylesheet for a path relative to <code>WEB-INF</code> as Berlioz selects a group
   * stylesheet: the file when it exists, otherwise, for a path under <code>xslt/</code>, the first
   * <code>META-INF/berlioz/</code> resource with the same path on the application classpath.
   *
   * <p>For example, <code>xslt/html/default.xsl</code> selects
   * <code>WEB-INF/xslt/html/default.xsl</code> when present, and otherwise
   * <code>classpath:META-INF/berlioz/xslt/html/default.xsl</code>. When neither exists, the
   * (missing) file is returned. A file that exists takes precedence even when it is unreadable or
   * invalid.
   *
   * @param webInf the application's <code>WEB-INF</code> directory.
   * @param path   the stylesheet path relative to <code>WEB-INF</code>; leading slashes are ignored.
   *
   * @return the selected filesystem or classpath location.
   *
   * @throws IllegalArgumentException if the path resolves outside <code>WEB-INF</code>.
   *
   * @since 0.14.5
   */
  @Beta
  public static StylesheetLocation select(Path webInf, String path) {
    File root = webInf.toFile();
    File file = new File(root, stripLeadingSlashes(path));
    if (!FileUtils.contains(root, file)) {
      throw new IllegalArgumentException("Stylesheet path resolves outside WEB-INF: " + path);
    }
    StylesheetLocation location = forFile(file.toPath());
    if (!Files.notExists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return location;
    Path relative = webInf.toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize());
    if (!relative.startsWith(XSLT_FOLDER)) return location;
    String resource = OVERLAY_FOLDER + relative.toString().replace(File.separatorChar, '/');
    URL url = resolveApplicationClassLoader().getResource(resource);
    return url != null ? forClasspath(url, CLASSPATH_PREFIX + resource) : location;
  }

  /**
   * Counts the overlay copies of a stylesheet on the application classpath, that is the
   * <code>META-INF/berlioz/</code> resources with the specified path.
   *
   * <p>Only the first copy can be selected, see {@link #select(Path, String)}; more than one copy
   * usually means that several JARs supply the same stylesheet.
   *
   * @param path the stylesheet path relative to <code>WEB-INF</code>, for example
   *             <code>xslt/html/default.xsl</code>; leading slashes are ignored.
   *
   * @return the number of copies on the classpath.
   *
   * @throws IllegalArgumentException if the path contains a <code>..</code> segment.
   * @throws IOException if the classpath resources cannot be listed.
   *
   * @since 0.14.5
   */
  @Beta
  public static int countClasspathCopies(String path) throws IOException {
    String relative = stripLeadingSlashes(path).replace('\\', '/');
    if (containsTraversalSegment(relative)) {
      throw new IllegalArgumentException("Invalid stylesheet path: " + path);
    }
    return Collections.list(resolveApplicationClassLoader().getResources(OVERLAY_FOLDER + relative)).size();
  }

  private static String stripLeadingSlashes(String path) {
    int start = 0;
    while (start < path.length() && (path.charAt(start) == '/' || path.charAt(start) == '\\')) {
      start++;
    }
    return path.substring(start);
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
   *         different JARs, and — for classpath locations — different application classloaders,
   *         so that a compiled template whose {@code classpath:} imports were resolved through one
   *         classloader is never reused for another (e.g. two webapps sharing the same JVM).
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
   * <p>A value with neither prefix is not a classpath reference at all — {@code null} is returned
   * so the caller can fall back to resolving it some other way (e.g. as a filesystem path). A value
   * that <em>does</em> carry the prefix but contains a <code>..</code> path segment is a different
   * case — a rejected classpath reference, not the absence of one — so it is reported by throwing
   * rather than by returning {@code null}: {@code ClassLoader} resource lookups for an
   * exploded/directory classpath entry resolve such a segment against the filesystem, which could
   * otherwise escape the intended classpath root. (A JAR-backed entry is not exploitable this way —
   * lookup there is a flat entry-name match — but the check is applied uniformly so every caller
   * gets one consistent, safe notion of "resource path" regardless of how the classpath entry
   * happens to be packaged.) Distinguishing the two cases this way also means a rejected reference
   * can never be silently reinterpreted as something else, such as a filesystem path: a caller that
   * only reacts to the {@code null} case, not catches this exception, simply propagates it.
   *
   * <p>The <code>resource:</code> prefix is a deprecated compatibility alias for
   * <code>classpath:</code>, scheduled for removal in 1.0.
   *
   * @param value the configured stylesheet value.
   *
   * @return the normalized resource path, or {@code null} if {@code value} does not use either prefix.
   *
   * @throws IllegalArgumentException if {@code value} uses either prefix but the referenced path
   *                                  contains a <code>..</code> traversal segment.
   */
  public static @Nullable String extractClasspathReference(String value) {
    if (value.startsWith(CLASSPATH_PREFIX)) return normalize(value, value.substring(CLASSPATH_PREFIX.length()));
    if (value.startsWith(RESOURCE_PREFIX)) return normalize(value, value.substring(RESOURCE_PREFIX.length()));
    return null;
  }

  private static String normalize(String original, String path) {
    String noLeadingSlash = path.startsWith("/") ? path.substring(1) : path;
    if (containsTraversalSegment(noLeadingSlash)) {
      throw new IllegalArgumentException("Invalid classpath stylesheet reference: " + original);
    }
    return noLeadingSlash;
  }

  private static boolean containsTraversalSegment(String path) {
    for (String segment : path.split("/")) {
      if ("..".equals(segment)) return true;
    }
    return false;
  }

  /**
   * Derives a safe, loggable <code>classpath:</code> reference from a resolved resource URL,
   * for callers that only have a {@link URL} and no original classpath-relative string to hand.
   *
   * <p>For a resource packaged in a JAR (a <code>jar:...!/entry</code> URL) this recovers the
   * entry path, which is exactly the classpath-relative reference that resolved it. For an
   * exploded/development classpath resource (a plain <code>file:</code> URL) there is no portable
   * way to recover the classpath-relative subpath from the URL alone, so only the file name is
   * kept — never the absolute path.
   *
   * @param url the resolved classpath resource URL.
   *
   * @return a safe <code>classpath:</code> reference, suitable for use as a {@link #logicalPath()}.
   */
  public static String safeClasspathLogicalPath(URL url) {
    Objects.requireNonNull(url, "url is required");
    String external = url.toExternalForm();
    int bang = external.lastIndexOf("!/");
    if (bang != -1) return CLASSPATH_PREFIX + external.substring(bang + 2);
    String path = url.getPath();
    int slash = path.lastIndexOf('/');
    return CLASSPATH_PREFIX + (slash == -1 ? path : path.substring(slash + 1));
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
    return x == -1 ? s.replace('\\', '/') : s.substring(x + from.length()).replace('\\', '/');
  }

}
