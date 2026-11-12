/*
 * Copyright 2015 Allette Systems (Australia)
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
package org.pageseeder.berlioz.generator;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.ConfigLoadDiagnostics;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.Redaction;
import org.pageseeder.berlioz.util.SHA256;

/**
 * Returns the global properties as XML or JSON.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 *
 * <h3>Returned XML</h3>
 * <p>This generator returns a flat list of the global properties as XML as below:
 * <pre>{@code
 * <properties source="[source]" load-status="[status]" loaded="[iso8601]" failed="[iso8601]">
 *   <property name="[nameA]" value="[valueA]"/>
 *   <property name="[nameB]" value="[REDACTED]"/>
 *   <property name="[nameC]" value="[valueC]"/>
 *   ...
 *   <load-failure source="[file]" line="[line]">[message]</load-failure>
 * </properties>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <p>This generator returns the same information as JSON as below:
 * <pre>{@code
 * {
 *   "source": "[source]",
 *   "loadStatus": "[status]", "loaded": "[iso8601]", "failed": "[iso8601]",
 *   "properties": [
 *     {"name": "[nameA]", "value": "[valueA]"},
 *     {"name": "[nameB]", "value": "[REDACTED]"},
 *     {"name": "[nameC]", "value": "[valueC]"}
 *   ],
 *   "loadFailure": {"source": "[file]", "line": [line], "message": "[message]"}
 * }
 * }</pre>
 *
 * <h3>Load status</h3>
 * <p>Since 0.14.6, {@code load-status} reports the outcome of the last attempts to load the
 * configuration ({@link GlobalSettings#getLoadDiagnostics()}): {@code pending}, {@code ready},
 * {@code startup-failed} (Berlioz uses the default value of every option) or
 * {@code reload-failed} (Berlioz keeps the properties of the last successful load, which are
 * the ones listed). The {@code loaded} (last successful load) and {@code failed} (last failed
 * attempt) timestamps are ISO 8601 and only written when known; {@code failed} only while the
 * last attempt has failed. After a failure, its cause follows as {@code load-failure}, with the
 * name of the file that failed. The line and the description of the error are only included
 * when {@code berlioz.errors.detail} is not {@code minimal}; otherwise the failure only has a
 * generic message.
 *
 * <p>Property values whose names match a sensitive keyword pattern (e.g. {@code password},
 * {@code secret}, {@code token}, {@code apikey}, {@code credential}, {@code privatekey})
 * are replaced with {@code [REDACTED]} to avoid exposing secrets via misconfiguration. In other
 * values, the password of a URL and the values of parameters with a sensitive name are replaced
 * ({@link Redaction#redact(String, String)}).
 *
 * <h3>Etag</h3>
 * <p>This generator uses an etag based on the effective properties (after merging the default
 * and mode-specific config files and resolving variables), the load status, the time of the last
 * failed attempt and whether diagnostic details are included, or <code>null</code> if no config
 * file could be found.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.8
 */
public final class GetGlobalConfig implements Generator, Cacheable {

  private static final String SOURCE = "source";

  @Override
  public @Nullable String getETag(Request req) {
    if (GlobalSettings.getPropertiesFile() == null) return null;
    // Sorted so that the hash does not depend on the iteration order of the settings map
    StringBuilder effective = new StringBuilder();
    for (Entry<String, String> e : new TreeMap<>(GlobalSettings.getAll()).entrySet()) {
      effective.append(e.getKey()).append('=').append(e.getValue()).append('\n');
    }
    ConfigLoadDiagnostics diagnostics = GlobalSettings.getLoadDiagnostics();
    Instant failed = diagnostics.failed();
    effective.append(diagnostics.status()).append('-').append(failed != null ? failed.toEpochMilli() : 0)
        .append('-').append(includeDetails());
    return SHA256.hash(effective.toString());
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    File global = GlobalSettings.getPropertiesFile();
    // Load the properties first, so that the diagnostics describe them
    Map<String, String> properties = GlobalSettings.getAll();
    ConfigLoadDiagnostics diagnostics = GlobalSettings.getLoadDiagnostics();

    out.startObject("properties");
    if (global != null) {
      out.field(SOURCE, global.getName());
    }
    out.field("load-status", diagnostics.status());
    out.optionalField("loaded", toIso(diagnostics.loaded()));
    out.optionalField("failed", toIso(diagnostics.failed()));

    out.startArray("properties", OutputWriter.ContextOption.JSON_ONLY);
    for (Entry<String, String> e : properties.entrySet()) {
      String name = e.getKey();
      String value = Redaction.redact(name, e.getValue());
      out.startObject("property");
      out.field("name", name);
      out.field("value", value);
      out.endObject();
    }
    out.endArray();
    writeFailure(diagnostics.failure(), out);
    out.endObject();
    return Response.ok();
  }

  private static void writeFailure(ConfigLoadDiagnostics.@Nullable Failure failure, OutputWriter out) {
    if (failure == null) return;
    boolean includeDetails = includeDetails();
    out.startObject("load-failure");
    out.optionalField(SOURCE, failure.source());
    if (includeDetails && failure.line() >= 0) out.field("line", failure.line());
    String detail = failure.detail();
    String message = includeDetails && detail != null ? detail : failure.message();
    out.field("message", message, OutputWriter.FieldOption.XML_TEXT);
    out.endObject();
  }

  private static boolean includeDetails() {
    return DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;
  }

  private static @Nullable String toIso(@Nullable Instant instant) {
    return instant != null ? instant.toString() : null;
  }

}
