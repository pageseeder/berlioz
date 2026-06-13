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
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.UpstreamException;
import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
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
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public final class JsonResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonResponse.class);

  private static final AtomicReference<@Nullable GeneratorListener> listener = new AtomicReference<>(null);

  private final MatchingService match;
  private final List<HttpContentRequest> requests;

  private final boolean profile;
  private final GeneratorOutcome outcome = new GeneratorOutcome();
  private final Map<Integer, String> etags = new HashMap<>();
  private final Map<String, String> responseHeaders = new LinkedHashMap<>();
  private final Map<String, String> responseHeadersView = Collections.unmodifiableMap(this.responseHeaders);

  public JsonResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config,
      MatchingService match, boolean profile) {
    CoreHttpRequest core = new CoreHttpRequest(req, res, config.getEnvironment());
    this.match = match;
    this.requests = GeneratorDispatch.configure(core, match);
    this.profile = profile;
  }

  public Service getService() {
    return this.match.service();
  }

  public ContentStatus getStatus() {
    return this.outcome.getStatus();
  }

  public @Nullable BerliozException getError() {
    return this.outcome.getError();
  }

  public @Nullable String getRedirectURL() {
    return this.outcome.getRedirectURL();
  }

  public Map<String, String> getHeaders() {
    return this.responseHeadersView;
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
          String localTag = retrieveETag(request);
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

    try (JsonStringBuilder jb = JsonStringBuilder.create()) {
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
      error = outcome.handleError(ex, generator);
      response = Response.status(ContentStatus.BAD_REQUEST);
    } catch (UpstreamException ex) {
      error = outcome.handleError(ex, generator);
      response = Response.status(ContentStatus.BAD_GATEWAY);
    } catch (Exception ex) {
      error = outcome.handleError(ex, generator);
      response = Response.status(ContentStatus.INTERNAL_SERVER_ERROR);
    }

    long end = System.nanoTime();
    ContentStatus generatorStatus = response.status();
    outcome.handleStatus(response, generator, service);
    GeneratorDispatch.accumulateHeaders(generator, response, this.responseHeaders);

    GeneratorListener l = listener.get();
    if (l != null) l.generate(service, generator, generatorStatus, request.getProfileEtag(), end - start);

    return new GeneratorResult(name, json, error, request.getProfileEtag(), end - start);
  }

  private String assemble(List<GeneratorResult> results, Service service) {
    if (results.isEmpty()) return "{}";

    // Direct service: generator output IS the response body — no name wrapper
    if (service.isDirect()) {
      return resolveValue(results.get(0));
    }

    // Envelope: {"name1": <json1>, "name2": <json2>} — even for a single generator
    try (JsonStringBuilder jb = JsonStringBuilder.create()) {
      jb.startObject();
      for (GeneratorResult r : results) {
        jb.fieldRaw(r.name, resolveValue(r));
      }
      if (this.profile) {
        appendProfile(jb, results);
      }
      jb.endObject();
      jb.flush();
      return jb.toString();
    }
  }

  private static String resolveValue(GeneratorResult r) {
    if (r.error != null) return errorJson(r.error);
    return r.json != null && !r.json.isEmpty() ? r.json : "null";
  }

  private static void appendProfile(JsonStringBuilder jb, List<GeneratorResult> results) {
    jb.startObject("_profile");
    for (GeneratorResult r : results) {
      jb.startObject(r.name);
      jb.field("etag", ProfileFormat.format(r.profileEtag));
      jb.field("process", ProfileFormat.format(r.profileProcess));
      jb.field("total", ProfileFormat.format(r.profileEtag + r.profileProcess));
      jb.endObject();
    }
    jb.endObject();
  }

  private String retrieveETag(HttpContentRequest request) {
    Integer key = request.order();
    String cached = this.etags.get(key);
    if (cached != null) return cached;
    BerliozGenerator generator = request.generator();
    String etag = null;
    if (generator instanceof Cacheable) {
      long start = System.nanoTime();
      etag = ((Cacheable) generator).getETag((Request) request);
      long end = System.nanoTime();
      request.setProfileEtag(end - start);
    }
    String result = etag != null ? etag : "";
    this.etags.put(key, result);
    return result;
  }

  private static String errorJson(BerliozException ex) {
    String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName();
    try (JsonStringBuilder jb = JsonStringBuilder.create()) {
      jb.startObject().field("error", msg).endObject();
      return jb.toString();
    }
  }

  private static final class GeneratorResult {
    final String name;
    final @Nullable String json;
    final @Nullable BerliozException error;
    final long profileEtag;
    final long profileProcess;

    GeneratorResult(String name, @Nullable String json, @Nullable BerliozException error,
        long profileEtag, long profileProcess) {
      this.name = name;
      this.json = json;
      this.error = error;
      this.profileEtag = profileEtag;
      this.profileProcess = profileProcess;
    }
  }

}
