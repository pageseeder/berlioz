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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.config.MovedLocationPattern;
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
 * <p>The rules are loaded on first use. A reload of the {@code redirects} scope reloads them on the
 * next request through the filter, whatever the request that asked for it, so a filter that the
 * reload request does not pass through is reloaded as well.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7231#section-3.1.4.2">HTTP 1.1 - Content-Location</a>
 *
 * @author Christophe Lauret
 * @author Jean-Baptiste Reure
 *
 * @version 0.14.6
 * @since 0.7
 */
public final class RelocationFilter implements Filter {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RelocationFilter.class);

  /**
   * The relocation rules, loaded if needed; <code>null</code> until initialized.
   */
  private @Nullable MappingRules<RelocationConfig> rules = null;

  /**
   * Initializes the Relocation Servlet.
   *
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>config</code> path to the URI relocation mapping XML file (e.g. '/config/relocation.xml')</li>
   * </ul>
   *
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   *
   * @param config The filter configuration.
   */
  @Override
  public void init(FilterConfig config) {
    // get the WEB-INF directory
    String name = config.getFilterName();
    String filterName = name != null ? name : "RelocationFilter";
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    String mapping = config.getInitParameter("config");

    // Mapping isn't specified
    if (mapping == null) {
      LOGGER.warn("Filter '{}': missing 'config' init-parameter - filter will have no effect", filterName);
      register(new MappingRules<>("relocation", filterName, null, null, LOGGER, RelocationConfig::newInstance,
          new RelocationConfig(), RelocationConfig::size, RelocationFilter::diagnosticRules));
      return;
    }

    // The mapping file does not exist
    File file = new File(webinfPath, mapping);
    if (!file.exists()) {
      LOGGER.warn("Filter '{}': 'config' init-parameter points to non existing file {} - filter will have no effect",
      filterName,
      file.getAbsolutePath());
    }

    // Store the mapping file, it will be loaded if needed only.
    register(new MappingRules<>("relocation", filterName, mapping, file, LOGGER, RelocationConfig::newInstance,
        new RelocationConfig(), RelocationConfig::size, RelocationFilter::diagnosticRules));
  }

  private void register(MappingRules<RelocationConfig> next) {
    MappingRules<RelocationConfig> previous = this.rules;
    if (previous != null) MappingFilters.unregister(previous);
    this.rules = next;
    MappingFilters.register(next);
  }

  /**
   * Returns a safe immutable view of every {@link RedirectFilter} and {@link RelocationFilter}
   * instance; the same as {@link RedirectFilter#diagnosticSnapshot()}.
   *
   * @return the diagnostic snapshot
   *
   * @since 0.14.6
   */
  public static ConfigurationSnapshot diagnosticSnapshot() {
    return MappingFilters.diagnosticSnapshot();
  }

  private static List<Map<String, Object>> diagnosticRules(RelocationConfig config) {
    return config.patterns().stream()
        .map(RelocationFilter::diagnosticRule)
        .collect(Collectors.toList());
  }

  private static Map<String, Object> diagnosticRule(MovedLocationPattern pattern) {
    return DiagnosticConfiguration.fields("from", pattern.from().toString(),
        "to", MappingFilters.redactTarget(pattern.to().toString()));
  }

  /**
   * Resets the target URL.
   */
  @Override
  public void destroy() {
    MappingRules<RelocationConfig> previous = this.rules;
    if (previous != null) MappingFilters.unregister(previous);
    this.rules = null;
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
    MappingRules<RelocationConfig> current = this.rules;
    if (current == null || !current.isConfigured()) {
      chain.doFilter(req, res);
      return;
    }

    // Reload the rules of every filter if requested
    boolean reloaded = MappingFilters.applyReload(req);

    // Load the config if needed
    RelocationConfig mapping = current.get();
    if (reloaded) ReloadScope.reportApplied(req, res, Set.of(ReloadScope.REDIRECTS));

    // Evaluate URI patterns
    String from = req.getRequestURI();
    String to = mapping.relocate(from);
    if (to != null) {
      to = ensureSafeTarget(to);
      if (to != null) {
        LOGGER.debug("Relocating from {} to {}", from, to);

        // And relocate
        RequestDispatcher dispatcher = req.getRequestDispatcher(to);
        if (dispatcher != null) {
          res.setHeader("Content-Location", to);
          dispatcher.forward(req, res);
          return;
        } else {
          LOGGER.debug("Invalid URL, no dispatcher found");
        }
      }
    }

    // Continue
    chain.doFilter(req, res);
  }

  private @Nullable String ensureSafeTarget(String to) {
    String sanitized = to.replaceAll("[\\n\\r]+", "");
    String decoded;
    try {
      decoded = URLDecoder.decode(sanitized, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Relocation target '{}' is not a valid encoded path, ignoring", to);
      return null;
    }
    // Decode to fixed point for the protection check only — catches any encoding depth
    // (%2fWEB-INF, %252fWEB-INF, %25252fWEB-INF, …) without discarding legitimate
    // percent-encoding in the path we actually dispatch (e.g. %20, %C3%A9).
    String fullyDecoded = fullyDecode(decoded);
    if (fullyDecoded == null || isProtectedTarget(normalizeTargetForCheck(fullyDecoded))) {
      LOGGER.warn("Relocation target '{}' resolves to protected path, ignoring", to);
      return null;
    }
    // Return single-decoded normalised path; other percent-encoded characters are preserved.
    return normalizeTarget(decoded);
  }

  /**
   * Repeatedly URL-decodes {@code s} until the result is stable (idempotent).
   * Returns {@code null} if any decode step produces a malformed sequence.
   */
  private static @Nullable String fullyDecode(String s) {
    String current = s;
    while (true) {
      String next;
      try {
        next = URLDecoder.decode(current, StandardCharsets.UTF_8);
      } catch (IllegalArgumentException ex) {
        return null;
      }
      if (next.equals(current)) return current;
      current = next;
    }
  }

  private static String normalizeTarget(String target) {
    return Paths.get(target).normalize().toString().replace('\\', '/');
  }

  private static String normalizeTargetForCheck(String target) {
    String check = target.startsWith("/") ? target : "/" + target;
    return normalizeTarget(check);
  }

  /** @param normalizedAbsPath an already-normalized absolute path (output of {@link #normalizeTargetForCheck}) */
  private static boolean isProtectedTarget(String normalizedAbsPath) {
    return "/WEB-INF".equals(normalizedAbsPath) || normalizedAbsPath.startsWith("/WEB-INF/")
        || "/META-INF".equals(normalizedAbsPath) || normalizedAbsPath.startsWith("/META-INF/");
  }
}
