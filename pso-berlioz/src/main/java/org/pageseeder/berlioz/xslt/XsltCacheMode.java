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

import org.pageseeder.berlioz.BerliozOption;

import java.util.Locale;

/**
 * Controls the caching behavior of XSLT templates.
 *
 * <p>Configured via the {@code berlioz.xslt.cache} property (see {@link BerliozOption#XSLT_CACHE}).
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
public enum XsltCacheMode {

  /**
   * Disable XSLT caching; templates are always recompiled from source.
   */
  NO,

  /**
   * Monitor XSLT source files for changes and automatically invalidate the cache when a change
   * is detected. Recommended for development.
   */
  AUTO,

  /**
   * Only update the cache when explicitly cleared via the {@code clear-xsl-cache} control
   * parameter. Recommended for production.
   */
  MANUAL;

  /**
   * Parses the configured string value into a cache mode.
   *
   * <p>Accepts {@code "no"}, {@code "auto"}, and {@code "manual"} (case-insensitive). For
   * backward compatibility {@code "false"} maps to {@code NO} and {@code "true"} maps to
   * {@code MANUAL}. Unrecognized values default to {@code MANUAL}.
   *
   * @param value the configured property value
   * @return the corresponding cache mode, never {@code null}
   */
  public static XsltCacheMode from(String value) {
    String v = value.toLowerCase(Locale.ROOT);
    if ("no".equals(v) || "false".equals(v)) return NO;
    if ("auto".equals(v)) return AUTO;
    return MANUAL;
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
