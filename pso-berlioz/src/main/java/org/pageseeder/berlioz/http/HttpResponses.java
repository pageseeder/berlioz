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
package org.pageseeder.berlioz.http;

import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for working with HTTP servlet responses.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class HttpResponses {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponses.class);

  /**
   * HTTP date formatter for the RFC 1123 date format (e.g. {@code Sat, 01 Jan 2000 00:00:00 GMT}).
   * {@link DateTimeFormatter} is immutable and thread-safe; no synchronization is needed.
   */
  private static final DateTimeFormatter HTTP_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US)
          .withZone(ZoneId.of("GMT"));

  /**
   * Utility class.
   */
  private HttpResponses() {
  }

  /**
   * Applies the supplied headers using replace semantics.
   *
   * <p>Application-owned headers should be applied before framework-owned headers so the
   * framework can subsequently enforce its values on a name collision.
   *
   * @param response the HTTP servlet response
   * @param headers  the headers to apply
   */
  public static void setHeaders(HttpServletResponse response, Map<String, String> headers) {
    headers.forEach(response::setHeader);
  }

  /**
   * Sets the content length handling the case when the value is larger than Max Integer.
   *
   * @param response      The HTTP servlet response.
   * @param contentLength The content length to set.
   */
  public static void setContentLength(HttpServletResponse response, long contentLength) {
    if (contentLength < Integer.MAX_VALUE) {
      response.setContentLength((int)contentLength);
    } else {
      // Set the content-length as String to be able to use a long value
      response.setHeader(HttpHeaders.CONTENT_LENGTH, "" + contentLength);
    }
  }

  /**
   * Computes the byte length of {@code content} in the given charset and sets it as the
   * response's {@code Content-Length}.
   *
   * <p>If the length cannot be determined (e.g. {@code content} contains an unpaired surrogate),
   * the header is left unset so the container falls back to chunked transfer encoding, and a
   * warning is logged rather than sending a bogus negative length.
   *
   * @param response The HTTP servlet response.
   * @param content  The content that will be written to the response body.
   * @param charset  The charset the content will be encoded with.
   */
  public static void setContentLength(HttpServletResponse response, CharSequence content, Charset charset) {
    int length = Charsets.length(content, charset);
    if (length < 0) {
      LOGGER.warn("Unable to determine content length for charset {}; omitting Content-Length header", charset);
      return;
    }
    setContentLength(response, length);
  }

  /**
   * Returns an HTTP date string for the given epoch-millisecond timestamp,
   * formatted according to RFC 1123 (e.g. {@code Sat, 01 Jan 2000 00:00:00 GMT}).
   *
   * @param instant the timestamp in milliseconds since the Unix epoch.
   * @return the timestamp formatted as an HTTP date string.
   */
  public static String toHttpDate(long instant) {
    return toHttpDate(Instant.ofEpochMilli(instant));
  }

  /**
   * Returns an HTTP date string for the given instant,
   * formatted according to RFC 1123 (e.g. {@code Sat, 01 Jan 2000 00:00:00 GMT}).
   *
   * @param instant the timestamp to format.
   * @return the timestamp formatted as an HTTP date string.
   */
  public static String toHttpDate(Instant instant) {
    return HTTP_DATE_FORMATTER.format(instant);
  }

  /**
   * Indicates whether the resource is compressible (only text is compressible by default).
   *
   * @param contentType The content type (MIME).
   *
   * @return <code>true</code> if the resource is compressible;
   *         <code>false</code> otherwise.
   */
  public static boolean isCompressible(@Nullable String contentType) {
    if (contentType == null) return false;
    return contentType.startsWith("text")
        || contentType.endsWith("xml")
        || contentType.endsWith("json")
        || contentType.endsWith("javascript");
  }

  /**
   * Returns a value suitable for the {@code Allow} response header listing the given HTTP methods.
   *
   * @param methods the list of allowed HTTP methods (e.g. {@code ["GET", "HEAD", "POST"]})
   * @return a comma-separated list of the allowed methods.
   */
  @Beta public static String allow(List<String> methods) {
    StringBuilder allow = new StringBuilder();
    boolean first = true;
    for (String m : methods) {
      if (first) {
        first = false;
      } else {
        allow.append(',');
      }
      allow.append(m);
    }
    return allow.toString();
  }

  /**
   * Indicates whether the given string is a valid HTTP header field name (RFC 9110 §5.1 token).
   *
   * @param name the header name to check
   * @return {@code true} if {@code name} is a non-empty valid token
   */
  public static boolean isValidHeaderName(String name) {
    if (name.isEmpty()) return false;
    for (int i = 0; i < name.length(); i++) {
      if (!isHeaderNameChar(name.charAt(i))) return false;
    }
    return true;
  }

  private static boolean isHeaderNameChar(char c) {
    if (c >= 'a' && c <= 'z') return true;
    if (c >= 'A' && c <= 'Z') return true;
    if (c >= '0' && c <= '9') return true;
    switch (c) {
      case '!':
      case '#':
      case '$':
      case '%':
      case '&':
      case '\'':
      case '*':
      case '+':
      case '-':
      case '.':
      case '^':
      case '_':
      case '`':
      case '|':
      case '~':
        return true;
      default:
        return false;
    }
  }

  /**
   * Indicates whether the given string is a valid HTTP header field value: no control characters
   * other than horizontal tab, which rules out CR/LF header-injection payloads (RFC 9110 §5.5).
   *
   * @param value the header value to check
   * @return {@code true} if {@code value} contains no disallowed control characters
   */
  public static boolean isValidHeaderValue(String value) {
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      if ((c < 0x20 && c != '\t') || c == 0x7f) return false;
    }
    return true;
  }

}
