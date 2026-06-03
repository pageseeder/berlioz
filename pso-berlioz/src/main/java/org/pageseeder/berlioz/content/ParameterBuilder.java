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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * Entry point for typed, validating access to a single request parameter.
 *
 * <p>Obtained via {@link ContentRequest#parameter(String)}. Call a type-conversion method
 * to get a {@link TypedParameter}, then call a terminal method to resolve the final value:
 *
 * <pre>
 * int page     = request.parameter("page").asInt().defaultValue(1);
 * int page     = request.parameter("page").asInt().orDefault(1);
 * LocalDate from = request.parameter("from").asLocalDate().required();
 * String sort  = request.parameter("sort").oneOf("name", "date", "title").defaultValue("name");
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
@Beta
public final class ParameterBuilder {

  private final String name;
  private final @Nullable String rawValue;

  /**
   * Package-private: created only by {@link ContentRequest#parameter(String)}.
   *
   * @param name     the parameter name
   * @param rawValue the raw string value, or {@code null} if the parameter was not submitted
   */
  ParameterBuilder(String name, @Nullable String rawValue) {
    this.name = name;
    this.rawValue = rawValue;
  }

  /**
   * Interprets the parameter as a {@link String}.
   *
   * @return a typed parameter resolving to a {@code String}
   */
  public TypedParameter<String> asString() {
    return new TypedParameter<>(this.name, this.rawValue, null);
  }

  /**
   * Interprets the parameter as an {@code int}.
   *
   * @return a typed parameter resolving to an {@code Integer}
   */
  public TypedParameter<Integer> asInt() {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    try {
      return new TypedParameter<>(this.name, Integer.parseInt(raw), null);
    } catch (NumberFormatException ex) {
      return new TypedParameter<>(this.name, null, InvalidParameterException.invalidFormat(this.name, raw, "integer"));
    }
  }

  /**
   * Interprets the parameter as a {@code long}.
   *
   * @return a typed parameter resolving to a {@code Long}
   */
  public TypedParameter<Long> asLong() {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    try {
      return new TypedParameter<>(this.name, Long.parseLong(raw), null);
    } catch (NumberFormatException ex) {
      return new TypedParameter<>(this.name, null, InvalidParameterException.invalidFormat(this.name, raw, "long integer"));
    }
  }

  /**
   * Interprets the parameter as a {@code boolean}.
   *
   * <p>Accepts {@code "true"} and {@code "false"} (case-insensitive). Any other non-null
   * value is treated as a format error.
   *
   * @return a typed parameter resolving to a {@code Boolean}
   */
  public TypedParameter<Boolean> asBoolean() {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    if ("true".equalsIgnoreCase(raw))  return new TypedParameter<>(this.name, Boolean.TRUE,  null);
    if ("false".equalsIgnoreCase(raw)) return new TypedParameter<>(this.name, Boolean.FALSE, null);
    return new TypedParameter<>(this.name, null, InvalidParameterException.invalidFormat(this.name, raw, "boolean (true/false)"));
  }

  /**
   * Interprets the parameter as a {@link LocalDate} in ISO-8601 format ({@code yyyy-MM-dd}).
   *
   * @return a typed parameter resolving to a {@code LocalDate}
   */
  public TypedParameter<LocalDate> asLocalDate() {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    try {
      return new TypedParameter<>(this.name, LocalDate.parse(raw), null);
    } catch (DateTimeParseException ex) {
      return new TypedParameter<>(this.name, null, InvalidParameterException.invalidFormat(this.name, raw, "date (yyyy-MM-dd)"));
    }
  }

  /**
   * Accepts the parameter only if its value is one of the given strings.
   *
   * <p>Equivalent to {@code asString()} with an additional allowed-values constraint.
   *
   * @param allowed the permitted values
   * @return a typed parameter resolving to a {@code String}
   */
  public TypedParameter<String> oneOf(String... allowed) {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    for (String v : allowed) {
      if (v.equals(raw)) return new TypedParameter<>(this.name, raw, null);
    }
    return new TypedParameter<>(this.name, null, InvalidParameterException.notAllowed(this.name, raw, allowed));
  }

}
