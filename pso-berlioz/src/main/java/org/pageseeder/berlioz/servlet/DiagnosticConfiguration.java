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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.xslt.StylesheetLocation;
import org.pageseeder.berlioz.xslt.StylesheetSourceKind;
import org.pageseeder.berlioz.xslt.XsltProcessorInfo;

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

  /**
   * Lists the servlets of the specified class that are registered but not initialized.
   *
   * <p>Each entry contains the name, the sorted mappings and <code>initialised=false</code>.
   * Nothing is listed if the container does not expose its registrations.
   */
  static List<Map<String, Object>> uninitialized(@Nullable ServletContext context, String className, Set<String> initialized) {
    if (context == null) return List.of();
    Map<String, ? extends ServletRegistration> registrations;
    try {
      registrations = context.getServletRegistrations();
    } catch (UnsupportedOperationException | IllegalStateException ex) {
      return List.of();
    }
    if (registrations == null) return List.of();
    List<Map<String, Object>> entries = new ArrayList<>();
    registrations.forEach((name, registration) -> {
      if (registration != null && className.equals(registration.getClassName()) && !initialized.contains(name)) {
        entries.add(fields("name", name, "mappings", sorted(registration.getMappings()), "initialised", false));
      }
    });
    return entries;
  }

  static @Nullable String safePath(String value) {
    String reference;
    try {
      reference = StylesheetLocation.extractClasspathReference(value);
    } catch (IllegalArgumentException ex) {
      return null; // rejected classpath reference (e.g. traversal) — nothing safe to report.
    }
    boolean classpath = reference != null;
    String path = classpath ? reference.replaceFirst("^/+", "") : value;
    if (path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")
        || path.contains("%") || path.chars().anyMatch(Character::isISOControl)) return null;
    for (String segment : path.split("/")) if (segment.equals("..")) return null;
    return classpath ? "classpath:" + path : path;
  }

  /**
   * Describes a configured stylesheet: its source and, when safe, its path.
   *
   * <p>A filesystem value is resolved within <code>WEB-INF</code>, ignoring leading separators,
   * so it is reported relative to <code>WEB-INF</code>: <code>/xslt/html/{GROUP}.xsl</code> is
   * reported as <code>xslt/html/{GROUP}.xsl</code>. Values with traversal are never reported.
   */
  static Map<String, Object> stylesheet(String value) {
    boolean classpath = isClasspath(value);
    Map<String, Object> result = fields("source", classpath ? "classpath" : "filesystem");
    String safe = safePath(classpath ? value : value.replaceFirst("^/+", ""));
    if (safe != null) result.put("path", safe);
    return result;
  }

  /**
   * Describes a selected stylesheet: its key, source and, when safe, its path.
   *
   * <p>A file is reported relative to the private folder, a classpath resource by its
   * <code>classpath:</code> reference. Anything else is reported without a path.
   */
  static Map<String, Object> selection(String name, StylesheetLocation location, Path privateFolder) {
    Map<String, Object> result = fields("name", name, "source", source(location.kind()));
    String path = null;
    Path file = location.path();
    if (location.kind() == StylesheetSourceKind.CLASSPATH) {
      path = safePath(location.logicalPath());
    } else if (file != null) {
      Path base = privateFolder.toAbsolutePath().normalize();
      Path normalized = file.toAbsolutePath().normalize();
      if (normalized.startsWith(base)) path = safePath(base.relativize(normalized).toString().replace('\\', '/'));
    }
    if (path != null) result.put("path", path);
    return result;
  }

  static Map<String, Object> processor(XsltProcessorInfo info) {
    Map<String, Object> result = new LinkedHashMap<>();
    putIfPresent(result, "factory", info.factory());
    putIfPresent(result, "vendor", info.vendor());
    putIfPresent(result, "xslt-version", info.xsltVersion());
    putIfPresent(result, "product-name", info.productName());
    putIfPresent(result, "product-version", info.productVersion());
    return result;
  }

  static Map<String, Object> container(Environment env) {
    Map<String, Object> result = new LinkedHashMap<>();
    putIfPresent(result, "server-info", env.getServerInfo());
    putIfPresent(result, "servlet-api", env.getServletApiVersion());
    return result;
  }

  private static void putIfPresent(Map<String, Object> fields, String name, @Nullable String value) {
    if (value != null) fields.put(name, value);
  }

  private static String source(StylesheetSourceKind kind) {
    switch (kind) {
      case CLASSPATH:  return "classpath";
      case FILESYSTEM: return "filesystem";
      default:         return "identity";
    }
  }

  private static boolean isClasspath(String value) {
    try {
      return StylesheetLocation.extractClasspathReference(value) != null;
    } catch (IllegalArgumentException ex) {
      return true; // still a classpath-style reference, just rejected — reported with no path.
    }
  }
}
