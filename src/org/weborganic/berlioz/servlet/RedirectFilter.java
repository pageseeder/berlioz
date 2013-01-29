/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.xml.XMLUtils;
import org.weborganic.furi.URIParameters;
import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolveResult;
import org.weborganic.furi.URIResolver;
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
 * @version Berlioz 0.9.15 - 30 January 2013
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
  private File _mappingFile;

  /**
   * Maps URI patterns to redirect to URI pattern target.
   */
  private transient Map<URIPattern, URIPattern> _mapping = null;

  /**
   * IF a URI pattern is on this list, it indicates that the redirect is permanent (301 instead of 302)
   */
  private transient List<URIPattern> _permanent = null;

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
    String mapping = config.getInitParameter("config");

    // Mapping not specified
    if (mapping == null) {
      LOGGER.warn("Missing 'config' init-parameter - filter will have no effect");
      return;
    }

    // Mapping does not exist
    File mappingFile = new File(webinfPath, mapping);
    if (!mappingFile.exists()) {
      LOGGER.warn("'config' init-parameter points to non existing file {} - filter will have no effect",
      mappingFile.getAbsolutePath());
    }

    // Simply set the mapping file, it will be loaded if needed only.
    this._mappingFile = mappingFile;
  }

  @Override
  public void destroy() {
    this._mappingFile = null;
    this._mapping = null;
    this._permanent = null;
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

    // Load the config if needed
    if (this._mapping == null) {
      loadConfig();
    }

    // Evaluate URI patterns
    for (URIPattern p : this._mapping.keySet()) {
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
   */
  private void redirect(HttpServletRequest req, HttpServletResponse res, URIPattern match) throws IOException {
    URIPattern target = this._mapping.get(match);
    HttpServletRequest hreq = req;

    // Resolve URI variables
    String from = hreq.getRequestURI();
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
    res.sendRedirect(to);
    if (this._permanent != null && this._permanent.contains(match)) {
      res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
  }

  /**
   * Load the URI redirect configuration file.
   *
   * @return <code>true</code> if loaded correctly;
   *         <code>false</code> otherwise.
   */
  private boolean loadConfig() {
    Handler handler = new Handler();
    boolean loaded = false;
    try {
      XMLUtils.parse(handler, this._mappingFile, false);
      loaded = true;
    } catch (BerliozException ex) {
      LOGGER.error("Unable to load redirect mapping {} : {}", this._mappingFile, ex);
    }
    this._mapping = handler.getMapping();
    this._permanent = handler.getPermanent();
    return loaded;
  }

  // Utility methods ------------------------------------------------------------------------------

  /**
   * Handles the XML for the URI pattern mapping configuration.
   *
   * @author Christophe Lauret
   * @version 8 October 2012
   */
  static class Handler extends DefaultHandler implements ContentHandler {

    /**
     * Displays debug information.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Handler.class);

    /**
     * Maps URI patterns to URI patterns.
     */
    private final Map<URIPattern, URIPattern> mapping = new HashMap<URIPattern, URIPattern>();

    /**
     * The list of permanent redirect.
     */
    private final List<URIPattern> permanent = new ArrayList<URIPattern>();

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
    private static URIPattern toPattern(String pattern) {
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
