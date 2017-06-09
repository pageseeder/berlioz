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
package org.pageseeder.berlioz.furi;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;


/**
 * Holds the results of a URI resolver.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public final class URIResolveResult implements ResolvedVariables {

  /**
   * The possible status of a resolve result.
   */
  public enum Status {UNRESOLVED, RESOLVED, ERROR}

  /**
   * Maps variable names to their corresponding resolved objects.
   */
  private final Map<String, Object> values = new HashMap<>();

  /**
   * The URI Pattern that was used to produce this result.
   */
  private final URIPattern _pattern;

  /**
   * The status of this result.
   */
  private Status status = Status.UNRESOLVED;

  /**
   * Constructs an instance of this class with fields initialised to null.
   */
  protected URIResolveResult(URIPattern pattern) {
    this._pattern = pattern;
  }

  @Override
  public Set<String> names() {
    return this.values.keySet();
  }

  @Override
  public @Nullable Object get(String name) {
    return this.values.get(name);
  }

  /**
   * Returns the status of this result.
   *
   * @return The status of this result.
   */
  public Status getStatus() {
    return this.status;
  }

  /**
   * Returns the URI Pattern that was used to produce this result.
   *
   * @return The URI Pattern that was used to produce this result.
   */
  public URIPattern getURIPattern() {
    return this._pattern;
  }

// protected methods --------------------------------------------------------

  /**
   * Puts the object corresponding to the specified variable name in the results.
   *
   * @param name The name of the variable.
   * @param o    The corresponding object.
   */
  protected void put(String name, Object o) {
    this.values.put(name, o);
  }

  /**
   * Sets the status of this result.
   *
   * @param status The status of the result.
   */
  protected void setStatus(Status status) {
    this.status = status;
  }

}
