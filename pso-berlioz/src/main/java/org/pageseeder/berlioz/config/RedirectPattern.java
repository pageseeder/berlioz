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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.furi.URIPattern;

/**
 * A pattern rule that redirects a request path to a target path.
 *
 * <p>Each instance pairs a source {@link URIPattern} with a target {@link URIPattern} and
 * a flag indicating whether the redirect is permanent (HTTP 301) or temporary (HTTP 302).
 * URI variables captured from the source pattern are expanded into the target pattern,
 * so path segments can be carried across the redirect.</p>
 *
 * <p>Redirect rules are typically loaded from {@code WEB-INF/config/redirect.xml} by
 * {@link RedirectConfig} and applied by the {@code RedirectFilter}.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.12.4
 */
public class RedirectPattern extends MovedLocationPattern {

  /**
   * Whether the redirect is permanent (301) or temporary (302).
   */
  private final boolean permanent;

  /**
   * Creates a new redirect pattern.
   *
   * @param from      the URI pattern matching the original path
   * @param to        the URI pattern for the target path
   * @param permanent {@code true} for a permanent (301) redirect; {@code false} for temporary (302)
   */
  public RedirectPattern(URIPattern from, URIPattern to, boolean permanent) {
    super(from, to);
    this.permanent = permanent;
  }

  /**
   * Returns {@code true} if this is a permanent (301) redirect; {@code false} for temporary (302).
   *
   * @return {@code true} if permanent
   */
  public boolean isPermanent() {
    return this.permanent;
  }

  /**
   * Evaluates this pattern against the given request path and returns the redirect result.
   *
   * <p>Returns {@code null} if the path does not match the source pattern.</p>
   *
   * @param path the request path to evaluate
   * @return a {@link RedirectLocation} describing the redirect, or {@code null} if no match
   */
  public @Nullable RedirectLocation redirect(String path) {
    if (!this.match(path)) return null;
    String target = this.getTarget(path);
    return new RedirectLocation(path, target, this.permanent);
  }

  /**
   * Returns a string representation of this redirect rule, showing the source pattern,
   * target pattern, and redirect type ({@code P} for permanent, {@code T} for temporary).
   *
   * @return a human-readable description of this redirect pattern
   */
  @Override
  public String toString() {
    return "redirect: "+from()+" -> "+to()+(this.permanent ? " P" : " T");
  }
}
