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

import java.text.ParseException;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.Location;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link javax.servlet.ServletRequest} instance and provide methods
 * to access the parameters and attributes in a consistent manner.
 *
 * @author Christophe Lauret
 * @author Tu Tak Tran
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public abstract class HttpRequestWrapper implements ContentRequest {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpRequestWrapper.class);

  /**
   * The wrapped {@link javax.servlet.ServletRequest}.
   */
  private final HttpServletRequest req;

  /**
   * The wrapped {@link javax.servlet.ServletResponse}.
   */
  private final HttpServletResponse res;

  /**
   * The environment.
   */
  private final Environment env;

  /**
   * The location of the resource requested.
   */
  private final Location location;

  /**
   * Maps parameter names to their values.
   */
  private final Map<String, String> parameters;

  // Constructors
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates a new wrapper around the specified HTTP servlet request.
   *
   * @param core       The core HTTP information.
   * @param parameters The list of parameters.
   *
   * @throws NullPointerException If the request is <code>null</code>.
   */
  HttpRequestWrapper(CoreHttpRequest core, Map<String, String> parameters) {
    Objects.requireNonNull(core, "Cannot construct wrapper around null request.");
    this.req = core.request();
    this.res = core.response();
    this.env = core.environment();
    this.location = core.location();
    this.parameters = parameters;
  }

// generic parameter methods ----------------------------------------------------------------------

  @Override
  public final String getBerliozPath() {
    return this.location.info().path();
  }

  @Override
  public final @Nullable String getParameter(String name) {
    @Nullable String value = this.parameters.get(name);
    if (value == null) {
      value = this.req.getParameter(name);
    }
    return ("".equals(value))? null : value;
  }

  @Override
  public final String getParameter(String name, String def) {
    String value = getParameter(name);
    return (value == null || value.isEmpty())? def : value;
  }

  @Override
  public final String @Nullable [] getParameterValues(String name) {
    @Nullable String value = this.parameters.get(name);
    if (value != null)
      return new String[]{value};
    else
      return this.req.getParameterValues(name);
  }

  @Override
  public final Enumeration<String> getParameterNames() {
    return Collections.enumeration(this.parameters.keySet());
  }

  @Override
  public final Environment getEnvironment() {
    return this.env;
  }

// specific methods ---------------------------------------------------------------------

  @Override
  public final int getIntParameter(String name, int def) {
    String value = getParameter(name);
    if (value == null || value.isEmpty()) return def;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      LOGGER.error("Unable to parse value "+value+" for "+name+" parameter.", ex);
      return def;
    }
  }

  @Override
  public final long getLongParameter(String name, long def) {
    String value = getParameter(name);
    if (value == null || value.isEmpty()) return def;
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException ex) {
      LOGGER.error("Unable to parse value "+value+" for "+name+" parameter.", ex);
      return def;
    }
  }

  @Override
  public final @Nullable Date getDateParameter(String name) {
    String value = this.getParameter(name);
    if (value == null) return null;
    try {
      return ISO8601.parseAuto(value);
    } catch (ParseException ex) {
      LOGGER.warn("The date parameter cannot be parsed :"+this.req.getParameter(name), ex);
      return null;
    }
  }

  public final @Nullable String getPathInfo() {
    return this.req.getPathInfo();
  }

  @Override
  public final Cookie @Nullable [] getCookies() {
    return this.req.getCookies();
  }

// attributes -------------------------------------------------------------------------------------

  @Override
  public final @Nullable Object getAttribute(String name) {
    return this.req.getAttribute(name);
  }

  @Override
  public final void setAttribute(String name, Object o) {
    this.req.setAttribute(name, o);
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
    return this.req;
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
    return this.res;
  }

  @Override
  public final Location getLocation() {
    return this.location;
  }

  /**
   * Utility method that determines whether this request contains multipart content.
   *
   * @return <code>true</code> if the request is multipart;
   *         <code>false</code> otherwise.
   */
  public final boolean isMultipartContent() {
    if (!"post".equals(this.req.getMethod().toLowerCase())) return false;
    String contentType = this.req.getContentType();
    if (contentType == null) return false;
    if (contentType.toLowerCase().startsWith("multipart/")) return true;
    return false;
  }

  /**
   * Returns the session of the wrapped HTTP servlet request.
   *
   * @return The session of the HTTP servlet request.
   */
  @Override
  public final @Nullable HttpSession getSession() {
    return this.req.getSession();
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
    String pathInfo = req.getPathInfo();
    if (pathInfo != null) return pathInfo;
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
  protected static Map<String, String> toParameters(HttpServletRequest req, URIResolveResult results) {
    Map<String, String> parameters = new HashMap<>();
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
