/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * An enumeration of status codes supported by Berlioz generators.
 *
 * <p>These are based on HTTP response code, and are used to determine the HTTP code that will be
 * returned by the service.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.3 - 27 June 2011
 * @since Berlioz 0.8.2
 */
public enum ContentStatus {

  // Successful 2xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The request has succeeded.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.1">HTTP/1.1 - 10.2.1 200 OK</a>
   */
  OK(200),

  /**
   * The request has been fulfilled and resulted in a new resource being created.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.2">HTTP/1.1 - 10.2.2 201 CREATED</a>
   */
  CREATED(201),

  /**
   * The request has been accepted for processing, but the processing has not been completed.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.3">HTTP/1.1 - 10.2.3 202 ACCEPTED</a>
   */
  ACCEPTED(202),

  /**
   * The server has fulfilled the request but does not need to return any content.
   *
   * <p>Berlioz will not send any content with the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.5">HTTP/1.1 - 10.2.5 204 No Content</a>
   */
  NO_CONTENT(204),

  /**
   * The server has fulfilled the request and the user agent SHOULD reset the document view which
   * caused the request to be sent.
   *
   * <p>Berlioz will not send any content with the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.2.6">HTTP/1.1 - 10.2.6 205 Reset Content</a>
   */
  RESET_CONTENT(205),

  // Redirection 3xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The requested resource has been assigned a new permanent URI and any future references to this
   * resource SHOULD use one of the returned URIs.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1 - 10.3.2 301 Moved Permanently</a>
   */
  MOVED_PERMANENTLY(301),

  /**
   * The requested resource resides temporarily under a different URI.
   *
   * <p>Since the redirection might be altered on occasion, the client SHOULD continue to use the
   * Request-URI for future requests.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1 - 10.3.3 302 Found</a>
   */
  FOUND(302),

  /**
   * The response to the request can be found under a different URI and SHOULD be retrieved using a
   * GET method on that resource.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.4">HTTP/1.1 - 10.3.4 303 See Other</a>
   */
  SEE_OTHER(303),

  /**
   * The requested resource resides temporarily under a different URI.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.8">HTTP/1.1 - 10.3.8 307 Temporary Redirect</a>
   */
  TEMPORARY_REDIRECT(307),

  // Client Error 4xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The request could not be understood by the server due to malformed syntax.
   *
   * <p>The client SHOULD NOT repeat the request without modifications.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.1">HTTP/1.1 - 10.4.1 400 Bad Request</a>
   */
  BAD_REQUEST(400),

  /**
   * The server understood the request, but is refusing to fulfill it.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.4">HTTP/1.1 - 10.4.4 403 Forbidden</a>
   */
  FORBIDDEN(403),

  /**
   * The server has not found anything matching the Request-URI.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.5">HTTP/1.1 - 10.4.5 404 Not Found</a>
   */
  NOT_FOUND(404),

  /**
   * The request could not be completed due to a conflict with the current state of the resource.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.10">HTTP/1.1 - 10.4.10 409 Conflict</a>
   */
  CONFLICT(409),

  /**
   * The requested resource is no longer available at the server and no forwarding address is known.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.4.11">HTTP/1.1 - 10.4.11 410 Gone</a>
   */
  GONE(410),

  // Server Error 5xx
  // ----------------------------------------------------------------------------------------------

  /**
   * The server encountered an unexpected condition which prevented it from fulfilling the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.1">HTTP/1.1 - 10.5.1 500 Internal Server Error</a>
   */
  INTERNAL_SERVER_ERROR(500),

  /**
   * The server does not support the functionality required to fulfill the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.2">HTTP/1.1 - 10.2.6 501 Not Implemented</a>
   */
  NOT_IMPLEMENTED(501),

  /**
   * The server, while acting as a gateway or proxy, received an invalid response from the upstream
   * server it accessed in attempting to fulfill the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.3">HTTP/1.1 - 10.5.3 502 Bad Gateway</a>
   */
  BAD_GATEWAY(502),

  /**
   * The server is currently unable to handle the request due to a temporary overloading or
   * maintenance of the server.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.4">HTTP/1.1 - 10.5.4 503 Service Unavailable</a>
   */
  SERVICE_UNAVAILABLE(503),

  /**
   * The server, while acting as a gateway or proxy, did not receive a timely response from the
   * upstream server specified by the URI or some other auxiliary server it needed to access in
   * attempting to complete the request.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.5.5">HTTP/1.1 - 10.2.6 504 Gateway Timeout</a>
   */
  GATEWAY_TIMEOUT(504);

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
   this._string = this.name().toLowerCase().replace('_', '-');
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
