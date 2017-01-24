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
package org.pageseeder.berlioz.content;

/**
 * An enumeration of status codes supported by Berlioz generators.
 *
 * <p>These are based on HTTP response code, and are used to determine the HTTP code that will be
 * returned by the service.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.10 - 3 December 2012
 * @since Berlioz 0.8.2
 */
public enum ContentStatus {

  // Successful 2xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The request has succeeded.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.1">HTTP/1.1 - 6.3.1. 200 OK</a>
   */
  OK(200),

  /**
   * The request has been fulfilled and resulted in a new resource being created.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.2">HTTP/1.1 - 6.3.2. 201 Created</a>
   */
  CREATED(201),

  /**
   * The request has been accepted for processing, but the processing has not been completed.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.3">HTTP/1.1 - 6.3.3. 202 Accepted</a>
   */
  ACCEPTED(202),

  /**
   * The request was successful but the enclosed payload has been modified from that of the
   * origin server's 200 OK response by a transforming proxy.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.4">HTTP/1.1 - 6.3.4. 203 Non-Authoritative Information</a>
   */
  NON_AUTHORITATIVE_INFORMATION(203),

  /**
   * The server has fulfilled the request but does not need to return any content.
   *
   * <p>Berlioz will not send any content with the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.5">HTTP/1.1 - 6.3.5. 204 No Content</a>
   */
  NO_CONTENT(204),

  /**
   * The server has fulfilled the request and the user agent SHOULD reset the document view which
   * caused the request to be sent.
   *
   * <p>Berlioz will not send any content with the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.3.6">HTTP/1.1 - 6.3.6. 205 Reset Content</a>
   */
  RESET_CONTENT(205),

  // Redirection 3xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The 300 (Multiple Choices) status code indicates that the target resource has more than one
   * representation, each with its own more specific identifier, and information about the
   * alternatives is being provided so that the user (or user agent) can select a preferred
   * representation by redirecting its request to one or more of those identifiers.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.1">HTTP/1.1 - 6.4.1. 301 Moved Permanently</a>
   */
  MULTIPLE_CHOICE(300),

  /**
   * The requested resource has been assigned a new permanent URI and any future references to this
   * resource SHOULD use one of the returned URIs.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.2">HTTP/1.1 - 6.4.2. 301 Moved Permanently</a>
   */
  MOVED_PERMANENTLY(301),

  /**
   * The requested resource resides temporarily under a different URI.
   *
   * <p>Since the redirection might be altered on occasion, the client SHOULD continue to use the
   * Request-URI for future requests.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.3">HTTP/1.1 - 6.4.3. 302 Found</a>
   */
  FOUND(302),

  /**
   * The response to the request can be found under a different URI and SHOULD be retrieved using a
   * GET method on that resource.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.4">HTTP/1.1 - 6.4.4. 303 See Other</a>
   */
  SEE_OTHER(303),

  /**
   * The requested resource resides temporarily under a different URI.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.4.7">HTTP/1.1 - 6.4.7. 307 Temporary Redirect</a>
   */
  TEMPORARY_REDIRECT(307),

  // Client Error 4xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The request could not be understood by the server due to malformed syntax.
   *
   * <p>The client SHOULD NOT repeat the request without modifications.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.1">HTTP/1.1 - 6.5.1. 400 Bad Request</a>
   */
  BAD_REQUEST(400),

  /**
   * The server understood the request, but is refusing to fulfill it.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.3">HTTP/1.1 - 6.5.3. 403 Forbidden</a>
   */
  FORBIDDEN(403),

  /**
   * The server has not found anything matching the Request-URI.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.4">HTTP/1.1 - 6.5.4. 404 Not Found</a>
   */
  NOT_FOUND(404),

  /**
   * The target resource does not have a current representation that would be acceptable to the
   * user agent and the server is unwilling to supply a default representation.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.6">HTTP/1.1 - 6.5.6. 406 Not Acceptable</a>
   */
  NOT_ACCEPTABLE(406),

  /**
   * The server did not receive a complete request message within the time that it was prepared to wait.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.7">HTTP/1.1 - 6.5.7. 408 Request Timeout</a>
   */
  REQUEST_TIMEOUT(408),

  /**
   * The request could not be completed due to a conflict with the current state of the resource.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.8">HTTP/1.1 - 6.5.8. 409 Conflict</a>
   */
  CONFLICT(409),

  /**
   * The requested resource is no longer available at the server and no forwarding address is known.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.9">HTTP/1.1 - 6.5.9. 410 Gone</a>
   */
  GONE(410),

  /**
   * The server refuses to accept the request without a defined Content-Length.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.10">HTTP/1.1 - 6.5.10. 411 Length Required</a>
   */
  LENGTH_REQUIRED(411),

  /**
   * The server refuses to process a request because the request payload is larger than the server
   * is willing or able to process.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.11">HTTP/1.1 - 6.5.11. 413 Payload Too Large</a>
   */
  PAYLOAD_TOO_LARGE(413),

  /**
   * The server refuses to service the request because the request-target is longer than the server
   * is willing to interpret.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.12">HTTP/1.1 - 6.5.12. 414 URI Too Long</a>
   */
  URI_TOO_LONG(414),

  /**
   * The server refuses to service the request because the payload is in a format not supported by
   * this method on the target resource.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.5.13">HTTP/1.1 - 6.5.13. 415 Unsupported Media Type</a>
   */
  UNSUPPORTED_MEDIA_TYPE(415),

