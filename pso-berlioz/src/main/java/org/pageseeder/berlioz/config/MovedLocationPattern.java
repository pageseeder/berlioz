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
package org.pageseeder.berlioz.config;

import org.pageseeder.berlioz.furi.URIParameters;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;

import java.util.Set;

/**
 * Pattern for moving a location and its corresponding template.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.4
 * @since  Berlioz 0.12.4
 */
public class MovedLocationPattern {

  /**
   * URI pattern matching the original path.
   */
  private final URIPattern from;

  /**
   * URI template of the target path.
   */
  private final URIPattern to;

  public MovedLocationPattern(URIPattern from, URIPattern to) {
    this.from = from;
    this.to = to;
  }

  /**
   * URI pattern matching the original path.
   */
  public URIPattern from() {
    return this.from;
  }

  /**
   * URI template of the target path.
   */
  public URIPattern to() {
    return this.to;
  }

  /**
   * Check whether the specified path matches this moved location pattern.
   *
   * @param path THe path to test
   * @return true if it matches; false otherwise.
   */
  public boolean match(String path) {
    return this.from.match(path);
  }

  /**
   * Generate the target path based on the specified path.
   *
   * @param path The path matching the origin pattern
   * @return The corresponding target
   */
  public String getTarget(String path) {
    // Resolve URI variables
    URIResolver resolver = new URIResolver(path);
    URIResolveResult result = resolver.resolve(this.from);

    // Expand the target URI with URI variables
    Set<String> names = result.names();
    URIParameters parameters = new URIParameters();
    for (String name : names) {
      parameters.set(name, (String) result.get(name));
    }
    return this.to.expand(parameters);
  }

}
