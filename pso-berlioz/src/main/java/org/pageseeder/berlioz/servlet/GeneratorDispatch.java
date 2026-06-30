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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.error.InvalidParameterException;
import org.pageseeder.berlioz.error.UpstreamException;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Parameter;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Package-private utilities shared between {@link XmlResponse} and {@link JsonResponse}.
 *
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.2
 */
final class GeneratorDispatch {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorDispatch.class);

  private static final AtomicReference<@Nullable GeneratorListener> listener = new AtomicReference<>(null);

  private GeneratorDispatch() {}

  /**
   * Headers that generators must not set — owned by the framework, service config, or security layer.
   * Any header in this set will be logged as a warning and dropped when a generator tries to set it.
   *
   * <p>Lookups are case-insensitive because HTTP header names are case-insensitive (RFC 7230 §3.2).</p>
   */
  static final Set<String> FRAMEWORK_HEADERS;
  static {
    TreeSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Collections.addAll(s,
        "Location",           // use Response.redirect()
        "ETag",               // use Cacheable interface
        "Last-Modified",      // framework caching concern
        "Cache-Control",      // service-level cache="" attribute
        "Expires",            // framework caching concern
        "Vary",               // framework content-negotiation concern
        "Set-Cookie",         // security layer, not generator scope
        "Content-Encoding",   // compression layer (BerliozOption.HTTP_COMPRESSION)
        "Transfer-Encoding",  // container concern
        "Server",             // container concern
        "Date"                // container concern
    );
    FRAMEWORK_HEADERS = Collections.unmodifiableSet(s);
  }

  /**
   * Creates the list of per-generator HTTP requests for a matched service.
   */
  static List<HttpContentRequest> configure(CoreHttpRequest core, MatchingService match) {
    Map<String, String> common = HttpRequestWrapper.toParameters(core.request(), match.result());
    Service service = match.service();
    List<HttpContentRequest> requests = new ArrayList<>();
    int order = 0;
    for (BerliozGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        requests.add(new HttpContentRequest(core, common, generator, service, order));
      } else {
        Map<String, String> specific = new HashMap<>(common);
        for (Parameter p : pconfig) {
          specific.put(p.name(), p.value(common));
        }
        requests.add(new HttpContentRequest(core, specific, generator, service, order));
      }
      order++;
    }
    return requests;
  }

  /**
   * Wraps any exception in a {@link BerliozException}, assigning a default error ID if absent.
   */
  static BerliozException toBerliozException(Exception ex) {
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
    if (ex instanceof UpstreamException) {
      UpstreamException ue = (UpstreamException) ex;
      String service = ue.getUpstreamService();
      String msg = service != null
          ? "Upstream service '" + service + "' unavailable: " + ue.getMessage()
          : "Upstream service unavailable: " + ue.getMessage();
      return new BerliozException(msg, ue, BerliozErrorID.UPSTREAM_ERROR);
    }
    if (ex instanceof HttpException) {
      HttpException he = (HttpException) ex;
      return new BerliozException("HTTP signal " + he.getHttpCode() + ": " + he.getMessage(),
          he, BerliozErrorID.HTTP_SIGNAL);
    }
    return new BerliozException("Unexpected exception caught", ex, BerliozErrorID.GENERATOR_ERROR_UNCHECKED);
  }

  /**
   * Applies generator response headers to the accumulator, warning and dropping any header that
   * belongs to the framework rather than generators.
   */
  static void accumulateHeaders(BerliozGenerator generator, Response response, Map<String, String> target) {
    response.headers().forEach((name, value) -> {
      if (FRAMEWORK_HEADERS.contains(name)) {
        LOGGER.warn("Generator {} set header '{}' which is managed by the framework — ignoring",
            generator.getClass().getName(), name);
      } else {
        target.put(name, value);
      }
    });
  }

  @Beta
  static void setListener(@Nullable GeneratorListener l) {
    listener.set(l);
  }

  @Beta
  static @Nullable GeneratorListener getListener() {
    return listener.get();
  }

  static boolean useProblemFormat() {
    return GlobalSettings.has(BerliozOption.ERROR_PROBLEM_FORMAT);
  }

  /**
   * Returns the cached ETag for the given request, computing and caching it on first call.
   *
   * <p>Returns an empty string when the generator is not {@link Cacheable} or returns {@code null}.
   */
  static String retrieveETag(HttpContentRequest request, Map<Integer, String> etags) {
    Integer key = request.order();
    String cached = etags.get(key);
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
    etags.put(key, result);
    return result;
  }

}
