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

/**
 * The result of evaluating a redirect rule against a request path.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.4
 */
public final class RedirectLocation {

  private final String from;

  private final String to;

  private final boolean permanent;

  RedirectLocation(String from, String to, boolean permanent) {
    this.from = from;
    this.to = to;
    this.permanent = permanent;
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
   * Returns {@code true} if this is a permanent (301) redirect; {@code false} for temporary (302).
   *
   * @return {@code true} if permanent.
   */
  public boolean isPermanent() {
    return this.permanent;
  }

}
