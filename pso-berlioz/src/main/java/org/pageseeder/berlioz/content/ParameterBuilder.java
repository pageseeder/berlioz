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
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.error.InvalidParameterException;

/**
 * Entry point for typed, validating access to a single request parameter.
 *
 * <p>Obtained via {@link ContentRequest#parameter(String)}. Call a type-conversion method
 * to get a {@link TypedParameter}, then call a terminal method to resolve the final value:
 *
 * <pre>{@code
 * int page       = request.parameter("page").asInt().clamp(1, 10000).defaultValue(1);
 * int count      = request.parameter("count").asInt().required(0);
 * LocalDate from = request.parameter("from").asLocalDate().optional();
 * Status status  = request.parameter("status").asEnum(Status.class).optional(Status.ACTIVE);
 * String sort    = request.parameter("sort").oneOf("name", "date", "title").required();
 * }</pre>
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

  /**
   * Interprets the parameter as an enum constant using exact name matching.
   *
   * <p>Equivalent to {@code asEnum(enumClass, Function.identity())}. The submitted value must
   * match a constant name exactly (e.g. {@code "ACTIVE"} for {@code Status.ACTIVE}).
   *
   * @param <E>       the enum type
   * @param enumClass the enum class
   * @return a typed parameter resolving to an {@code E}
   */
  public <E extends Enum<E>> TypedParameter<E> asEnum(Class<E> enumClass) {
    return asEnum(enumClass, UnaryOperator.identity());
  }

  /**
   * Interprets the parameter as an enum constant, matching against normalized constant names.
   *
   * <p>The {@code nameMapper} is applied to each constant's {@link Enum#name()} to produce the
   * string the client is expected to send. The submitted value must equal that normalized name
   * exactly — no further transformation is applied to the input. This means each constant has
   * exactly one accepted string, which keeps cache keys unambiguous.
   *
   * <pre>{@code
   * // Only "active", "inactive", "pending" are accepted
   * Status s = request.parameter("status").asEnum(Status.class, String::toLowerCase).required();
   *
   * // Only "active-status", "inactive-status" are accepted (kebab-case)
   * Status s = request.parameter("status").asEnum(Status.class, n -> n.toLowerCase(Locale.ROOT).replace('_', '-')).required();
   * }</pre>
   *
   * <p>The error message lists the accepted (normalized) names, so the client knows exactly what
   * values to send.
   *
   * @param <E>        the enum type
   * @param enumClass  the enum class
   * @param nameMapper applied to each constant's name to derive its accepted string
   * @return a typed parameter resolving to an {@code E}
   */
  public <E extends Enum<E>> TypedParameter<E> asEnum(Class<E> enumClass, UnaryOperator<String> nameMapper) {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    for (E constant : enumClass.getEnumConstants()) {
      if (nameMapper.apply(constant.name()).equals(raw)) {
        return new TypedParameter<>(this.name, constant, null);
      }
    }
    String[] accepted = Arrays.stream(enumClass.getEnumConstants())
        .map(e -> nameMapper.apply(e.name()))
        .toArray(String[]::new);
    return new TypedParameter<>(this.name, null, InvalidParameterException.notAllowed(this.name, raw, accepted));
  }

  /**
   * Parses the parameter into a custom type using the given parser function.
   *
   * <p>The type's {@linkplain Class#getSimpleName() simple name} is used in error messages.
   * Prefer this overload when the class name is a sufficient description of the expected value.
   *
   * <pre>{@code
   * ID   id    = request.parameter("id").as(ID.class, ID::of).required();
   * UUID token = request.parameter("token").as(UUID.class, UUID::fromString).optional();
   * }</pre>
   *
   * <p>If the parser throws a {@link RuntimeException} the parameter is marked invalid with
   * an {@link InvalidParameterException.Reason#INVALID_FORMAT} error.
   *
   * @param <T>    the target type
   * @param type   the target class; its simple name is used in the error message
   * @param parser a function that converts the raw string to {@code T}; may throw
   *               {@link RuntimeException} to signal a parse failure
   * @return a typed parameter resolving to a {@code T}
   */
  public <T> TypedParameter<T> as(Class<T> type, Function<String, T> parser) {
    return as(parser, type.getSimpleName());
  }

  /**
   * Parses the parameter into a custom type using the given parser function and type description.
   *
   * <p>Use this overload when the class simple name is a poor description of the expected value
   * — for example, when a more specific format hint improves the error message.
   *
   * <pre>{@code
   * Slug slug = request.parameter("slug").as(Slug::parse, "URL slug (lowercase, hyphens only)").required();
   * }</pre>
   *
   * <p>If the parser throws a {@link RuntimeException} the parameter is marked invalid with
   * an {@link InvalidParameterException.Reason#INVALID_FORMAT} error.
   *
   * @param <T>      the target type
   * @param parser   a function that converts the raw string to {@code T}; may throw
   *                 {@link RuntimeException} to signal a parse failure
   * @param typeName a short description of the expected format, used in the error message
   * @return a typed parameter resolving to a {@code T}
   */
  public <T> TypedParameter<T> as(Function<String, T> parser, String typeName) {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    try {
      return new TypedParameter<>(this.name, parser.apply(raw), null);
    } catch (RuntimeException ex) {
      return new TypedParameter<>(this.name, null,
          InvalidParameterException.invalidFormat(this.name, raw, typeName, ex));
    }
  }

  /**
   * Accepts the parameter only if its value matches the given compiled pattern (full match).
   *
   * <p>Equivalent to {@code asString()} with a regex format constraint. Prefer this overload
   * when the same pattern is reused across requests — compile it once at class-load time
   * and pass it here.
   *
   * @param pattern the compiled regex the value must fully match
   * @return a typed parameter resolving to a {@code String}
   */
  public TypedParameter<String> matchingRegex(Pattern pattern) {
    String raw = this.rawValue;
    if (raw == null) return new TypedParameter<>(this.name, null, null);
    if (pattern.matcher(raw).matches()) return new TypedParameter<>(this.name, raw, null);
    return new TypedParameter<>(this.name, null,
        InvalidParameterException.invalidFormat(this.name, raw, "text matching /" + pattern.pattern() + "/"));
  }

  /**
   * Accepts the parameter only if its value matches the given regex (full match).
   *
   * <p>Compiles the pattern on every call. For hot-path parameters, prefer
   * {@link #matchingRegex(Pattern)} with a pre-compiled constant.
   *
   * @param regex the regular expression the value must fully match
   * @return a typed parameter resolving to a {@code String}
   */
  public TypedParameter<String> matchingRegex(String regex) {
    return matchingRegex(Pattern.compile(regex));
  }

}
