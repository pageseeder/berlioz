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
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.ILoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.Status;

/**
 * Configures logback from the first logging file that loads.
 *
 * <p>This is the only class of Berlioz that refers to logback, which is an optional dependency:
 * callers must check that SLF4J is bound to logback, without loading any logback class, before
 * using it (see {@link #LOGGER_CONTEXT_CLASS}).
 *
 * <p>Only logback APIs whose signatures are the same from logback 1.3 onward are used.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
final class LogbackSetup {

  /**
   * The class name of the logback logger context, to compare with the SLF4J logger factory.
   */
  static final String LOGGER_CONTEXT_CLASS = "ch.qos.logback.classic.LoggerContext";

  /**
   * The pattern of the console appender used when no configuration file loads.
   */
  static final String CONSOLE_PATTERN = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";

  /**
   * The name of the logger used to report problems with the logging configuration.
   */
  private static final String LOGGER_NAME = AppInitializer.class.getName();

  /** What happened to a candidate configuration file. */
  enum Outcome {

    /** The file does not exist. */
    ABSENT,

    /** The file was loaded without errors. */
    LOADED,

    /** The file was loaded, but logback reported errors: the configuration may be incomplete or not applied. */
    LOADED_WITH_ERRORS,

    /** The file could not be loaded. */
    FAILED
  }

  /** The outcome of a candidate configuration file. */
  static final class Attempt {

    private final File file;
    private final Outcome outcome;
    private final List<String> problems;

    Attempt(File file, Outcome outcome, List<String> problems) {
      this.file = file;
      this.outcome = outcome;
      this.problems = Collections.unmodifiableList(problems);
    }

    File file() {
      return this.file;
    }

    Outcome outcome() {
      return this.outcome;
    }

    /** @return the warnings and errors reported while loading the file */
    List<String> problems() {
      return this.problems;
    }
  }

  /** The result of the logback configuration. */
  static final class Result {

    private final List<Attempt> attempts;
    private final @Nullable File loaded;
    private final boolean consoleDefault;

    Result(List<Attempt> attempts, @Nullable File loaded, boolean consoleDefault) {
      this.attempts = Collections.unmodifiableList(attempts);
      this.loaded = loaded;
      this.consoleDefault = consoleDefault;
    }

    /** @return each candidate in the order tried */
    List<Attempt> attempts() {
      return this.attempts;
    }

    /** @return the file in use, if any */
    @Nullable File loaded() {
      return this.loaded;
    }

    /** @return whether no file could be loaded, so that logback logs to the console */
    boolean consoleDefault() {
      return this.consoleDefault;
    }

    /** @return whether logback is configured, from a file or with the console default */
    boolean isConfigured() {
      return this.loaded != null || this.consoleDefault;
    }
  }

  private LogbackSetup() {
  }

  /**
   * Configures logback from the first candidate file that loads.
   *
   * @param factory    The SLF4J logger factory, which must be a logback logger context.
   * @param candidates The configuration files in the order to try.
   * @param console    Where to report what is done.
   *
   * @return whether logback is configured, from a file or with the console default
   */
  static boolean configure(ILoggerFactory factory, List<File> candidates, Consumer<String> console) {
    return configure((LoggerContext) factory, candidates, console).isConfigured();
  }

  /**
   * Configures logback from the first candidate file that loads.
   *
   * <p>A file that exists is loaded after resetting the context. If it fails to load, the next
   * candidate is tried, and the failure is logged once a later file loads. A file that loads with
   * errors is kept, as logback does, and its errors are reported. If every file that exists fails,
   * the context, which was reset, is configured to log to the console at INFO level.
   *
   * <p>If no candidate exists, the context is left untouched.
   *
   * @param context    The logback logger context.
   * @param candidates The configuration files in the order to try.
   * @param console    Where to report what is done.
   *
   * @return what happened to each candidate
   */
  static Result configure(LoggerContext context, List<File> candidates, Consumer<String> console) {
    List<Attempt> attempts = new ArrayList<>();
    List<File> failed = new ArrayList<>();
    for (File file : candidates) {
      if (!file.exists()) {
        console.accept("Logging: config/" + file.getName() + " not found");
        attempts.add(new Attempt(file, Outcome.ABSENT, Collections.emptyList()));
        continue;
      }
      console.accept("Logging: found config/" + file.getName() + " [logback config file]");
      Attempt attempt = load(context, file, console);
      attempts.add(attempt);
      if (attempt.outcome() != Outcome.FAILED) {
        reportFallback(context, file, failed, console);
        return new Result(attempts, file, false);
      }
      failed.add(file);
    }
    if (failed.isEmpty()) return new Result(attempts, null, false);
    configureConsole(context);
    console.accept("(!) Logging: no logback configuration file could be loaded, logging to the console at INFO level");
    context.getLogger(LOGGER_NAME).warn("No logback configuration file could be loaded ({}), logging to the console at INFO level", names(failed));
    return new Result(attempts, null, true);
  }

  private static Attempt load(LoggerContext context, File file, Consumer<String> console) {
    context.reset();
    // Statuses are kept across resets: only report those added by this file
    Set<Status> previous = Collections.newSetFromMap(new IdentityHashMap<>());
    previous.addAll(context.getStatusManager().getCopyOfStatusList());
    JoranException failure = null;
    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      configurator.doConfigure(file);
    } catch (JoranException ex) {
      failure = ex;
    }
    List<String> problems = new ArrayList<>();
    int highest = Status.INFO;
    for (Status status : context.getStatusManager().getCopyOfStatusList()) {
      if (status.getLevel() >= Status.WARN && !previous.contains(status)) {
        problems.add(toString(status));
        highest = Math.max(highest, status.getLevel());
      }
    }
    Outcome outcome;
    if (failure != null) {
      outcome = Outcome.FAILED;
    } else if (highest >= Status.ERROR) {
      outcome = Outcome.LOADED_WITH_ERRORS;
    } else {
      outcome = Outcome.LOADED;
    }
    report(context, file, outcome, problems, failure, console);
    return new Attempt(file, outcome, problems);
  }

  private static void report(LoggerContext context, File file, Outcome outcome, List<String> problems,
      @Nullable JoranException failure, Consumer<String> console) {
    String name = "config/" + file.getName();
    if (outcome == Outcome.FAILED) {
      console.accept("(!) Logging: " + name + " could not be loaded: " + failure);
    } else if (outcome == Outcome.LOADED_WITH_ERRORS) {
      console.accept("(!) Logging: " + name + " loaded with errors, the logging configuration may be incomplete or not applied");
    }
    for (String problem : problems) {
      console.accept("(!) Logging: " + problem);
    }
    if (outcome == Outcome.LOADED) {
      console.accept("Logging: logback config file OK");
      console.accept("Logging: OK ---------------------------------------------------");
    } else if (outcome == Outcome.LOADED_WITH_ERRORS) {
      context.getLogger(LOGGER_NAME).warn("Logging configuration {} loaded with errors: {}", name, problems);
      console.accept("Logging: DONE (with errors) -----------------------------------");
    }
  }

  private static void reportFallback(LoggerContext context, File loaded, List<File> failed, Consumer<String> console) {
    if (failed.isEmpty()) return;
    console.accept("(!) Logging: using config/" + loaded.getName() + " because " + names(failed) + " could not be loaded");
    context.getLogger(LOGGER_NAME).warn("Logging configuration config/{} is used because {} could not be loaded", loaded.getName(), names(failed));
  }

  private static void configureConsole(LoggerContext context) {
    context.reset();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern(CONSOLE_PATTERN);
    encoder.start();
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setContext(context);
    appender.setName("console");
    appender.setEncoder(encoder);
    appender.start();
    Logger root = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    root.setLevel(Level.INFO);
    root.addAppender(appender);
  }

  private static String names(List<File> files) {
    List<String> names = new ArrayList<>(files.size());
    for (File file : files) {
      names.add("config/" + file.getName());
    }
    return String.join(", ", names);
  }

  private static String toString(Status status) {
    String level = status.getLevel() >= Status.ERROR ? "ERROR" : "WARN";
    Throwable cause = status.getThrowable();
    return level + " " + status.getMessage() + (cause != null ? " [" + cause + "]" : "");
  }

}
