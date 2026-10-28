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

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoadDiagnostics;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.content.ServiceSource;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.OutputWriter.ContextOption;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.berlioz.util.CollectedError;
import org.xml.sax.SAXParseException;

/**
 * Returns the live service registry as XML or JSON.
 *
 * <p>Unlike {@code GetServices}, which re-reads the services XML files from disk, this generator
 * exposes a safe {@code load-status} field (pending, ready, startup-failed, or reload-failed) and
 * reflects the current in-memory state of the service registry. It is intended for developers and
 * operations tooling to inspect which services are actually loaded and active.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Service sources</h3>
 * <p>Each service names the {@code source} that declared it. When a later source declared the
 * same HTTP method and URI pattern, the winning service lists each replaced declaration as
 * {@code overridden}, in load order, with the replaced service ID, its source and the pattern.
 *
 * <h3>Load diagnostics</h3>
 * <p>After the services, the output lists the configuration sources of the last successful load
 * in load order (classpath sources first), then the warnings of that load. If the last attempt
 * failed, its cause follows as {@code load-failure}; it is kept until the next successful load,
 * while the last valid services, sources and warnings remain listed.
 *
 * <p>The {@code loaded} (last successful load), {@code failed} (last failed attempt) and
 * {@code retry-after} (when Berlioz will try again) timestamps are ISO 8601 and only written when
 * known; {@code failed} and {@code retry-after} only while the last attempt has failed.
 *
 * <p>Source names are display names (a JAR file name and resource path, or a path relative to the
 * configuration directory), never full URLs. The {@code line} attributes and the parser's
 * description of a load failure are only included when {@code berlioz.errors.detail} is
 * {@code standard} or {@code full}; otherwise the failure only has a generic message.
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <live-services load-status="ready" loaded="[iso8601]" failed="[iso8601]" retry-after="[iso8601]">
 *   <service id="[id]" group="[group]" method="[method]" cacheable="[true|false]" ... source="[name]">
 *     <response-code use="[use]" rule="[rule]"/>
 *     <url pattern="[uri-pattern]"/>
 *     <generator class="[class]" name="[name]" type="[type]" cacheable="[true|false]" .../>
 *     ...
 *     <overridden service="[id]" source="[name]" pattern="[uri-pattern]"/>
 *   </service>
 *   ...
 *   <source kind="[classpath|filesystem]" name="[name]"/>
 *   ...
 *   <warning source="[name]" line="[line]">[message]</warning>
 *   ...
 *   <load-failure source="[name]" line="[line]">[message]</load-failure>
 * </live-services>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "loadStatus": "ready", "loaded": "[iso8601]", "failed": "[iso8601]", "retryAfter": "[iso8601]",
 *   "liveServices": [
 *     {
 *       "id": "[id]", "group": "[group]", "method": "[method]", "cacheable": [true|false], ...,
 *       "source": "[name]",
 *       "responseCode": {"use": "[use]", "rule": "[rule]"},
 *       "urls": [{"pattern": "[uri-pattern]"}, ...],
 *       "generators": [
 *         {"class": "[class]", "name": "[name]", "type": "[type]", "cacheable": [true|false], ...},
 *         ...
 *       ],
 *       "overridden": [{"service": "[id]", "source": "[name]", "pattern": "[uri-pattern]"}, ...]
 *     },
 *     ...
 *   ],
 *   "sources": [{"kind": "[classpath|filesystem]", "name": "[name]"}, ...],
 *   "warnings": [{"source": "[name]", "line": [line], "message": "[message]"}, ...],
 *   "loadFailure": {"source": "[name]", "line": [line], "message": "[message]"}
 * }
 * }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.ListRuntimeServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <p>{@code GetLiveServices} is a deprecated forwarding alias of this class, retained so that
 * existing {@code services.xml} generator references keep resolving unchanged until removal
 * at 1.0.
 *
 * <h3>Etag</h3>
 * <p>The ETag is the registry version counter, which increments each time the services are
 * reloaded, together with the load status and the time of the last failed attempt. This
 * invalidates the response when the configuration, load status or failure changes.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.3
 */
@Beta
public class ListRuntimeServices implements Generator, Cacheable {

  private static final String SOURCE = "source";

  @Override
  public String getETag(Request req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    ServiceLoadDiagnostics diagnostics = ServiceLoader.getInstance().getLoadDiagnostics();
    Instant failed = diagnostics.failed();
    return registry.version() + "-" + diagnostics.status() + (failed != null ? "-" + failed.toEpochMilli() : "");
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    HttpEnvironment httpEnv = (HttpEnvironment) req.getEnvironment();
    ServiceLoadDiagnostics diagnostics = ServiceLoader.getInstance().getLoadDiagnostics();
    boolean includeDetails = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL)) != DetailLevel.MINIMAL;

    out.startObject("live-services");
    out.field("load-status", diagnostics.status());
    out.optionalField("loaded", toIso(diagnostics.loaded()));
    out.optionalField("failed", toIso(diagnostics.failed()));
    out.optionalField("retry-after", toIso(diagnostics.retryAfter()));
    out.startArray("live-services", ContextOption.JSON_ONLY);
    for (HttpMethod method : HttpMethod.mappable()) {
      List<Service> services = registry.getServices(method);
      for (Service service : services) {
        List<String> urls = registry.matches(service);
        service.writeTo(out, method, urls, httpEnv.getCacheControl(), registry);
      }
    }
    out.endArray();
    writeSources(diagnostics.sources(), out);
    writeWarnings(diagnostics, out, includeDetails);
    writeFailure(diagnostics.failure(), out, includeDetails);
    out.endObject();

    return Response.ok();
  }

  private static void writeSources(List<ServiceSource> sources, OutputWriter out) {
    out.startArray("sources", ContextOption.JSON_ONLY);
    for (ServiceSource source : sources) {
      out.startObject(SOURCE);
      out.field("kind", source.kind().name().toLowerCase(Locale.ROOT));
      out.field("name", source.origin().displayName());
      out.endObject();
    }
    out.endArray();
  }

  private static void writeWarnings(ServiceLoadDiagnostics diagnostics, OutputWriter out, boolean includeDetails) {
    out.startArray("warnings", ContextOption.JSON_ONLY);
    for (CollectedError<SAXParseException> warning : diagnostics.warnings()) {
      SAXParseException ex = warning.error();
      out.startObject("warning");
      out.optionalField(SOURCE, diagnostics.sourceName(ex.getSystemId()));
      if (includeDetails && ex.getLineNumber() >= 0) out.field("line", ex.getLineNumber());
      String message = ex.getMessage();
      out.field("message", message != null ? message : "(No message)", OutputWriter.FieldOption.XML_TEXT);
      out.endObject();
    }
    out.endArray();
  }

  private static void writeFailure(ServiceLoadDiagnostics.@Nullable Failure failure, OutputWriter out, boolean includeDetails) {
    if (failure == null) return;
    out.startObject("load-failure");
    out.optionalField(SOURCE, failure.source());
    if (includeDetails && failure.line() >= 0) out.field("line", failure.line());
    String detail = failure.detail();
    String message = includeDetails && detail != null ? detail : failure.message();
    out.field("message", message, OutputWriter.FieldOption.XML_TEXT);
    out.endObject();
  }

  private static @Nullable String toIso(@Nullable Instant instant) {
    return instant != null ? instant.toString() : null;
  }

}
