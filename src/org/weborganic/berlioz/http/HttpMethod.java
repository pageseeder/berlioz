package org.weborganic.berlioz.http;

/**
 * An enumeration of HTTP methods supported by Berlioz.
 * 
 * @author Christophe Lauret
 * @since Berlioz 0.8.2
 */
public enum HttpMethod {

  /**
   * HEAD HTTP Method.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 GET</a>
   */
  HEAD(false),

  /**
   * GET HTTP Method.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 GET</a>
   */
  GET(true),

  /**
   * POST HTTP Method.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 POST</a>
   */
  POST(true),

  /**
   * PUT HTTP Method.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 PUT</a>
   */
  PUT(true),

  /**
   * DELETE HTTP Method.
   * 
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-9.3">HTTP/1.1 - 9.3 DELETE</a>
   */
  DELETE(true);

  /**
   * Indicates whether it can be mapped to a content generator.
   */
  private final boolean mappable;

  /**
   * Creates a new constant.
   * @param isMappable whether it can be mapped to a Berlioz Service.
   */
  private HttpMethod(boolean isMappable) {
    this.mappable = isMappable;
  }

  /**
   * Indicates whether it can be mapped to a Berlioz Service.
   * 
   * @return <code>true</code> if it can be mapped to a Berlioz Service;
   *         <code>false</code> otherwise.
   */
  public boolean isMappable() {
    return this.mappable;
  }

}
