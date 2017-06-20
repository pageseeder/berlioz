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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.furi.URIParameters;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.xml.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A basic filter to redirect URI patterns to other URI patterns.
 *
 * <p>The redirect mapping can be specified as below:
 *
 * <pre>{@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <redirect-mapping>
 *   <redirect from="/"             to="/html/home"/>
 *   <redirect from="/index.html"   to="/html/home"/>
 *   <redirect from="/html"         to="/html/home"/>
 *   <redirect from="/xml"          to="/xml/home"/>
 *   <redirect from="/{+path}.psml" to="/html/{+path}"/>
 * </redirect-mapping>
 * }</pre>
 *
 * <p>All redirects are currently temporary (302) unless the attribute 'permanent' is set to 'yes'
 * in which case the HTTP code will be 301
 *
 * <p>See {@link #init(javax.servlet.ServletConfig)} for details for configuration options.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1 - Moved Permanently</a>
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1 - Found</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public final class RedirectFilter implements Filter, Serializable {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 25684657834543L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

  // class attributes
  // ---------------------------------------------------------------------------------------------

  /**
   * Where the redirect config is located.
   */
  private @Nullable File mappingFile;

  /**
   * Maps URI patterns to redirect to URI pattern target.
   */
  private transient @Nullable Map<URIPattern, URIPattern> mapping = null;

  /**
   * IF a URI pattern is on this list, it indicates that the redirect is permanent (301 instead of 302)
   */
  private transient List<URIPattern> permanent = Collections.emptyList();

  /**
   * The control key
   */
  private String controlKey = "";

  // servlet methods
  // ---------------------------------------------------------------------------------------------

  /**
   * Initialises the Redirector Servlet.
   *
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>config</code> path to the URI redirect mapping XML file (eg. '/config/redirect.xml')
   * </ul>
   *
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   *
   * @param config The filter configuration.
   */
  @Override
  public void init(FilterConfig config) {
    // get the WEB-INF directory
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    String mappingConfig = config.getInitParameter("config");

    this.controlKey = GlobalSettings.get(BerliozOption.XML_CONTROL_KEY);

    // Mapping not specified
    if (mappingConfig == null) {
      LOGGER.warn("Missing 'config' init-parameter - filter will have no effect");
      return;
    }

    // Mapping does not exist
    File mappingFile = new File(webinfPath, mappingConfig);
    if (!mappingFile.exists()) {
      LOGGER.warn("'config' init-parameter points to non existing file {} - filter will have no effect",
      mappingFile.getAbsolutePath());
    }

    // Simply set the mapping file, it will be loaded if needed only.
    this.mappingFile = mappingFile;
  }

  @Override
  public void destroy() {
    this.mappingFile = null;
    this.mapping = null;
    this.controlKey = "";
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws ServletException, IOException {
    if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
      doHTTPFilter((HttpServletRequest)req, (HttpServletResponse)res, chain);
    }
  }

  /**
   * Do the filtering for a HTTP request.
   *
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param chain The filter chain.
   *
   * @throws IOException      Should an error occurs while writing the response.
   * @throws ServletException If thrown by the filter chain.
   */
  public void doHTTPFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    // Reset mapping on reload
    if ("true".equals(req.getParameter("berlioz-reload"))
     && BerliozConfig.hasControl(req, this.controlKey)) {
      ;
    } {
      this.mapping = null;
    }

    // Load the config if needed
    Map<URIPattern, URIPattern> mapping = mapping();


    // Evaluate URI patterns
    for (URIPattern p : mapping.keySet()) {
      String uri = req.getRequestURI();
      if (p.match(uri)) {
        redirect(req, res, p);
        return;
      }
    }

    // Continue
    chain.doFilter(req, res);
  }

  /**
   * Actually performs the redirect.
   *
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param match The URI matched pattern.
   *
   * @throws IOException If thrown by the HTTP the response.
   * @throws ServletException If the a relative URL was used.
   */
  private boolean redirect(HttpServletRequest req, HttpServletResponse res, URIPattern match)
      throws IOException, ServletException {
    URIPattern target = mapping().get(match);

    // Unlikely but possible
    if (target == null) return false;

    // Resolve URI variables
    String from = req.getRequestURI();
    URIResolver resolver = new URIResolver(from);
    URIResolveResult result = resolver.resolve(match);

    // Expand the target URI with URI variables
    Set<String> names = result.names();
    URIParameters parameters = new URIParameters();
    for (String name : names) {
      parameters.set(name, (String)result.get(name));
    }
    String to = target.expand(parameters);

    // Encode URL
    res.setCharacterEncoding("utf-8");
    LOGGER.debug("Redirecting from {} to {}", from, to);

    // And redirect
    boolean isPermanent = this.permanent.contains(match);
    sendRedirect(req, res, to, isPermanent);
    return true;
  }

  /**
   * @return the URI pattern mapping loading the configuration file if necessary.
   */
  private Map<URIPattern, URIPattern> mapping() {
    Map<URIPattern, URIPattern> mapping = this.mapping;
    if (mapping == null) {
      mapping = loadConfig(this.mappingFile);
      this.mapping = mapping;
    }
    return mapping;
  }

  /**
   * Load the URI relocation configuration file.
   *
   * @return <code>true</code> if loaded correctly;
   *         <code>false</code> otherwise.
   */
  private Map<URIPattern, URIPattern> loadConfig(@Nullable File file) {
    Map<URIPattern, URIPattern> mapping = new HashMap<>();
    if (file != null) {
      Handler handler = new Handler();
      try {
        XMLUtils.parse(handler, file, false);
      } catch (BerliozException ex) {
        LOGGER.error("Unable to load redirect mapping {} : {}", file, ex);
      }
      mapping = handler.getMapping();
      this.permanent = handler.getPermanent();
    }
    return mapping;
  }

  // Utility methods ------------------------------------------------------------------------------

  /**
   * Send the redirection.
   *
   * <p>We need to use our own implementation as some servers will not allow headers to be set
   * after the <code>senRedirect</code> method has been called.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.30">HTTP/1.1 - 14.30 Location</a>
   *
   * @param req       The HTTP servlet request
   * @param res       The HTTP servlet response
   * @param location  The target location for "Location" header
   * @param permanent <code>true</code> to use HTTP 301 status;
   *                  <code>false</code> to HTTP 302.
   *
   * @throws IOException      Should an I/O error be reported by servlet response.
   * @throws ServletException If the a relative URL was used.
   */
  private static void sendRedirect(HttpServletRequest req, HttpServletResponse res, String location, boolean permanent)
      throws IOException, ServletException {
    String url = location;

    // Must use absolute URI
    if (location.indexOf("://") < 4) {
      StringBuilder buffer = HttpLocation.toBaseURL(req);
      if (location.startsWith("/")) {
        buffer.append(location);
      } else throw new ServletException("Cannot use relative URL to redirect: "+location);
      url = buffer.toString();
    }

    // Reset response and sent new location
    res.reset();
    res.setHeader(HttpHeaders.LOCATION, url);
    res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400, must-revalidate");
    if (permanent) {
      res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    } else {
      res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    }
    res.setContentLength(0);
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   */
  private static class Handler extends DefaultHandler implements ContentHandler {

    /**
     * Displays debug information.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);

    /**
     * Maps URI patterns to URI patterns.
     */
    private final Map<URIPattern, URIPattern> mapping = new HashMap<>();

    /**
     * The list of permanent redirect.
     */
    private final List<URIPattern> permanent = new ArrayList<>();

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if ("redirect".equals(localName)) {
        URIPattern from = toPattern(atts.getValue("from"));
        URIPattern to = toPattern(atts.getValue("to"));
        if (from == null || to == null) return;
        boolean isPermanent = "yes".equals(atts.getValue("permanent"));
        this.mapping.put(from, to);
        if (isPermanent) {
          this.permanent.add(from);
        }
      }
    }

    /**
     * Parse the specified URI Pattern.
     *
     * @param pattern The URI pattern as a string.
     * @return the <code>URIPattern</code> instance or <code>null</code>.
     */
    private static @Nullable URIPattern toPattern(@Nullable String pattern) {
      if (pattern == null) return null;
      try {
        return new URIPattern(pattern);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
        return null;
      }
    }

    /**
     * Return the complete mapping of URI patterns that need be redirected.
     *
     * @return The mapping of URI patterns that need be redirected.
     */
    public Map<URIPattern, URIPattern> getMapping() {
      return this.mapping;
    }

    /**
     * The list of redirected URI patterns which will return a permanent redirect.
     *
     * @return The list of redirected URI patterns which will return a permanent redirect.
     */
    public List<URIPattern> getPermanent() {
      return this.permanent;
    }

  }
}
