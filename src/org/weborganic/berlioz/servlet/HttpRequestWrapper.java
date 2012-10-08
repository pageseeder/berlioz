/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.util.ISO8601;
import org.weborganic.furi.URIResolveResult;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods
 * to access the parameters and attributes in a consistent manner.
 *
 * @author Christophe Lauret (Weborganic)
 * @author Tu Tak Tran (Allette Systems)
 * 
 * @version 12 April 2011
 */
public abstract class HttpRequestWrapper implements ContentRequest {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestWrapper.class);

  /**
   * The wrapped {@link javax.servlet.ServletRequest}.
   */
  private final HttpServletRequest _req;

  /**
   * The wrapped {@link javax.servlet.ServletResponse}.
   */
  private final HttpServletResponse _res;

  /**
   * The environment.
   */
  private final Environment _env;

  /**
   * Maps parameter names to their values.
   */
  private final Map<String, String> _parameters;

  // Constructors
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   * 
   * @param wrapper The request to wrap.
   * 
   * @throws NullPointerException If the wrapper is <code>null</code>.
   */
  HttpRequestWrapper(HttpRequestWrapper wrapper) {
    if (wrapper == null) throw new NullPointerException("Cannot construct wrapper from null wrapper.");
    this._req = wrapper._req;
    this._res = wrapper._res;
    this._env = wrapper._env;
    this._parameters = new HashMap<String, String>();
  }

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   * 
   * @param req The request to wrap.
   * @param res The response to wrap.
   * @param env The environment for this request.
   * 
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  public HttpRequestWrapper(HttpServletRequest req, HttpServletResponse res, Environment env)
      throws IllegalArgumentException {
    if (req == null)
      throw new IllegalArgumentException("Cannot construct wrapper around null request.");
    this._req = req;
    this._res = res;
    this._env = env;
    this._parameters = new HashMap<String, String>();
  }

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   * 
   * @param req        The request to wrap.
   * @param res        The response to wrap.
   * @param env        The environment for this request.
   * @param parameters The list of parameters.
   * 
   * @throws IllegalArgumentException If the request is <code>null</code>.
   */
  public HttpRequestWrapper(HttpServletRequest req, HttpServletResponse res, Environment env, 
      Map<String, String> parameters) throws IllegalArgumentException {
    if (req == null)
      throw new IllegalArgumentException("Cannot construct wrapper around null request.");
    this._req = req;
    this._res = res;
    this._env = env;
    this._parameters = parameters;
  }

// generic parameter methods ----------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public final String getBerliozPath() {
    return getBerliozPath(this._req);
  };

  /**
   * {@inheritDoc}
   */
  public final String getParameter(String name) {
    String value = this._parameters.get(name);
    if (value == null) {
      value = this._req.getParameter(name);
    }
    return ("".equals(value))? null : value;
  }

  /**
   * {@inheritDoc}
   */
  public final String getParameter(String name, String def) {
    String value = getParameter(name);
    return (value == null || "".equals(value))? def : value;
  }

  /**
   * {@inheritDoc}
   */
  public final String[] getParameterValues(String name) {
    String value = this._parameters.get(name);
    if (value != null)
      return new String[]{value};
    else
      return this._req.getParameterValues(name);
  }

  /**
   * {@inheritDoc}
   */
  public final Enumeration<String> getParameterNames() {
    return Collections.enumeration(this._parameters.keySet()); 
  }

  /**
   * {@inheritDoc}
   */
  public final Environment getEnvironment() {
    return this._env;
  }

