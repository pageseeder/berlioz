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
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public final class HttpLocation implements Location, Serializable {

  /**
   * As per requirement for <code>Serializable</code>.
   */
  private static final long serialVersionUID = -7167736932510978113L;

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

  /**
   * @return the _scheme
   */
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
    url.append(this._scheme).append("://");
    url.append(this._host);
    if ("http".equals(this._scheme) && this.port() != 80
     || "http".equals(this._scheme) && this.port() != 443) {
      url.append(':').append(this._port);
    }
    url.append(this._path);
    if (this._query != null) {
      url.append('?').append(this._query);
    }
    xml.writeText(url.toString());
    xml.closeElement();
  }

  public static HttpLocation build(HttpServletRequest req) {
    return new HttpLocation(req);
  }

}
