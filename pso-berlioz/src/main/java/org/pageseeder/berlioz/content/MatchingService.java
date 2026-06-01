/*
 * Copyright 2015 Allette Systems (Australia)
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

import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;

/**
 * Encapsulates the results of the service matching.
 *
 * <p>Implementation note: all fields are immutable.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.7
 * @since Berlioz 0.6
 */
public final class MatchingService {

  /**
   * The matched service.
   */
  private final Service service;

  /**
   * The URI pattern it matched.
   */
  private final URIPattern pattern;

  /**
   * The resolved URI variables.
   */
  private final URIResolveResult result;

  /**
   * Creates a new matching service.
   *
   * @param service The matched service.
   * @param pattern The URI pattern it matched.
   * @param result  The resolved URI variables.
   *
   * @throws NullPointerException If any argument is <code>null</code>
   */
  public MatchingService(Service service, URIPattern pattern, URIResolveResult result) {
    this.service = Objects.requireNonNull(service, "Cannot match null service");
    this.pattern = Objects.requireNonNull(pattern, "Pattern must be specified");
    this.result = Objects.requireNonNull(result, "Resolution results must be specified");
  }

  /**
   * Indicates whether this response is cacheable.
   *
   * <p>A response is cacheable only is the service has been found and all its generators are
   * cacheable.
   *
   * @return <code>true</code> if this response is cacheable;
   *         <code>false</code> otherwise.
   */
  public boolean isCacheable() {
    return this.service.isCacheable();
  }

  /**
   * Always returns the matched service (always a value).
   *
   * @return The matched service (never <code>null</code>).
   */
  public Service service() {
    return this.service;
  }

  /**
   * Always returns the matching URI pattern.
   *
   * @return The URI pattern it matched (never <code>null</code>).
   */
  public URIPattern pattern() {
    return this.pattern;
  }

  /**
   * Always returns the resolved URI variables.
   *
   * @return The resolved URI variables (never <code>null</code>).
   */
  public URIResolveResult result() {
    return this.result;
  }

}
