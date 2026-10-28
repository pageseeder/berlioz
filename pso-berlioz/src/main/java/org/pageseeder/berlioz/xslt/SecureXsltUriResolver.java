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

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.jspecify.annotations.Nullable;

/**
 * Resolves {@code xsl:import}/{@code xsl:include} references for the secure {@code TransformerFactory}.
 *
 * <p>Supports three forms:
 * <ul>
 *   <li>Relative filesystem imports from filesystem templates — left to the processor's default
 *       resolution (this resolver returns {@code null}), constrained by {@code accessExternalStylesheet}.</li>
 *   <li>Relative imports within a classpath/JAR template — also left to default resolution, which
 *       naturally stays within the same artifact since it resolves against the importing
 *       stylesheet's own {@code jar:} system ID.</li>
 *   <li>Explicit {@code classpath:}/{@code resource:} references — resolved here, through the
 *       application classloader, allowing deliberate cross-artifact sharing.</li>
 * </ul>
 *
 * <p>As defense in depth beyond the {@code accessExternalStylesheet} restriction, this resolver
 * independently rejects any reference — relative or absolute — that resolves to a protocol other
 * than local {@code file} or {@code jar:file}, so network schemes are never followed even if a processor's
 * own enforcement differs.
 *
 * <p>Relative references require an absolute, usable base URL so their locality can be checked.
 * Missing, empty, or relative bases are rejected rather than delegated to processor-specific
 * resolution against the working directory. Absolute references are validated independently
 * of the base. Unrecognized or malformed URLs are also rejected; parsing failure never bypasses
 * the local-resource checks.
 *
 * <p>Exposed publicly through {@link XsltResolvers}.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.3
 */
final class SecureXsltUriResolver implements URIResolver {

  private final ClassLoader classLoader;

  SecureXsltUriResolver(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public @Nullable Source resolve(String href, @Nullable String base) throws TransformerException {
    if (href.isEmpty()) return null;
    String classpathPath = classpathReference(href);
    if (classpathPath != null) return new StreamSource(locateClasspath(href, classpathPath).toString());
    locateUrl(href, base);
    // Approved relative/file/jar reference: let the processor's own default resolution proceed.
    return null;
  }

  /**
   * Returns the URL of the resource the renderer loads for the specified reference.
   *
   * <p>Applies the same checks as {@link #resolve(String, String)}, but returns the URL of approved
   * relative, {@code file:} and {@code jar:file:} references instead of leaving them to the
   * processor.
   *
   * @param href The reference, as found in {@code xsl:import} or {@code xsl:include}
   * @param base The absolute URL of the referencing stylesheet, if any
   *
   * @return the URL of a local resource
   * @throws TransformerException If the reference is denied, malformed or cannot be found.
   */
  URL locate(String href, @Nullable String base) throws TransformerException {
    String classpathPath = classpathReference(href);
    if (classpathPath != null) return locateClasspath(href, classpathPath);
    return locateUrl(href, base);
  }

  private static @Nullable String classpathReference(String href) throws TransformerException {
    try {
      return StylesheetLocation.extractClasspathReference(href);
    } catch (IllegalArgumentException ex) {
      throw new TransformerException("Invalid classpath XSLT import/include: " + href, ex);
    }
  }

  private URL locateClasspath(String href, String classpathPath) throws TransformerException {
    URL url = this.classLoader.getResource(classpathPath);
    if (url == null) {
      throw new TransformerException("Unable to resolve classpath XSLT import/include: " + href);
    }
    requireLocalUrl(url);
    return url;
  }

  private static URL locateUrl(String href, @Nullable String base) throws TransformerException {
    try {
      URL resolved = resolveAbsoluteOrRelative(href, base);
      requireLocalUrl(resolved);
      return resolved;
    } catch (MalformedURLException ex) {
      throw new TransformerException("Unable to resolve XSLT resource URL: href='" + href
          + "', base=" + (base == null ? "<absent>" : "'" + base + "'")
          + ". Expected an absolute resource URL or a relative reference with an absolute base URL.", ex);
    }
  }

  /**
   * Resolves an absolute reference directly; falls back to resolving against the base URL
   * only when the href is relative and a base is available.
   */
  private static URL resolveAbsoluteOrRelative(String href, @Nullable String base) throws MalformedURLException {
    try {
      // An absolute reference does not depend on the importing stylesheet's base.
      return new URL(href);
    } catch (MalformedURLException ex) {
      if (base == null) throw ex;
      return new URL(new URL(base), href);
    }
  }

  /**
   * Validates before opening a connection, including the backing URL of a JAR resource.
   * Only local files and a single JAR backed by a local file are supported.
   */
  static void requireLocalUrl(URL url) throws TransformerException {
    URL backing = url;
    if ("jar".equalsIgnoreCase(url.getProtocol())) {
      String path = url.getFile();
      int separator = path.indexOf("!/");
      if (separator < 0) throw new TransformerException("Invalid XSLT JAR resource URL");
      try {
        backing = new URL(path.substring(0, separator));
      } catch (MalformedURLException ex) {
        throw new TransformerException("Invalid XSLT JAR backing URL", ex);
      }
    }
    if (!"file".equalsIgnoreCase(backing.getProtocol())
        || (!backing.getHost().isEmpty() && !"localhost".equalsIgnoreCase(backing.getHost()))) {
      throw new TransformerException("Denied non-local XSLT resource protocol: " + backing.getProtocol());
    }
  }

}
