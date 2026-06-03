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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.Cookie;

import org.jspecify.annotations.Nullable;

/**
 * Provides a generic and uniform mechanism for the content generator to access parameters
 * and attributes from a request and to signal the desired response status.
 *
 * <p>This interface extends {@link RequestContext}, which carries the clean, read-only
 * request-reading API. New code should prefer {@link RequestContext} where response
 * signaling is not needed.
 *
 * <p>{@link #getParameterNames()}, {@link #getParameterValues(String)}, and {@link #getCookies()}
 * are deprecated in favor of their {@link RequestContext} equivalents.
 * {@link #getDateParameter(String)} is deprecated because {@link java.util.Date} is a legacy type;
 * use {@code request.parameter(name).asLocalDate()} instead.
 * {@link #getIntParameter(String, int)} and {@link #getLongParameter(String, long)} are retained
 * as ergonomic shorthands and are not deprecated.
 *
 * @author Tu Tak Tran
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.6
 */
public interface ContentRequest extends RequestContext {

  // --- Bridge defaults from RequestContext -----------------------------------

  /**
   * Returns the named parameter parsed as an {@code int}, or {@code def} on failure.
   *
   * @param name the parameter name
   * @param def  fallback value when absent or unparseable
   * @return the parsed value, or {@code def}
   */
  int getIntParameter(String name, int def);

  /**
   * Returns the named parameter parsed as a {@code long}, or {@code def} on failure.
   *
   * @param name the parameter name
   * @param def  fallback value when absent or unparseable
   * @return the parsed value, or {@code def}
   */
  long getLongParameter(String name, long def);

  /**
   * {@inheritDoc}
   *
   * <p>Default implementation delegates to the deprecated {@link #getParameterNames()}.
   * Implementations should override this directly.
   */
  @Override
  @SuppressWarnings({"java:S1874"})
  default Collection<String> parameterNames() {
    return Collections.list(getParameterNames());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Default implementation delegates to the deprecated {@link #getParameterValues(String)}.
   * Implementations should override this directly.
   */
  @Override
  @SuppressWarnings({"java:S1874"})
  default List<String> parameterValues(String name) {
    String[] values = getParameterValues(name);
    return values != null ? Arrays.asList(values) : List.of();
  }

  /**
   * {@inheritDoc}
   *
   * <p>Default implementation delegates to the deprecated {@link #getCookies()}.
   * Implementations should override this directly.
   */
  @Override
  @SuppressWarnings({"java:S1874"})
  default List<Cookie> cookies() {
    Cookie[] arr = getCookies();
    return arr != null ? Arrays.asList(arr) : List.of();
  }

  // --- Deprecated legacy methods --------------------------------------------

  /**
   * Returns the names of all parameters in this request.
   *
   * @return an enumeration of parameter names
   *
   * @deprecated Use {@link #parameterNames()} instead.
   */
  @Deprecated(since = "0.13.1")
  Enumeration<String> getParameterNames();

  /**
   * Returns all values submitted for the named parameter, or {@code null} if absent.
   *
   * @param name the parameter name
   * @return an array of values, or {@code null}
   *
   * @deprecated Use {@link #parameterValues(String)} instead.
   */
  @Deprecated(since = "0.13.1")
  String @Nullable[] getParameterValues(String name);

  /**
   * Returns the cookies sent with this request, or {@code null} if none.
   *
   * @return an array of cookies, or {@code null}
   *
   * @deprecated Use {@link #cookies()} instead.
   */
  @Deprecated(since = "0.13.1")
  Cookie @Nullable[] getCookies();

  /**
   * Returns the named parameter parsed as a {@link Date} (ISO 8601), or {@code null} if absent or unparseable.
   *
   * @param name the parameter name
   * @return a {@code Date} instance, or {@code null}
   *
   * @deprecated Use {@code request.parameter(name).asLocalDate()} instead.
   */
  @Deprecated(since = "0.13.1")
  @Nullable Date getDateParameter(String name);

  // --- Response signalling --------------------------------------------------
  // These remain here pending a future generator API change that introduces
  // a dedicated ContentResponse parameter.

  /**
   * Sets the desired HTTP status for this request's response.
   *
   * @param code the status code
   *
   * @throws NullPointerException     if {@code code} is {@code null}
   * @throws IllegalArgumentException if {@code code} is a redirect status
   */
  void setStatus(ContentStatus code);

  /**
   * Sets the desired redirect target and status for this request's response.
   *
   * @param url  the redirect URL
   * @param code the redirect status
   *
   * @throws NullPointerException     if {@code url} is {@code null}
   * @throws IllegalArgumentException if {@code code} is not a redirect status
   */
  void setRedirect(String url, ContentStatus code);

}
