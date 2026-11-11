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
package org.pageseeder.berlioz.config;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.furi.URIPattern;

/**
 * A rule of a redirect or relocation mapping that is ignored when the file is loaded, because its
 * <code>from</code> or <code>to</code> pattern is missing or cannot be parsed.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.14.6
 */
public final class IgnoredRule {

  /** Why a rule is ignored. */
  public enum Reason {

    /** The rule has no <code>from</code> attribute. */
    MISSING_FROM("missing-from"),

    /** The <code>from</code> attribute is not a valid URI pattern. */
    INVALID_FROM("invalid-from"),

    /** The rule has no <code>to</code> attribute. */
    MISSING_TO("missing-to"),

    /** The <code>to</code> attribute is not a valid URI pattern. */
    INVALID_TO("invalid-to");

    private final String token;

    Reason(String token) {
      this.token = token;
    }

    /** @return the name of the reason in diagnostics, such as <code>invalid-from</code> */
    public String token() {
      return this.token;
    }
  }

  private final int position;
  private final @Nullable String from;
  private final @Nullable String to;
  private final Reason reason;

  private IgnoredRule(int position, @Nullable String from, @Nullable String to, Reason reason) {
    this.position = position;
    this.from = from;
    this.to = to;
    this.reason = reason;
  }

  /**
   * Returns the ignored rule, or <code>null</code> if both patterns are valid.
   *
   * @param position    the position of the rule among the rules of the file, from 1
   * @param from        the <code>from</code> attribute
   * @param fromPattern its pattern, <code>null</code> if missing or invalid
   * @param to          the <code>to</code> attribute
   * @param toPattern   its pattern, <code>null</code> if missing or invalid
   */
  static @Nullable IgnoredRule check(int position, @Nullable String from, @Nullable URIPattern fromPattern,
      @Nullable String to, @Nullable URIPattern toPattern) {
    Reason reason;
    if (from == null) reason = Reason.MISSING_FROM;
    else if (fromPattern == null) reason = Reason.INVALID_FROM;
    else if (to == null) reason = Reason.MISSING_TO;
    else if (toPattern == null) reason = Reason.INVALID_TO;
    else return null;
    return new IgnoredRule(position, from, to, reason);
  }

  /** @return the position of the rule among the rules of the file, from 1 */
  public int position() {
    return this.position;
  }

  /** @return the <code>from</code> attribute as written, if any */
  public @Nullable String from() {
    return this.from;
  }

  /** @return the <code>to</code> attribute as written, if any */
  public @Nullable String to() {
    return this.to;
  }

  /** @return why the rule is ignored; the first problem found, <code>from</code> first */
  public Reason reason() {
    return this.reason;
  }

  @Override
  public String toString() {
    return "IgnoredRule[" + this.position + ", " + this.reason.token() + "]";
  }

}
