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

import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.util.CollectedError.Level;

/**
 * Controls which diagnostics reported through JAXP's {@code ErrorListener} make an XSLT operation fail.
 *
 * <p>Configured via the {@code berlioz.xslt.sensitivity} property (see
 * {@link BerliozOption#XSLT_SENSITIVITY}).
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public enum XsltErrorSensitivity {

  /** Only fatal diagnostics make the operation fail. */
  FATAL(Level.FATAL),

  /** Error and fatal diagnostics make the operation fail. */
  ERROR(Level.ERROR),

  /** Warning, error, and fatal diagnostics all make the operation fail. */
  WARNING(Level.WARNING);

  private final Level threshold;

  XsltErrorSensitivity(Level threshold) {
    this.threshold = threshold;
  }

  /**
   * Parses the configured value, defaulting to {@link #ERROR} when it is absent or unrecognized.
   *
   * @param value the configured property value
   * @return the corresponding sensitivity, never {@code null}
   */
  public static XsltErrorSensitivity from(@Nullable String value) {
    if (value == null) return ERROR;
    switch (value.trim().toLowerCase(Locale.ROOT)) {
      case "fatal":   return FATAL;
      case "warning": return WARNING;
      case "error":
      default:        return ERROR;
    }
  }

  Level threshold() {
    return this.threshold;
  }

  boolean includes(Level level) {
    return this.threshold.compareTo(level) <= 0;
  }

  @Override
  public String toString() {
    return name().toLowerCase(Locale.ROOT);
  }
}
