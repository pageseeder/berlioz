package org.weborganic.berlioz.http;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for HTTP Status codes.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10">HTTP/1.1 - 10 Status Code Definitions</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.8.3 - 1 July 2011
 * @since Berlioz 0.8.3
 */
public final class HttpStatusCodes {

  /**
   * The default titles to use for the various HTTP Codes (reusing titles defined by RFC 2616)
   */
  private static final Map<Integer, String> HTTP_CODE_TITLE = new HashMap<Integer, String>();
  static {
    // Informational 1xx
    HTTP_CODE_TITLE.put(100, "Continue");
    HTTP_CODE_TITLE.put(101, "Switching Protocols");
    // Successful 2xx
    HTTP_CODE_TITLE.put(200, "OK");
    HTTP_CODE_TITLE.put(201, "Created");
    HTTP_CODE_TITLE.put(202, "Accepted");
    HTTP_CODE_TITLE.put(203, "Non-Authoritative Information");
    HTTP_CODE_TITLE.put(204, "No Content");
    HTTP_CODE_TITLE.put(205, "Reset Content");
    HTTP_CODE_TITLE.put(206, "Partial Content");
    // Redirection 3xx
    HTTP_CODE_TITLE.put(300, "Multiple Choices");
    HTTP_CODE_TITLE.put(301, "Moved Permanently");
    HTTP_CODE_TITLE.put(302, "Found");
    HTTP_CODE_TITLE.put(303, "See Other");
    HTTP_CODE_TITLE.put(304, "Not Modified");
    HTTP_CODE_TITLE.put(305, "Use Proxy");
    HTTP_CODE_TITLE.put(307, "Temporary Redirect");
    // Client Error 4xx
    HTTP_CODE_TITLE.put(400, "Bad Request");
    HTTP_CODE_TITLE.put(401, "Unauthorized");
    HTTP_CODE_TITLE.put(402, "Payment Required");
    HTTP_CODE_TITLE.put(403, "Forbidden");
    HTTP_CODE_TITLE.put(404, "Not Found");
    HTTP_CODE_TITLE.put(405, "Method Not Allowed");
    HTTP_CODE_TITLE.put(406, "Not Acceptable");
    HTTP_CODE_TITLE.put(407, "Proxy Authentication Required");
    HTTP_CODE_TITLE.put(408, "Request Timeout");
    HTTP_CODE_TITLE.put(409, "Conflict");
    HTTP_CODE_TITLE.put(410, "Gone");
    HTTP_CODE_TITLE.put(411, "Length Required");
    HTTP_CODE_TITLE.put(412, "Precondition Failed");
    HTTP_CODE_TITLE.put(413, "Request Entity Too Large");
    HTTP_CODE_TITLE.put(414, "Request-URI Too Long");
    HTTP_CODE_TITLE.put(415, "Unsupported Media Type");
    HTTP_CODE_TITLE.put(416, "Requested Range Not Satisfiable");
    HTTP_CODE_TITLE.put(417, "Expectation Failed");
    // Server Error 5xx
    HTTP_CODE_TITLE.put(500, "Internal Server Error");
    HTTP_CODE_TITLE.put(501, "Not Implemented");
    HTTP_CODE_TITLE.put(502, "Bad Gateway");
    HTTP_CODE_TITLE.put(503, "Service Unavailable");
    HTTP_CODE_TITLE.put(504, "Gateway Timeout");
    HTTP_CODE_TITLE.put(505, "HTTP Version Not Supported");
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
