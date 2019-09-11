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

import java.util.Objects;
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
   * The metric name.
   */
  private final String _name;

  /**
   * The server-specified metric description.
   */
  private final String _description;

  /**
   * The server-specified metric duration in milliseconds
   */
  private final double _duration;

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
  public PerformanceServerTiming(String name, String description, double duration) {
    // TODO Check valid strings https://tools.ietf.org/html/rfc7230#section-3.2.6
    this._name = Objects.requireNonNull(name);
    this._description = description != null? description : "";
    this._duration = duration;
  }

  public String name() {
    return this._name;
  }

  public String description() {
    return this._description;
  }

  public double duration() {
    return this._duration;
  }

  public String toHeaderString() {
    StringBuilder header = new StringBuilder(this._name);
    if (this._description.length() > 0) {
      if (this._description.indexOf(' ') >= 0) {
        header.append(";desc=").append(this._description);
      } else {
        header.append(";desc=\"").append(this._description).append('"');
      }
    }
    // TODO Use decimal format
    header.append(";dur=").append(this._duration);
    return header.toString();
  }
}
