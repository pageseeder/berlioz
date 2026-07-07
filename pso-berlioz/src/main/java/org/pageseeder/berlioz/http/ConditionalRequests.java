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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.util.EntityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Evaluates the HTTP conditional request headers ({@code If-Match}, {@code If-Modified-Since},
 * {@code If-None-Match}, {@code If-Unmodified-Since}) against an entity's metadata, updating
 * the response as required.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class ConditionalRequests {

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalRequests.class);

  /**
   * Utility class.
   */
  private ConditionalRequests() {
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
   * <p>When the entity info is available, this method should be used in the servlet as follows:
   * <pre>
   *   if (!ConditionalRequests.checkIfHeaders(request, response, info)) {
   *     return;
   *   }
   * </pre>
   *
   * @param request  The servlet request we are processing.
   * @param response The servlet response we are creating.
   * @param info     The entity information.
   *
   * @return <code>true</code> if the entity info fails to meet all the specified conditions, continue processing;
   *         <code>false</code> if any entity info meets any the specified conditions,
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
  private static boolean checkIfMatch(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
     throws IOException {

    String eTag = info.getETag();
    String headerValue = req.getHeader(HttpHeaders.IF_MATCH);
    if (headerValue != null && (headerValue.indexOf('*') == -1)) {

        StringTokenizer commaTokenizer = new StringTokenizer(headerValue, ",");
        boolean conditionSatisfied = false;

        while (!conditionSatisfied && commaTokenizer.hasMoreTokens()) {
          String currentToken = commaTokenizer.nextToken().trim();
          // Handle ETags of GZipped resources
          if (currentToken.endsWith(ETags.GZIP_ETAG_SUFFIX)) {
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
  private static boolean checkIfModifiedSince(HttpServletRequest req, HttpServletResponse res, EntityInfo info) {
    try {
      long headerValue = req.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE);
      long lastModified = info.getLastModified();
      // If an If-None-Match header has been specified, if modified since is ignored.
      if (headerValue != -1 && req.getHeader(HttpHeaders.IF_NONE_MATCH) == null && (lastModified < headerValue + 1000)) {
        // The entity has not been modified since the date specified by the client. This is not an error case.
        res.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
        String etag = info.getETag();
        // Use the GZIP ETag for compressible resources
        if (HttpResponses.isCompressible(info.getMimeType()) && HttpRequests.acceptsGZipCompression(req)) {
          etag = ETags.getETagForGZip(etag);
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
  private static boolean checkIfNoneMatch(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
      throws IOException {
    String eTag = info.getETag();
    String headerValue = req.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (headerValue == null) return true;

    Boolean gzipMatch = findETagMatch(headerValue, eTag);
    if (gzipMatch == null) return true;

    // For GET and HEAD respond with 304 Not Modified; for all other methods, 412 Precondition Failed.
    if ("GET".equals(req.getMethod()) || "HEAD".equals(req.getMethod())) {
      String responseETag = Boolean.TRUE.equals(gzipMatch) ? ETags.getETagForGZip(eTag) : eTag;
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
      if (token.endsWith(ETags.GZIP_ETAG_SUFFIX)) {
        String baseToken = token.substring(0, token.length() - ETags.GZIP_ETAG_SUFFIX.length()) + '"';
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
  private static boolean checkIfUnmodifiedSince(HttpServletRequest req, HttpServletResponse res, EntityInfo info)
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

}
