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
package org.pageseeder.berlioz.error;

/**
 * Controls how much diagnostic information is included in error responses.
 *
 * <ul>
 *   <li>{@link #MINIMAL} — no exception detail; standard RFC 9457 members only</li>
 *   <li>{@link #STANDARD} — adds exception class, message, and source location when available.
 *       No stack trace.</li>
 *   <li>{@link #FULL} — adds stack trace and recursive cause chain on top of {@code STANDARD}</li>
 * </ul>
 *
 * <p>The string representation (lower-case enum name) is the value used in
 * {@code berlioz.errors.detail} configuration.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
public enum DetailLevel {

  MINIMAL, STANDARD, FULL;

  /**
   * Parses the config string value, returning {@link #MINIMAL} for any unrecognised value.
   *
   * @param value the raw config string (e.g. {@code "minimal"}, {@code "standard"}, {@code "full"})
   * @return the matching level, or {@link #MINIMAL} if the value is not recognised
   */
  public static DetailLevel parse(String value) {
    switch (value.toLowerCase()) {
      case "standard": return STANDARD;
      case "full":     return FULL;
      default:         return MINIMAL;
    }
  }

}
