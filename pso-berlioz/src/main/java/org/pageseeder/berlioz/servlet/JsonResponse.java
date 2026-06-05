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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Parameter;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.json.JsonStringBuilder;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JSON response produced directly from generators, bypassing the XSLT pipeline.
 *
 * <p>Only services whose {@link Service#supported()} set includes {@link org.pageseeder.berlioz.output.OutputType#JSON}
 * reach this path. Each generator writes JSON via its {@link JsonGenerator#generate} or
 * {@link Generator#generate} method.</p>
 *
 * <p>For a single-generator service the generator's JSON output is the entire response body.
 * For a multi-generator service the outputs are wrapped in a JSON object keyed by generator name:
 * {@code {"gen-a": ..., "gen-b": ...}}.</p>
 *
 * <p>This class is not thread-safe.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public final class JsonResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonResponse.class);

  private static final AtomicReference<@Nullable GeneratorListener> listener = new AtomicReference<>(null);

  private final CoreHttpRequest core;
  private final MatchingService match;
  private final List<HttpContentRequest> requests;
  private final boolean profile;

  private @Nullable ContentStatus status = null;
  private @Nullable String redirect = null;
  private @Nullable BerliozException exception = null;
  private final Map<String, String> responseHeaders = new LinkedHashMap<>();

  public JsonResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config,
      MatchingService match, boolean profile) {
    this.core = new CoreHttpRequest(req, res, config.getEnvironment());
    this.match = match;
    this.requests = configure(this.core, match);
    this.profile = profile;
  }

  public Service getService() {
    return this.match.service();
  }

  public ContentStatus getStatus() {
    ContentStatus s = this.status;
    return s == null ? ContentStatus.OK : s;
  }

  public @Nullable BerliozException getError() {
    return this.exception;
  }

  public @Nullable String getRedirectURL() {
    return this.redirect;
  }

  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(this.responseHeaders);
  }

  /**
   * Invokes all generators and returns the assembled JSON body.
   *
   * <p>Single-generator service: the generator's output is returned as-is.
   * Multi-generator service: outputs are assembled as {@code {"name": <json>, ...}}.</p>
   *
   * @return the JSON body string
   * @throws IOException if an I/O error occurs
   */
  public String generate() throws IOException {
    Service service = this.match.service();
    List<GeneratorResult> results = new ArrayList<>(this.requests.size());

    for (HttpContentRequest request : this.requests) {
      results.add(invoke(request, service));
    }

    return assemble(results, service);
  }

  // Static listener management (mirrors XmlResponse)
  // ----------------------------------------------------------------------------------------------

  @Beta
  static void setListener(@Nullable GeneratorListener listener) {
    JsonResponse.listener.set(listener);
  }

  @Beta
  static @Nullable GeneratorListener getListener() {
    return listener.get();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  private GeneratorResult invoke(HttpContentRequest request, Service service) {
    BerliozGenerator generator = request.generator();
    String name = service.name(generator);

    Response response = Response.ok();
    String json = null;
    BerliozException error = null;
    long start = System.nanoTime();

    try {
      JsonStringBuilder jb = JsonStringBuilder.create();
      if (generator instanceof JsonGenerator) {
        response = ((JsonGenerator) generator).generate(request, jb);
        jb.flush();
      } else if (generator instanceof Generator) {
        JsonOutputAdapter oa = new JsonOutputAdapter(jb);
        response = ((Generator) generator).generate(request, oa);
        oa.flush();
      } else {
        LOGGER.warn("Generator {} does not support JSON output — skipping", generator.getClass().getName());
      }
      json = jb.toString();
    } catch (InvalidParameterException ex) {
      error = handleError(ex, generator);
      response = Response.status(ContentStatus.BAD_REQUEST);
    } catch (Exception ex) {
      error = handleError(ex, generator);
      response = Response.status(ContentStatus.INTERNAL_SERVER_ERROR);
    }

    long end = System.nanoTime();
    ContentStatus generatorStatus = response.status();
    boolean wasSet = handleStatus(generatorStatus, generator, service);
    if (wasSet && response.isRedirect()) {
      this.redirect = response.redirectLocation();
    }
    response.headers().forEach((headerName, value) -> this.responseHeaders.put(headerName, value));

    GeneratorListener l = listener.get();
    if (l != null) l.generate(service, generator, generatorStatus, 0, end - start);

    return new GeneratorResult(name, json, error);
  }

  private String assemble(List<GeneratorResult> results, Service service) {
    if (results.isEmpty()) return "{}";

    // Single generator: return its output directly, or a problem JSON if it failed
    if (results.size() == 1) {
      GeneratorResult r = results.get(0);
      if (r.error != null) return errorJson(r.error);
      return r.json != null && !r.json.isEmpty() ? r.json : "null";
    }

    // Multiple generators: {"name1": <json1>, "name2": <json2>}
    StringBuilder sb = new StringBuilder("{");
    boolean first = true;
    for (GeneratorResult r : results) {
      if (!first) sb.append(',');
      first = false;
      sb.append('"').append(r.name).append("\":");
      if (r.error != null) {
        sb.append(errorJson(r.error));
      } else if (r.json != null && !r.json.isEmpty()) {
        sb.append(r.json);
      } else {
        sb.append("null");
      }
    }
    sb.append('}');
    return sb.toString();
  }

  private static String errorJson(BerliozException ex) {
    String msg = ex.getMessage();
    if (msg == null) msg = ex.getClass().getName();
    // Minimal safe JSON — message is escaped below
    return "{\"error\":" + jsonString(msg) + "}";
  }

  private static String jsonString(String value) {
    StringBuilder sb = new StringBuilder(value.length() + 2).append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"':  sb.append("\\\""); break;
        case '\\': sb.append("\\\\"); break;
        case '\n': sb.append("\\n");  break;
        case '\r': sb.append("\\r");  break;
        case '\t': sb.append("\\t");  break;
        default:
          if (c < 0x20) { sb.append(String.format("\\u%04x", (int) c)); }
          else { sb.append(c); }
      }
    }
    return sb.append('"').toString();
  }

  private BerliozException handleError(Exception ex, BerliozGenerator generator) {
    LOGGER.warn("Handling {} thrown by {}", ex.getClass().getName(), generator.getClass().getName());
    BerliozException bex = toBerliozException(ex);
    accumulateError(bex);
    return bex;
  }

  private static BerliozException toBerliozException(Exception ex) {
    if (ex instanceof BerliozException) {
      BerliozException bex = (BerliozException) ex;
      if (bex.id() == null) bex.setId(BerliozErrorID.GENERATOR_ERROR_UNFORCED);
      return bex;
    }
    if (ex instanceof InvalidParameterException) {
      InvalidParameterException ipe = (InvalidParameterException) ex;
      return new BerliozException("Invalid parameter '" + ipe.getParameterName() + "': " + ipe.getMessage(),
          ipe, BerliozErrorID.INVALID_PARAMETER);
    }
    return new BerliozException("Unexpected exception caught", ex, BerliozErrorID.GENERATOR_ERROR_UNCHECKED);
  }

  private void accumulateError(BerliozException bex) {
    if (this.exception == null) {
      this.exception = bex;
    } else if (this.exception instanceof CompoundBerliozException) {
      collectCause(((CompoundBerliozException) this.exception).getCollector(), bex);
    } else {
      ErrorCollector<Throwable> collector = new ErrorCollector<>();
      BerliozException first = this.exception;
      this.exception = new CompoundBerliozException(
          "Multiple errors thrown by generators", BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
      collectCause(collector, first);
      collectCause(collector, bex);
    }
  }

  private static void collectCause(ErrorCollector<Throwable> collector, BerliozException bex) {
    Throwable cause = bex.getCause();
    collector.collectQuietly(Level.ERROR, cause != null ? cause : bex);
  }

  private boolean handleStatus(ContentStatus status, BerliozGenerator generator, Service service) {
    if (!service.affectStatus(generator)) return false;
    CodeRule rule = service.rule().rule();
    ContentStatus current = this.status;
    boolean update = current == null
        || (rule == CodeRule.HIGHEST && status.code() > current.code())
        || (rule == CodeRule.LOWEST  && status.code() < current.code());
    if (update) this.status = status;
    return update;
  }

  private static List<HttpContentRequest> configure(CoreHttpRequest core, MatchingService match) {
    Map<String, String> common = HttpRequestWrapper.toParameters(core.request(), match.result());
    Service service = match.service();
    List<HttpContentRequest> requests = new ArrayList<>();
    int order = 0;
    for (BerliozGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        requests.add(new HttpContentRequest(core, common, generator, service, order));
      } else {
        Map<String, String> specific = new java.util.HashMap<>(common);
        for (Parameter p : pconfig) {
          specific.put(p.name(), p.value(common));
        }
        requests.add(new HttpContentRequest(core, specific, generator, service, order));
      }
      order++;
    }
    return requests;
  }

  // Simple holder for a generator's captured output
  private static final class GeneratorResult {
    final String name;
    final @Nullable String json;
    final @Nullable BerliozException error;

    GeneratorResult(String name, @Nullable String json, @Nullable BerliozException error) {
      this.name = name;
      this.json = json;
      this.error = error;
    }
  }

}
