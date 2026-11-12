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

import org.jspecify.annotations.Nullable;

/**
 * The logging backend that SLF4J uses, identified by the class name of its logger factory.
 *
 * <p>Only the class name is compared, so no class of the backend is loaded.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
enum LoggingBackend {

  /** Logback. */
  LOGBACK("logback", LogbackSetup.LOGGER_CONTEXT_CLASS, "logback.configurationFile"),

  /** Log4j 2 through its SLF4J 2 provider. */
  LOG4J2("log4j2", "org.apache.logging.slf4j.Log4jLoggerFactory", "log4j2.configurationFile"),

  /** <code>java.util.logging</code> through the <code>slf4j-jdk14</code> provider. */
  JUL("jul", "org.slf4j.jul.JDK14LoggerFactory", "java.util.logging.config.file"),

  /** SLF4J Simple. */
  SIMPLE("simple", "org.slf4j.simple.SimpleLoggerFactory", null),

  /** No logging, when SLF4J finds no provider. */
  NOP("nop", "org.slf4j.helpers.NOPLoggerFactory", null),

  /** Any other provider, such as reload4j or a custom one. */
  UNKNOWN("unknown", "", null);

  private final String label;

  private final String factoryClass;

  private final @Nullable String nativeProperty;

  LoggingBackend(String label, String factoryClass, @Nullable String nativeProperty) {
    this.label = label;
    this.factoryClass = factoryClass;
    this.nativeProperty = nativeProperty;
  }

  /** @return the name of the backend in diagnostics, such as <code>logback</code> */
  String label() {
    return this.label;
  }

  /** @return the documented system property that locates the native configuration of the backend, if any */
  @Nullable String nativeProperty() {
    return this.nativeProperty;
  }

  /**
   * @param factoryClass the class name of the SLF4J logger factory
   * @return the backend using this logger factory, or {@link #UNKNOWN}
   */
  static LoggingBackend of(String factoryClass) {
    for (LoggingBackend backend : values()) {
      if (backend != UNKNOWN && backend.factoryClass.equals(factoryClass)) return backend;
    }
    return UNKNOWN;
  }
}
