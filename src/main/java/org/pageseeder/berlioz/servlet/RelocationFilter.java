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
import java.nio.file.Paths;

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
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.config.ConfigException;
import org.pageseeder.berlioz.config.RelocationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>See {@link #init} for details about configuration options.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.14">HTTP 1.1 - Content-Location</a>
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 *
 * @version Berlioz 0.12.4
 * @since Berlioz 0.7
 */
public final class RelocationFilter implements Filter {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RelocationFilter.class);

  /**
   * The control key
   */
  private String controlKey = "";

  /**
   * Where the relocation config is located.
   */
  private @Nullable File mappingFile;

  /**
   * The actual relocation config.
   */
  private @Nullable transient RelocationConfig config = null;

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
    this.config = null;
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
    if ("true".equals(req.getParameter("berlioz-reload")) && BerliozConfig.hasControl(req, this.controlKey)) {
      this.config = null;
    }

    // Load the config if needed
    RelocationConfig mapping = config();

    // Evaluate URI patterns
    String from = req.getRequestURI();
    String to = mapping.relocate(from);
    if (to != null) {
      to = ensureSafeTarget(to.replaceAll("[\\n\\r]*", ""));
      LOGGER.debug("Relocating from {} to {}", from, to);

      // And relocate
      RequestDispatcher dispatcher = req.getRequestDispatcher(to);
      if (dispatcher != null) {
        res.setHeader("Content-Location", to);
        dispatcher.forward(req, res);
      } else {
        LOGGER.debug("Invalid URL, no dispatcher found");
      }
    }

    // Continue
    chain.doFilter(req, res);
  }

  /**
   * @return the config loading the configuration file if necessary.
   */
  private RelocationConfig config() {
    RelocationConfig config = this.config;
    if (config == null) {
      try {
        config = RelocationConfig.newInstance(this.mappingFile);
        this.config = config;
      } catch (ConfigException ex) {
        LOGGER.warn("Unable to load configuration: {}", ex.getMessage());
      }
    }
    return config;
  }

  private String ensureSafeTarget(String to) {
    return Paths.get(to.replaceAll("[\\n\\r]*", "")).normalize().toString();
  }
}
