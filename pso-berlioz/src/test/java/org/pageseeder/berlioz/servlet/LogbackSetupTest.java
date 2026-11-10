package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.servlet.LogbackSetup.Outcome;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.read.ListAppender;

/**
 * Runs with the oldest supported logback in {@code test} and with the latest in {@code testLogbackLatest}.
 */
final class LogbackSetupTest {

  private static final String VALID = "<configuration>"
      + "<appender name='LIST' class='ch.qos.logback.core.read.ListAppender'/>"
      + "<root level='WARN'><appender-ref ref='LIST'/></root>"
      + "</configuration>";

  private static final String MALFORMED = "<configuration><root level='WARN'>";

  private static final String UNKNOWN_APPENDER = "<configuration>"
      + "<appender name='X' class='com.example.NoSuchAppender'/>"
      + "<root level='WARN'><appender-ref ref='X'/></root>"
      + "</configuration>";

  @TempDir
  Path config;

  private final LoggerContext context = new LoggerContext();

  private final List<String> console = new ArrayList<>();

  @BeforeEach
  void setUp() {
    this.context.setName("test");
  }

  @Test
  void testRunsWithExpectedLogbackVersion() {
    String expected = System.getProperty("berlioz.test.logback.version");
    assertEquals(expected, Context.class.getPackage().getImplementationVersion());
  }

  @Test
  void testNoFile_leavesContextUntouched() {
    Logger root = root();
    ListAppender<ILoggingEvent> existing = new ListAppender<>();
    existing.setName("existing");
    root.addAppender(existing);

    LogbackSetup.Result result = configure(file("logback-dev.xml"), file("logback.xml"));

    assertFalse(result.isConfigured());
    assertNull(result.loaded());
    assertEquals(Arrays.asList(Outcome.ABSENT, Outcome.ABSENT), outcomes(result));
    assertTrue(root.isAttached(existing));
  }

  @Test
  void testValidFile_isLoaded() throws IOException {
    File valid = write("logback.xml", VALID);

    LogbackSetup.Result result = configure(file("logback-dev.xml"), valid);

    assertEquals(valid, result.loaded());
    assertFalse(result.consoleDefault());
    assertEquals(Arrays.asList(Outcome.ABSENT, Outcome.LOADED), outcomes(result));
    assertTrue(result.attempts().get(1).problems().isEmpty());
    assertEquals(Level.WARN, root().getLevel());
    assertTrue(this.console.contains("Logging: logback config file OK"));
  }

  @Test
  void testMalformedFile_fallsBackToNextFileAndSaysSo() throws IOException {
    File malformed = write("logback-dev.xml", MALFORMED);
    File valid = write("logback.xml", VALID);

    LogbackSetup.Result result = configure(malformed, valid);

    assertEquals(valid, result.loaded());
    assertEquals(Arrays.asList(Outcome.FAILED, Outcome.LOADED), outcomes(result));
    assertFalse(result.attempts().get(0).problems().isEmpty());
    // Errors of the failed file are not reported again for the next one
    assertTrue(result.attempts().get(1).problems().isEmpty());
    assertTrue(this.console.contains("(!) Logging: using config/logback.xml because config/logback-dev.xml could not be loaded"));
    // The fallback is reported in the log itself
    List<ILoggingEvent> events = listAppender().list;
    assertEquals(1, events.size());
    assertEquals(Level.WARN, events.get(0).getLevel());
    assertTrue(events.get(0).getFormattedMessage().contains("config/logback-dev.xml could not be loaded"));
  }

  @Test
  void testOnlyMalformedFile_logsToConsole() throws IOException {
    File malformed = write("logback.xml", MALFORMED);

    LogbackSetup.Result result = configure(file("logback-dev.xml"), malformed);

    assertTrue(result.isConfigured());
    assertTrue(result.consoleDefault());
    assertNull(result.loaded());
    assertEquals(Arrays.asList(Outcome.ABSENT, Outcome.FAILED), outcomes(result));
    Logger root = root();
    assertEquals(Level.INFO, root.getLevel());
    Appender<ILoggingEvent> appender = root.getAppender("console");
    assertTrue(appender instanceof ConsoleAppender);
    assertTrue(appender.isStarted());
    assertTrue(this.console.stream().anyMatch(line -> line.startsWith("(!) Logging: config/logback.xml could not be loaded")));
  }

  @Test
  void testUnknownAppender_isLoadedWithErrorsAndKept() throws IOException {
    File broken = write("logback-dev.xml", UNKNOWN_APPENDER);
    File valid = write("logback.xml", VALID);

    LogbackSetup.Result result = configure(broken, valid);

    assertEquals(broken, result.loaded());
    assertFalse(result.consoleDefault());
    assertEquals(Arrays.asList(Outcome.LOADED_WITH_ERRORS), outcomes(result));
    List<String> problems = result.attempts().get(0).problems();
    assertTrue(problems.stream().anyMatch(p -> p.startsWith("ERROR ") && p.contains("com.example.NoSuchAppender")), problems.toString());
    assertFalse(this.console.contains("Logging: logback config file OK"));
    assertTrue(this.console.contains("(!) Logging: config/logback-dev.xml loaded with errors, the logging configuration may be incomplete or not applied"));
  }

  private LogbackSetup.Result configure(File... candidates) {
    return LogbackSetup.configure(this.context, Arrays.asList(candidates), this.console::add);
  }

  private Logger root() {
    return this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
  }

  @SuppressWarnings("unchecked")
  private ListAppender<ILoggingEvent> listAppender() {
    return (ListAppender<ILoggingEvent>) root().getAppender("LIST");
  }

  private File file(String name) {
    return this.config.resolve(name).toFile();
  }

  private File write(String name, String content) throws IOException {
    return Files.write(this.config.resolve(name), content.getBytes(StandardCharsets.UTF_8)).toFile();
  }

  private static List<Outcome> outcomes(LogbackSetup.Result result) {
    return result.attempts().stream().map(LogbackSetup.Attempt::outcome).collect(Collectors.toList());
  }

}
