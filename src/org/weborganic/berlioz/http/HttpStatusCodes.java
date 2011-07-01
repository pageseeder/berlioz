package org.weborganic.berlioz.http;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

/**
 * A utility class for HTTP Status codes.
 * 
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10">HTTP/1.1 - 10 Status Code Definitions</a>
 * 
 * @author Christophe Lauret
 * @version 1 July 2011
 */
public final class HttpStatusCodes {

  /**
   * The default titles to use for the various HTTP Codes (reusing titles defined by RFC 2616)
   */
  private static final Map<Integer, String> HTTP_CODE_TITLE = new HashMap<Integer, String>();
  static {
    // Informational 1xx
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_CONTINUE, "Continue");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_SWITCHING_PROTOCOLS, "Switching Protocols");
    // Successful 2xx
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_OK, "OK");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_CREATED, "Created");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_ACCEPTED, "Accepted");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION, "Non-Authoritative Information");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NO_CONTENT, "No Content");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_RESET_CONTENT, "Reset Content");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_PARTIAL_CONTENT, "Partial Content");
    // Redirection 3xx
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_MULTIPLE_CHOICES, "Multiple Choices");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_MOVED_PERMANENTLY, "Moved Permanently");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_FOUND, "Found");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_SEE_OTHER, "See Other");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NOT_MODIFIED, "Not Modified");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_USE_PROXY, "Use Proxy");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_TEMPORARY_REDIRECT, "Temporary Redirect");
    // Client Error 4xx
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_PAYMENT_REQUIRED, "Payment Required");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NOT_FOUND, "Not Found");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NOT_ACCEPTABLE, "Not Acceptable");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED, "Proxy Authentication Required");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_REQUEST_TIMEOUT, "Request Timeout");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_CONFLICT, "Conflict");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_GONE, "Gone");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_LENGTH_REQUIRED, "Length Required");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_PRECONDITION_FAILED, "Precondition Failed");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Request Entity Too Large");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_REQUEST_URI_TOO_LONG, "Request-URI Too Long");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE, "Requested Range Not Satisfiable");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_EXPECTATION_FAILED, "Expectation Failed");
    // Server Error 5xx
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not Implemented");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_BAD_GATEWAY, "Bad Gateway");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service Unavailable");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_GATEWAY_TIMEOUT, "Gateway Timeout");
    HTTP_CODE_TITLE.put(HttpServletResponse.SC_HTTP_VERSION_NOT_SUPPORTED, "HTTP Version Not Supported");
  }

  /**
   * Utility class.
   */
  private HttpStatusCodes() {
  }

  /**
   * Returns the title for the specified code based on the name defined in the RFC.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10">HTTP/1.1 - 10 Status Code Definitions</a>
   * 
   * @param code the HTTP status code.
   * @return the corresponding title as defined in RFC 2616 or <code>null</code> if the code does not exist. 
   */
  public static String getTitle(int code) {
    return HTTP_CODE_TITLE.get(Integer.valueOf(code));
  }

  /**
   * Returns the class of the HTTP status code based on the class defined in the RFC.
   * 
   * <p>Will return for range:
   * <ul>
   *   <li>100-199: <code>"Informational"</code></li>
   *   <li>200-299: <code>"Successful"</code></li>
   *   <li>300-399: <code>"Redirection"</code></li>
   *   <li>400-499: <code>"Client Error"</code></li>
   *   <li>500-599: <code>"Server Error"</code></li>
   * </ul>
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-10">HTTP/1.1 - 10 Status Code Definitions</a>
   *
   * @param code the HTTP status code.
   * @return the class of the HTTP status code based on the class defined in the RFC;
   *         or <code>null</code> if code is out of range (less than 100 or greater than 599)
   */
  public static String getClassOfStatus(int code) {
    int x = code / 100;
    switch (x) {
      case 1: return "Informational";
      case 2: return "Successful";
      case 3: return "Redirection";
      case 4: return "Client Error";
      case 5: return "Server Error";
      default:
        return null;
    }
  }
}
