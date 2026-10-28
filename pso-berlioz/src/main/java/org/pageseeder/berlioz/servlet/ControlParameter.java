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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Berlioz control parameters.
 *
 * <p>Control parameters are applied only when the request is authorized, see
 * {@link org.pageseeder.berlioz.security.ControlAuthorization#hasControl(javax.servlet.http.HttpServletRequest)}.
 * Only {@code berlioz-reload} and {@code berlioz-profile} remain in 1.0; the other parameters are
 * deprecated aliases of a {@link ReloadScope}.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public enum ControlParameter {

  /** Reloads the specified scopes, see {@link ReloadScope#parse(String)}. */
  BERLIOZ_RELOAD("berlioz-reload", null),

  /** Enables profiling for the request when {@code true}. */
  BERLIOZ_PROFILE("berlioz-profile", null),

  /** Same as {@code berlioz-reload=services} when {@code true}. */
  @Deprecated(since = "0.14.5", forRemoval = true)
  RELOAD_SERVICES("reload-services", ReloadScope.SERVICES),

  /** Same as {@code berlioz-reload=xslt} when {@code true}. */
  @Deprecated(since = "0.14.5", forRemoval = true)
  CLEAR_XSL_CACHE("clear-xsl-cache", ReloadScope.XSLT),

  /** Same as {@code berlioz-reload=etags} when {@code true}. */
  @Deprecated(since = "0.14.5", forRemoval = true)
  RESET_ETAGS("reset-etags", ReloadScope.ETAGS);

  private static final Logger LOGGER = LoggerFactory.getLogger(ControlParameter.class);

  private final String parameter;

  private final @Nullable ReloadScope alias;

  private final AtomicBoolean warned = new AtomicBoolean();

  ControlParameter(String parameter, @Nullable ReloadScope alias) {
    this.parameter = parameter;
    this.alias = alias;
  }

  /**
   * @return the name of the HTTP parameter.
   */
  public String parameter() {
    return this.parameter;
  }

  /**
   * @return the reload scope this deprecated parameter is an alias of; <code>null</code> for
   *         <code>berlioz-reload</code> and <code>berlioz-profile</code>.
   */
  public @Nullable ReloadScope alias() {
    return this.alias;
  }

  /**
   * Indicates whether the specified HTTP parameter name is a Berlioz control parameter.
   *
   * @param name the name of the HTTP parameter.
   * @return <code>true</code> if it is a control parameter, including deprecated aliases;
   *         <code>false</code> otherwise.
   */
  public static boolean isControlParameter(@Nullable String name) {
    for (ControlParameter p : values()) {
      if (p.parameter.equals(name)) return true;
    }
    return false;
  }

  /**
   * Logs a deprecation warning the first time a deprecated alias is applied.
   */
  void warnIfDeprecated() {
    if (this.alias != null && this.warned.compareAndSet(false, true)) {
      LOGGER.warn("Control parameter {}=true is deprecated since 0.14.5 and will be removed in 1.0;"
          + " use berlioz-reload={} instead", this.parameter, this.alias.token());
    }
  }
}
