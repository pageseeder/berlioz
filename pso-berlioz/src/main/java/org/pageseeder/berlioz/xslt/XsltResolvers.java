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

import java.net.URL;
import java.util.Objects;

import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Gives access to the policy Berlioz applies to the stylesheets referenced by
 * {@code xsl:import} and {@code xsl:include}, so that tools reading stylesheets apply the same
 * rules as the renderer.
 *
 * <p>The policy is:
 * <ul>
 *   <li>{@code classpath:} references (and the deprecated {@code resource:} alias) are resolved
 *     through the specified classloader to their {@code jar:file:} or {@code file:} URL; a
 *     single leading slash is ignored and a path with a {@code ..} segment is rejected before any
 *     lookup;</li>
 *   <li>other references are resolved against the URL of the referencing stylesheet, which must
 *     then be absolute;</li>
 *   <li>only local resources are allowed: a {@code file:} URL without a remote host, or a
 *     {@code jar:} URL backed by such a file. Any other scheme, including network schemes, is
 *     denied before anything is read.</li>
 * </ul>
 *
 * <p>The renderer also configures its {@code TransformerFactory} with secure processing, no
 * external DTD and {@code accessExternalStylesheet} limited to {@code file,jar:file}. Tools that
 * compile stylesheets with the resolver should apply the same settings.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public final class XsltResolvers {

  private XsltResolvers() {
  }

  /**
   * Returns the resolver the renderer uses, for the application classloader.
   *
   * @return A new resolver.
   *
   * @see #secure(ClassLoader)
   * @see StylesheetLocation#resolveApplicationClassLoader()
   */
  public static @NonNull URIResolver secure() {
    return secure(StylesheetLocation.resolveApplicationClassLoader());
  }

  /**
   * Returns the resolver the renderer uses, for the specified classloader.
   *
   * <p>The resolver returns a source for {@code classpath:} references and throws a
   * {@link TransformerException} for any reference that is denied. For other approved references,
   * it returns {@code null} so that the processor resolves them itself, as specified by
   * {@link URIResolver}: tools that read stylesheets without a processor should use
   * {@link #locate(String, String, ClassLoader)} instead.
   *
   * @param loader The classloader for {@code classpath:} references
   *
   * @return A new resolver.
   */
  public static @NonNull URIResolver secure(@NonNull ClassLoader loader) {
    return new SecureXsltUriResolver(Objects.requireNonNull(loader, "loader is required"));
  }

  /**
   * Returns the URL of the resource the renderer loads for a reference, after applying the same
   * checks as the resolver.
   *
   * <p>Unlike the resolver, this method returns the URL of every approved reference, so that
   * tools can read the stylesheet themselves. Nothing is read: a relative or {@code file:}
   * reference is not checked for existence, while a {@code classpath:} reference must be found by
   * the classloader.
   *
   * @param href   The reference, as found in {@code xsl:import} or {@code xsl:include}
   * @param base   The absolute URL of the referencing stylesheet; required for a relative reference
   * @param loader The classloader for {@code classpath:} references
   *
   * @return The URL of a local resource
   *
   * @throws TransformerException If the reference is denied, malformed, relative without an
   *         absolute base, or a {@code classpath:} reference that cannot be found.
   */
  public static @NonNull URL locate(@NonNull String href, @Nullable String base, @NonNull ClassLoader loader)
      throws TransformerException {
    Objects.requireNonNull(href, "href is required");
    return new SecureXsltUriResolver(Objects.requireNonNull(loader, "loader is required")).locate(href, base);
  }

}
