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
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.error.ProblemDetails;

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
 * class collapses to the first value, matching {@code HttpRequestWrapper}'s existing behaviour for
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

  private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

  /** Bounds how much of a QUERY body is buffered; larger bodies are rejected with HTTP 413. */
  private static final int MAX_BODY_BYTES = 1024 * 1024;

  /** Bounds decoded field occurrences, including repetitions of the same field name. */
  private static final int MAX_FORM_PARAMETERS = 1_000;

  /** Avoids trusting a client-supplied Content-Length for a large eager allocation. */
  private static final int INITIAL_BODY_CAPACITY = 8192;

  private QueryBodyParameters() {}

  /**
   * Returns the parameters carried in the body of a {@code QUERY} request, parsing the body only
   * when all of the following hold: the request method is {@code QUERY}, the content type is
   * {@code application/x-www-form-urlencoded}, and the servlet engine does not already expose the
   * body parameters through {@link HttpServletRequest#getParameterMap()}.
   *
   * @param req the HTTP servlet request
   * @return the body parameters, or an empty map when none of the above apply
   * @throws HttpException with status 400 if the form body cannot be read or decoded, or status
   *                       413 if it exceeds the supported byte or parameter-count limits
   */
  public static Map<String, String> parse(HttpServletRequest req) {
    if (!"QUERY".equalsIgnoreCase(req.getMethod())) return Map.of();
    if (!isFormUrlEncoded(req.getContentType())) return Map.of();
    try {
      if (engineAlreadyExposesBody(req)) return Map.of();
    } catch (IllegalArgumentException ex) {
      throw invalidBody("Malformed URI query component on QUERY request", ex);
    }
    try {
      return decode(readBody(req));
    } catch (IOException ex) {
      throw invalidBody("Unable to read the QUERY request body", ex);
    } catch (IllegalArgumentException ex) {
      throw invalidBody("Malformed application/x-www-form-urlencoded QUERY request body", ex);
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
    long contentLength = req.getContentLengthLong();
    if (contentLength > MAX_BODY_BYTES) {
      throw queryBodyTooLarge("QUERY request body exceeds " + MAX_BODY_BYTES + " bytes");
    }
    if (contentLength == 0) return "";

    // Content-Length is client-supplied and untrusted. It can justify an early rejection above,
    // but never an eager allocation larger than this small starting buffer.
    int initialCapacity = contentLength > 0
        ? (int) Math.min(contentLength, INITIAL_BODY_CAPACITY)
        : INITIAL_BODY_CAPACITY;
    ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity);
    byte[] buffer = new byte[INITIAL_BODY_CAPACITY];
    int total = 0;
    try (InputStream in = req.getInputStream()) {
      int read;
      while ((read = in.read(buffer)) != -1) {
        total += read;
        if (total > MAX_BODY_BYTES) {
          throw queryBodyTooLarge("QUERY request body exceeds " + MAX_BODY_BYTES + " bytes");
        }
        out.write(buffer, 0, read);
      }
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string into a name-value map, collapsing
   * repeated names to their first value.
   */
  private static Map<String, String> decode(@Nullable String encoded) {
    if (encoded == null || encoded.isEmpty()) return Map.of();
    Map<String, String> result = new LinkedHashMap<>();
    int count = 0;
    int start = 0;
    while (start <= encoded.length()) {
      int end = encoded.indexOf('&', start);
      if (end < 0) end = encoded.length();
      if (end > start) {
        if (++count > MAX_FORM_PARAMETERS) {
          throw queryBodyTooLarge("QUERY form body contains more than "
              + MAX_FORM_PARAMETERS + " parameters");
        }
        int equals = encoded.indexOf('=', start);
        if (equals < 0 || equals > end) equals = end;
        String rawName = encoded.substring(start, equals);
        String rawValue = equals < end ? encoded.substring(equals + 1, end) : "";
        String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
        result.putIfAbsent(name, value);
      }
      if (end == encoded.length()) break;
      start = end + 1;
    }
    return result;
  }

  /**
   * Decodes an {@code application/x-www-form-urlencoded} string into a name to values map,
   * preserving every occurrence of a repeated name in encounter order.
   */
  private static Map<String, List<String>> decodeMulti(@Nullable String encoded) {
    if (encoded == null || encoded.isEmpty()) return Map.of();
    Map<String, List<String>> result = new LinkedHashMap<>();
    int start = 0;
    while (start <= encoded.length()) {
      int end = encoded.indexOf('&', start);
      if (end < 0) end = encoded.length();
      if (end > start) {
        int equals = encoded.indexOf('=', start);
        if (equals < 0 || equals > end) equals = end;
        String rawName = encoded.substring(start, equals);
        String rawValue = equals < end ? encoded.substring(equals + 1, end) : "";
        String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
        String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
        result.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
      }
      if (end == encoded.length()) break;
      start = end + 1;
    }
    return result;
  }

  private static HttpException queryBodyTooLarge(String detail) {
    return HttpException.of(ProblemDetails.of(ContentStatus.PAYLOAD_TOO_LARGE)
        .type("urn:berlioz:problem:query-body-too-large")
        .title("Payload Too Large")
        .detail(detail));
  }

  private static HttpException invalidBody(String detail, Exception cause) {
    return HttpException.of(ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .type("urn:berlioz:problem:invalid-query-body")
        .title("Bad Request")
        .detail(detail), cause);
  }

}
