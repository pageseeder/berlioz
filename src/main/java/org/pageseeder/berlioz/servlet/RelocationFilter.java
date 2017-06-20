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

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.furi.URIParameters;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.furi.URIResolver;
import org.pageseeder.berlioz.xml.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>See {@link #init(javax.servlet.ServletConfig)} for details for configuration options.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.14">HTTP 1.1 - Content-Location</a>
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public final class RelocationFilter implements Filter {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RelocationFilter.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * Where the relocation config is located.
   */
  private @Nullable File mappingFile;

  /**
   * Maps URI patterns to relocate to URI pattern target.
   */
  private @Nullable Map<URIPattern, URIPattern> mapping = null;

  /**
   * The control key
   */
  private String controlKey = "";

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
  @Override
  public void init(FilterConfig config) {
    // get the WEB-INF directory
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    String mapping = config.getInitParameter("config");

    this.controlKey = GlobalSettings.get(BerliozOption.XML_CONTROL_KEY);

    // Mapping not specified
    if (mapping == null) {
      LOGGER.warn("Missing 'config' init-parameter - filter will have no effect");
      return;
    }

    // The mapping file does not exist
    File mappingFile = new File(webinfPath, mapping);
    if (!mappingFile.exists()) {
      LOGGER.warn("'config' init-parameter points to non existing file {} - filter will have no effect",
      mappingFile.getAbsolutePath());
    }

    // Store the mapping file
    this.mappingFile = mappingFile;
  }

  /**
   * Resets the target URL.
   */
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
      if (p.match(uri) && relocate(req, res, p)) return;
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
  private static Map<URIPattern, URIPattern> loadConfig(@Nullable File file) {
    Map<URIPattern, URIPattern> mapping = new HashMap<>();
    if (file != null) {
      Handler handler = new Handler(mapping);
      try {
        XMLUtils.parse(handler, file, false);
      } catch (BerliozException ex) {
        LOGGER.error("Unable to load relocation mapping {} : {}", file, ex);
      }
    }
    return mapping;
  }

  /**
   * Handles the XML for the URI pattern mapping configuration.
   */
  static class Handler extends DefaultHandler implements ContentHandler {

    /**
     * Maps URI patterns to URI patterns.
     */
    private final Map<URIPattern, URIPattern> _mapping;

    /**
     * @param mapping The mapping to use for relocation.
     */
    public Handler(Map<URIPattern, URIPattern> mapping) {
      this._mapping = mapping;
    }

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
    private @Nullable URIPattern toPattern(@Nullable String pattern) {
      if (pattern == null) return null;
      try {
        return new URIPattern(pattern);
      } catch (IllegalArgumentException ex) {
        LOGGER.warn("Unparseable URI pattern: {} - ignored mapping", pattern);
        return null;
      }
    }

  }

}
