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
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;

final class LoggingSetupTest {

  @TempDir
  Path root;

  @AfterEach
  void tearDown() {
    LoggingSetup.clear();
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
    assertTrue(xml.startsWith("<logging-config initialised=\"true\" framework=\"logback\" mode=\"dev\""
        + " logger-factory=\"ch.qos.logback.classic.LoggerContext\" time=\""), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"web-inf\" path=\"config/logback-dev.xml\""
        + " outcome=\"failed\" problem-count=\"1\" failure=\"JoranException: bad\">"
        + "<problem level=\"error\" message=\"XML parsing error\"/></candidate>"), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"web-inf\" path=\"config/logback.xml\""
        + " outcome=\"loaded-with-errors\" in-use=\"true\" problem-count=\"2\">"
        + "<problem level=\"warn\" message=\"first\"/><problem level=\"error\" message=\"second\"/></candidate>"), xml);

    String json = json(LoggingSetup.current().toSnapshot(true));
    assertTrue(json.contains("\"loggerFactory\":\"ch.qos.logback.classic.LoggerContext\""), json);
    assertTrue(json.contains("{\"framework\":\"logback\",\"location\":\"web-inf\",\"path\":\"config/logback.xml\","
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
    assertTrue(xml.contains("framework=\"logback\""), xml);
    assertTrue(xml.contains("console-default=\"true\""), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"web-inf\" path=\"config/logback.xml\""
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
    assertTrue(xml.contains("location=\"app-data\" path=\"config/logback-dev.xml\""), xml);
    assertTrue(xml.contains("message=\"Could not open [[app-data]/config/logback-dev.xml] with smtp://alert:[REDACTED]@mail.example.org\""), xml);
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

    invokeConfigureLogger(env);

    // The tests are bound to slf4j-simple, so logback files are ignored
    String xml = xml(AppInitializer.loggingSnapshot());
    assertTrue(xml.contains("framework=\"none\" mode=\"dev\""), xml);
    assertTrue(xml.contains("<candidate framework=\"logback\" location=\"web-inf\" path=\"config/logback-dev.xml\" outcome=\"absent\"/>"
        + "<candidate framework=\"logback\" location=\"web-inf\" path=\"config/logback.xml\" outcome=\"ignored\"/>"
        + "<candidate framework=\"log4j\" location=\"web-inf\" path=\"config/log4j-dev.properties\" outcome=\"absent\"/>"
        + "<candidate framework=\"log4j\" location=\"web-inf\" path=\"config/log4j.properties\" outcome=\"absent\"/>"), xml);
  }

  private static InitEnvironment env(File webInf, File appData) {
    InitEnvironment env = InitEnvironment.create(webInf).mode("dev");
    return appData != null ? env.appData(appData) : env;
  }

  private static void invokeConfigureLogger(InitEnvironment env) throws Exception {
    Method method = AppInitializer.class.getDeclaredMethod("configureLogger", InitEnvironment.class);
    method.setAccessible(true);
    PrintStream original = System.out;
    System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    try {
      method.invoke(null, env);
    } finally {
      System.setOut(original);
    }
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
