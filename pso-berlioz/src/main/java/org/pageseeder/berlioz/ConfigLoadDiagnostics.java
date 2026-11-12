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
package org.pageseeder.berlioz;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * An immutable snapshot of the outcome of the last attempts to load the global configuration,
 * returned by {@link GlobalSettings#getLoadDiagnostics()}.
 *
 * <p>When a reload fails, Berlioz keeps the last valid configuration; this snapshot tells
 * that it happened, when, and why. The failure is kept until the next successful load.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.14.6
 */
public final class ConfigLoadDiagnostics {

  /** The configuration has not been loaded yet. */
  public static final String PENDING = "pending";

  /** The last attempt to load the configuration succeeded. */
  public static final String READY = "ready";

  /** No attempt has succeeded: Berlioz uses the default value of every option. */
  public static final String STARTUP_FAILED = "startup-failed";

  /** The last attempt failed: Berlioz keeps the configuration of the last successful load. */
  public static final String RELOAD_FAILED = "reload-failed";

  static final ConfigLoadDiagnostics INITIAL = new ConfigLoadDiagnostics(PENDING, null, null, null);

  private final String status;

  private final @Nullable Instant loaded;

  private final @Nullable Instant failed;

  private final @Nullable Failure failure;

  private ConfigLoadDiagnostics(String status, @Nullable Instant loaded, @Nullable Instant failed,
      @Nullable Failure failure) {
    this.status = status;
    this.loaded = loaded;
    this.failed = failed;
    this.failure = failure;
  }

  /**
   * @param time when the configuration was loaded.
   * @return the diagnostics of a successful load.
   */
  static ConfigLoadDiagnostics ready(Instant time) {
    return new ConfigLoadDiagnostics(READY, time, null, null);
  }

  /**
   * @param time    when the attempt failed.
   * @param failure the cause of the failure.
   * @return the diagnostics after this failed attempt, keeping the time of the last successful load.
   */
  ConfigLoadDiagnostics failed(Instant time, Failure failure) {
    String next = this.loaded != null ? RELOAD_FAILED : STARTUP_FAILED;
    return new ConfigLoadDiagnostics(next, this.loaded, time, failure);
  }

  /**
   * @return {@value #PENDING}, {@value #READY}, {@value #STARTUP_FAILED} or {@value #RELOAD_FAILED}.
   */
  public String status() {
    return this.status;
  }

  /**
   * @return when the configuration was last loaded successfully, or {@code null} if never.
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
   * @return the cause of the last failed attempt, or {@code null} if the last attempt succeeded.
   */
  public @Nullable Failure failure() {
    return this.failure;
  }

  /**
   * The cause of a failed load attempt.
   *
   * <p>The message is always safe to expose. The detail, when known, is the description of the
   * error by the parser or the file system, without absolute paths or credentials; it may quote
   * the content of the file.
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
     * @return the name of the configuration file that failed, such as {@code config-dev.xml},
     *         or {@code null} if the failure is not attributable to a file.
     */
    public @Nullable String source() {
      return this.source;
    }

    /**
     * @return the line of the error in the file, or {@code -1} if unknown.
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
     * @return the description of the error, or {@code null} if unknown.
     */
    public @Nullable String detail() {
      return this.detail;
    }
  }

}
