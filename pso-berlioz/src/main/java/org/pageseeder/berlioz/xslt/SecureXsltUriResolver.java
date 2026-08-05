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
import java.util.Set;

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
 * than {@code file} or {@code jar}, so network schemes are never followed even if a processor's
 * own enforcement differs.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.2
 * @since 0.14.2
 */
final class SecureXsltUriResolver implements URIResolver {

  private static final Set<String> ALLOWED_SCHEMES = Set.of("file", "jar");

  private final ClassLoader classLoader;

  SecureXsltUriResolver(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  @Override
  public @Nullable Source resolve(String href, @Nullable String base) throws TransformerException {
    if (href == null || href.isEmpty()) return null;
    String classpathPath = StylesheetLocation.extractClasspathReference(href);
    if (classpathPath != null) return resolveClasspath(href, classpathPath);
    denyUnlessApprovedScheme(href, base);
    // Approved relative/file/jar reference: let the processor's own default resolution proceed.
    return null;
  }

  private Source resolveClasspath(String href, String classpathPath) throws TransformerException {
    URL url = this.classLoader.getResource(classpathPath);
    if (url == null) {
      throw new TransformerException("Unable to resolve classpath XSLT import/include: " + href);
    }
    if (!ALLOWED_SCHEMES.contains(url.getProtocol().toLowerCase())) {
      throw new TransformerException("Denied external protocol for XSLT import/include: " + url.getProtocol());
    }
    return new StreamSource(url.toString());
  }

  private void denyUnlessApprovedScheme(String href, @Nullable String base) throws TransformerException {
    try {
      URL resolved = base != null ? new URL(new URL(base), href) : new URL(href);
      String scheme = resolved.getProtocol();
      if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
        throw new TransformerException("Denied external protocol for XSLT import/include: " + scheme);
      }
    } catch (MalformedURLException ex) {
      // Not a URL this resolver understands (e.g. a bare relative path with no usable base);
      // fall through and let the processor's own default resolution and access checks apply.
    }
  }

}
