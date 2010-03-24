/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

/**
 * An abstract implementation of the content generator interface.
 *
 * <p>This class can be used as a base class for content generator implementations.
 *
 * @author Christophe Lauret (Weborganic)
 * @version 26 November 2009
 */
public abstract class ContentGeneratorBase implements ContentGenerator {

  /**
   * The name of the service for this content generator.
   */
  private String _service;

  /**
   * The path info of this content generator.
   */
  private String _pathInfo;

  /**
   * The name of the area for this content generator.
   */
  private String _area;

  /**
   * Some redirect information.
   */
  private ContentRedirect _redirect;

// methods  ------------------------------------------------------------------------------------

  /**
   * Allows a redirect object to be specified.
   *
   * @param redirect The redirect object.
   */
  protected final void setRedirect(ContentRedirect redirect) {
    this._redirect = redirect;
  }

  /**
   * {@inheritDoc}
   */
  public final String getRedirectURL(ContentRequest req) {
    StringBuffer url = new StringBuffer(this._redirect.getPathInfo());
    boolean first = true;
    for (Iterator<String> i = this._redirect.getParameterNames(); i.hasNext();) {
      String name = i.next();
      String value = req.getParameter(name);
      if (value == null) {
        Object o = req.getAttribute(name);
        if (o != null) value = o.toString();
      }
      if (value != null) {
        url.append(first? '?' : '&').append(name).append('=').append(encodeURL(value));
        first = false;
      }
    }
    return url.toString();
  }

  /**
   * {@inheritDoc}
   */
  public final String getSubPath(ContentRequest req) {
    String pathInfo = req.getPathInfo();
    int wildcard = this._pathInfo.indexOf('*');
    if (wildcard >= 0 && pathInfo.startsWith(this._pathInfo.substring(0, wildcard))) {
      return pathInfo.substring(wildcard);
    } else return "";
  }

  /**
   * {@inheritDoc} 
   */
  public final boolean redirect() {
    return this._redirect != null;
  }

  /**
   * {@inheritDoc}
   */
  public final void setArea(String area) {
    this._area = area;
  }

  /**
   * {@inheritDoc}
   */
  public final String getArea() {
    return this._area;
  }

  /**
   * {@inheritDoc}
   */
  public final void setService(String service) {
    this._service = service;
  }

  /**
   * {@inheritDoc}
   */
  public final String getService() {
    return this._service;
  }

  /**
   * {@inheritDoc}
   */
  public final void setPathInfo(String pathInfo) {
    this._pathInfo = pathInfo;
  }

  /**
   * {@inheritDoc}
   */
  public final String getPathInfo() {
    return this._pathInfo;
  }

  /**
   * Encodes the specified URL as UTF-8.
   * 
   * @param value the value to encode.
   */
  private final static String encodeURL(String value) {
    try {
      return URLEncoder.encode(value, "utf-8");
    } catch (UnsupportedEncodingException ex) {
      // Should NEVER happen, but if it does we want to know
      throw new IllegalStateException("Unable to encode the specified value "+value, ex);
    }
  }

}
