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
package org.pageseeder.berlioz.servlet;

import java.util.EnumSet;
import java.util.Set;
import java.util.StringJoiner;

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scopes of the {@code berlioz-reload} control parameter.
 *
 * <p>{@code berlioz-reload} accepts a comma-separated list of scopes, or {@code true} for all
 * scopes. Scopes are always applied in the order of this enumeration, whatever their order in
 * the parameter, and no scope implies another.
 *
 * <p>Authorized requests that apply at least one scope receive the {@value #HEADER} response
 * header listing the applied scopes.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public enum ReloadScope {

  /** Reloads the global configuration; the last valid configuration is kept on failure. */
  CONFIG("config"),

  /** Reloads the services once a complete candidate registry has been built. */
  SERVICES("services"),

  /** Clears the XSLT cache. */
  XSLT("xslt"),

  /** Resets the ETag seed so that clients must revalidate. */
  ETAGS("etags"),

  /** Reloads the rules of the {@link RedirectFilter} and {@link RelocationFilter}. */
  REDIRECTS("redirects");

  /**
   * The response header listing the scopes applied for the request.
   */
  public static final String HEADER = "X-Berlioz-Reload";

  private static final Logger LOGGER = LoggerFactory.getLogger(ReloadScope.class);

  private final String token;

  ReloadScope(String token) {
    this.token = token;
  }

  /**
   * @return the name of the scope in the <code>berlioz-reload</code> parameter.
   */
  public String token() {
    return this.token;
  }

  /**
   * Parses the value of the {@code berlioz-reload} parameter.
   *
   * <ul>
   *   <li><code>null</code>, empty or <code>false</code>: no scope.</li>
   *   <li><code>true</code>: every scope.</li>
   *   <li>Otherwise, a comma-separated list of scope tokens, e.g. <code>config,services</code>.</li>
   * </ul>
   *
   * @param value the value of the parameter.
   * @return a new modifiable set of scopes.
   *
   * @throws IllegalArgumentException if the list contains an unknown or empty scope.
   */
  public static Set<ReloadScope> parse(@Nullable String value) {
    if (value == null || value.isEmpty() || "false".equals(value)) return EnumSet.noneOf(ReloadScope.class);
    if ("true".equals(value)) return EnumSet.allOf(ReloadScope.class);
    Set<ReloadScope> scopes = EnumSet.noneOf(ReloadScope.class);
    for (String token : value.split(",", -1)) {
      scopes.add(forToken(token.trim()));
    }
    return scopes;
  }

  /**
   * Formats the scopes for the {@value #HEADER} header, e.g. <code>config, services</code>.
   *
   * @param scopes the scopes to format.
   * @return the scope tokens in application order, separated by a comma and a space.
   */
  public static String format(Set<ReloadScope> scopes) {
    StringJoiner joiner = new StringJoiner(", ");
    for (ReloadScope scope : values()) {
      if (scopes.contains(scope)) joiner.add(scope.token);
    }
    return joiner.toString();
  }

  /**
   * Returns the scopes requested by {@code berlioz-reload} and the deprecated aliases.
   *
   * <p>Callers must check that the request is authorized for Berlioz control. When
   * {@code berlioz-reload} contains an unknown scope, no scope is returned at all.
   *
   * @param req the HTTP request.
   * @return a new modifiable set of scopes.
   */
  @SuppressWarnings("removal")
  static Set<ReloadScope> fromRequest(HttpServletRequest req) {
    Set<ReloadScope> scopes;
    try {
      scopes = parse(req.getParameter(ControlParameter.BERLIOZ_RELOAD.parameter()));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Ignoring berlioz-reload: {}", ex.getMessage());
      return EnumSet.noneOf(ReloadScope.class);
    }
    for (ControlParameter parameter : ControlParameter.values()) {
      ReloadScope alias = parameter.alias();
      if (alias != null && "true".equals(req.getParameter(parameter.parameter()))) {
        parameter.warnIfDeprecated();
        scopes.add(alias);
      }
    }
    return scopes;
  }

  private static ReloadScope forToken(String token) {
    for (ReloadScope scope : values()) {
      if (scope.token.equals(token)) return scope;
    }
    // The token comes from the request
    throw new IllegalArgumentException("unknown reload scope '" + safe(token) + "'");
  }

  private static String safe(String token) {
    String safe = token.replaceAll("[^\\w-]", "?");
    return safe.length() > 32 ? safe.substring(0, 32) + "..." : safe;
  }
}
