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
import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.xml.XMLUtils;
import org.weborganic.furi.URIParameters;
import org.weborganic.furi.URIPattern;
import org.weborganic.furi.URIResolveResult;
import org.weborganic.furi.URIResolver;

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
 * <p>Note: All redirects are currently temporary (302).
 * 
 * <p>See {@link #init(ServletConfig)} for details for configuration options.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 11 August 2010
 */
@Beta public final class RedirectFilter implements Filter {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 25684657834543L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectFilter.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * Where the redirect config is located.
   */
  private File _mappingFile;

  /**
   * Maps URI patterns to redirect to URI pattern target.
   */
  private Map<URIPattern, URIPattern> _mapping = null;

  /**
   * IF a URI pattern is on this list, it indicates that the redirect is permanent (301 instead of 302)
   */
  private List<URIPattern> _permanent = null;

// servlet methods --------------------------------------------------------------------------------

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

    File mappingFile = new File(webinfPath, mapping);
    if (!mappingFile.exists()) {
      LOGGER.warn("'config' init-parameter points to non existing file {} - filter will have no effect", 
      mappingFile.getAbsolutePath());
    }

    this._mappingFile = mappingFile;
  }

  /**
   * Resets the target URL.
   */
  public void destroy() {
    this._mappingFile = null;
    this._mapping = null;
    this._permanent = null;
  }

  /**
   * {@inheritDoc}
   */
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
    HttpServletRequest hreq = (HttpServletRequest)req;

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
    String encoded = res.encodeRedirectURL(to);

    // And redirect
    res.sendRedirect(to);
    if (this._permanent != null && this._permanent.contains(target)) {
      res.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    }
    res.getWriter().print(getMessage(to, encoded));
  }

  /**
   * Load the URI redirect configuration file.
   * 
   * @return <code>true</code> if loaded correctly;
   *         <code>false</code> otherwise.
   */
  private boolean loadConfig() {
    RedirectMappingHandler handler = new RedirectMappingHandler();
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
   * Returns the HTML message when redirected including a link to the target page.
   * 
   * @param url     The URL to redirect to.
   * @param encoded The encoded URL.
   * @return the HTML message when redirected.
   */
  private static String getMessage(String url, String encoded) {
    StringBuilder html = new StringBuilder();
    html.append("<html>");
    html.append("<head>");
    html.append("<title>Redirect to ").append(url).append("</title>");
    html.append("</head>");
    html.append("<body>");
    html.append("<p>You should be automatically redirected to ");
    html.append("<a href=\"").append(encoded).append("\">").append(url).append("</a>");
    html.append("</p>");
    html.append("</body>");
    html.append("</html>");
    return html.toString();
  }

}