  /**
   * The server understands the content type of the request entity (hence a 415 Unsupported Media
   * Type status code is inappropriate), and the syntax of the request entity is correct (thus a 400 Bad
   * Request status code is inappropriate) but was unable to process the contained instructions.
   *
   * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.2">WebDAV - 11.2. 422 Unprocessable Entity</a>
   */
  UNPROCESSABLE_ENTITY(422),

  /**
   * The source or destination resource of a method is locked.
   *
   * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.3">WebDAV - 11.3. 423 Locked</a>
   */
  LOCKED(423),

  /**
   * The method could not be performed on the resource because the requested action depended on another
   * action and that action failed.
   *
   * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.4">WebDAV - 11.4. 424 Failed Dependency</a>
   */
  FAILED_DEPENDENCY(424),

  /**
   * The server requires the request to be conditional.
   *
   * Its typical use is to avoid the "lost update" problem, where a client
   * GETs a resource's state, modifies it, and PUTs it back to the server,
   * when meanwhile a third party has modified the state on the server,
   * leading to a conflict.  By requiring requests to be conditional, the
   * server can assure that clients are working with the correct copies.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6585#section-3">Additional HTTP Status Codes - 3. 428 Precondition Required</a>
   */
  PRECONDITION_REQUIRED(428),

  /**
   * The user has sent too many requests in a given amount of time ("rate limiting").
   *
   * @see <a href="https://tools.ietf.org/html/rfc6585#section-4">Additional HTTP Status Codes - 4. 429 Too Many Requests</a>
   */
  TOO_MANY_REQUESTS(429),

  /**
   * The server is unwilling to process the request because its header fields are too large.
   *
   * @see <a href="https://tools.ietf.org/html/rfc6585#section-5">Additional HTTP Status Codes - 5. 431 Request Header Fields Too Large</a>
   */
  REQUEST_HEADER_FIELDS_TOO_LARGE(431),

  /**
   * The server is denying access to the resource as a consequence of a legal demand.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7725#section-3">An HTTP Status Code to Report Legal Obstacles</a>
   */
  UNAVAILABLE_FOR_LEGAL_REASONS(451),

  // Server Error 5xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The server encountered an unexpected condition which prevented it from fulfilling the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.1">HTTP/1.1 - 6.6.1. 500 Internal Server Error</a>
   */
  INTERNAL_SERVER_ERROR(500),

  /**
   * The server does not support the functionality required to fulfill the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.2">HTTP/1.1 - 6.6.2. 501 Not Implemented</a>
   */
  NOT_IMPLEMENTED(501),

  /**
   * The server, while acting as a gateway or proxy, received an invalid response from the upstream
   * server it accessed in attempting to fulfill the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.3">HTTP/1.1 - 6.6.3. 502 Bad Gateway</a>
   */
  BAD_GATEWAY(502),

  /**
   * The server is currently unable to handle the request due to a temporary overloading or
   * maintenance of the server.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.4">HTTP/1.1 - 6.6.4. 503 Service Unavailable</a>
   */
  SERVICE_UNAVAILABLE(503),

  /**
   * The server, while acting as a gateway or proxy, did not receive a timely response from the
   * upstream server specified by the URI or some other auxiliary server it needed to access in
   * attempting to complete the request.
   *
   * @see <a href="https://tools.ietf.org/html/rfc7231#section-6.6.5">HTTP/1.1 - 6.6.5. 504 Gateway Timeout</a>
   */
  GATEWAY_TIMEOUT(504),

  /**
   * The method could not be performed on the resource because the server is unable to store
   * the representation needed to successfully complete the request
   *
   * @see <a href="https://tools.ietf.org/html/rfc4918#section-11.5">WebDAV - 6.6.5. 507 Insufficient Storage</a>
   */
  INSUFFICIENT_STORAGE(507);

  /**
   * The corresponding HTTP code value.
   */
  private final int _code;

  /**
   * The corresponding string value.
   */
  private final String _string;

  /**
   * Creates a new code.
   * @param code the corresponding HTTP response code.
   */
  private ContentStatus(int code) {
   this._code = code;
   this._string = name().toLowerCase().replace('_', '-');
  }

  /**
   * The HTTP response code for this enum value.
   * @return The HTTP response code for this enum value.
   */
  public int code() {
    return this._code;
  }

  /**
   * Returns the the content status corresponding to the specified HTTP status code.
   *
   * @param code The HTTP code.
   * @return the corresponding enum constant or <code>null</code>.
   *
   * @since Berlioz 0.8.3
   */
  public static ContentStatus forCode(int code) {
    for (ContentStatus status : values()) {
      // First match (all content status have a different HTTP code)
      if (status.code() == code) return status;
    }
    // Could not be found.
    return null;
  }

  /**
   * Indicates whether the specified status corresponds to an HTTP redirect code.
   *
   * @param status The  content status
   * @return <code>true</code> if the content status greater than or equal to 300 and less than 400;
   *         <code>false</code> otherwise.
   *
   * @since Berlioz 0.9.10
   */
  public static boolean isRedirect(ContentStatus status) {
    return status._code >= 300 && status._code < 400;
  }

  /**
   * Returns the status as a string for use in the XML.
   *
   * <p>The string representation is always XML safe (does not need to be escaped) and corresponds
   * to the name of the enum value as lower case and using '-' instead of '_' between words.
   *
   * @return the status as a string.
   */
  @Override
  public String toString() {
    return this._string;
  }
}