// specific methods ---------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public final int getIntParameter(String name, int def) {
    String value = getParameter(name);
    if (value == null || "".equals(value)) return def;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      LOGGER.error("Unable to parse value "+value+" for "+name+" parameter.", ex);
      return def;
    }
  }

  /**
   * {@inheritDoc}
   */
  public final Date getDateParameter(String name) {
    try {
      return ISO8601.parseAuto(this.getParameter(name));
    } catch (ParseException ex) {
      LOGGER.warn("The date parameter cannot be parsed :"+this._req.getParameter(name), ex);
      return null;
    }
  }

  /**
   * Returns the path information (what comes after servlet path).
   * 
   * @return The path information (what comes after servlet path).
   */
  public final String getPathInfo() {
    return this._req.getPathInfo();
  }

  /**
   * {@inheritDoc} 
   */
  public final Cookie[] getCookies() {
    return this._req.getCookies();
  }

// attributes -------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public final Object getAttribute(String name) {
    return this._req.getAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  public final void setAttribute(String name, Object o) {
    this._req.setAttribute(name, o);
  }

  /**
   * Returns the wrapped HTTP servlet request.
   * 
   * <p>This effectively enables the content generator to bypass the clean and simple 
   * methods of the content request, use wisely...
   * 
   * @return The wrapped HTTP servlet request.
   */
  public final HttpServletRequest getHttpRequest() {
    return this._req;
  }

  /**
   * Returns the attached HTTP servlet response.
   * 
   * <p>This effectively enables the content generator to make use of the HTTP servlet
   * response, use wisely...
   * 
   * @return The attached HTTP servlet response.
   */
  public final HttpServletResponse getHttpResponse() {
    return this._res;
  }

  /**
   * Utility method that determines whether this request contains multipart content.
   *
   * @return <code>true</code> if the request is multipart;
   *         <code>false</code> otherwise.
   */
  public boolean isMultipartContent() {
    if (!"post".equals(this._req.getMethod().toLowerCase())) return false;
    String contentType = this._req.getContentType();
    if (contentType == null) return false;
    if (contentType.toLowerCase().startsWith("multipart/")) return true;
    return false;
  }

  /**
   * Returns the session of the wrapped HTTP servlet request.
   * 
   * @return The session of the HTTP servlet request.
   */
  public final HttpSession getSession() {
    return this._req.getSession();
  }

  // utility methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the Berlioz path from an HTTP Servlet request.
   * 
   * <p>The Berlioz path corresponds to:
   * <ul>
   *   <li>the <code>pathInfo</code> when the Berlioz Servlet is mapped using a prefix servlet 
   *   (for example <code>/html/*</code>);</li>
   *   <li>the <code>servletPath</code> when the Berlioz Servlet is mapped using a suffix servlet 
   *   (for example <code>*.html</code>);</li>
   * </ul>
   * 
   * <p>Use this method in preference to the {@link #getPathInfo()} which only works if Berlioz is
   * mapped to prefixes.
   * 
   * @param req The HTTP servlet request.
   * @return the corresponding Berlioz Path.
   */
  public static String getBerliozPath(HttpServletRequest req) {
    // Try to get the path info (when mapped to '/prefix/*')
    if (req.getPathInfo() != null) return req.getPathInfo();
    // Otherwise assume that it is mapped to '*.suffix'
    String path = req.getServletPath();
    int dot = path.lastIndexOf('.');
    return (dot != -1)? path.substring(0, dot) : path;
  }

  // protected and private methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Configure this request wrapper for the specified service match.
   * 
   * @param req     The HTTP servlet request.
   * @param results The results of the URI resolution.
   * 
   * @return A map of the parameters for the specified request and results
   */
  @SuppressWarnings("unchecked")
  protected static Map<String, String> toParameters(HttpServletRequest req, URIResolveResult results) {
    Map<String, String> parameters = new HashMap<String, String>(); 
    // Load all HTTP parameters from the Query String first
    Map<String, String[]> map = req.getParameterMap();
    for (Entry<String, String[]> entry : map.entrySet()) {
      parameters.put(entry.getKey(), entry.getValue()[0]);
    }
    // Load all URL parameters (takes precedence over HTTP parameters)
    for (String name : results.names()) {
      Object o = results.get(name);
      if (o != null) {
        parameters.put(name, o.toString());
      }
    }
    return parameters;
  }

}
