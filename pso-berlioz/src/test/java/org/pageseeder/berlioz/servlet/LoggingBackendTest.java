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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

final class LoggingBackendTest {

  @Test
  void testKnownFactories() {
    assertBackend("logback", "logback.configurationFile", "ch.qos.logback.classic.LoggerContext");
    assertBackend("log4j2", "log4j2.configurationFile", "org.apache.logging.slf4j.Log4jLoggerFactory");
    assertBackend("jul", "java.util.logging.config.file", "org.slf4j.jul.JDK14LoggerFactory");
    assertBackend("simple", null, "org.slf4j.simple.SimpleLoggerFactory");
    assertBackend("nop", null, "org.slf4j.helpers.NOPLoggerFactory");
  }

  @Test
  void testUnknownFactories() {
    assertBackend("unknown", null, "org.slf4j.reload4j.Reload4jLoggerFactory");
    assertBackend("unknown", null, "org.slf4j.helpers.SubstituteLoggerFactory");
    assertBackend("unknown", null, "com.example.CustomLoggerFactory");
    assertBackend("unknown", null, "");
  }

  @Test
  void testTestProvider_isSimple() {
    assertEquals(LoggingBackend.SIMPLE, LoggingBackend.of(LoggerFactory.getILoggerFactory().getClass().getName()));
  }

  @Test
  void testOption() {
    List<String> console = new ArrayList<>();
    assertEquals(LoggingOption.AUTO, LoggingOption.parse(null, console::add));
    assertEquals(LoggingOption.AUTO, LoggingOption.parse("auto", console::add));
    assertEquals(LoggingOption.NATIVE, LoggingOption.parse("native", console::add));
    assertEquals(LoggingOption.NATIVE, LoggingOption.parse(" native\n", console::add));
    assertEquals(List.of(), console);
  }

  @Test
  void testOption_invalidValueFallsBackToAuto_withoutEchoingIt() {
    for (String value : new String[] {"", "  ", "NATIVE", "external", "native\u001b[31m"}) {
      List<String> console = new ArrayList<>();
      assertEquals(LoggingOption.AUTO, LoggingOption.parse(value, console::add), value);
      assertEquals(List.of("(!) Logging: invalid 'berlioz.logging' value, expected 'auto' or 'native'; using 'auto'"), console);
    }
  }

  private static void assertBackend(String label, String property, String factoryClass) {
    LoggingBackend backend = LoggingBackend.of(factoryClass);
    assertEquals(label, backend.label(), factoryClass);
    if (property == null) assertNull(backend.nativeProperty(), factoryClass);
    else assertEquals(property, backend.nativeProperty(), factoryClass);
  }
}
