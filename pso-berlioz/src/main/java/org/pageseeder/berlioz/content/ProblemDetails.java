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

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable RFC 9457 Problem Details object for reporting errors from generators.
 *
 * <p>Standard members:</p>
 * <ul>
 *   <li>{@code type} — a URI identifying the problem type (should resolve to documentation)</li>
 *   <li>{@code title} — a short, human-readable summary of the problem type</li>
 *   <li>{@code status} — the HTTP status code</li>
 *   <li>{@code detail} — a human-readable explanation specific to this occurrence</li>
 *   <li>{@code instance} — a URI identifying the specific occurrence</li>
 * </ul>
 *
 * <p>Extension members (e.g. {@code errors} for parameter validation failures) can be
 * added via {@link #extension(String, Object)}.</p>
 *
 * <p>Berlioz renders problem details according to the negotiated output path:</p>
 * <ul>
 *   <li>JSON: {@code application/problem+json}</li>
 *   <li>XML: {@code application/problem+xml}</li>
 *   <li>HTML: problem XML fed through XSLT</li>
 * </ul>
 *
 * <p>All {@code with*} methods return a new instance; this class is immutable.</p>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9457">RFC 9457 – Problem Details for HTTP APIs</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class ProblemDetails {

  private final ContentStatus status;
  private final @Nullable String type;
  private final @Nullable String title;
  private final @Nullable String detail;
  private final @Nullable String instance;
  private final Map<String, Object> extensions;

  private ProblemDetails(ContentStatus status, @Nullable String type, @Nullable String title,
      @Nullable String detail, @Nullable String instance, Map<String, Object> extensions) {
    this.status = status;
    this.type = type;
    this.title = title;
    this.detail = detail;
    this.instance = instance;
    this.extensions = Collections.unmodifiableMap(new LinkedHashMap<>(extensions));
  }

  /**
   * Creates a minimal problem with only a status code.
   *
   * @param status the HTTP status; must not be {@code null}
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails of(ContentStatus status) {
    Objects.requireNonNull(status, "status");
    return new ProblemDetails(status, null, null, null, null, Map.of());
  }

  /**
   * Returns a copy with the given {@code type} URI.
   *
   * @param type a URI identifying the problem type
   * @return a new instance
   */
  public ProblemDetails type(String type) {
    Objects.requireNonNull(type, "type");
    return new ProblemDetails(this.status, type, this.title, this.detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given human-readable {@code title}.
   *
   * @param title a short summary of the problem type
   * @return a new instance
   */
  public ProblemDetails title(String title) {
    Objects.requireNonNull(title, "title");
    return new ProblemDetails(this.status, this.type, title, this.detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given {@code detail} explanation.
   *
   * @param detail a human-readable explanation of this specific occurrence
   * @return a new instance
   */
  public ProblemDetails detail(String detail) {
    Objects.requireNonNull(detail, "detail");
    return new ProblemDetails(this.status, this.type, this.title, detail, this.instance, this.extensions);
  }

  /**
   * Returns a copy with the given {@code instance} URI.
   *
   * @param instance a URI identifying this specific occurrence of the problem
   * @return a new instance
   */
  public ProblemDetails instance(String instance) {
    Objects.requireNonNull(instance, "instance");
    return new ProblemDetails(this.status, this.type, this.title, this.detail, instance, this.extensions);
  }

  /**
   * Returns a copy with the given extension member added.
   *
   * <p>Extension members allow frameworks and applications to add problem-specific data.
   * For example, a validation failure might add an {@code errors} member listing each
   * invalid field.</p>
   *
   * @param name  the extension member name; must not conflict with standard RFC 9457 names
   * @param value the extension member value
   * @return a new instance
   */
  public ProblemDetails extension(String name, Object value) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    Map<String, Object> copy = new LinkedHashMap<>(this.extensions);
    copy.put(name, value);
    return new ProblemDetails(this.status, this.type, this.title, this.detail, this.instance, copy);
  }

  /** @return the HTTP status code */
  public ContentStatus status() { return this.status; }

  /** @return the problem type URI, or {@code null} if not set */
  public @Nullable String type() { return this.type; }

  /** @return the short human-readable summary, or {@code null} if not set */
  public @Nullable String title() { return this.title; }

  /** @return the human-readable explanation for this occurrence, or {@code null} if not set */
  public @Nullable String detail() { return this.detail; }

  /** @return the URI identifying this specific occurrence, or {@code null} if not set */
  public @Nullable String instance() { return this.instance; }

  /** @return an unmodifiable map of extension members; empty if none were set */
  public Map<String, Object> extensions() { return this.extensions; }

}
