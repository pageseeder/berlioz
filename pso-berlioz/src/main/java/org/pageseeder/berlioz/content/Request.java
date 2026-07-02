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
package org.pageseeder.berlioz.content;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * Read-only view of an incoming request, providing typed and modern-API access to parameters,
 * attributes, cookies, and environment context.
 *
 * <p>This is the clean interface that content generators should prefer. It carries only
 * request-reading concerns; response signaling ({@code setStatus}, {@code setRedirect})
 * remains on {@link ContentRequest} pending a future generator API change.
 *
 * <p>New collection-returning methods ({@link #parameterNames()}, {@link #parameterValues(String)},
 * {@link #cookies()}) replace the legacy array and {@code Enumeration} variants on
 * {@link ContentRequest}.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.1
 */
@Beta
public interface Request {

  /**
   * Returns the dynamic path of the Berlioz request.
   *
   * @return the Berlioz path, never {@code null}
   */
  String getBerliozPath();

  /**
   * Returns the named parameter value, or {@code null} if absent or empty.
   *
   * @param name the parameter name
   * @return the value, or {@code null}
   */
  @Nullable String getParameter(String name);

  /**
   * Returns the named parameter value, or {@code def} if absent or empty.
   *
   * @param name the parameter name
   * @param def  the fallback value
   * @return the value, or {@code def}
   */
  String getParameter(String name, String def);

  /**
   * Returns a builder for typed, validating access to the named parameter.
   *
   * <pre>{@code
   * int page       = request.parameter("page").asInt().clamp(1, 10000).defaultValue(1);
   * LocalDate from = request.parameter("from").asLocalDate().optional();
   * String sort    = request.parameter("sort").oneOf("name","date","title").required();
   * }</pre>
   *
   * @param name the parameter name
   * @return a builder for the named parameter
   */
  default ParameterBuilder parameter(String name) {
    return new ParameterBuilder(name, getParameter(name));
  }

  /**
   * Resolves a request parameter using the given spec.
   *
   * <p>The spec encodes the parameter name, type conversion, constraints, and terminal behavior.
   * Define specs as {@code public static final} fields and share them across generators:
   *
   * <pre>{@code
   * interface AppParameters {
   *   ParameterSpec<Integer> PAGE   = ParameterSpec.of("page",   b -> b.asInt().clamp(1, 10000).defaultValue(1));
   *   ParameterSpec<Status>  STATUS = ParameterSpec.of("status", b -> b.asEnum(Status.class).optional(Status.ACTIVE));
   * }
   *
   * int    page   = req.parameter(AppParameters.PAGE);
   * Status status = req.parameter(AppParameters.STATUS);
   * }</pre>
   *
   * @param <T>  the resolved type
   * @param spec the parameter spec
   * @return the resolved value
   * @throws org.pageseeder.berlioz.error.InvalidParameterException if the spec's resolver throws (e.g., a required parameter is absent)
   */
  default <T> T parameter(ParameterSpec<T> spec) {
    return spec.resolve(parameter(spec.name()));
  }

  /**
   * Returns the names of all parameters in this request.
   *
   * @return an unordered, non-null collection of parameter names; empty if none
   */
  Collection<String> parameterNames();

  /**
   * Returns all values submitted for the named parameter.
   *
   * @param name the parameter name
   * @return a non-null list of values; empty if the parameter was not submitted
   */
  List<String> parameterValues(String name);

  /**
   * Returns the named request attribute, or {@code null} if not set.
   *
   * @param name the attribute name
   * @return the attribute value, or {@code null}
   */
  @Nullable Object getAttribute(String name);

  /**
   * Sets the named request attribute.
   *
   * @param name the attribute name
   * @param o    the value
   */
  void setAttribute(String name, Object o);

  /**
   * Returns the cookies sent with this request.
   *
   * @return a non-null, unmodifiable list of cookies; empty if none were sent
   */
  List<Cookie> cookies();

  /**
   * Returns the HTTP session, or {@code null} if none exists.
   *
   * @return the session, or {@code null}
   */
  @Nullable HttpSession getSession();

  /**
   * Returns the environment of this request.
   *
   * @return the environment, never {@code null}
   */
  Environment getEnvironment();

  /**
   * Returns location information for this request.
   *
   * @return the location, never {@code null}
   */
  Location getLocation();

}
