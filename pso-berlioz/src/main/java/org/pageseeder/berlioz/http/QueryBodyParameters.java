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
package org.pageseeder.berlioz.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emulates {@code application/x-www-form-urlencoded} body-parameter parsing for the HTTP
 * {@link HttpMethod#QUERY} method on servlet engines that do not natively support it yet.
 *
 * <p>The Servlet API predates {@code QUERY}, so most containers only parse the request body into
 * parameters for {@code POST}; for every other method, {@link HttpServletRequest#getParameterMap()}
 * only ever reflects the URL query string. This is a self-limiting stopgap, not a configurable
 * feature: it first checks whether the running engine is an exception to that — i.e. whether it
 * already exposes body parameters through {@code getParameterMap()} — and only reads the body
 * itself when it clearly does not, and only for {@code application/x-www-form-urlencoded} bodies.
 * Once a container adds native {@code QUERY} support, this class becomes a no-op for it
 * automatically, with no configuration change required.
 *
 * <p>Reading the body does consume it, so a {@code QUERY} generator that wants to read a
 * {@code application/x-www-form-urlencoded} body itself (e.g. for multi-valued fields, which this
 * class collapses to the last value, matching {@code HttpRequestWrapper}'s existing behaviour for
 * query-string and POST parameters) will find it already drained. A generator using any other
 * content type is untouched.
 *
 * <p><b>Precedence when a name appears in both the URL and the body:</b> neither RFC 3986/9110
 * (the URI and HTTP standards, which treat a query string as an opaque string with no defined
 * "parameter" concept) nor the HTTP QUERY method draft (which treats the body as ordinary,
 * media-type-defined content and says nothing about reconciling it with the URI's query
 * component) specify an answer. This is a Berlioz policy choice, not a standard requirement: the
 * caller ({@link org.pageseeder.berlioz.servlet.HttpRequestWrapper#toParameters}) merges these
 * body parameters first, so same-named URL query-string parameters — and, above those, URI
 * template path variables — always take precedence.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.1
 * @since 0.14.1
 */
public final class QueryBodyParameters {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryBodyParameters.class);

  private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

  /** Bounds how much of a QUERY body is buffered; larger bodies are not parsed as parameters. */
  private static final int MAX_BODY_BYTES = 1024 * 1024;

  private QueryBodyParameters() {}

  /**
   * Returns the parameters carried in the body of a {@code QUERY} request, parsing the body only
   * when all of the following hold: the request method is {@code QUERY}, the content type is
   * {@code application/x-www-form-urlencoded}, and the servlet engine does not already expose the
   * body parameters through {@link HttpServletRequest#getParameterMap()}.
   *
   * @param req the HTTP servlet request
   * @return the body parameters, or an empty map when none of the above apply
   */
  public static Map<String, String> parse(HttpServletRequest req) {
    if (!"QUERY".equalsIgnoreCase(req.getMethod())) return Map.of();
    if (!isFormUrlEncoded(req.getContentType())) return Map.of();
    if (engineAlreadyExposesBody(req)) return Map.of();
    try {
      return decode(readBody(req));
    } catch (IOException | IllegalArgumentException ex) {
      LOGGER.warn("Unable to parse QUERY request body for {}", req.getRequestURI(), ex);
      return Map.of();
    }
  }

  /**
   * @return {@code true} if {@code contentType} is {@code application/x-www-form-urlencoded},
   *         ignoring case and any parameters (e.g. {@code charset}).
   */
  private static boolean isFormUrlEncoded(@Nullable String contentType) {
    if (contentType == null) return false;
    int semicolon = contentType.indexOf(';');
    String type = (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).strip();
    return FORM_URLENCODED.equalsIgnoreCase(type);
  }

  /**
   * Detects native {@code QUERY} body support: {@code true} when {@code getParameterMap()}
   * contains a parameter whose values (including repeated occurrences of the same name in the
   * URL query string, e.g. {@code ?tag=a&tag=b}) do not match a plain parse of the URL query
   * string alone, meaning the engine must have parsed the body itself to produce them.
   */
  private static boolean engineAlreadyExposesBody(HttpServletRequest req) {
    Map<String, List<String>> fromQueryString = decodeMulti(req.getQueryString());
    for (Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
      List<String> expected = fromQueryString.getOrDefault(entry.getKey(), List.of());
      if (!expected.equals(Arrays.asList(entry.getValue()))) return true;
    }
    return false;
  }

  /**
   * Reads the full request body as UTF-8, refusing to buffer more than {@link #MAX_BODY_BYTES}.
   */
  private static String readBody(HttpServletRequest req) throws IOException {
    // Content-Length is client-supplied and untrusted: clamp it so a forged large value can't
    // force an oversized allocation before the byte-counted loop below gets to enforce the cap.
    int contentLength = req.getContentLength();
    int initialCapacity = contentLength > 0 ? Math.min(contentLength, MAX_BODY_BYTES) : 8192;
    ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity);
    byte[] buffer = new byte[8192];
    int total = 0;
    try (InputStream in = req.getInputStream()) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > MAX_BODY_BYTES) {
          LOGGER.warn("QUERY request body for {} exceeds {} bytes; not parsed as parameters",
              req.getRequestURI(), MAX_BODY_BYTES);
          return "";
        }
        out.write(buffer, 0, read);
      }
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string into a name-value map, collapsing
   * repeated names to their last value.
   */
  private static Map<String, String> decode(@Nullable String encoded) {
    Map<String, String> result = new LinkedHashMap<>();
    decodeMulti(encoded).forEach((name, values) -> result.put(name, values.get(values.size() - 1)));
    return result;
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string into a name to values map,
   * preserving every occurrence of a repeated name in encounter order.
   */
  private static Map<String, List<String>> decodeMulti(@Nullable String encoded) {
    if (encoded == null || encoded.isEmpty()) return Map.of();
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (String pair : encoded.split("&")) {
      if (pair.isEmpty()) continue;
      int equals = pair.indexOf('=');
      String rawName = equals >= 0 ? pair.substring(0, equals) : pair;
      String rawValue = equals >= 0 ? pair.substring(equals + 1) : "";
      String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
      String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
      result.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }
    return result;
  }

}
