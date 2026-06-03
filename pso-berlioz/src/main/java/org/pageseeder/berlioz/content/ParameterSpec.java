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

import java.util.Objects;
import java.util.function.Function;

import org.pageseeder.berlioz.Beta;

/**
 * A reusable, named parameter specification that encodes the full resolution chain
 * (type conversion, constraints, and terminal) for a single request parameter.
 *
 * <p>Define specs as {@code public static final} fields — in an interface for grouping,
 * or directly on the generator class — and pass them to
 * {@link RequestContext#parameter(ParameterSpec)}:
 *
 * <pre>{@code
 * // Defined once, shared across generators
 * interface AppParameters {
 *   ParameterSpec<Integer> PAGE   = ParameterSpec.of("page",   b -> b.asInt().clamp(1, 10000).defaultValue(1));
 *   ParameterSpec<Status>  STATUS = ParameterSpec.of("status", b -> b.asEnum(Status.class).orDefault(Status.ACTIVE));
 * }
 *
 * // Used in any generator
 * int    page   = req.parameter(AppParameters.PAGE);
 * Status status = req.parameter(AppParameters.STATUS);
 * }</pre>
 *
 * <p>The resolver lambda must include a terminal call ({@code .defaultValue()},
 * {@code .required()}, {@code .orDefault()}, or {@code .nullable()}) so that
 * {@link RequestContext#parameter(ParameterSpec)} returns {@code T} directly.
 *
 * @param <T> the resolved type returned by this spec
 *
 * @author Christophe Lauret
 *
 * @version 0.13.1
 * @since 0.13.1
 */
@Beta
public final class ParameterSpec<T> {

  private final String name;
  private final Function<ParameterBuilder, T> resolver;

  private ParameterSpec(String name, Function<ParameterBuilder, T> resolver) {
    this.name = name;
    this.resolver = resolver;
  }

  /**
   * Creates a new parameter spec.
   *
   * @param <T>      the resolved type
   * @param name     the HTTP parameter name
   * @param resolver a function from {@link ParameterBuilder} to {@code T}; must call a terminal
   *                 method ({@code .defaultValue()}, {@code .required()}, etc.)
   * @return the spec
   */
  public static <T> ParameterSpec<T> of(String name, Function<ParameterBuilder, T> resolver) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(resolver, "resolver");
    return new ParameterSpec<>(name, resolver);
  }

  /**
   * Returns the HTTP parameter name this spec resolves.
   *
   * @return the parameter name, never {@code null}
   */
  public String name() {
    return this.name;
  }

  /**
   * Resolves the parameter value from the given builder.
   *
   * @param builder the builder for this parameter's raw value
   * @return the resolved value
   */
  T resolve(ParameterBuilder builder) {
    return this.resolver.apply(builder);
  }

}
