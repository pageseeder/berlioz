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

import org.pageseeder.berlioz.output.OutputType;

import java.util.Set;

/**
 * Common base for all Berlioz generator types.
 *
 * <p>Each subtype declares the output formats it can produce via {@link #supported()}.
 * Berlioz intersects the sets of all generators in a service at load time to determine
 * which output paths the service makes available to clients.</p>
 *
 * <p>This interface is not sealed — third-party generators may implement it directly and
 * override {@link #supported()} to declare a custom format set. The four built-in subtypes
 * ({@link XmlGenerator}, {@link JsonGenerator}, {@link Generator}, {@link RawGenerator})
 * cover the common cases.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public interface BerliozGenerator {

  /**
   * Returns the set of output formats this generator can produce.
   *
   * <p>Berlioz calls this once at service registration time to build the service's capability
   * set. Implementations should return a fixed, non-empty set.</p>
   *
   * @return the supported output types; never {@code null} or empty
   */
  Set<OutputType> supported();

}
