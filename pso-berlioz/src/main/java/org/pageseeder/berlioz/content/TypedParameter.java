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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * A typed, optionally-constrained view of a single request parameter.
 *
 * <p>Obtained by calling {@link ParameterBuilder#asString()}, {@link ParameterBuilder#asInt()},
 * and similar methods. Terminal methods ({@link #required()}, {@link #defaultValue(Object)},
 * {@link #nullable()}) resolve the final value or throw {@link InvalidParameterException}.
 *
 * <p>Three states are possible:
 * <ul>
 *   <li><b>Present and valid</b> — {@code parsedValue} is non-null.</li>
 *   <li><b>Absent</b> — the parameter was not submitted; {@code rawValue} and {@code formatError} are null.</li>
 *   <li><b>Present but invalid</b> — the parameter was submitted but could not be parsed or failed a constraint;
 *       {@code parsedValue} is null and {@code formatError} is set.</li>
 * </ul>
 *
 * <p>Terminal method behaviour:
 * <ul>
 *   <li>{@link #required()} — throws on absent or invalid.</li>
 *   <li>{@link #defaultValue(Object)} — returns the default on absent or invalid.</li>
 *   <li>{@link #nullable()} — returns {@code null} on absent; throws on invalid.</li>
 * </ul>
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

  /**
   * Returns the value, throwing {@link InvalidParameterException} if absent or invalid.
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
   * Returns the value, or {@code def} if the parameter is absent or invalid.
   *
   * @param def the fallback value
   * @return the parameter value, or {@code def}
   */
  public T defaultValue(T def) {
    T v = this.parsedValue;
    return v != null ? v : def;
  }

  /**
   * Returns the value or {@code null} if absent, throwing {@link InvalidParameterException} if invalid.
   *
   * @return the parameter value, or {@code null} if absent
   * @throws InvalidParameterException if the parameter was submitted but could not be parsed or failed a constraint
   */
  public @Nullable T nullable() {
    if (this.formatError != null) throw this.formatError;
    return this.parsedValue;
  }

}
