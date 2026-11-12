/*
 * Copyright 2026 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
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
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.util.Redaction;

/**
 * What Berlioz did to configure logging at startup.
 *
 * <p>This is a record of the startup only: an application may reconfigure its logging framework
 * afterwards, which is not reflected here.
 *
 * <p>Paths are recorded relative to <code>WEB-INF</code> or the application data folder, and the
 * absolute paths of these folders are removed from messages when they are recorded.
 *
 * <p>The <code>configuration</code> says how logging ended up, as far as Berlioz knows:
 * <code>file</code> when Berlioz loaded one of its files, <code>console-default</code> when its
 * files failed so that it logs to the console, <code>native</code> when
 * <code>berlioz.logging=native</code> so that it looked at no file, and <code>none</code> when it
 * looked and applied nothing. The last two say nothing about the effective configuration of the
 * backend.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
final class LoggingSetup {

  /** The framework name when Berlioz did not configure logging. */
  static final String NONE = "none";

  /** The configuration when Berlioz loaded one of its files. */
  static final String FILE = "file";

  /** The configuration when Berlioz's files failed, so that it logs to the console. */
  static final String CONSOLE_DEFAULT = "console-default";

  /** The configuration when Berlioz left logging to the native configuration of the backend. */
  static final String NATIVE = "native";

  /** The maximum number of problems listed for each candidate. */
  static final int PROBLEM_LIMIT = 20;

  /** The maximum length of a message. */
  static final int MESSAGE_LIMIT = 512;

  /** The setup recorded at startup, published once logging is configured and read by request threads. */
  private static final AtomicReference<@Nullable LoggingSetup> CURRENT = new AtomicReference<>();

  private final String framework;
  private final LoggingBackend backend;
  private final String configuration;
  private final String mode;
  private final String loggerFactory;
  private final Instant time;
  private final boolean consoleDefault;
  private final @Nullable Map<String, Object> nativeSetting;
  private final List<Candidate> candidates;

  private LoggingSetup(Recorder recorder) {
    this.framework = recorder.framework;
    this.backend = recorder.backend;
    this.configuration = recorder.configuration();
    this.mode = recorder.mode;
    this.loggerFactory = recorder.loggerFactory;
    this.time = Instant.now();
    this.consoleDefault = recorder.consoleDefault;
    this.nativeSetting = recorder.nativeSetting;
    this.candidates = Collections.unmodifiableList(new ArrayList<>(recorder.candidates));
  }

  /** @return the logging setup recorded at startup, if Berlioz was initialized */
  static @Nullable LoggingSetup current() {
    return CURRENT.get();
  }

  /** Removes the recorded logging setup (for tests). */
  static void clear() {
    CURRENT.set(null);
  }

  /** @return the safe snapshot of the logging setup recorded at startup */
  static ConfigurationSnapshot diagnosticSnapshot() {
    LoggingSetup setup = CURRENT.get();
    if (setup == null) {
      return new ConfigurationSnapshot("logging-config", DiagnosticConfiguration.fields("initialised", false));
    }
    boolean details = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;
    return setup.toSnapshot(details);
  }

  ConfigurationSnapshot toSnapshot(boolean details) {
    Map<String, Object> fields = DiagnosticConfiguration.fields("initialised", true,
        "framework", this.framework, "backend", this.backend.label(), "configuration", this.configuration,
        "mode", this.mode, "logger-factory", this.loggerFactory, "time", this.time.toString());
    if (this.consoleDefault) fields.put(CONSOLE_DEFAULT, true);
    // The hint is safe at every detail level: folders are replaced and credentials redacted when recorded
    if (this.nativeSetting != null) fields.put("native-setting", this.nativeSetting);
    List<Map<String, Object>> entries = new ArrayList<>();
    for (Candidate candidate : this.candidates) entries.add(candidate.toFields(details));
    if (!entries.isEmpty()) fields.put("candidates", entries);
    return new ConfigurationSnapshot("logging-config", fields);
  }

  /** A configuration file considered by Berlioz. */
  static final class Candidate {

    private final String framework;
    private final @Nullable String location;
    private final String path;
    private final String outcome;
    private final List<String> problems;
    private final @Nullable String failure;
    private boolean inUse;

    private Candidate(String framework, @Nullable String location, String path, String outcome,
        List<String> problems, @Nullable String failure) {
      this.framework = framework;
      this.location = location;
      this.path = path;
      this.outcome = outcome;
      this.problems = problems;
      this.failure = failure;
    }

    private Map<String, Object> toFields(boolean details) {
      Map<String, Object> fields = DiagnosticConfiguration.fields("framework", this.framework);
      if (this.location != null) fields.put("location", this.location);
      fields.put("path", this.path);
      fields.put("outcome", this.outcome);
      if (this.inUse) fields.put("in-use", true);
      if (!this.problems.isEmpty()) fields.put("problem-count", (long) this.problems.size());
      if (details && this.failure != null) fields.put("failure", this.failure);
      if (details && !this.problems.isEmpty()) fields.put("problems", problemFields());
      return fields;
    }

    /** @return the first problems, at most {@link #PROBLEM_LIMIT} */
    private List<Map<String, Object>> problemFields() {
      List<Map<String, Object>> list = new ArrayList<>();
      for (String problem : this.problems.subList(0, Math.min(this.problems.size(), PROBLEM_LIMIT))) {
        list.add(problemFields(problem));
      }
      return list;
    }

    /** @return the level and message of a problem recorded as <code>"[ERROR|WARN] message"</code> */
    private static Map<String, Object> problemFields(String problem) {
      int space = problem.indexOf(' ');
      String level = space > 0 ? problem.substring(0, space) : "";
      boolean known = "ERROR".equals(level) || "WARN".equals(level);
      return DiagnosticConfiguration.fields("level", known ? level.toLowerCase() : "warn",
          "message", known ? problem.substring(space + 1) : problem);
    }
  }

  /**
   * Records the configuration files as Berlioz considers them, then publishes the setup.
   */
  static final class Recorder {

    private final String mode;
    private final String loggerFactory;
    private final LoggingBackend backend;
    private final File webInf;
    private final @Nullable File appData;
    private final List<Folder> folders = new ArrayList<>();
    private final List<Candidate> candidates = new ArrayList<>();
    private String framework = NONE;
    private boolean consoleDefault;
    private boolean nativeConfiguration;
    private @Nullable Map<String, Object> nativeSetting;

    /**
     * @param env           the initial environment
     * @param loggerFactory the class name of the SLF4J logger factory
     */
    Recorder(InitEnvironment env, String loggerFactory) {
      this.mode = env.mode();
      this.loggerFactory = loggerFactory;
      this.backend = LoggingBackend.of(loggerFactory);
      this.webInf = env.webInf();
      this.appData = env.hasAppData() ? env.appData() : null;
      if (this.appData != null) addFolder(this.appData, "[appdata]");
      addFolder(this.webInf, "[webinf]");
      // Replace the longest paths first, so that a folder within another is replaced whole
      this.folders.sort(Comparator.comparingInt((Folder folder) -> folder.path.length()).reversed());
    }

    /** @return the logging backend that SLF4J uses */
    LoggingBackend backend() {
      return this.backend;
    }

    /**
     * Records the documented system property locating the native configuration of the backend.
     *
     * <p>This is a hint only: the value is not resolved, opened or verified.
     *
     * @param name  the name of the property
     * @param value its value, recorded with the known folders replaced, without control characters
     *              or credentials, truncated
     */
    void nativeSetting(String name, String value) {
      this.nativeSetting = DiagnosticConfiguration.fields("name", name, "value", hint(value));
    }

    /** Records that Berlioz left logging to the native configuration of the backend. */
    void nativeConfiguration() {
      this.nativeConfiguration = true;
    }

    /**
     * Records a candidate file.
     *
     * @param framework the framework of the file: <code>logback</code> or <code>log4j</code>
     * @param file      the file
     * @param outcome   <code>absent</code>, <code>ignored</code>, <code>loaded</code>,
     *                  <code>loaded-with-errors</code> or <code>failed</code>
     * @param problems  the problems reported by the framework while loading the file
     * @param failure   why the file could not be loaded, if it failed
     */
    void candidate(String framework, File file, String outcome, List<String> problems, @Nullable String failure) {
      List<String> safe = new ArrayList<>(problems.size());
      for (String problem : problems) safe.add(safe(problem));
      Location where = where(file);
      this.candidates.add(new Candidate(framework, where.folder, where.path, outcome,
          Collections.unmodifiableList(safe), failure != null ? safe(failure) : null));
    }

    /**
     * Records the file in use.
     *
     * @param framework the framework configured by Berlioz
     * @param file      the file in use, if any
     */
    void configured(String framework, @Nullable File file) {
      this.framework = framework;
      if (file == null) return;
      for (int i = this.candidates.size() - 1; i >= 0; i--) {
        Candidate candidate = this.candidates.get(i);
        if (candidate.framework.equals(framework) && !"absent".equals(candidate.outcome)
            && where(file).path.equals(candidate.path)) {
          candidate.inUse = true;
          return;
        }
      }
    }

    /** Records that no file could be loaded, so that Berlioz configured logback to log to the console. */
    void consoleDefault() {
      this.framework = "logback";
      this.consoleDefault = true;
    }

    private String configuration() {
      if (this.nativeConfiguration) return NATIVE;
      if (this.consoleDefault) return CONSOLE_DEFAULT;
      return NONE.equals(this.framework) ? NONE : FILE;
    }

    /** Publishes the setup recorded so far. */
    void publish() {
      CURRENT.set(new LoggingSetup(this));
    }

    /** @return the folder and the path of the file relative to it, or only its name outside both folders */
    private Location where(File file) {
      File parent = this.appData;
      if (parent != null) {
        String relative = relative(parent, file);
        if (relative != null) return new Location("appdata", relative);
      }
      String relative = relative(this.webInf, file);
      if (relative != null) return new Location("webinf", relative);
      return new Location(null, file.getName());
    }

    private static @Nullable String relative(File folder, File file) {
      Path base = folder.toPath().toAbsolutePath().normalize();
      Path path = file.toPath().toAbsolutePath().normalize();
      if (!path.startsWith(base) || path.equals(base)) return null;
      return base.relativize(path).toString().replace('\\', '/');
    }

    private void addFolder(File folder, String label) {
      this.folders.add(new Folder(folder.getAbsolutePath(), label));
      try {
        String canonical = folder.getCanonicalPath();
        if (!canonical.equals(folder.getAbsolutePath())) this.folders.add(new Folder(canonical, label));
      } catch (IOException ex) {
        // The absolute path is removed anyway
      }
    }

    /** A known folder and the label that replaces it. */
    private static final class Folder {
      private final String path;
      private final String label;
      private final Pattern boundary;
      private final String replacement;

      private Folder(String path, String label) {
        this.path = path;
        this.label = label;
        // The whole folder only: not within a longer name, nor a sibling folder sharing its prefix
        this.boundary = Pattern.compile("(?<![\\w.~-])" + Pattern.quote(path) + "(?=[/\\\\]|$)");
        this.replacement = Matcher.quoteReplacement(label);
      }
    }

    private static final class Location {
      private final @Nullable String folder;
      private final String path;

      private Location(@Nullable String folder, String path) {
        this.folder = folder;
        this.path = path;
      }
    }

    /** @return the message without absolute folder paths, control characters or credentials, truncated */
    private String safe(String message) {
      String safe = message;
      for (Folder folder : this.folders) safe = safe.replace(folder.path, folder.label);
      return bounded(safe);
    }

    /** @return the value with the known folders replaced at directory boundaries, then made safe */
    private String hint(String value) {
      String hint = value;
      for (Folder folder : this.folders) hint = folder.boundary.matcher(hint).replaceAll(folder.replacement);
      return bounded(hint);
    }

    /** @return the text without control characters or credentials, truncated */
    private static String bounded(String text) {
      String safe = Redaction.redactValue(text.replaceAll("\\p{Cntrl}+", " ").trim());
      return safe.length() > MESSAGE_LIMIT ? safe.substring(0, MESSAGE_LIMIT) + "…" : safe;
    }
  }
}
