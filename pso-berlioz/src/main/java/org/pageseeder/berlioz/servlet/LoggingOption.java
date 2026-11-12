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

import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * Who configures logging, set with <code>berlioz.logging</code> before logging is initialized.
 *
 * @author Christophe Lauret
 * @since 0.14.6
 */
enum LoggingOption {

  /** Berlioz looks for its mode-based configuration files (default). */
  AUTO,

  /** Berlioz looks at no file: logging is left to the native configuration of the backend. */
  NATIVE;

  /**
   * Parses the value of the option.
   *
   * <p>Only <code>auto</code> and <code>native</code> are accepted, after trimming. Any other
   * value, including an empty one, is reported to the console without being echoed, and
   * {@link #AUTO} is used.
   *
   * @param value   the value of the option from the first source that defines it, if any
   * @param console where to report an invalid value
   * @return the option to use
   */
  static LoggingOption parse(@Nullable String value, Consumer<String> console) {
    if (value == null) return AUTO;
    String option = value.trim();
    if ("auto".equals(option)) return AUTO;
    if ("native".equals(option)) return NATIVE;
    console.accept("(!) Logging: invalid 'berlioz.logging' value, expected 'auto' or 'native'; using 'auto'");
    return AUTO;
  }
}
