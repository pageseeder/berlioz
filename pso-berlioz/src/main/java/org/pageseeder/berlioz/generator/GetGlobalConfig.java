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
import java.util.Collections;
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
 * <p>This generator uses an etag based on the content of the response, as written: the source,
 * the load status and timestamps, the redacted properties and the load failure, or
 * <code>null</code> if no config file could be found. The etag and the response share one
 * snapshot of the configuration per request, so that a reload in between cannot make them
 * describe different states.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.8
 */
public final class GetGlobalConfig implements Generator, Cacheable {

  private static final String SOURCE = "source";

  /** The request attribute holding the snapshot shared by the etag and the response. */
  private static final String ATTRIBUTE = GetGlobalConfig.class.getName() + ".snapshot";

  @Override
  public @Nullable String getETag(Request req) {
    return snapshot(req).etag();
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    snapshot(req).writeTo(out);
    return Response.ok();
  }

  private static Snapshot snapshot(Request req) {
    Object existing = req.getAttribute(ATTRIBUTE);
    if (existing instanceof Snapshot) return (Snapshot) existing;
    Snapshot snapshot = Snapshot.capture();
    req.setAttribute(ATTRIBUTE, snapshot);
    return snapshot;
  }

  /**
   * The immutable content of the response: the redacted properties and the load diagnostics,
   * with the failure details only when they may be shown.
   */
  private static final class Snapshot {

    private final @Nullable String source;
    private final String status;
    private final @Nullable String loaded;
    private final @Nullable String failed;
    /** Redacted values, sorted by name. */
    private final Map<String, String> properties;
    private final @Nullable Failure failure;

    private Snapshot(@Nullable String source, ConfigLoadDiagnostics diagnostics, Map<String, String> properties) {
      this.source = source;
      this.status = diagnostics.status();
      this.loaded = toIso(diagnostics.loaded());
      this.failed = toIso(diagnostics.failed());
      Map<String, String> redacted = new TreeMap<>();
      properties.forEach((name, value) -> redacted.put(name, Redaction.redact(name, value)));
      this.properties = Collections.unmodifiableMap(redacted);
      ConfigLoadDiagnostics.Failure cause = diagnostics.failure();
      this.failure = cause != null ? new Failure(cause, includeDetails()) : null;
    }

    static Snapshot capture() {
      // Load the properties first, so that the diagnostics describe them
      Map<String, String> properties = GlobalSettings.getAll();
      File global = GlobalSettings.getPropertiesFile();
      return new Snapshot(global != null ? global.getName() : null, GlobalSettings.getLoadDiagnostics(), properties);
    }

    void writeTo(OutputWriter out) {
      out.startObject("properties");
      out.optionalField(SOURCE, this.source);
      out.field("load-status", this.status);
      out.optionalField("loaded", this.loaded);
      out.optionalField("failed", this.failed);
      out.startArray("properties", OutputWriter.ContextOption.JSON_ONLY);
      for (Entry<String, String> e : this.properties.entrySet()) {
        out.startObject("property");
        out.field("name", e.getKey());
        out.field("value", e.getValue());
        out.endObject();
      }
      out.endArray();
      Failure cause = this.failure;
      if (cause != null) cause.writeTo(out);
      out.endObject();
    }

    /** @return the hash of every field written, or <code>null</code> without a config file */
    @Nullable String etag() {
      if (this.source == null) return null;
      // Each value is prefixed by its length, so that no two contents give the same text
      StringBuilder content = new StringBuilder("global-config-v2");
      append(content, this.source);
      append(content, this.status);
      append(content, this.loaded);
      append(content, this.failed);
      content.append('#').append(this.properties.size());
      this.properties.forEach((name, value) -> append(append(content, name), value));
      Failure cause = this.failure;
      if (cause != null) {
        append(append(content.append('!'), cause.source), cause.message).append('#').append(cause.line);
      }
      return SHA256.hash(content.toString());
    }

    private static StringBuilder append(StringBuilder content, @Nullable String value) {
      if (value == null) return content.append('-');
      return content.append(value.length()).append(':').append(value);
    }

  }

  /** The cause of the last failed attempt, as written. */
  private static final class Failure {

    private final @Nullable String source;
    /** The line, or -1 when unknown or hidden. */
    private final int line;
    private final String message;

    Failure(ConfigLoadDiagnostics.Failure failure, boolean includeDetails) {
      this.source = failure.source();
      this.line = includeDetails ? failure.line() : -1;
      String detail = failure.detail();
      this.message = includeDetails && detail != null ? detail : failure.message();
    }

    void writeTo(OutputWriter out) {
      out.startObject("load-failure");
      out.optionalField(SOURCE, this.source);
      if (this.line >= 0) out.field("line", this.line);
      out.field("message", this.message, OutputWriter.FieldOption.XML_TEXT);
      out.endObject();
    }

  }

  private static boolean includeDetails() {
    return DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;
  }

  private static @Nullable String toIso(@Nullable Instant instant) {
    return instant != null ? instant.toString() : null;
  }

}
