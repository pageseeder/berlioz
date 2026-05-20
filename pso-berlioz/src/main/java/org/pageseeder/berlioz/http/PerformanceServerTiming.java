/*
 * Copyright 2019 Allette Systems (Australia)
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
package org.pageseeder.berlioz.http;

import org.eclipse.jdt.annotation.Nullable;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

/**
 * Holds single performance server timing information for use in the server-timing header.
 *
 * @see <a href="https://www.w3.org/TR/server-timing/">W3: Server Timing</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN: Server Timing</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.5
 * @since Berlioz 0.11.5
 */
public final class PerformanceServerTiming {

  /**
   * A valid <code>token</code> according to https://tools.ietf.org/html/rfc7230#section-3.2.6
   *
   * <pre>
   * token          = 1*tchar
   * tchar          = "!" / "#" / "$" / "%" / "&" / "'" / "*"
   *                / "+" / "-" / "." / "^" / "_" / "`" / "|" / "~"
   *                / DIGIT / ALPHA
   * </pre>
   *
   * To be safe we don't allow characters outside ASCII range.
   */
  private static final Pattern VALID_TOKEN = Pattern.compile("^[!#$%&'*+\\-.^_`|~0-9a-zA-Z]+$");

  /**
   * The metric name.
   */
  private final String name;

  /**
   * The server-specified metric description.
   */
  private final String description;

  /**
   * The server-specified metric duration in milliseconds
   */
  private final double duration;

  /**
   * @param name     The metric name.
   * @param duration The server-specified metric duration in milliseconds
   *
   * @throws NullPointerException if name is null
   */
  public PerformanceServerTiming(String name, double duration) {
    this(name, "", duration);
  }

  /**
   * @param name        The metric name.
   * @param description The server-specified metric description.
   * @param duration    The server-specified metric duration in milliseconds
   *
   * @throws NullPointerException if name is null
   */
  public PerformanceServerTiming(String name, @Nullable String description, double duration) {
    this.name = checkName(name);
    this.description = ensureValidDescription(description);
    this.duration = duration;
  }

  public String name() {
    return this.name;
  }

  public String description() {
    return this.description;
  }

  public double duration() {
    return this.duration;
  }

  /**
   * Generate the header value string for the "Server-Timing" header.
   *
   * <p>This method ensure that only valid characters are used to prevent HTTP header attacks such as header splitting.
   *
   * @return the header value string.
   */
  public String toHeaderString() {
    StringBuilder header = new StringBuilder(this.name);
    if (this.description.length() > 0) {
      if (VALID_TOKEN.matcher(this.description).matches()) {
        header.append(";desc=").append(this.description);
      } else {
        header.append(";desc=\"").append(this.description.replaceAll("([\"\\\\])", "\\\\$1")).append('"');
      }
    }
    if (this.duration >= 0) {
      DecimalFormat format = new DecimalFormat("#.###");
      format.setRoundingMode(RoundingMode.CEILING);
      header.append(";dur=").append(format.format(this.duration));
    }
    return header.toString();
  }

  /**
   * Check that the server timing parameter value is a valid.
   *
   * @return A valid name with invalid characters replaced by '_'
   *
   * @throws NullPointerException If the name is null
   * @throws IllegalArgumentException If the name contains illegal characters
   */
  private String checkName(String name) {
    if (name.isEmpty()) throw new IllegalArgumentException("Name must be at least 1 character long");
    if (!VALID_TOKEN.matcher(name).matches()) throw new IllegalArgumentException("Invalid name used for server timing");
    return name;
  }

  private static final Pattern NON_VCHAR = Pattern.compile("[^\\u0009\\u0020!-~]");

  /**
   * Check that the server timing parameter value is a valid <code>token</code> / <code>quoted-string</code>
   * according to https://tools.ietf.org/html/rfc7230#section-3.2.6
   *
   * @return A valid description with invalid characters replaced by '_'
   */
  private String ensureValidDescription(@Nullable String description) {
    if (description == null || description.isEmpty()) return "";
    return NON_VCHAR.matcher(description).replaceAll("_");
  }
}
