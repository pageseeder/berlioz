package org.weborganic.berlioz.servlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A basic filter to relocate URI patterns to other URI patterns.
 * 
 * <p>The relocation mapping can be specified as below:
 * 
 * <pre>{@code
 * <?xml version="1.0" encoding="utf-8"?>
 * <relocation-mapping>
 *   <relocation from="/"             to="/html/home"/>
 *   <relocation from="/index.html"   to="/html/home"/>
 *   <relocation from="/html"         to="/html/home"/>
 *   <relocation from="/xml"          to="/xml/home"/>
 *   <relocation from="/{+path}.psml" to="/html/{+path}"/>
 * </relocation-mapping>
 * }</pre>
 * 
 * <p>See {@link #init(ServletConfig)} for details for configuration options.
 * 
 * @author Christophe Lauret (Weborganic)
 * @author Jean-Baptiste Reure (Weborganic)
 * @version 15 June 2011
 */
@Beta public final class RelocationFilter implements Filter {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RelocationFilter.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * Where the relocation config is located.
   */
  private File _mappingFile;

  /**
   * Maps URI patterns to relocate to URI pattern target.
   */
  private Map<URIPattern, URIPattern> _mapping = null;

// servlet methods --------------------------------------------------------------------------------

  /**
   * Initialises the Relocation Servlet.
   * 
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>config</code> path to the URI relocation mapping XML file (eg. '/config/relocation.xml')</li>
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
      if (p.match(uri) && relocate(req, res, p)) {
        return;
      }
    }

    // Continue
    chain.doFilter(req, res);
  }

  /**
   * Actually performs the relocate.
   * 
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param match The URI matched pattern.
   * 
   * @throws ServletException If thrown by the HTTP servlet it was forwarded to.
   * @throws IOException      If thrown while writing  the HTTP the response.
   * 
   * @return <code>true</code> to relocate; <code>false</code> otherwise.
   */
  private boolean relocate(HttpServletRequest req, HttpServletResponse res, URIPattern match) 
      throws ServletException, IOException {
    URIPattern target = this._mapping.get(match);

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
    LOGGER.debug("Relocating from {} to {}", from, to);

    // And relocate
    RequestDispatcher dispatcher = req.getRequestDispatcher(to);
    if (dispatcher == null) {
      LOGGER.debug("Invalid URL, no dispatcher found");
      return false;
    }
    // set Content-Location header
    res.setHeader("Content-Location", to);
    dispatcher.forward(req, res);
    return true;
  }

  /**
   * Load the URI redirect configuration file.
   * 
   * @return <code>true</code> if loaded correctly;
   *         <code>false</code> otherwise.
   */
  private boolean loadConfig() {
    this._mapping = new HashMap<URIPattern, URIPattern>();
    RelocationMappingHandler handler = new RelocationMappingHandler(this._mapping);
    boolean loaded = false;
    try {
      XMLUtils.parse(handler, this._mappingFile, false);
      loaded = true;
    } catch (BerliozException ex) {
      LOGGER.error("Unable to load redirect mapping {} : {}", this._mappingFile, ex);
    }
    return loaded;
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   * 
   * @author Christophe Lauret (Weborganic)
   * @author Jean-Baptiste Reure (Weborganic)
   * @version 15 June 2011
   */
  @Beta class RelocationMappingHandler extends DefaultHandler implements ContentHandler {

    /**
     * Maps URI patterns to URI patterns.
     */
    private final Map<URIPattern, URIPattern> _mapping;

    /**
     * 
     */
    public RelocationMappingHandler(Map<URIPattern, URIPattern> mapping) {
      this._mapping = mapping;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if ("relocation".equals(localName)) {
        URIPattern from = toPattern(atts.getValue("from"));
        URIPattern to = toPattern(atts.getValue("to"));
        if (from == null || to == null) return;
        this._mapping.put(from, to);
      }
    }

    /**
     * Parse the specified URI Pattern.
     * 
     * @param pattern The URI pattern as a string.
     * @return the <code>URIPattern</code> instance or <code>null</code>.
     */
    private URIPattern toPattern(String pattern) {
      try {
        return new URIPattern(pattern);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
        return null;
      }
    }

  }

}
