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
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Parameter;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Package-private utilities shared between {@link XmlResponse} and {@link JsonResponse}.
 */
final class GeneratorDispatch {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeneratorDispatch.class);

  private GeneratorDispatch() {}

  /**
   * Headers that generators must not set — owned by the framework, service config, or security layer.
   * Any header in this set will be logged as a warning and dropped when a generator tries to set it.
   */
  static final Set<String> FRAMEWORK_HEADERS = Set.of(
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

}
