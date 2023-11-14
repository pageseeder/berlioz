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
package org.pageseeder.berlioz.servlet;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.pageseeder.berlioz.content.Location;
import org.pageseeder.berlioz.content.PathInfo;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.util.StringUtils;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Capture details about the location of a resource.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.13
 */
public final class HttpLocation implements Location, Serializable {

  /**
   * As per requirement for <code>Serializable</code>.
   */
  private static final long serialVersionUID = -7167736932510978113L;

  /**
   * The default port number for HTTP.
   */
  private static final int DEFAULT_PORT_HTTP = 80;

  /**
   * The default port number for HTTP.
   */
  private static final int DEFAULT_PORT_HTTPS = 443;

  /** The scheme "http" or "https" */
  private final String scheme;

  /** The host */
  private final String host;

  /** The port */
  private final int port;

  /** The path */
  private final String path;

  /** The query path */
  private final String query;

  /** The path info */
  private final HttpPathInfo info;

  /**
   * Create a new location instance from the specified request.
   *
   * @param req The HTTP servlet request.
   */
  private HttpLocation(HttpServletRequest req) {
    this.scheme = req.getScheme();
    this.host = req.getServerName();
    this.port = req.getServerPort();
    this.path = req.getRequestURI();
    String queryString = req.getQueryString();
    this.query = queryString != null? queryString : "";
    this.info = new HttpPathInfo(req);
  }

  @Override
  public String scheme() {
    return this.scheme;
  }

  @Override
  public String host() {
    return this.host;
  }

  @Override
  public int port() {
    return this.port;
  }

  @Override
  public String path() {
    return this.path;
  }

  @Override
  public String query() {
    return this.query;
  }

  @Override
  public PathInfo info() {
    return this.info;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("location");
    xml.attribute("scheme", this.scheme);
    xml.attribute("host", this.host);
    xml.attribute("port", Integer.toString(this.port));
    xml.attribute("path", this.path);
    xml.attribute("query", this.query);
    StringBuilder url = new StringBuilder();
    url.append(this.scheme).append("://").append(this.host);
    if (!isDefaultPort(this.scheme, this.port)) {
      url.append(':').append(this.port);
    }
    // Include the base (without the path or query)
    xml.attribute("base", url.toString());
    url.append(this.path);
    if (!this.query.isEmpty()) {
      url.append('?').append(this.query);
    }
    xml.writeText(url.toString());
    xml.closeElement();
  }

  /**
   * Build a new instance from the specified servlet request.
   *
   * @param req The servlet request to use.
   *
   * @return a new instance.
   */
  public static HttpLocation build(HttpServletRequest req) {
    return new HttpLocation(req);
  }

  /**
   * Returns the base URL as a string builder.
   *
   * <p>This method construct the base URL using the following methods:
   * <ul>
   *   <li><code>getScheme</code></li>
   *   <li><code>getServerName</code></li>
   *   <li><code>getServerPort</code></li>
   * </ul>
   *
   * <p>The port number is only included if required.
   *
   * @param req the HTTP servlet request to use to build the base URL
   * @return the corresponding base url
   */
  public static StringBuilder toBaseURL(HttpServletRequest req) {
    StringBuilder base = new StringBuilder();
    String scheme = getScheme(req);
    int port = getPort(req);
    base.append(scheme).append("://").append(req.getServerName());
    if (port > 0 && !isDefaultPort(scheme, port)) {
      base.append(':').append(port);
    }
    return base;
  }


  /**
   * Returns the scheme (http or https).
   *
   * <p>Check if this information is in the reverse proxy header otherwise get from expected header
   *
   * <p>The reason it tries to get the original scheme is to avoid multiple redirecting. The scenario we found is:
   *
   * 1 - User access https
   * 2 - If there is a NGINX reverse proxy, the request will be sent to jetty using http probably
   * 3 - if the berlioz redirect is used, it will redirect over the http
   * 4 - And NGINX may be set to not accept http then it redirects again to https
   *
   * <p>By getting the original scheme (used in above step 1) the step 3 will redirect to the correct scheme and the step 4
   * will not happen.
   *
   * @param req the HTTP servlet request to use to build the base URL
   * @return the corresponding scheme
   */
  private static String getScheme(HttpServletRequest req) {
    //If there is a reverse proxy, the original scheme maybe in X_FORWARDED_PROTO header
    String scheme = req.getHeader(HttpHeaders.X_FORWARDED_PROTO);

    if (StringUtils.isBlank(scheme)) {
      scheme = req.getScheme();
    }

    return scheme;
  }

  /**
   * Returns the port used in this request
   *
   * Check if this information is in the reverse proxy header otherwise get from expected header
   *
   * @param req the HTTP servlet request to use to build the base URL
   * @return the corresponding port or -1 if the reverse proxy host does not have the port. Then it should use the
   * default according the scheme.
   */
  private static int getPort(HttpServletRequest req) {
    int port = req.getServerPort();

    //If there is a reverse proxy, the original port maybe in CustomHttpHeaders.X_FORWARDED_HOST header
    //It is not compulsory to have the port in the host.
    String reverseProxyScheme = req.getHeader(HttpHeaders.X_FORWARDED_PROTO);

    // If theres is a reverse proxy scheme then it should not use the req.getServerPort
    if (!StringUtils.isBlank(reverseProxyScheme)) {
      // If there is not a reverse proxy port, then set to -1 to indicate that there is reverse port but the port has
      // not been sent.
      port = -1;
      String reverseProxyPort = StringUtils.substringAfter(req.getHeader(HttpHeaders.X_FORWARDED_HOST), ":");
      if (reverseProxyPort.matches("\\d{1,5}")) {
        port = Integer.parseInt(reverseProxyPort);
      }
    }

    return port;
  }

  /**
   * Indicates whether the default port is used for the specified scheme.
   *
   * <p>The port is considered to be the default for the scheme used if it is:
   * <ul>
   *   <li>less than 0</li>
   *   <li>equal to 80 for "http"</li>
   *   <li>equal to 443 for "https"</li>
   * </ul>
   *
   * @param scheme The scheme ("http" or "http")
   * @param port   the port number
   *
   * @return <code>true</code> if a default port is used;
   *         <code>false</code> otherwise.
   */
  private static boolean isDefaultPort(String scheme, int port) {
    if (port < 0) return true;
    return (DEFAULT_PORT_HTTP == port  && "http".equals(scheme))
        || (DEFAULT_PORT_HTTPS == port && "https".equals(scheme));
  }
}
