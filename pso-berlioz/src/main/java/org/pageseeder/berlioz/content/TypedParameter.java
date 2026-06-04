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

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * A typed, optionally constrained, view of a single request parameter.
 *
 * <p>Obtained by calling {@link ParameterBuilder#asString()}, {@link ParameterBuilder#asInt()},
 * and similar methods. Terminal methods resolve the final value or throw
 * {@link InvalidParameterException}.
 *
 * <p>Three states are possible:
 * <ul>
 *   <li><b>Present and valid</b> — {@code parsedValue} is non-null.</li>
 *   <li><b>Absent</b> — the parameter was not submitted; {@code parsedValue} and {@code formatError} are null.</li>
 *   <li><b>Present but invalid</b> — the parameter was submitted but could not be parsed or failed a constraint;
 *       {@code parsedValue} is null and {@code formatError} is set.</li>
 * </ul>
 *
 * <p>Chainable constraint/transform methods refine the value before a terminal is called.
 * They pass absent and already-invalid states through unchanged:
 * <ul>
 *   <li>{@link #clamp(Object, Object)} — silently coerces an out-of-range value to the nearest bound.</li>
 *   <li>{@link #inRange(Object, Object)} — marks an out-of-range value as invalid ({@link InvalidParameterException.Reason#OUT_OF_RANGE}).</li>
 *   <li>{@link #matching(Predicate, String)} — marks a value that fails a predicate as invalid ({@link InvalidParameterException.Reason#NOT_ALLOWED}).</li>
 * </ul>
 *
 * <p>Terminal methods — behavior by state:
 * <table class="striped">
 *   <caption>Terminal method behavior by parameter state</caption>
 *   <tr><th>Terminal</th>          <th>Absent</th>       <th>Invalid</th></tr>
 *   <tr><td>{@link #required()}</td>        <td>throws</td>  <td>throws</td></tr>
 *   <tr><td>{@link #required(Object)}</td>  <td>throws</td>  <td>{@code def}</td></tr>
 *   <tr><td>{@link #optional()}</td>        <td>{@code null}</td> <td>throws</td></tr>
 *   <tr><td>{@link #optional(Object)}</td>  <td>{@code def}</td>  <td>throws</td></tr>
 *   <tr><td>{@link #defaultValue(Object)}</td> <td>{@code def}</td> <td>{@code def}</td></tr>
 * </table>
 *
 * @param <T> the resolved type of the parameter value
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
@Beta
public final class TypedParameter<T> {

  private final String parameterName;
  private final @Nullable T parsedValue;
  private final @Nullable InvalidParameterException formatError;

  TypedParameter(String parameterName, @Nullable T parsedValue, @Nullable InvalidParameterException formatError) {
    this.parameterName = parameterName;
    this.parsedValue = parsedValue;
    this.formatError = formatError;
  }

  // ---------------------------------------------------------------------------
  // Constraint methods
  // ---------------------------------------------------------------------------

  /**
   * Silently coerces the value to the nearest bound when it falls outside {@code [min, max]}.
   *
   * <p>Absent and already-invalid states are passed through unchanged, so the terminal method
   * handles them normally. {@code T} must implement {@link Comparable}.
   *
   * <pre>
   * int page = request.parameter("page").asInt().clamp(1, 1000).defaultValue(1);
   * </pre>
   *
   * @param min the lower bound (inclusive)
   * @param max the upper bound (inclusive)
   * @return a typed parameter whose value is within {@code [min, max]}, or the same state if absent/invalid
   */
  public TypedParameter<T> clamp(T min, T max) {
    T v = this.parsedValue;
    if (v == null) return this;
    if (!(v instanceof Comparable))
      throw new UnsupportedOperationException("clamp() requires T to implement Comparable; got " + v.getClass().getName());
    @SuppressWarnings("unchecked") Comparable<T> c = (Comparable<T>) v;
    if (c.compareTo(min) < 0) return new TypedParameter<>(this.parameterName, min, null);
    if (c.compareTo(max) > 0) return new TypedParameter<>(this.parameterName, max, null);
    return this;
  }

  /**
   * Marks the value as invalid when it falls outside {@code [min, max]}.
   *
   * <p>Absent and already-invalid states are passed through unchanged, so the terminal method
   * handles them normally. {@code T} must implement {@link Comparable}.
   *
   * <pre>{@code
   * int page = request.parameter("page").asInt().inRange(1, 1000).required();      // throw if out of range
   * int page = request.parameter("page").asInt().inRange(1, 1000).defaultValue(1); // default if out of range
   * }</pre>
   *
   * @param min the lower bound (inclusive)
   * @param max the upper bound (inclusive)
   * @return a typed parameter with an {@link InvalidParameterException.Reason#OUT_OF_RANGE} error if out of range,
   *         or the same state if in range, absent, or already invalid
   */
  public TypedParameter<T> inRange(T min, T max) {
    T v = this.parsedValue;
    if (v == null) return this;
    if (!(v instanceof Comparable))
      throw new UnsupportedOperationException("inRange() requires T to implement Comparable; got " + v.getClass().getName());
    @SuppressWarnings("unchecked") Comparable<T> c = (Comparable<T>) v;
    if (c.compareTo(min) < 0 || c.compareTo(max) > 0) {
      return new TypedParameter<>(this.parameterName, null,
          InvalidParameterException.outOfRange(this.parameterName, v.toString(),
              "must be between " + min + " and " + max));
    }
    return this;
  }

  /**
   * Marks the value as invalid when it fails the given predicate.
   *
   * <p>Absent and already-invalid states are passed through unchanged, so the terminal method
   * handles them normally.
   *
   * <pre>{@code
   * int n    = request.parameter("count").asInt().matching(v -> v % 2 == 0, "must be even").required();
   * String s = request.parameter("sku").asString().matching(v -> v.startsWith("SKU-"), "must start with SKU-").required();
   * }</pre>
   *
   * @param predicate   the constraint the value must satisfy
   * @param description a short description used in the error message when the predicate fails
   *                    (e.g. {@code "must be even"}, {@code "must start with SKU-"})
   * @return a typed parameter with a {@link InvalidParameterException.Reason#NOT_ALLOWED} error if the predicate
   *         fails, or the same state if the predicate passes, is absent, or is already invalid
   */
  public TypedParameter<T> matching(Predicate<T> predicate, String description) {
    T v = this.parsedValue;
    if (v == null) return this;
    if (!predicate.test(v)) {
      return new TypedParameter<>(this.parameterName, null,
          InvalidParameterException.constraintFailed(this.parameterName, v.toString(), description));
    }
    return this;
  }

  // ---------------------------------------------------------------------------
  // Terminal methods
  // ---------------------------------------------------------------------------

  /**
   * Returns the value, throwing {@link InvalidParameterException} if absent or invalid.
   *
   * <pre>
   * LocalDate from = request.parameter("from").asLocalDate().required();
   * </pre>
   *
   * @return the parameter value
   * @throws InvalidParameterException if the parameter is absent ({@link InvalidParameterException.Reason#REQUIRED})
   *         or invalid ({@link InvalidParameterException.Reason#INVALID_FORMAT}, etc.)
   */
  public T required() {
    if (this.formatError != null) throw this.formatError;
    T v = this.parsedValue;
    if (v == null) throw InvalidParameterException.required(this.parameterName);
    return v;
  }

  /**
   * Returns the value, or {@code def} if the parameter was submitted but is invalid; throws if absent.
   *
   * <p>Use this when the parameter must be present, but a malformed value should fall back to a
   * safe default rather than surfacing an error.
   *
   * <pre>
   * int page = request.parameter("page").asInt().required(1); // throws if absent, 1 if malformed
   * </pre>
   *
   * @param def the fallback value used when the parameter is present but invalid
   * @return the parameter value, or {@code def} if present but invalid
   * @throws InvalidParameterException if the parameter is absent ({@link InvalidParameterException.Reason#REQUIRED})
   */
  public T required(T def) {
    if (this.formatError != null) return def;
    T v = this.parsedValue;
    if (v == null) throw InvalidParameterException.required(this.parameterName);
    return v;
  }

  /**
   * Returns the value, or {@code null} if absent; throws if the parameter was submitted but is invalid.
   *
   * <p>Use this when the parameter is optional but must be well-formed if provided.
   *
   * <pre>
   * LocalDate from = request.parameter("from").asLocalDate().optional(); // null if not provided
   * </pre>
   *
   * @return the parameter value, or {@code null} if absent
   * @throws InvalidParameterException if the parameter was submitted but could not be parsed or failed a constraint
   */
  public @Nullable T optional() {
    if (this.formatError != null) throw this.formatError;
    return this.parsedValue;
  }

  /**
   * Returns the value, or {@code def} if absent; throws if the parameter was submitted but is invalid.
   *
   * <p>Use this when the parameter is optional but must be well-formed if provided.
   *
   * <pre>
   * Status s = request.parameter("status").asEnum(Status.class).optional(Status.ACTIVE);
   * </pre>
   *
   * @param def the fallback value used when the parameter is absent
   * @return the parameter value, or {@code def} if absent
   * @throws InvalidParameterException if the parameter is present but invalid
   *         ({@link InvalidParameterException.Reason#INVALID_FORMAT},
   *         {@link InvalidParameterException.Reason#NOT_ALLOWED}, etc.)
   */
  public T optional(T def) {
    if (this.formatError != null) throw this.formatError;
    T v = this.parsedValue;
    return v != null ? v : def;
  }

  /**
   * Returns the value, or {@code def} if the parameter is absent or invalid.
   *
   * <p>Use this when any missing or unrecognized value should silently fall back to a default.
   *
   * <pre>
   * int page = request.parameter("page").asInt().clamp(1, 1000).defaultValue(1);
   * </pre>
   *
   * @param def the fallback value
   * @return the parameter value, or {@code def}
   */
  public T defaultValue(T def) {
    T v = this.parsedValue;
    return v != null ? v : def;
  }

}
