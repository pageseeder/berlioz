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
package org.pageseeder.berlioz.servlet;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.config.ConfigException;
import org.slf4j.Logger;

/**
 * The rules of one {@link RedirectFilter} or {@link RelocationFilter} instance.
 *
 * <p>The rules are loaded on first use, and loaded again on the next use after a reload of the
 * {@code redirects} scope ({@link MappingFilters#generation()}), whatever the request that asked
 * for it. Each load is recorded for diagnostics.
 *
 * @param <C> the type of configuration
 */
final class MappingRules<C> {

  /** Loads the configuration from a file. */
  @FunctionalInterface
  interface Loader<C> {
    C load(File file) throws ConfigException;
  }

  /** The outcome of a load. */
  private static final class Loaded<C> {
    private final C config;
    private final long generation;
    private final Instant time;
    private final @Nullable ConfigException failure;

    Loaded(C config, long generation, Instant time, @Nullable ConfigException failure) {
      this.config = config;
      this.generation = generation;
      this.time = time;
      this.failure = failure;
    }
  }

  /** The name of the diagnostic status field. */
  private static final String STATUS = "status";

  private final String type;
  private final String filterName;
  private final @Nullable String parameter;
  private final @Nullable File file;
  private final Logger logger;
  private final Loader<C> loader;
  private final C empty;
  private final Function<C, List<Map<String, Object>>> rules;
  private final Function<C, List<Map<String, Object>>> ignored;

  private final AtomicReference<@Nullable Loaded<C>> loaded = new AtomicReference<>();

  /**
   * @param type       the type of filter, {@code redirect} or {@code relocation}
   * @param filterName the name of the filter instance
   * @param parameter  the {@code config} init parameter, if any
   * @param file       the configuration file, if any
   * @param logger     the logger of the filter
   * @param loader     loads the configuration
   * @param empty      the configuration without rules, used when there is no file or it fails to load
   * @param rules      the diagnostic fields of each rule of a configuration
   * @param ignored    the diagnostic fields of each rule ignored when loading a configuration
   */
  @SuppressWarnings("java:S107") // Internal constructor; the filters pass the specifics of their type
  MappingRules(String type, String filterName, @Nullable String parameter, @Nullable File file, Logger logger,
      Loader<C> loader, C empty, Function<C, List<Map<String, Object>>> rules,
      Function<C, List<Map<String, Object>>> ignored) {
    this.type = type;
    this.filterName = filterName;
    this.parameter = parameter;
    this.file = file;
    this.logger = logger;
    this.loader = loader;
    this.empty = empty;
    this.rules = rules;
    this.ignored = ignored;
  }

  /** @return the type of filter, {@code redirect} or {@code relocation} */
  String type() {
    return this.type;
  }

  /** @return the name of the filter instance */
  String filterName() {
    return this.filterName;
  }

  /** @return {@code true} if the filter has a configuration file */
  boolean isConfigured() {
    return this.file != null;
  }

  /**
   * Returns the rules, loading them if they were not loaded yet, or if a reload was requested
   * since they were loaded.
   *
   * @return the configuration in use
   */
  C get() {
    File f = this.file;
    if (f == null) return this.empty;
    long generation = MappingFilters.generation();
    Loaded<C> current = this.loaded.get();
    if (current != null && current.generation == generation) return current.config;
    Loaded<C> next = load(f, generation);
    this.loaded.set(next);
    return next.config;
  }

  private Loaded<C> load(File f, long generation) {
    try {
      C config = this.loader.load(f);
      int count = this.rules.apply(config).size();
      if (count == 0) {
        this.logger.warn("Filter '{}': {} configuration {} has no rules - filter will have no effect",
            this.filterName, this.type, f.getAbsolutePath());
      } else {
        this.logger.info("Filter '{}': loaded {} configuration: {} rules found", this.filterName, this.type, count);
      }
      return new Loaded<>(config, generation, Instant.now(), null);
    } catch (ConfigException ex) {
      this.logger.error("Filter '{}': unable to load {} configuration: {}", this.filterName, this.type, ex.getMessage());
      return new Loaded<>(this.empty, generation, Instant.now(), ex);
    }
  }

  /**
   * Returns the diagnostic fields of this filter.
   *
   * @param details whether the message of a load failure may be included
   * @return the fields, in output order
   */
  Map<String, Object> diagnosticEntry(boolean details) {
    Map<String, Object> entry = DiagnosticConfiguration.fields("name", this.filterName, "type", this.type);
    File f = this.file;
    String param = this.parameter;
    if (f == null || param == null) {
      entry.put(STATUS, "no-config");
      return entry;
    }
    String path = DiagnosticConfiguration.safePath(param.replaceFirst("^/+", ""));
    if (path != null) entry.put("config", path);
    entry.put("exists", f.isFile());
    Loaded<C> current = this.loaded.get();
    if (current == null) {
      entry.put(STATUS, "not-loaded");
      return entry;
    }
    entry.put(STATUS, current.failure == null ? "loaded" : "failed");
    entry.put("loaded", current.time.toString());
    if (current.generation != MappingFilters.generation()) entry.put("reload-pending", true);
    if (current.failure != null) {
      entry.put("failure", failureMessage(current.failure, details));
      return entry;
    }
    List<Map<String, Object>> all = this.rules.apply(current.config);
    List<Map<String, Object>> skipped = this.ignored.apply(current.config);
    entry.put("rule-count", (long) all.size());
    if (!skipped.isEmpty()) entry.put("ignored-count", (long) skipped.size());
    if (all.size() > MappingFilters.RULE_LIMIT || skipped.size() > MappingFilters.RULE_LIMIT) {
      entry.put("limit", (long) MappingFilters.RULE_LIMIT);
    }
    if (!all.isEmpty()) entry.put("rules", bounded(all));
    if (!skipped.isEmpty()) entry.put("ignored-rules", bounded(skipped));
    return entry;
  }

  private static List<Map<String, Object>> bounded(List<Map<String, Object>> list) {
    return list.size() > MappingFilters.RULE_LIMIT ? new ArrayList<>(list.subList(0, MappingFilters.RULE_LIMIT)) : list;
  }

  private static String failureMessage(ConfigException ex, boolean details) {
    String message = ex.getMessage();
    if (!details || message == null || message.isBlank()) return "Unable to load the configuration file";
    Throwable cause = ex.getCause();
    String reason = cause != null ? cause.getMessage() : null;
    if (reason != null && !reason.isBlank() && !message.contains(reason)) message = message + ": " + reason;
    String safe = message.replaceAll("\\p{Cntrl}+", " ").trim();
    return safe.length() > MappingFilters.MESSAGE_LIMIT ? safe.substring(0, MappingFilters.MESSAGE_LIMIT) + "…" : safe;
  }

}
