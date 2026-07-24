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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

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
 * <p>The body is always decoded as UTF-8; a declared {@code charset} parameter other than UTF-8
 * is rejected with a 400 rather than silently mis-decoded, since supporting other encodings is
 * out of scope for this class.
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
   * @throws HttpException with status 400 if the form body cannot be read or decoded, or a
   *                       non-UTF-8 charset is declared, or status 413 if it exceeds the
   *                       supported byte or parameter-count limits
   */
  public static Map<String, String> parse(HttpServletRequest req) {
    if (!"QUERY".equalsIgnoreCase(req.getMethod())) return Map.of();
    String contentType = req.getContentType();
    if (!isFormUrlEncoded(contentType)) return Map.of();
    requireUtf8Charset(contentType);
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
   * Rejects a QUERY body whose {@code Content-Type} declares a charset other than UTF-8.
   *
   * <p>This class only ever decodes the body as UTF-8 (see {@link #readBody} and
   * {@link #parsePair}); accepting a declared mismatch would silently mis-decode the body
   * instead of reporting it. A missing charset parameter defaults to UTF-8 and is accepted.
   *
   * @param contentType the request's {@code Content-Type}, already confirmed to be
   *                     {@code application/x-www-form-urlencoded} by {@link #isFormUrlEncoded}
   * @throws HttpException with status 400 if a non-UTF-8 or unrecognised charset is declared
   */
  private static void requireUtf8Charset(String contentType) {
    int semicolon = contentType.indexOf(';');
    if (semicolon < 0) return;
    for (String param : contentType.substring(semicolon + 1).split(";")) {
      int equals = param.indexOf('=');
      if (equals < 0 || !"charset".equalsIgnoreCase(param.substring(0, equals).strip())) continue;
      String declared = param.substring(equals + 1).strip().replaceAll("^\"|\"$", "");
      Charset charset;
      try {
        charset = Charset.forName(declared);
      } catch (IllegalArgumentException ex) {
        throw invalidBody("QUERY body declares an unsupported charset: " + declared, ex);
      }
      if (!charset.equals(StandardCharsets.UTF_8)) {
        throw invalidBody("QUERY body charset must be UTF-8, got: " + declared,
            new IllegalArgumentException("Unsupported charset: " + declared));
      }
    }
  }

  /**
   * Detects native {@code QUERY} body support: {@code true} when {@code getParameterMap()}
   * exposes more parameter occurrences than the URL query string contains, meaning the engine
   * must have parsed the body and aggregated its parameters itself.
   *
   * <p>This deliberately compares counts rather than decoded names and values. Query-string
   * decoding is container-specific and may use a different character encoding from the UTF-8
   * form-body policy used here.
   */
  private static boolean engineAlreadyExposesBody(HttpServletRequest req) {
    int queryParameterCount = countQueryParameters(req.getQueryString());
    int exposedParameterCount = 0;
    for (String[] values : req.getParameterMap().values()) {
      exposedParameterCount += values.length;
      if (exposedParameterCount > queryParameterCount) return true;
    }
    return false;
  }

  /**
   * Counts non-empty ampersand-delimited query parameter occurrences without decoding them.
   *
   * <p>Percent escapes are still validated so malformed query components retain the existing
   * HTTP 400 behaviour.
   */
  private static int countQueryParameters(@Nullable String encoded) {
    if (encoded == null || encoded.isEmpty()) return 0;
    int count = 0;
    boolean hasContent = false;
    for (int i = 0; i < encoded.length(); i++) {
      char c = encoded.charAt(i);
      if (c == '&') {
        if (hasContent) count++;
        hasContent = false;
      } else {
        hasContent = true;
        if (c == '%') {
          if (i + 2 >= encoded.length()
              || Character.digit(encoded.charAt(i + 1), 16) < 0
              || Character.digit(encoded.charAt(i + 2), 16) < 0) {
            throw new IllegalArgumentException("Invalid percent escape in URI query component");
          }
          i += 2;
        }
      }
    }
    return hasContent ? count + 1 : count;
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
    Map<String, String> result = new LinkedHashMap<>();
    if (encoded == null || encoded.isEmpty()) return result;
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
        String[] pair = parsePair(encoded, start, end);
        result.putIfAbsent(pair[0], pair[1]);
      }
      if (end == encoded.length()) break;
      start = end + 1;
    }
    return result;
  }

  /**
   * Decodes the {@code name=value} segment of {@code encoded} delimited by {@code [start, end)}.
   */
  private static String[] parsePair(String encoded, int start, int end) {
    int equals = encoded.indexOf('=', start);
    if (equals < 0 || equals > end) equals = end;
    String rawName = encoded.substring(start, equals);
    String rawValue = equals < end ? encoded.substring(equals + 1, end) : "";
    String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8);
    String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
    return new String[] {name, value};
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
