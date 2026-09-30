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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;

/** Internal helpers for allowlisted configuration diagnostics. */
final class DiagnosticConfiguration {
  private DiagnosticConfiguration() { }

  static Map<String, Object> fields(Object... pairs) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) values.put((String) pairs[i], pairs[i + 1]);
    return values;
  }

  static List<String> sorted(Collection<String> values) {
    return values.stream().sorted().collect(Collectors.toUnmodifiableList());
  }

  static List<String> mappings(ServletConfig config) {
    if (config.getServletContext() == null) return List.of();
    try {
      ServletRegistration registration = config.getServletContext().getServletRegistration(config.getServletName());
      return registration == null ? List.of() : sorted(registration.getMappings());
    } catch (UnsupportedOperationException | IllegalStateException ex) {
      return List.of();
    }
  }

  static @Nullable String safePath(String value) {
    String path = value;
    boolean classpath = path.startsWith("classpath:") || path.startsWith("resource:");
    if (classpath) path = path.substring(path.indexOf(':') + 1).replaceFirst("^/+", "");
    if (path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")
        || path.contains("%") || path.chars().anyMatch(Character::isISOControl)) return null;
    for (String segment : path.split("/")) if (segment.equals("..")) return null;
    return classpath ? "classpath:" + path : path;
  }

  static Map<String, Object> stylesheet(String value) {
    Map<String, Object> result = fields("source",
        value.startsWith("classpath:") || value.startsWith("resource:") ? "classpath" : "filesystem");
    String safe = safePath(value);
    if (safe != null) result.put("path", safe);
    return result;
  }
}
