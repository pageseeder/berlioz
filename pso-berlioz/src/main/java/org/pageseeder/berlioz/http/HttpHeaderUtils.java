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

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.util.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for HTTP headers.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.6
 */
public final class HttpHeaderUtils {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderUtils.class);

  private static final String GZIP_ETAG_SUFFIX = "-gzip\"";

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
  private HttpHeaderUtils() {
  }

  /**
   * Check if the conditions specified in the optional If headers require further processing from the servlet.
   *
   * <p>If any of the conditions are met, the HTTP response headers will be updated.
   *
   * <p>The following conditional headers are checked:
   * <ul>
   *   <li><code>If-Match</code></li>
   *   <li><code>If-Modified-Since</code></li>
   *   <li><code>If-None-Match</code></li>
   *   <li><code>If-Unmodified-Since</code></li>
   * </ul>
   *
   * <p>When an entity info is available, this method should be used in the servlet as follows:
   * <pre>
   *   if (!HttpHeaderUtils.checkIfHeaders(request, response, info)) {
   *     return;
   *   }
   * </pre>
   *
   * @param request  The servlet request we are processing.
   * @param response The servlet response we are creating.
   * @param info     The entity information.
   *
   * @return <code>true</code> if the entity info fail to meet all the specified conditions, continue processing;
   *         <code>false</code> if the any entity info meets any the specified conditions,
   *         further processing is unnecessary.
   *
   * @throws IOException If thrown during checking.
   */
  public static boolean checkIfHeaders(HttpServletRequest request, HttpServletResponse response, EntityInfo info)
      throws IOException {
    return checkIfMatch(request, response, info)
        && checkIfModifiedSince(request, response, info)
        && checkIfNoneMatch(request, response, info)
        && checkIfUnmodifiedSince(request, response, info);
  }

  /**
   * Check if the <code>If-Match</code> condition is satisfied.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.24">HTTP/1.1 - 14.24 If-Match</a>
   *
   * @param req  The servlet request we are processing
   * @param res  The servlet response we are creating
   * @param info Resource metadata
   *
   * @return <code>true</code> if the resource meets the specified condition;
   *         <code>false</code> if the condition is not satisfied, in which case request processing is stopped.
   *
   * @throws IOException If thrown while setting the response status code.
   */
  protected static boolean checkIfMatch(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
     throws IOException {

    String eTag = info.getETag();
    String headerValue = req.getHeader(HttpHeaders.IF_MATCH);
    if (headerValue != null && (headerValue.indexOf('*') == -1)) {

        StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
        boolean conditionSatisfied = false;

        while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
          String currentToken = commaTokenizer.nextToken().trim();
          // Handle ETags of GZipped resources
          if (currentToken.endsWith(GZIP_ETAG_SUFFIX)) {
            currentToken = currentToken.substring(0, currentToken.length()-6) +'\"';
          }
          if (currentToken.equals(eTag)) {
            conditionSatisfied = true;
          }
        }

        // If none of the given ETags match, 412 Precondition failed is sent back
        if (!conditionSatisfied) {
          res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
          return false;
        }


    }
    return true;
  }

  /**
   * Check if the <code>If-Modified-Since</code> condition is satisfied.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.25">HTTP/1.1 - 14.25 If-Modified-Since</a>
   *
   * @param req  The servlet request we are processing
   * @param res  The servlet response we are creating
   * @param info Resource metadata
   *
   * @return <code>true</code> if the resource meets the specified condition;
   *         <code>false</code> if the condition is not satisfied, in which case request processing is stopped.
   *
   */
  protected static boolean checkIfModifiedSince(HttpServletRequest req, HttpServletResponse res, EntityInfo info) {
    try {
      long headerValue = req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
      long lastModified = info.getLastModified();
      // If an If-None-Match header has been specified, if modified since is ignored.
      if (headerValue != -1 && req.getHeader(HttpHeaders.IF_NONE_MATCH) == null && (lastModified < headerValue + 1000)) {
        // The entity has not been modified since the date specified by the client. This is not an error case.
        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        String etag = info.getETag();
        // Use the GZIP ETag for compressible resources
        if (isCompressible(info.getMimeType()) && acceptsGZipCompression(req)) {
          etag = getETagForGZip(etag);
        }
        res.setHeader(HttpHeaders.ETAG, etag);
        LOGGER.debug("If-Modified-Since check: NOT MODIFIED, etag={}", etag);
        return false;
      }
    } catch (IllegalArgumentException ex) {
      // If the header value can't be converted to a date
      return true;
    }
    return true;
  }

  /**
   * Check if the <code>If-None-Match</code> condition is satisfied.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.26">HTTP/1.1 - 14.26 If-None-Match</a>
   *
   * @param req  The servlet request we are processing
   * @param res  The servlet response we are creating
   * @param info Resource metadata
   *
   * @return <code>true</code> if the resource meets the specified condition;
   *         <code>false</code> if the condition is not satisfied, in which case request processing is stopped.
   *
   * @throws IOException If thrown while setting the response status code.
   */
  protected static boolean checkIfNoneMatch(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
      throws IOException {
    String eTag = info.getETag();
    String headerValue = req.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (headerValue == null) return true;

    Boolean gzipMatch = findETagMatch(headerValue, eTag);
    if (gzipMatch == null) return true;

    // For GET and HEAD respond with 304 Not Modified; for all other methods 412 Precondition Failed.
    if ("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) {
      String responseETag = Boolean.TRUE.equals(gzipMatch) ? getETagForGZip(eTag) : eTag;
      res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
      res.setHeader(HttpHeaders.ETAG, responseETag);
      LOGGER.debug("If-None-Match check: match etag={}", responseETag);
    } else {
      LOGGER.debug("If-None-Match check: PRECONDITION FAILED, method={}", req.getMethod());
      res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
    }
    return false;
  }

  /**
   * Searches the {@code If-None-Match} header value for a token matching the given ETag.
   *
   * @param headerValue the {@code If-None-Match} header value (non-null)
   * @param eTag        the ETag to match against
   *
   * @return {@link Boolean#TRUE} if a GZip-suffixed ETag token matched,
   *         {@link Boolean#FALSE} if a wildcard ({@code *}) or plain ETag token matched,
   *         {@code null} if no token matched.
   */
  private static @Nullable Boolean findETagMatch(String headerValue, @Nullable String eTag) {
    if ("*".equals(headerValue)) return Boolean.FALSE;
    StringTokenizer tokenizer = new StringTokenizer(headerValue, ",");
    while (tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken().trim();
      if (token.endsWith(GZIP_ETAG_SUFFIX)) {
        String baseToken = token.substring(0, token.length() - GZIP_ETAG_SUFFIX.length()) + '"';
        if (baseToken.equals(eTag)) return Boolean.TRUE;
      } else if (token.equals(eTag)) {
        return Boolean.FALSE;
      }
    }
    return null;
  }

  /**
   * Check if the <code>If-Unmodified-Since</code> condition is satisfied.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.28">HTTP/1.1 - 14.28 If-Unmodified-Since</a>
   *
   * @param req  The servlet request we are processing
   * @param res  The servlet response we are creating
   * @param info Resource metadata
   *
   * @return <code>true</code> if the resource meets the specified condition;
   *         <code>false</code> if the condition is not satisfied, in which case request processing is stopped.
   *
   * @throws IOException If thrown while setting the response status code.
   */
  protected static boolean checkIfUnmodifiedSince(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
      throws IOException {
    try {
      long lastModified = info.getLastModified();
      long headerValue = req.getDateHeader(HttpHeaders.IF_UNMODIFIED_SINCE);
      if (headerValue != -1 && (lastModified >= (headerValue + 1000))) {
          // The entity has not been modified since the date specified by the client. This is not an error case.
          res.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
          LOGGER.debug("If-Unmodified-Since check: PRECONDITION FAILED, last modified: {} >= {}", lastModified, headerValue);
          return false;

      }
    } catch (IllegalArgumentException ex) {
      return true;
    }
    return true;
  }

  /**
   * Indicates whether the client accepts GZip compression.
   *
   * @param req The HTTP servlet request.
   *
   * @return <code>true</code> if the 'Accept-Encoding' header contains "gzip";
   *         <code>false</code> otherwise.
   */
  public static boolean acceptsGZipCompression(HttpServletRequest req) {
    String encoding = req.getHeader(HttpHeaders.ACCEPT_ENCODING);
    return HttpAcceptHeader.accepts(encoding, "gzip");
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
      // Set the content-length as String to be able to use a long
      response.setHeader(HttpHeaders.CONTENT_LENGTH, "" + contentLength);
    }
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
   * Returns the entity tag for a compressed response.
   *
   * @param etag the entity tag of the response before compression.
   * @return the entity tag of the compressed response.
   */
  public static @Nullable String getETagForGZip(@Nullable String etag) {
    if (etag == null) return null;
    int q = etag.lastIndexOf("\"");
    return (q > 0)? etag.substring(0, q)+GZIP_ETAG_SUFFIX : etag;
  }

  /**
   * Returns the entity tag for an uncompressed response by stripping the GZip suffix.
   *
   * <p>For example, {@code "abc-gzip"} becomes {@code "abc"}.
   * If the ETag does not carry the GZip suffix it is returned unchanged.
   *
   * @param etag the entity tag, possibly carrying the GZip suffix.
   * @return the base entity tag without the GZip suffix, or the original ETag unchanged.
   */
  public static @Nullable String getETagForUncompressed(@Nullable String etag) {
    if (etag == null) return null;
    int q = etag.lastIndexOf(GZIP_ETAG_SUFFIX);
    return (q > 0) ? etag.substring(0, q) + '"' : etag;
  }

  /**
   * Returns an HTTP date string for the given epoch-millisecond timestamp,
   * formatted according to RFC 1123 (e.g. {@code Sat, 01 Jan 2000 00:00:00 GMT}).
   *
   * @param modified the last-modified time in milliseconds since the Unix epoch.
   * @return the timestamp formatted as an HTTP date string.
   */
  public static String toLastModified(long modified) {
    return HTTP_DATE_FORMATTER.format(Instant.ofEpochMilli(modified));
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
}
