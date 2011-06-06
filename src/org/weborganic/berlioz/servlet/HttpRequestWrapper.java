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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Parameter;
import org.weborganic.berlioz.content.Service;
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
public final class HttpRequestWrapper implements ContentRequest {

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
  private Environment _env;

  /**
   * Maps parameter names to their values.
   */
  private Map<String, String> parameters = new HashMap<String, String>();

// sole constructor -------------------------------------------------------------------------------

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
  }

// generic parameter methods ----------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public String getBerliozPath() {
    return getBerliozPath(this._req);
  };

  /**
   * {@inheritDoc}
   */
  public String getParameter(String name) {
    String value = this.parameters.get(name);
    if (value == null)
      value = this._req.getParameter(name);
    return ("".equals(value))? null : value;
  }

  /**
   * {@inheritDoc}
   */
  public String getParameter(String name, String def) {
    String value = getParameter(name);
    return (value == null || "".equals(value))? def : value;
  }

  /**
   * {@inheritDoc}
   */
  public String[] getParameterValues(String name) {
    String value = this.parameters.get(name);
    if (value != null)
      return new String[]{value};
    else
      return this._req.getParameterValues(name);
  }

  /**
   * {@inheritDoc}
   */
  public Enumeration<String> getParameterNames() {
    return Collections.enumeration(this.parameters.keySet()); 
  }

  /**
   * {@inheritDoc}
   */
  public Environment getEnvironment() {
    return this._env;
  }

// specific methods ---------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public int getIntParameter(String name, int def) {
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
  public Date getDateParameter(String name) {
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
  public String getPathInfo() {
    return this._req.getPathInfo();
  }

  /**
   * {@inheritDoc} 
   */
  public Cookie[] getCookies() {
    return this._req.getCookies();
  }

  /**
   * {@inheritDoc}
   */
  public void returnNotFound() {
    this._res.setStatus(HttpServletResponse.SC_NOT_FOUND);
  }

// attributes -------------------------------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public Object getAttribute(String name) {
    return this._req.getAttribute(name);
  }

  /**
   * {@inheritDoc}
   */
  public void setAttribute(String name, Object o) {
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
  public HttpServletRequest getHttpRequest() {
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
  public HttpServletResponse getHttpResponse() {
    return this._res;
  }

  /**
   * Utility method that determines whether this request contains multipart content.
   *
   * @return <code>true</code> if the request is multipart;
   *         <code>false</code> otherwise.
   */
  public boolean isMultipartContent() {
    if (!"post".equals(this._req.getMethod().toLowerCase())) {
      return false;
    }
    String contentType = this._req.getContentType();
    if (contentType == null) {
      return false;
    }
    if (contentType.toLowerCase().startsWith("multipart/")) {
      return true;
    }
    return false;
  }

  /**
   * Returns the session of the wrapped HTTP servlet request.
   * 
   * @return The session of the HTTP servlet request.
   */
  public HttpSession getSession() {
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
    if (req.getPathInfo() != null) { return req.getPathInfo(); }
    // Otherwise assume that it is mapped to '*.suffix'
    String path = req.getServletPath();
    int dot = path.lastIndexOf('.');
    return (dot != -1)? path.substring(0, dot) : path;
  }

  // protected and private methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Configure this request wrapper for the specified service match and generator.
   * 
   * @param match     the matching service info.
   * @param generator the generator for which is request is used.
   */
  void configure(MatchingService match, ContentGenerator generator) {
    this.parameters.clear();
    URIResolveResult results = match.result();
    // Load all URL parameters (takes precedence over HTTP parameters)
    for (String name : results.names()) {
      Object o = results.get(name);
      if (o != null)
        this.parameters.put(name, o.toString());
    }
    // Load the service configuration
    Service service = match.service();
    for (Parameter p : service.parameters(generator)) {
      String value = getParameterValue(p, results);
      if (value != null)
        this.parameters.put(p.name(), value);
      else if (p.def() != null)
        this.parameters.put(p.name(), p.def());
    }
  }

  /**
   * Retrieve the parameters value based on the source. 
   * 
   * @param p       The parameter
   * @param results The URI resolutions results
   *
   * @return The parameter value 
   */
  private String getParameterValue(Parameter p, URIResolveResult results) {
    switch (p.source()) {
      case QUERY_STRING: 
        return this._req.getParameter(p.value());
      case URI_VARIABLE:
        Object o = results.get(p.value());
        return o != null? o.toString() : null;
      case STRING: return p.value();
      default: return null;
    }
  }

}
