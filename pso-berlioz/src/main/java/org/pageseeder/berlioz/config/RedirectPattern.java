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
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.furi.URIPattern;

import java.util.Objects;

/**
 * A pattern rule that redirects a request path to a target path.
 *
 * <p>Each instance pairs a source {@link URIPattern} with a target {@link URIPattern} and
 * the HTTP status of the redirect: 301, 302, 307 or 308.
 * URI variables captured from the source pattern are expanded into the target pattern,
 * so path segments can be carried across the redirect.</p>
 *
 * <p>Redirect rules are typically loaded from {@code WEB-INF/config/redirect.xml} by
 * {@link RedirectConfig} and applied by the {@code RedirectFilter}.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.12.4
 */
public class RedirectPattern extends MovedLocationPattern {

  /**
   * The HTTP status of the redirect.
   */
  private final ContentStatus status;

  /**
   * Creates a new redirect pattern.
   *
   * @param from      the URI pattern matching the original path
   * @param to        the URI pattern for the target path
   * @param permanent {@code true} for a permanent (301) redirect; {@code false} for temporary (302)
   *
   * @deprecated Use {@link #RedirectPattern(URIPattern, URIPattern, ContentStatus)} with
   *             {@link ContentStatus#MOVED_PERMANENTLY} or {@link ContentStatus#FOUND};
   *             will be removed in 1.0.
   */
  @Deprecated(since = "0.14.6", forRemoval = true)
  public RedirectPattern(URIPattern from, URIPattern to, boolean permanent) {
    this(from, to, permanent ? ContentStatus.MOVED_PERMANENTLY : ContentStatus.FOUND);
  }

  /**
   * Creates a new redirect pattern with the specified HTTP status.
   *
   * @param from   the URI pattern matching the original path
   * @param to     the URI pattern for the target path
   * @param status the status of the redirect: 301, 302, 307 or 308
   *
   * @throws IllegalArgumentException if the status is not one of the supported redirect statuses
   * @since 0.14.6
   */
  public RedirectPattern(URIPattern from, URIPattern to, ContentStatus status) {
    super(from, to);
    this.status = checkStatus(status);
  }

  /**
   * Returns the HTTP status of this redirect: 301, 302, 307 or 308.
   *
   * @return the redirect status
   * @since 0.14.6
   */
  public ContentStatus status() {
    return this.status;
  }

  /**
   * Returns {@code true} if this is a permanent redirect (301 or 308); {@code false} for a
   * temporary one (302 or 307).
   *
   * @return {@code true} if permanent
   */
  public boolean isPermanent() {
    return isPermanent(this.status);
  }

  private static boolean isSupported(ContentStatus status) {
    return status == ContentStatus.MOVED_PERMANENTLY
        || status == ContentStatus.FOUND
        || status == ContentStatus.TEMPORARY_REDIRECT
        || status == ContentStatus.PERMANENT_REDIRECT;
  }

  static boolean isPermanent(ContentStatus status) {
    return status == ContentStatus.MOVED_PERMANENTLY || status == ContentStatus.PERMANENT_REDIRECT;
  }

  private static ContentStatus checkStatus(ContentStatus status) {
    Objects.requireNonNull(status, "status");
    if (!isSupported(status))
      throw new IllegalArgumentException("Unsupported redirect status: " + status.code());
    return status;
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
    return new RedirectLocation(path, target, this.status);
  }

  /**
   * Returns a string representation of this redirect rule, showing the source pattern,
   * target pattern, and redirect status.
   *
   * @return a human-readable description of this redirect pattern
   */
  @Override
  public String toString() {
    return "redirect: "+from()+" -> "+to()+" "+this.status.code();
  }
}
