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
 * @author Christophe Lauret
 * @since 0.14.6
 */
final class LoggingSetup {

  /** The framework name when Berlioz did not configure logging. */
  static final String NONE = "none";

  /** The maximum number of problems listed for each candidate. */
  static final int PROBLEM_LIMIT = 20;

  /** The maximum length of a message. */
  static final int MESSAGE_LIMIT = 512;

  private static volatile @Nullable LoggingSetup current;

  private final String framework;
  private final String mode;
  private final String loggerFactory;
  private final Instant time;
  private final boolean consoleDefault;
  private final List<Candidate> candidates;

  private LoggingSetup(Recorder recorder) {
    this.framework = recorder.framework;
    this.mode = recorder.mode;
    this.loggerFactory = recorder.loggerFactory;
    this.time = Instant.now();
    this.consoleDefault = recorder.consoleDefault;
    this.candidates = Collections.unmodifiableList(new ArrayList<>(recorder.candidates));
  }

  /** @return the logging setup recorded at startup, if Berlioz was initialized */
  static @Nullable LoggingSetup current() {
    return current;
  }

  /** Removes the recorded logging setup (for tests). */
  static void clear() {
    current = null;
  }

  /** @return the safe snapshot of the logging setup recorded at startup */
  static ConfigurationSnapshot diagnosticSnapshot() {
    LoggingSetup setup = current;
    if (setup == null) {
      return new ConfigurationSnapshot("logging-config", DiagnosticConfiguration.fields("initialised", false));
    }
    boolean details = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;
    return setup.toSnapshot(details);
  }

  ConfigurationSnapshot toSnapshot(boolean details) {
    Map<String, Object> fields = DiagnosticConfiguration.fields("initialised", true,
        "framework", this.framework, "mode", this.mode, "logger-factory", this.loggerFactory,
        "time", this.time.toString());
    if (this.consoleDefault) fields.put("console-default", true);
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
      if (details && !this.problems.isEmpty()) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String problem : this.problems.subList(0, Math.min(this.problems.size(), PROBLEM_LIMIT))) {
          int space = problem.indexOf(' ');
          String level = space > 0 ? problem.substring(0, space) : "";
          boolean known = "ERROR".equals(level) || "WARN".equals(level);
          list.add(DiagnosticConfiguration.fields("level", known ? level.toLowerCase() : "warn",
              "message", known ? problem.substring(space + 1) : problem));
        }
        fields.put("problems", list);
      }
      return fields;
    }
  }

  /**
   * Records the configuration files as Berlioz considers them, then publishes the setup.
   */
  static final class Recorder {

    private final String mode;
    private final String loggerFactory;
    private final File webInf;
    private final @Nullable File appData;
    private final List<String[]> folders = new ArrayList<>();
    private final List<Candidate> candidates = new ArrayList<>();
    private String framework = NONE;
    private boolean consoleDefault;

    /**
     * @param env           the initial environment
     * @param loggerFactory the class name of the SLF4J logger factory
     */
    Recorder(InitEnvironment env, String loggerFactory) {
      this.mode = env.mode();
      this.loggerFactory = loggerFactory;
      this.webInf = env.webInf();
      this.appData = env.hasAppData() ? env.appData() : null;
      if (this.appData != null) addFolder(this.appData, "[app-data]");
      addFolder(this.webInf, "[WEB-INF]");
      // Replace the longest paths first, so that a folder within another is replaced whole
      this.folders.sort(Comparator.comparingInt((String[] folder) -> folder[0].length()).reversed());
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

    /** Publishes the setup recorded so far. */
    void publish() {
      current = new LoggingSetup(this);
    }

    /** @return the folder and the path of the file relative to it, or only its name outside both folders */
    private Location where(File file) {
      File parent = this.appData;
      if (parent != null) {
        String relative = relative(parent, file);
        if (relative != null) return new Location("app-data", relative);
      }
      String relative = relative(this.webInf, file);
      if (relative != null) return new Location("web-inf", relative);
      return new Location(null, file.getName());
    }

    private static @Nullable String relative(File folder, File file) {
      Path base = folder.toPath().toAbsolutePath().normalize();
      Path path = file.toPath().toAbsolutePath().normalize();
      if (!path.startsWith(base) || path.equals(base)) return null;
      return base.relativize(path).toString().replace('\\', '/');
    }

    private void addFolder(File folder, String label) {
      this.folders.add(new String[] {folder.getAbsolutePath(), label});
      try {
        String canonical = folder.getCanonicalPath();
        if (!canonical.equals(folder.getAbsolutePath())) this.folders.add(new String[] {canonical, label});
      } catch (IOException ex) {
        // The absolute path is removed anyway
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
      for (String[] folder : this.folders) safe = safe.replace(folder[0], folder[1]);
      safe = Redaction.redactValue(safe.replaceAll("\\p{Cntrl}+", " ").trim());
      return safe.length() > MESSAGE_LIMIT ? safe.substring(0, MESSAGE_LIMIT) + "…" : safe;
    }
  }
}
