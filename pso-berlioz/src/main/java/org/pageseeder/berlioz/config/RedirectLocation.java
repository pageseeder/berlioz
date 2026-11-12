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

import org.pageseeder.berlioz.content.ContentStatus;

/**
 * The result of evaluating a redirect rule against a request path.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.12.4
 */
public final class RedirectLocation {

  private final String from;

  private final String to;

  private final ContentStatus status;

  RedirectLocation(String from, String to, ContentStatus status) {
    this.from = from;
    this.to = to;
    this.status = status;
  }

  /**
   * Returns the original request path that matched the redirect rule.
   *
   * @return The source path.
   */
  public String from() {
    return this.from;
  }

  /**
   * Returns the target path to redirect to.
   *
   * @return The target path.
   */
  public String to() {
    return this.to;
  }

  /**
   * Returns the HTTP status of the redirect: 301, 302, 307 or 308.
   *
   * @return The redirect status.
   * @since 0.14.6
   */
  public ContentStatus status() {
    return this.status;
  }

  /**
   * Returns {@code true} if this is a permanent redirect (301 or 308); {@code false} for a
   * temporary one (302 or 307).
   *
   * @return {@code true} if permanent.
   */
  public boolean isPermanent() {
    return RedirectPattern.isPermanent(this.status);
  }

}
