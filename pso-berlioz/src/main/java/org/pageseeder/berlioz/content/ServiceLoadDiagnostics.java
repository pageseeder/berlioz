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

import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.util.CollectedError;
import org.xml.sax.SAXParseException;

/**
 * An immutable snapshot of the state of the {@link ServiceLoader}, captured atomically by
 * {@link ServiceLoader#getLoadDiagnostics()}.
 *
 * <p>The sources and warnings are those of the last successful load, so they always describe the
 * services currently published. A failed reload is reported separately by {@link #failure()} and
 * is kept until the next successful load.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public final class ServiceLoadDiagnostics {

  private final String status;

  private final @Nullable Instant loaded;

  private final @Nullable Instant failed;

  private final @Nullable Instant retryAfter;

  private final List<ServiceSource> sources;

  private final List<CollectedError<SAXParseException>> warnings;

  private final @Nullable Failure failure;

  ServiceLoadDiagnostics(String status, @Nullable Instant loaded, @Nullable Instant failed,
      @Nullable Instant retryAfter, List<ServiceSource> sources,
      List<CollectedError<SAXParseException>> warnings, @Nullable Failure failure) {
    this.status = status;
    this.loaded = loaded;
    this.failed = failed;
    this.retryAfter = retryAfter;
    this.sources = List.copyOf(sources);
    this.warnings = List.copyOf(warnings);
    this.failure = failure;
  }

  /**
   * @return pending, ready, startup-failed, or reload-failed (see {@link ServiceLoader#getLoadStatus()})
   */
  public String status() {
    return this.status;
  }

  /**
   * @return when the services were last loaded successfully, or {@code null} if never.
   */
  public @Nullable Instant loaded() {
    return this.loaded;
  }

  /**
   * @return when the last failed attempt occurred, or {@code null} if the last attempt succeeded.
   */
  public @Nullable Instant failed() {
    return this.failed;
  }

  /**
   * @return when Berlioz will next attempt to load the services after a failure, or {@code null}
   *         if the last attempt succeeded.
   */
  public @Nullable Instant retryAfter() {
    return this.retryAfter;
  }

  /**
   * @return the sources of the last successful load, in load order (classpath sources first).
   */
  public List<ServiceSource> sources() {
    return this.sources;
  }

  /**
   * @return the warnings of the last successful load (see {@link ServiceLoader#getLastLoadWarnings()}).
   */
  public List<CollectedError<SAXParseException>> warnings() {
    return this.warnings;
  }

  /**
   * @return the cause of the last failed attempt, or {@code null} if the last attempt succeeded.
   */
  public @Nullable Failure failure() {
    return this.failure;
  }

  /**
   * Returns the safe display name of the source with the specified system ID.
   *
   * <p>Parser warnings carry the URL of the source as their system ID, which must not be
   * exposed in diagnostics; this maps it back to the source's display name.
   *
   * @param systemId the system ID of a warning
   *
   * @return the display name of the matching source, or {@code null} if none matches.
   */
  public @Nullable String sourceName(@Nullable String systemId) {
    if (systemId == null) return null;
    for (ServiceSource source : this.sources) {
      if (source.url().toExternalForm().equals(systemId)) return source.origin().displayName();
    }
    return null;
  }

  /**
   * The cause of a failed load attempt.
   *
   * <p>The message is always safe to expose. The detail, when known, is the parser's description
   * of the error, which may quote the content of the source.
   */
  public static final class Failure {

    private final @Nullable String source;

    private final int line;

    private final String message;

    private final @Nullable String detail;

    Failure(@Nullable String source, int line, String message, @Nullable String detail) {
      this.source = source;
      this.line = line;
      this.message = message;
      this.detail = detail;
    }

    /**
     * @return the display name of the source that failed, or {@code null} if the failure is not
     *         attributable to a single source (e.g. discovery).
     */
    public @Nullable String source() {
      return this.source;
    }

    /**
     * @return the line of the error in the source, or {@code -1} if unknown.
     */
    public int line() {
      return this.line;
    }

    /**
     * @return a generic message describing the failure, safe to expose.
     */
    public String message() {
      return this.message;
    }

    /**
     * @return the parser's description of the error, or {@code null} if unknown.
     */
    public @Nullable String detail() {
      return this.detail;
    }
  }

}
