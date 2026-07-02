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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.http.ServerTimingHeader;
import org.pageseeder.berlioz.json.JsonStringBuilder;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.util.ProfileFormat;
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
 * @version 0.13.5
 * @since 0.13.2
 */
public final class JsonResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonResponse.class);

  private final CoreHttpRequest core;
  private final MatchingService match;
  private final List<HttpContentRequest> requests;

  private final boolean profile;
  private boolean serverTiming;
  private final GeneratorOutcome outcome = new GeneratorOutcome();
  private final Map<Integer, String> etags = new HashMap<>();
  private final Map<String, String> responseHeaders = new LinkedHashMap<>();
  private final Map<String, String> responseHeadersView = Collections.unmodifiableMap(this.responseHeaders);

  /**
   * Non-null only for direct services whose sole generator produced a problem response.
   */
  private @Nullable ProblemDetails topLevelProblem = null;

  public JsonResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config,
                      MatchingService match, boolean profile) {
    this.core = new CoreHttpRequest(req, res, config.getEnvironment());
    this.match = match;
    this.requests = GeneratorDispatch.configure(this.core, match);
    this.profile = profile;
  }

  /**
   * Enables per-generator {@code Server-Timing} metrics for this response.
   */
  public void enableServerTiming() {
    this.serverTiming = true;
  }

  /**
   * Returns the service corresponding to this response.
   *
   * @return the service corresponding to this response.
   */
  public Service getService() {
    return this.match.service();
  }

  /**
   * Returns the status of this service response.
   *
   * @return the status of this service response.
   */
  public ContentStatus getStatus() {
    return this.outcome.getStatus();
  }

  /**
   * Returns the exact HTTP status code selected for this response.
   *
   * <p>Unlike {@link #getStatus()}, this preserves custom problem status codes that are not
   * represented by {@link ContentStatus}.</p>
   *
   * @return the HTTP status code
   */
  public int getStatusCode() {
    return this.outcome.getStatusCode();
  }

  /**
   * Returns a Berlioz exception wrapping any generator error.
   *
   * @return the generator error, or {@code null} if generation succeeded
   */
  public @Nullable BerliozException getError() {
    return this.outcome.getError();
  }

  /**
   * Returns the redirect URL selected by the service outcome.
   *
   * @return the redirect URL, or {@code null} if this response is not a redirect
   */
  public @Nullable String getRedirectUrl() {
    return this.outcome.getRedirectURL();
  }

  /**
   * Returns response headers accumulated from generator {@link Response} objects.
   *
   * @return an unmodifiable map of headers; never {@code null}
   */
  public Map<String, String> getHeaders() {
    return this.responseHeadersView;
  }

  /**
   * Returns the top-level problem for this response, or {@code null} if the response is not a
   * problem response.
   *
   * <p>Only set for direct services where the sole generator returned {@code Response.problem()}.
   * For multi-generator services, generator problems are serialized inline inside the envelope
   * object and the overall response remains {@code application/json}.
   *
   * @return the problem details, or {@code null}
   */
  public @Nullable ProblemDetails getProblem() {
    return this.topLevelProblem;
  }

  /**
   * Returns the ETag for this response.
   *
   * <p>The ETag is computed from the ETags returned by each generator. If any generator is not
   * cacheable or returns a blank ETag, the response is not cacheable and {@code null} is returned.
   *
   * @return the combined ETag if all generators are cacheable; {@code null} otherwise
   */
  public @Nullable String getEtag() {
    Service service = this.match.service();
    boolean cacheable = service.isCacheable();
    StringBuilder etag = new StringBuilder();
    if (cacheable) {
      for (HttpContentRequest request : this.requests) {
        BerliozGenerator generator = request.generator();
        if (generator instanceof Cacheable) {
          String localTag = GeneratorDispatch.retrieveETag(request, this.etags);
          if (localTag.isEmpty()) return null;
          etag.append(localTag).append('/');
        } else {
          cacheable = false;
          break;
        }
      }
    }
    return cacheable ? etag.toString() : null;
  }

  /**
   * Invokes all generators and returns the assembled JSON body.
   *
   * <p>Single-generator service: the generator's output is returned as-is.
   * Multi-generator service: outputs are assembled as {@code {"name": <json>, ...}}.</p>
   *
   * @return the JSON body string
   */
  public String generate() {
    Service service = this.match.service();
    List<GeneratorResult> results = new ArrayList<>(this.requests.size());

    int position = 0;
    for (HttpContentRequest request : this.requests) {
      results.add(invoke(request, ++position, service));
    }

    return assemble(results, service);
  }

  // Static listener management
  // ----------------------------------------------------------------------------------------------

  @Beta
  static void setListener(@Nullable GeneratorListener listener) {
    GeneratorDispatch.setListener(listener);
  }

  @Beta
  static @Nullable GeneratorListener getListener() {
    return GeneratorDispatch.getListener();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  private GeneratorResult invoke(HttpContentRequest request, int position, Service service) {
    BerliozGenerator generator = request.generator();
    String name = service.name(generator);

    Response response = Response.ok();
    String json = null;
    BerliozException error = null;
    long start = System.nanoTime();

    try (JsonStringBuilder jb = JsonStringBuilder.create()) {
      response = dispatchJson(generator, request, jb);
      json = jb.toString();
    } catch (Exception ex) {
      GeneratorFailure failure = GeneratorFailure.handle(ex, generator, this.outcome);
      error = failure.error();
      response = failure.response();
    }

    long end = System.nanoTime();
    ContentStatus generatorStatus = response.status();
    outcome.handleStatus(response, generator, service);
    GeneratorDispatch.accumulateHeaders(generator, response, this.responseHeaders);

    if (this.serverTiming) {
      String safeName = name.replaceAll("[^!#$%&'*+\\-.^_`|~0-9a-zA-Z]", "_");
      ServerTimingHeader.addMetricNano(this.core.response(), "json" + position, "Source " + safeName, request.getProfileEtag() + end - start);
    }

    GeneratorListener listener = GeneratorDispatch.getListener();
    if (listener != null) listener.generate(service, generator, generatorStatus, request.getProfileEtag(), end - start);

    ProblemDetails problem = response.isProblem() ? response.problem() : null;
    return new GeneratorResult(name, json, problem, error, request.getProfileEtag(), end - start);
  }

  private static Response dispatchJson(BerliozGenerator generator, HttpContentRequest request, JsonStringBuilder jb) {
    if (generator instanceof JsonGenerator) {
      Response response = ((JsonGenerator) generator).generate(request, jb);
      jb.flush();
      return response;
    }
    if (generator instanceof Generator) {
      JsonOutputAdapter oa = new JsonOutputAdapter(jb);
      Response response = ((Generator) generator).generate(request, oa);
      oa.flush();
      return response;
    }
    LOGGER.warn("Generator {} does not support JSON output — skipping", generator.getClass().getName());
    return Response.ok();
  }

  private String assemble(List<GeneratorResult> results, Service service) {
    if (results.isEmpty()) return "{}";

    // Direct service: generator output IS the response body — no name wrapper
    if (service.isDirect()) {
      GeneratorResult r = results.get(0);
      if (r.problem != null) this.topLevelProblem = r.problem;
      return resolveValue(r);
    }

    // Envelope: {"name1": <json1>, "name2": <json2>} — even for a single generator
    try (JsonStringBuilder json = JsonStringBuilder.create()) {
      json.startObject();
      for (GeneratorResult r : results) {
        json.fieldRaw(r.name, resolveValue(r));
      }
      if (this.profile) {
        appendProfile(json, results);
      }
      json.endObject();
      json.flush();
      return json.toString();
    }
  }

  private static String resolveValue(GeneratorResult r) {
    if (r.problem != null) {
      DetailLevel level = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL));
      return r.problem.forDetailLevel(level).toJson();
    }
    if (r.error != null) return errorJson(r.error);
    return r.json != null && !r.json.isEmpty() ? r.json : "null";
  }

  private static void appendProfile(JsonStringBuilder json, List<GeneratorResult> results) {
    json.startObject("_profile");
    for (GeneratorResult r : results) {
      json.startObject(r.name);
      json.field("etag", ProfileFormat.format(r.profileEtag));
      json.field("process", ProfileFormat.format(r.profileProcess));
      json.field("total", ProfileFormat.format(r.profileEtag + r.profileProcess));
      json.endObject();
    }
    json.endObject();
  }

  private static String errorJson(BerliozException ex) {
    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
    try (JsonStringBuilder json = JsonStringBuilder.create()) {
      json.startObject().field("error", msg).endObject();
      json.flush();
      return json.toString();
    }
  }

  private static final class GeneratorResult {
    final String name;
    final @Nullable String json;
    final @Nullable ProblemDetails problem;
    final @Nullable BerliozException error;
    final long profileEtag;
    final long profileProcess;

    GeneratorResult(String name, @Nullable String json, @Nullable ProblemDetails problem,
                    @Nullable BerliozException error,
                    long profileEtag, long profileProcess) {
      this.name = name;
      this.json = json;
      this.problem = problem;
      this.error = error;
      this.profileEtag = profileEtag;
      this.profileProcess = profileProcess;
    }
  }

}
