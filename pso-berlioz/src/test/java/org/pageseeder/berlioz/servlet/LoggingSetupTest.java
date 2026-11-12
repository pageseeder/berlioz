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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.PropertyConfigurator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

final class LoggingSetupTest {

  @TempDir
  Path root;

  private static final String VALID_LOGBACK = "<configuration>"
      + "<appender name='LIST' class='ch.qos.logback.core.read.ListAppender'/>"
      + "<root level='WARN'><appender-ref ref='LIST'/></root>"
      + "</configuration>";

  private static final String LOGBACK_PROPERTY = "logback.configurationFile";

  private String console = "";

  @AfterEach
  void tearDown() {
    LoggingSetup.clear();
    PropertyConfigurator.CONFIGURED.clear();
  }

  @Test
  void testNotInitialised() {
    LoggingSetup.clear();
    ConfigurationSnapshot snapshot = LoggingSetup.diagnosticSnapshot();
    assertEquals("<logging-config initialised=\"false\"/>", xml(snapshot));
    assertEquals("{\"initialised\":false}", json(snapshot));
  }

  @Test
  void testCandidatesInOrder_withFileInUse() {
    File webInf = this.root.resolve("WEB-INF").toFile();
    LoggingSetup.Recorder recorder = new LoggingSetup.Recorder(env(webInf, null), "ch.qos.logback.classic.LoggerContext");
    File mode = new File(webInf, "config/logback-dev.xml");
    File base = new File(webInf, "config/logback.xml");
    recorder.candidate("logback", mode, "failed", List.of("ERROR XML parsing error"), "JoranException: bad");
    recorder.candidate("logback", base, "loaded-with-errors", List.of("WARN first", "ERROR second"), null);
    recorder.configured("logback", base);
    recorder.publish();

    String xml = xml(LoggingSetup.current().toSnapshot(true));
    assertTrue(xml.startsWith("<logging-config initialised=\"true\" framework=\"logback\" backend=\"logback\""
        + " configuration=\"file\" mode=\"dev\""
        + " logger-factory=\"ch.qos.logback.classic.LoggerContext\" time=\""), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"webinf\" path=\"config/logback-dev.xml\""
        + " outcome=\"failed\" problem-count=\"1\" failure=\"JoranException: bad\">"
        + "<problem level=\"error\" message=\"XML parsing error\"/></candidate>"), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"webinf\" path=\"config/logback.xml\""
        + " outcome=\"loaded-with-errors\" in-use=\"true\" problem-count=\"2\">"
        + "<problem level=\"warn\" message=\"first\"/><problem level=\"error\" message=\"second\"/></candidate>"), xml);

    String json = json(LoggingSetup.current().toSnapshot(true));
    assertTrue(json.contains("\"loggerFactory\":\"ch.qos.logback.classic.LoggerContext\""), json);
    assertTrue(json.contains("{\"framework\":\"logback\",\"location\":\"webinf\",\"path\":\"config/logback.xml\","
        + "\"outcome\":\"loaded-with-errors\",\"inUse\":true,\"problemCount\":2,"
        + "\"problems\":[{\"level\":\"warn\",\"message\":\"first\"},{\"level\":\"error\",\"message\":\"second\"}]}"), json);
  }

  @Test
  void testMinimalDetail_omitsProblemsAndFailure() {
    File webInf = this.root.resolve("WEB-INF").toFile();
    LoggingSetup.Recorder recorder = new LoggingSetup.Recorder(env(webInf, null), "x");
    recorder.candidate("logback", new File(webInf, "config/logback.xml"), "failed", List.of("ERROR secret"), "boom");
    recorder.consoleDefault();
    recorder.publish();

    String xml = xml(LoggingSetup.current().toSnapshot(false));
    assertTrue(xml.contains("framework=\"logback\" backend=\"unknown\" configuration=\"console-default\""), xml);
    assertTrue(xml.contains("console-default=\"true\""), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"webinf\" path=\"config/logback.xml\""
        + " outcome=\"failed\" problem-count=\"1\"/>"), xml);
    assertFalse(xml.contains("secret"), xml);
    assertFalse(xml.contains("boom"), xml);
    assertFalse(xml.contains("in-use"), xml);
  }

  @Test
  void testAppDataFolder_andSafeMessages() throws Exception {
    File webInf = Files.createDirectories(this.root.resolve("WEB-INF")).toFile();
    File appData = Files.createDirectories(this.root.resolve("data")).toFile();
    LoggingSetup.Recorder recorder = new LoggingSetup.Recorder(env(webInf, appData), "x");
    File file = new File(appData, "config/logback-dev.xml");
    String absolute = file.getAbsolutePath();
    List<String> problems = new ArrayList<>();
    for (int i = 0; i < LoggingSetup.PROBLEM_LIMIT + 5; i++) problems.add("WARN problem " + i);
    problems.set(0, "ERROR Could not open [" + absolute + "]\nwith smtp://alert:s3cret@mail.example.org");
    recorder.candidate("logback", file, "loaded-with-errors", problems, null);
    recorder.candidate("logback", new File("/elsewhere/logback.xml"), "absent", List.of(), null);
    recorder.publish();

    String xml = xml(LoggingSetup.current().toSnapshot(true));
    assertTrue(xml.contains("location=\"appdata\" path=\"config/logback-dev.xml\""), xml);
    assertTrue(xml.contains("message=\"Could not open [[appdata]/config/logback-dev.xml] with smtp://alert:[REDACTED]@mail.example.org\""), xml);
    assertFalse(xml.contains(this.root.toString()), xml);
    assertFalse(xml.contains("s3cret"), xml);
    assertTrue(xml.contains("problem-count=\"25\""), xml);
    assertEquals(LoggingSetup.PROBLEM_LIMIT, xml.split("<problem ", -1).length - 1, xml);
    // A file outside both folders is reported by name only
    assertTrue(xml.contains("<candidate framework=\"logback\" path=\"logback.xml\" outcome=\"absent\"/>"), xml);
  }

  @Test
  void testConfigureLogger_withoutLogbackBinding() throws Exception {
    Path webInf = Files.createDirectories(this.root.resolve("WEB-INF/config")).getParent();
    Files.writeString(webInf.resolve("config/logback.xml"), "<configuration/>");
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, LoggerFactory.getILoggerFactory(), LoggingOption.AUTO, Map.of());

    // The tests are bound to slf4j-simple, so logback files are ignored
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"none\" backend=\"simple\" configuration=\"none\" mode=\"dev\""), xml);
    assertFalse(xml.contains("native-setting"), xml);
    assertTrue(this.console.contains("(!) Logging: found config/logback.xml but SLF4J does not use logback"), this.console);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"webinf\" path=\"config/logback-dev.xml\" outcome=\"absent\"/>"
        + "<candidate framework=\"logback\" location=\"webinf\" path=\"config/logback.xml\" outcome=\"ignored\"/>"
        + "<candidate framework=\"log4j\" location=\"webinf\" path=\"config/log4j-dev.properties\" outcome=\"absent\"/>"
        + "<candidate framework=\"log4j\" location=\"webinf\" path=\"config/log4j.properties\" outcome=\"absent\"/>"), xml);
  }

  @Test
  void testConfigurationValues() {
    File webInf = this.root.resolve("WEB-INF").toFile();
    File file = new File(webInf, "config/logback.xml");

    LoggingSetup.Recorder none = new LoggingSetup.Recorder(env(webInf, null), "org.slf4j.simple.SimpleLoggerFactory");
    none.publish();
    assertTrue(xml(LoggingSetup.current().toSnapshot(true)).contains("framework=\"none\" backend=\"simple\" configuration=\"none\""));

    LoggingSetup.Recorder loaded = new LoggingSetup.Recorder(env(webInf, null), LogbackSetup.LOGGER_CONTEXT_CLASS);
    loaded.candidate("logback", file, "loaded", List.of(), null);
    loaded.configured("logback", file);
    loaded.publish();
    assertTrue(xml(LoggingSetup.current().toSnapshot(true)).contains("framework=\"logback\" backend=\"logback\" configuration=\"file\""));

    LoggingSetup.Recorder legacy = new LoggingSetup.Recorder(env(webInf, null), "org.slf4j.jul.JDK14LoggerFactory");
    legacy.configured("log4j", new File(webInf, "config/log4j.properties"));
    legacy.publish();
    String xml = xml(LoggingSetup.current().toSnapshot(true));
    assertTrue(xml.contains("framework=\"log4j\" backend=\"jul\" configuration=\"file\""), xml);
    // A legacy file loaded for another backend implies nothing about the native setting
    assertFalse(xml.contains("native-setting"), xml);

    LoggingSetup.Recorder nativeConfig = new LoggingSetup.Recorder(env(webInf, null), "org.apache.logging.slf4j.Log4jLoggerFactory");
    nativeConfig.nativeConfiguration();
    nativeConfig.publish();
    assertTrue(xml(LoggingSetup.current().toSnapshot(true)).contains("framework=\"none\" backend=\"log4j2\" configuration=\"native\""));
  }

  @Test
  void testNativeSetting_sameAtEveryDetailLevel_inXmlAndJson() throws IOException {
    File webInf = Files.createDirectories(this.root.resolve("WEB-INF")).toFile();
    LoggingSetup.Recorder recorder = new LoggingSetup.Recorder(env(webInf, null), LogbackSetup.LOGGER_CONTEXT_CLASS);
    recorder.nativeSetting(LOGBACK_PROPERTY, new File(webInf, "config/logback-production.xml").getAbsolutePath());
    recorder.publish();

    String expected = "<native-setting name=\"logback.configurationFile\" value=\"[webinf]/config/logback-production.xml\"/>";
    String full = xml(LoggingSetup.current().toSnapshot(true));
    String minimal = xml(LoggingSetup.current().toSnapshot(false));
    assertTrue(full.contains(expected), full);
    assertTrue(minimal.contains(expected), minimal);
    String json = json(LoggingSetup.current().toSnapshot(false));
    assertTrue(json.contains("\"nativeSetting\":{\"name\":\"logback.configurationFile\","
        + "\"value\":\"[webinf]/config/logback-production.xml\"}"), json);
  }

  @Test
  void testNativeSetting_folderReplacement() throws IOException {
    // The application data folder is nested in WEB-INF: the longest folder is replaced whole
    File webInf = Files.createDirectories(this.root.resolve("WEB-INF")).toFile();
    File appData = Files.createDirectories(this.root.resolve("WEB-INF/data")).toFile();
    String webInfPath = webInf.getAbsolutePath();

    assertEquals("[appdata]/config/logback-production.xml",
        hint(webInf, appData, new File(appData, "config/logback-production.xml").getAbsolutePath()));
    assertEquals("[webinf]/config/logback.xml", hint(webInf, appData, webInfPath + "/config/logback.xml"));
    assertEquals("[webinf]", hint(webInf, appData, webInfPath));
    assertEquals("file:[appdata]/logback.xml", hint(webInf, appData, "file:" + appData.getAbsolutePath() + "/logback.xml"));
    // Canonical forms (the temporary folder is a symbolic link on some systems)
    assertEquals("[appdata]/logback.xml", hint(webInf, appData, appData.getCanonicalPath() + "/logback.xml"));
    assertEquals("[webinf]/logback.xml", hint(webInf, appData, webInf.getCanonicalPath() + "/logback.xml"));
    // Sibling folders sharing a prefix, and folders within another folder, are not replaced
    assertEquals(webInfPath + "-backup/logback.xml", hint(webInf, appData, webInfPath + "-backup/logback.xml"));
    // A sibling of the nested folder is within WEB-INF only
    assertEquals("[webinf]/data2/logback.xml", hint(webInf, appData, appData.getAbsolutePath() + "2/logback.xml"));
    assertEquals("/copy" + webInfPath + "/logback.xml", hint(webInf, appData, "/copy" + webInfPath + "/logback.xml"));
    // Paths outside both folders remain visible
    assertEquals("/etc/app/logback.xml", hint(webInf, appData, "/etc/app/logback.xml"));
    assertEquals("logback-test.xml", hint(webInf, appData, "logback-test.xml"));
  }

  @Test
  void testNativeSetting_isSafe() throws IOException {
    File webInf = Files.createDirectories(this.root.resolve("WEB-INF")).toFile();
    assertEquals("https://ops:[REDACTED]@config.example.org/logback.xml",
        hint(webInf, null, "https://ops:s3cret@config.example.org/logback.xml"));
    assertEquals("/etc/log back.xml", hint(webInf, null, "\t/etc/log\r\n\u001bback.xml\n"));
    String bounded = hint(webInf, null, "/x".repeat(LoggingSetup.MESSAGE_LIMIT));
    assertEquals(LoggingSetup.MESSAGE_LIMIT + 1, bounded.length());
    assertTrue(bounded.endsWith("…"), bounded);
  }

  @Test
  void testConfigureLogger_native_looksAtNoFile() throws IOException {
    Path webInf = webInfWithConfig();
    Files.writeString(webInf.resolve("config/logback.xml"), VALID_LOGBACK);
    Files.writeString(webInf.resolve("config/log4j.properties"), "");
    LoggerContext context = logbackContext();
    ListAppender<ILoggingEvent> existing = attach(context);
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, context, LoggingOption.NATIVE, Map.of(LOGBACK_PROPERTY, webInf.resolve("config/other.xml").toString()));

    // Neither reset nor loaded, and no file was checked
    Logger rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    assertTrue(rootLogger.isAttached(existing));
    assertTrue(PropertyConfigurator.CONFIGURED.isEmpty());
    assertFalse(this.console.contains("config/"), this.console);
    assertTrue(this.console.contains("Logging: berlioz.logging=native, logging is left to the native configuration of logback"), this.console);

    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"none\" backend=\"logback\" configuration=\"native\""), xml);
    assertTrue(xml.contains("<native-setting name=\"logback.configurationFile\" value=\"[webinf]/config/other.xml\"/>"), xml);
    assertFalse(xml.contains("<candidate"), xml);
  }

  @Test
  void testConfigureLogger_autoWithNativeLocation_berliozFileWins() throws IOException {
    Path webInf = webInfWithConfig();
    Files.writeString(webInf.resolve("config/logback.xml"), VALID_LOGBACK);
    LoggerContext context = logbackContext();
    ListAppender<ILoggingEvent> existing = attach(context);
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, context, LoggingOption.AUTO, Map.of(LOGBACK_PROPERTY, "/etc/secret-location/logback.xml"));

    assertFalse(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).isAttached(existing));
    assertTrue(this.console.contains("(!) Logging: 'logback.configurationFile' is set, but config/logback.xml was used;"
        + " set berlioz.logging=native to leave configuration to the backend"), this.console);
    // The value is not printed to the console
    assertFalse(this.console.contains("secret-location"), this.console);
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"logback\" backend=\"logback\" configuration=\"file\""), xml);
    assertTrue(xml.contains("value=\"/etc/secret-location/logback.xml\""), xml);
    assertTrue(xml.contains("path=\"config/logback.xml\" outcome=\"loaded\" in-use=\"true\""), xml);
  }

  @Test
  void testConfigureLogger_autoWithNativeLocation_consoleDefault() throws IOException {
    Path webInf = webInfWithConfig();
    Files.writeString(webInf.resolve("config/logback.xml"), "<configuration><root level='WARN'>");
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, logbackContext(), LoggingOption.AUTO, Map.of(LOGBACK_PROPERTY, "/etc/logback.xml"));

    assertTrue(this.console.contains("(!) Logging: 'logback.configurationFile' is set, but Berlioz applied its console default;"), this.console);
    assertFalse(this.console.contains("was used"), this.console);
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("configuration=\"console-default\""), xml);
  }

  @Test
  void testConfigureLogger_autoWithoutBerliozFile_keepsNativeConfiguration() throws IOException {
    Path webInf = webInfWithConfig();
    LoggerContext context = logbackContext();
    ListAppender<ILoggingEvent> existing = attach(context);
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, context, LoggingOption.AUTO, Map.of(LOGBACK_PROPERTY, "/etc/logback.xml"));

    assertTrue(context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).isAttached(existing));
    assertFalse(this.console.contains("is set, but"), this.console);
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"none\" backend=\"logback\" configuration=\"none\""), xml);
    assertTrue(xml.contains("<native-setting name=\"logback.configurationFile\" value=\"/etc/logback.xml\"/>"), xml);
  }

  @Test
  void testConfigureLogger_otherBackendProperty_isNotReported() throws IOException {
    Path webInf = webInfWithConfig();
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    // The test provider is slf4j-simple, which has no location property
    configureLogger(env, LoggerFactory.getILoggerFactory(), LoggingOption.AUTO, Map.of(LOGBACK_PROPERTY, "/etc/logback.xml"));

    assertFalse(xml(AppInitializer.loggingSnapshot()).contains("native-setting"));
  }

  @Test
  void testConfigureLogger_legacyLog4jWithAnotherBackend_isDeprecatedAndMismatched() throws IOException {
    Path webInf = webInfWithConfig();
    Files.writeString(webInf.resolve("config/log4j.properties"), "");
    InitEnvironment env = InitEnvironment.create(webInf.toFile()).mode("dev");

    configureLogger(env, LoggerFactory.getILoggerFactory(), LoggingOption.AUTO, Map.of());

    assertEquals(1, PropertyConfigurator.CONFIGURED.size());
    assertTrue(this.console.contains("(!) Logging: Log4j 1 configuration files are deprecated and will not be loaded in Berlioz 1.0"), this.console);
    assertTrue(this.console.contains("(!) Logging: SLF4J uses simple; this legacy configuration may not configure Berlioz logs"), this.console);
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"log4j\" backend=\"simple\" configuration=\"file\""), xml);
  }

  private static String hint(File webInf, File appData, String value) {
    LoggingSetup.Recorder recorder = new LoggingSetup.Recorder(env(webInf, appData), LogbackSetup.LOGGER_CONTEXT_CLASS);
    recorder.nativeSetting(LOGBACK_PROPERTY, value);
    recorder.publish();
    String json = json(LoggingSetup.current().toSnapshot(false));
    int start = json.indexOf("\"value\":\"") + 9;
    return json.substring(start, json.indexOf('"', start)).replace("\\/", "/");
  }

  private static InitEnvironment env(File webInf, File appData) {
    InitEnvironment env = InitEnvironment.create(webInf).mode("dev");
    return appData != null ? env.appData(appData) : env;
  }

  private void configureLogger(InitEnvironment env, ILoggerFactory factory, LoggingOption option,
      Map<String, String> properties) {
    PrintStream original = System.out;
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    try {
      AppInitializer.configureLogger(env, factory, option, properties::get);
    } finally {
      System.setOut(original);
    }
    this.console = out.toString(StandardCharsets.UTF_8);
  }

  private static LoggerContext logbackContext() {
    LoggerContext context = new LoggerContext();
    context.setName("test");
    return context;
  }

  private static ListAppender<ILoggingEvent> attach(LoggerContext context) {
    ListAppender<ILoggingEvent> existing = new ListAppender<>();
    existing.setName("existing");
    context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).addAppender(existing);
    return existing;
  }

  private Path webInfWithConfig() throws IOException {
    return Files.createDirectories(this.root.resolve("WEB-INF/config")).getParent();
  }

  private static String xml(ConfigurationSnapshot snapshot) {
    OutputWriter out = new XmlOutputAdapter();
    snapshot.writeTo(out);
    return out.toString();
  }

  private static String json(ConfigurationSnapshot snapshot) {
    OutputWriter out = new JsonOutputAdapter();
    snapshot.writeTo(out);
    return out.toString();
  }
}
