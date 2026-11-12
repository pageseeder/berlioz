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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.config.RedirectConfig;
import org.pageseeder.berlioz.config.RedirectLocation;
import org.pageseeder.berlioz.config.RedirectPattern;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.http.HttpRequests;
import org.pageseeder.berlioz.util.Redaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * <p>The attribute 'status' sets the HTTP status of a redirect: 301, 302, 307 or 308. Without it,
 * redirects are temporary (302) unless the deprecated attribute 'permanent' is set to 'yes', in
 * which case the HTTP code will be 301. When both are specified, 'status' takes precedence.
 *
 * <p>See {@link #init} for details for configuration options.
 *
 * <p>The rules are loaded on first use. A reload of the {@code redirects} scope reloads them on the
 * next request through the filter, whatever the request that asked for it, so a filter that the
 * reload request does not pass through is reloaded as well.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.2">HTTP/1.1 - Moved Permanently</a>
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-10.3.3">HTTP/1.1 - Found</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#section-15.4">HTTP Semantics - Redirection 3xx</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.6
 * @since 0.7
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
   * The redirect rules, loaded if needed; <code>null</code> until initialized.
   */
  private transient @Nullable MappingRules<RedirectConfig> rules = null;

  // servlet methods
  // ---------------------------------------------------------------------------------------------

  /**
   * Initializes the Redirector Servlet.
   *
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>config</code> path to the URI redirect mapping XML file (e.g. '/config/redirect.xml')
   * </ul>
   *
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   *
   * @param config The filter configuration.
   */
  @Override
  public void init(FilterConfig config) {
    String name = config.getFilterName();
    String filterName = name != null ? name : "RedirectFilter";
    String parameter = config.getInitParameter("config");
    // The file is loaded on first use only
    File file = MappingFilters.configFile(config, filterName, parameter, LOGGER);
    register(new MappingRules<>("redirect", filterName, parameter, file, LOGGER, RedirectConfig::newInstance,
        new RedirectConfig(), RedirectFilter::diagnosticRules, RedirectConfig::ignored));
  }

  private void register(MappingRules<RedirectConfig> next) {
    MappingRules<RedirectConfig> previous = this.rules;
    if (previous != null) MappingFilters.unregister(previous);
    this.rules = next;
    MappingFilters.register(next);
  }

  @Override
  public void destroy() {
    MappingRules<RedirectConfig> previous = this.rules;
    if (previous != null) MappingFilters.unregister(previous);
    this.rules = null;
  }

  /**
   * Returns a safe immutable view of every {@link RedirectFilter} and {@link RelocationFilter}
   * instance.
   *
   * <p>For each filter: its name, type, <code>config</code> parameter (relative to
   * <code>WEB-INF</code>), whether the file exists, and the last load: its status, time, failure
   * and rules. Rule targets are redacted. Filters are sorted by type, then name.
   *
   * @return the diagnostic snapshot
   *
   * @since 0.14.6
   */
  public static ConfigurationSnapshot diagnosticSnapshot() {
    return MappingFilters.diagnosticSnapshot();
  }

  private static List<Map<String, Object>> diagnosticRules(RedirectConfig config) {
    return MappingRules.diagnosticView(config.patterns(), RedirectFilter::diagnosticRule);
  }

  private static Map<String, Object> diagnosticRule(RedirectPattern pattern) {
    return DiagnosticConfiguration.fields("from", pattern.from().toString(),
        "to", Redaction.redactUriPattern(pattern.to().toString()), "status", (long) pattern.status().code());
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
    throws ServletException, IOException {
    if (req instanceof HttpServletRequest && res instanceof HttpServletResponse) {
      doHTTPFilter((HttpServletRequest)req, (HttpServletResponse)res, chain);
    }
  }

  /**
   * Do the filtering for an HTTP request.
   *
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param chain The filter chain.
   *
   * @throws IOException      Should an error occur while writing the response.
   * @throws ServletException If thrown by the filter chain.
   */
  public void doHTTPFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {

    // Without a 'config' init-parameter the filter has no effect
    MappingRules<RedirectConfig> mapping = this.rules;
    if (mapping == null || !mapping.isConfigured()) {
      chain.doFilter(req, res);
      return;
    }

    // Reload the rules of every filter if requested
    boolean reloaded = MappingFilters.applyReload(req);

    // Load the config if needed
    RedirectConfig redirectConfig = mapping.get();
    if (reloaded) ReloadScope.reportApplied(req, res, Set.of(ReloadScope.REDIRECTS));

    // Evaluate URI patterns
    RedirectLocation location = redirectConfig.redirect(req.getRequestURI());
    if (location != null) {
      res.setCharacterEncoding(StandardCharsets.UTF_8.name());
      LOGGER.debug("Redirecting from {} to {}", location.from(), location.to());

      // And redirect (resets the response, so report the reloaded scopes again)
      sendRedirect(req, res, location.to(), location.status());
      ReloadScope.reportApplied(req, res, Set.of());
      return;
    }

    // Continue
    chain.doFilter(req, res);
  }

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
   * @param location  The target location for the "Location" header
   * @param status    The HTTP status of the redirect: 301, 302, 307 or 308
   *
   * @throws ServletException If a relative URL was used.
   */
  private static void sendRedirect(HttpServletRequest req, HttpServletResponse res, String location, ContentStatus status)
      throws ServletException {
    if (!HttpRequests.isSafeRedirectURL(location, req) || location.startsWith("//")) {
      throw new ServletException("Unsafe redirect URL: "+location);
    }

    String url = location;

    // Must use absolute URI
    if (location.indexOf("://") < 4) {
      StringBuilder buffer = HttpLocation.toBaseURL(req);
      if (location.startsWith("/")) {
        buffer.append(location);
      } else throw new ServletException("Cannot use relative URL to redirect: "+location);
      url = buffer.toString().replaceAll("[\\n\\r]+", "");
    }

    // Reset response and sent new location
    res.reset();
    res.setHeader(HttpHeaders.LOCATION, url);
    res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=86400, must-revalidate");
    res.setStatus(status.code());
    res.setContentLength(0);
  }

}
