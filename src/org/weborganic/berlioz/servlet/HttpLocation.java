/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.weborganic.berlioz.content.Location;
import org.weborganic.berlioz.content.PathInfo;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Capture details about the location of a resource.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.15 - 30 January 2013
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
  private final String _scheme;

  /** The host */
  private final String _host;

  /** The port */
  private final int _port;

  /** The path */
  private final String _path;

  /** The query path */
  private final String _query;

  /** The path info */
  private final PathInfo _info;

  /**
   * Create a new location instance from the specified request.
   *
   * @param req The HTTP servlet request.
   */
  private HttpLocation(HttpServletRequest req) {
    this._scheme = req.getScheme();
    this._host = req.getServerName();
    this._port = req.getServerPort();
    this._path = req.getRequestURI();
    this._query = req.getQueryString();
    this._info = new HttpPathInfo(req);
  }

  @Override
  public String scheme() {
    return this._scheme;
  }

  @Override
  public String host() {
    return this._host;
  }

  @Override
  public int port() {
    return this._port;
  }

  @Override
  public String path() {
    return this._path;
  }

  @Override
  public String query() {
    return this._query;
  }

  @Override
  public PathInfo info() {
    return this._info;
  }

  @Override
  public void toXML(XMLWriter xml) throws IOException {
    xml.openElement("location");
    xml.attribute("scheme", this._scheme);
    xml.attribute("host", this._host);
    xml.attribute("port", Integer.toString(this._port));
    xml.attribute("path", this._path);
    xml.attribute("query", this._query);
    StringBuilder url = new StringBuilder();
    url.append(this._scheme).append("://").append(this._host);
    if (!isDefaultPort(this._scheme, this._port)) {
      url.append(':').append(this._port);
    }
    url.append(this._path);
    if (this._query != null) {
      url.append('?').append(this._query);
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
   * Returns the a base URL as a string builder.
   *
   * <p>This method contruct the base URL using the following methods:
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
    String scheme = req.getScheme();
    int port = req.getServerPort();
    base.append(scheme).append("://").append(req.getServerName());
    if (!isDefaultPort(scheme, port)) {
      base.append(':').append(port);
    }
    return base;
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
    if (DEFAULT_PORT_HTTP == port  && "http".equals(scheme)) return true;
    if (DEFAULT_PORT_HTTPS == port && "https".equals(scheme)) return true;
    return false;
  }
}
