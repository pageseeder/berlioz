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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.ErrorID;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.error.Problems;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.json.Json;
import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.security.ControlAuthorization;
import org.pageseeder.berlioz.http.*;
import org.pageseeder.berlioz.util.*;
import org.pageseeder.berlioz.xml.Xml;
import org.pageseeder.berlioz.xslt.XsltTransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Berlioz servlet.
 *
 * <p>A berlioz servlet can only generate one content type and use one set of XSLT templates, these are defined at
 * initialization. See {@link #init(ServletConfig)} for details.
 *
 * <p>This servlet will pass on HTTP parameters to the underlying generators for the service it matches.
 *
 * <h3>XSLT Caching</h3>
 *
 * <p>The XSLT templates are cached by default unless the XSLT Cache global option property was set to
 * <code>false</code>; in other words XSLT templates are parsed once and reused for each call. The special parameter
 * <code>clear-xsl-cache</code> can be used to clear the XSLT cache.
 *
 * <h3>HTTP Caching</h3>
 *
 * <p>The response is considered cacheable if all the generators in the matching service are cacheable; that is if
 * they implement the {@link org.pageseeder.berlioz.content.Cacheable} interface.
 *
 * <p>For cacheable responses, Berlioz will return the following Headers:
 * <pre>
 *   Expires: <i>[Expiry date 1 year from now]</i>
 *   Cache-Control: [Cache control] or "max-age=<i>[max age in seconds]</i>, must-revalidate"
 *   Etag: <i>[Etag for generator]</i>
 * </pre>
 *
 * <p>The global option HTTP_MAX_AGE can be used to define the maximum age used in the
 * <code>Cache-Control</code> HTTP Header of cacheable response.
 *
 * <p>The <code>Etag</code> is computed from the list of Etags of each generator and an Etag generated for the
 * XSLT templates.
 *
 * <p>Non cacheable responses always return:
 * <pre>
 *   Expires: 0
 *   Cache-Control: no-cache
 * </pre>
 *
 * <p>For security, the Berlioz administration ("control") parameters are disabled by default.
 * See {@link org.pageseeder.berlioz.BerliozOption#CONTROL_KEY},
 * {@link org.pageseeder.berlioz.BerliozOption#CONTROL_NETWORK}, and
 * {@link org.pageseeder.berlioz.security.ControlAuthorization#CONTROL_AUTHORIZED_ATTRIBUTE} to enable them.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.7
 */
public final class BerliozServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2010071926180001L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozServlet.class);

  /**
   * Minimum content length (in bytes) below which GZip compression is skipped: below this size,
   * the fixed gzip header/footer overhead and CPU cost outweigh the bandwidth savings, and the
   * response typically fits within a single network packet regardless.
   */
  private static final int COMPRESSION_THRESHOLD = 1024;

  // Class attributes
  // ----------------------------------------------------------------------------------------------

  /**
   * The transformer factory to generate the templates
   */
  private transient @Nullable BerliozConfig berliozConfig;

  /**
   * The services managed by this servlet.
   */
  private transient @Nullable ServiceRegistry serviceRegistry;

  /**
   * The request dispatcher to forward to the error handler.
   */
  private transient @Nullable RequestDispatcher errorHandler;

// servlet methods --------------------------------------------------------------------------------

  /**
   * Initializes the Berlioz Servlet.
   *
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>content-type</code> to specify the content type used by this Berlioz instance.
   *   <li><code>stylesheet</code> to specify the XSLT stylesheet to use for this Berlioz instance.
   * </ul>
   *
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   *
   * @param servletConfig The servlet configuration.
   *
   * @throws ServletException Should an exception occur.
   */
  @Override
  public void init(ServletConfig servletConfig) throws ServletException {
    super.init(servletConfig);
    this.berliozConfig = BerliozConfig.newConfig(servletConfig);
    this.serviceRegistry = ServiceLoader.getInstance().getDefaultRegistry();
    this.errorHandler = servletConfig.getServletContext().getNamedDispatcher("ErrorHandlerServlet");
    if (this.errorHandler == null) {
      LOGGER.info("No ErrorHandlerServlet is defined in the Web descriptor");
      LOGGER.info("Berlioz will use the fail safe error handler instead");
    }
  }

  @Override
  public void destroy() {
    super.destroy();
    LOGGER.info("Destroying Berlioz Servlet");
    BerliozConfig.unregister(getBerliozConfig());
    this.berliozConfig = null;
    this.serviceRegistry = null;
    this.errorHandler = null;
  }

  // Standard HTTP Methods
  // ----------------------------------------------------------------------------------------------

  // Overridden so the container always dispatches here regardless of HTTP method;
  // the doXXX methods below are not reached via this path and exist for direct/test invocation.
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse res) {
    try {
      HttpMethod method = HttpMethod.valueOf(req.getMethod());
      if (method == HttpMethod.OPTIONS) {
        doOptions(req, res);
      } else {
        process(req, res, method, method != HttpMethod.HEAD);
      }
    } catch (IllegalArgumentException ex) {
      sendError(req, res, HttpServletResponse.SC_NOT_IMPLEMENTED, "Unsupported HTTP method", null);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doHead(HttpServletRequest req, HttpServletResponse res) {
    try {
      process(req, res, HttpMethod.HEAD, false);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {
      process(req, res, HttpMethod.GET, true);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) {
    try {
      process(req, res, HttpMethod.POST, true);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse res) {
    try {
      process(req, res, HttpMethod.PUT, true);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse res) {
    try {
      process(req, res, HttpMethod.DELETE, true);
    } catch (IOException ex) {
      logIOError(req, ex);
    }
  }

  @Override
  public void doOptions(HttpServletRequest req, HttpServletResponse res) {
    ServiceLoader loader = ServiceLoader.getInstance();
    try {
      loader.loadIfRequired();
    } catch (BerliozException ex) {
      sendError(req, res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service configuration Error", ex);
      return;
    }
    ServiceRegistry services = getServiceRegistry();
    String path = HttpRequestWrapper.getBerliozPath(req);
    List<String> methods = services.allows(path);
    res.setHeader(HttpHeaders.ALLOW, HttpResponses.allow(methods));
  }

  // Standard HTTP Methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Handles requests.
   *
   * @param req            The HTTP servlet request.
   * @param res            The HTTP servlet response.
   * @param includeContent Whether to include the content in the response.
   *
   * @throws IOException For any IO exception.
   */
  private void process(HttpServletRequest req, HttpServletResponse res, HttpMethod method, boolean includeContent)
      throws IOException {

    // Use Berlioz config locally
    BerliozConfig config = getBerliozConfig();
    ServiceRegistry services = getServiceRegistry();

    // Setup and ensure that we use UTF-8 to read data
    req.setCharacterEncoding(StandardCharsets.UTF_8.name());
    res.setContentType(config.getContentType());

    // Notify the client not to attempt a range request if it does attempt to do so
    if (req.getHeader(HttpHeaders.RANGE) != null) {
      res.setHeader(HttpHeaders.ACCEPT_RANGES, "none");
    }

    // Determine the method in use.
    ServiceLoader loader = ServiceLoader.getInstance();
    boolean profile = GlobalSettings.has(BerliozOption.PROFILE);
    boolean serverTiming = GlobalSettings.has(BerliozOption.HTTP_SERVER_TIMING);
    boolean serviceHeader = GlobalSettings.has(BerliozOption.HTTP_SERVICE_HEADER);

    // Apply Berlioz control parameters (cache clearing, reloading, etc.)
    profile = applyBerliozControl(req, config, loader, profile);

    // Load the services if required
    try {
      long beforeLoad = System.nanoTime();
      boolean loaded = loader.loadIfRequired();
      if (loaded && serverTiming) {
        ServerTimingHeader.addMetricNano(res,"load", "Loading services", System.nanoTime() - beforeLoad);
      }
    } catch (BerliozException ex) {
      sendError(req, res, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Service configuration Error", ex);
      return;
    }

    // Start handling XML content
    String path = HttpRequestWrapper.getBerliozPath(req);
    MatchingService match = findMatch(services, path, method);

    // No matching service
    if (match == null) {
      handleNoMatch(req, res, services, path, method);
      return;
    }

    // RFC 10008 requires every QUERY request to identify the media type of its query content.
    // Validate only after routing so an unknown resource still receives the expected 404.
    if (!validateQueryContentType(req, res, method)) return;

    var service = match.service();

    // Include the service as a header for information
    if (serviceHeader) {
      res.setHeader("X-Berlioz-Service", toSafeHeader(service.id()));
    }
    LOGGER.debug("{} -> {}", path, service);

    // Is Berlioz used to handle an error?
    Integer code = (Integer)req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

    // Detect whether a direct JSON response is appropriate:
    // the servlet is configured with a JSON media type AND the service supports direct JSON output.
    boolean jsonRequest = Json.isJsonMediaType(config.getMediaType());
    boolean serviceSupportsJson = jsonRequest && service.supported().contains(OutputType.JSON);

    // Check whether the service can produce the requested output format.
    // JSON requests fall back to XML+XSLT when the service doesn't support JSON directly,
    // but if the service doesn't support XML either, nothing can be produced.
    if (!serviceSupportsJson && !service.supported().contains(OutputType.XML)) {
      sendError(req, res, HttpServletResponse.SC_NOT_FOUND, "Resource not found", null);
      LOGGER.debug("Service {} does not support the requested output format for: {}", service.id(), req.getRequestURI());
      return;
    }

    ProcessingContext context = new ProcessingContext(match, method, code, profile, serverTiming, includeContent);
    try {
      if (serviceSupportsJson) {
        processJson(req, res, config, context);
      } else {
        processXml(req, res, config, context);
      }
    } catch (HttpException ex) {
      if (method != HttpMethod.QUERY) throw ex;
      String message = Objects.requireNonNullElse(ex.getMessage(), "Invalid QUERY request");
      sendError(req, res, ex.getHttpCode(), message, null);
    }
  }

  /**
   * Validates the media-type requirement for a matched {@code QUERY} request.
   *
   * <p>RFC 10008 requires the server to fail a QUERY request when its {@code Content-Type} field
   * is missing. Whether a present media type is supported remains specific to the matched
   * resource and is therefore validated by its generator.</p>
   */
  private boolean validateQueryContentType(HttpServletRequest req, HttpServletResponse res, HttpMethod method) {
    if (method != HttpMethod.QUERY) return true;
    String contentType = req.getContentType();
    if (contentType != null && !contentType.isBlank()) return true;
    sendError(req, res, HttpServletResponse.SC_BAD_REQUEST,
        "QUERY requests require a Content-Type header", null);
    return false;
  }

  /**
   * Handles requests whose service supports direct JSON output, bypassing the XSLT pipeline.
   */
  @SuppressWarnings("java:S3776") // sequential HTTP protocol steps; splitting would harm readability
  private void processJson(HttpServletRequest req, HttpServletResponse res, BerliozConfig config, ProcessingContext context)
      throws IOException {

    JsonResponse json = new JsonResponse(req, res, config, context.match, context.profile);
    if (context.serverTiming) json.enableServerTiming();

    // Indicate that the representation may vary depending on the encoding
    if (config.enableCompression()) {
      res.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
    }

    // Compute the ETag for the request if cacheable and method is GET or HEAD
    String etag = null;
    boolean cacheable = context.errorCode == null && context.match.isCacheable()
        && (context.method == HttpMethod.GET || context.method == HttpMethod.HEAD);
    if (cacheable) {
      String etagJSON = json.getEtag();
      if (etagJSON != null) {
        etag = '"' + SHA256.hash(config.getETagSeed() + "~" + etagJSON) + '"';
        applyCacheHeaders(res, config, context.match, etag);

        // Check conditional request headers (may return 304 without generating content)
        if (!ConditionalRequests.checkIfHeaders(req, res, new ServiceInfo(etag))) return;

      } else {
        cacheable = false;
      }
    }

    if (!cacheable) applyNoCacheHeaders(res);

    // Generate JSON content
    long start = System.nanoTime();
    String content = json.generate();
    long end = System.nanoTime();
    if (context.profile && LOGGER.isInfoEnabled()) {
      LOGGER.info("JSON content generated in {} ms", ProfileFormat.format(end - start));
    }
    if (context.serverTiming) {
      ServerTimingHeader.addMetricNano(res, "json", "JSON Response", end - start);
    }

    // Examine status
    ContentStatus status = json.getStatus();
    int statusCode = resolveStatusCode(context.errorCode, json.getStatusCode());
    res.setStatus(statusCode);

    // If errors occurred and should percolate
    if (checkAndSendError(req, res, statusCode, json.getError())) return;

    // Redirection
    if (handleRedirect(req, res, status, json.getRedirectUrl())) return;

    // Apply generator response headers
    json.getHeaders().forEach(res::setHeader);

    // Write JSON — with optional GZip compression; use problem+json when a top-level problem was signalled
    ProblemDetails topLevelProblem = json.getProblem();
    String jsonMediaType = topLevelProblem != null ? "application/problem+json" : "application/json";
    res.setContentType(jsonMediaType);
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    BerliozOutput jsonOutput = new JsonContent(content, jsonMediaType);
    writeOutput(req, res, jsonOutput, etag, StandardCharsets.UTF_8, config, context.includeContent);
  }

  /**
   * Handles requests via the XML content generation + XSLT pipeline (the original Berlioz path).
   */
  @SuppressWarnings("java:S3776") // sequential HTTP protocol steps; splitting would harm readability
  private void processXml(HttpServletRequest req, HttpServletResponse res, BerliozConfig config,
      ProcessingContext ctx) throws IOException {

    // Prepare the XML Response
    XmlResponse xml = new XmlResponse(req, res, config, ctx.match, ctx.profile);
    if (ctx.serverTiming) xml.enableServerTiming();

    // Identify the transformer; direct services bypass XSLT entirely
    XsltTransformer transformer = ctx.match.service().isDirect() ? null : config.getTransformer(ctx.match.service());
    long start = System.nanoTime();

    // Indicate that the representation may vary depending on the encoding
    if (config.enableCompression()) {
      res.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
    }

    // Compute the ETag for the request if cacheable and methods GET or HEAD
    String etag = null;
    boolean cacheable = ctx.errorCode == null && ctx.match.isCacheable()
        && (ctx.method == HttpMethod.GET || ctx.method == HttpMethod.HEAD);
    if (cacheable) {
      String etagXML = xml.getEtag();
      if (etagXML != null) {
        String etagXSL = transformer != null? transformer.getEtag() : null;
        etag = '"'+ SHA256.hash(config.getETagSeed()+"~"+etagXML+"--"+etagXSL)+'"';

        // Update the headers (they should also be included in case of redirect)
        applyCacheHeaders(res, config, ctx.match, etag);

        // Check if the conditions specified in the optional If headers are satisfied.
        if (!ConditionalRequests.checkIfHeaders(req, res, new ServiceInfo(etag))) return;

      } else {
        cacheable = false;
      }
    }

    // Prevents caching
    if (!cacheable) applyNoCacheHeaders(res);

    // Generate the XML content
    String content = xml.generate();
    long end = System.nanoTime();
    if (ctx.profile && LOGGER.isInfoEnabled()) {
      LOGGER.info("Content generated in {} ms", ProfileFormat.format(end - start));
    }
    if (ctx.serverTiming) {
      ServerTimingHeader.addMetricNano(res,"xml", "XML Response", end - start);
    }

    // Examine the status
    ContentStatus status = xml.getStatus();
    int statusCode = resolveStatusCode(ctx.errorCode, xml.getStatusCode());
    res.setStatus(statusCode);

    // If errors occurred and should percolate
    if (checkAndSendError(req, res, statusCode, xml.getError())) return;

    // Redirection (Beta)
    if (handleRedirect(req, res, status, xml.getRedirectURL())) return;

    // Apply response headers set by generators (last-writer-wins per name).
    xml.getHeaders().forEach(res::setHeader);

    // Produce the output (XSLT transform or pass through raw XML)
    BerliozOutput result;
    try {
      result = executeTransform(content, req, xml, transformer, res, config, ctx);
    } catch (XsltTransformException ex) {
      handleTransformFailure(req, res, ex);
      return;
    }

    // Resolve and validate encoding from XSLT output; canonical name is safe for HTTP headers
    Charset charset = resolveCharset(result.getEncoding());
    res.setContentType(result.getMediaType());
    res.setCharacterEncoding(charset.name());

    // Write the response body, applying GZip compression when appropriate
    writeOutput(req, res, result, etag, charset, config, ctx.includeContent);
  }

  /**
   * Returns the service matching {@code path} and {@code method}, falling back to GET when the
   * global option {@link BerliozOption#HTTP_GET_VIA_POST} allows POST requests to be treated as GET
   * (backward compatibility).
   */
  @SuppressWarnings("removal") // HTTP_GET_VIA_POST removed in 1.0; fallback path guarded here until then
  private static @Nullable MatchingService findMatch(ServiceRegistry services, String path, HttpMethod method) {
    // No matching service (backward compatibility)
    MatchingService match = services.get(path, method);
    if (match == null && method == HttpMethod.POST && GlobalSettings.has(BerliozOption.HTTP_GET_VIA_POST)) {
      match = services.get(path, HttpMethod.GET);
    }
    return match;
  }

  /**
   * Sends the appropriate error response when no service matches the request. For non-safe
   * methods Berlioz first checks whether the path is known for other methods and replies with
   * {@code 405 Method Not Allowed} instead of {@code 404} when it is. {@code GET}, {@code HEAD},
   * and {@code QUERY} are treated as safe/idempotent and always receive a plain {@code 404}.
   */
  private void handleNoMatch(HttpServletRequest req, HttpServletResponse res,
      ServiceRegistry services, String path, HttpMethod method) {
    // If the method is not safe/idempotent, look if the path matches any other method (just in case)
    if (!(method == HttpMethod.HEAD || method == HttpMethod.GET || method == HttpMethod.QUERY)) {
      List<String> methods = services.allows(path);
      if (!methods.isEmpty()) {
        String allowed = HttpResponses.allow(methods);
        res.setHeader(HttpHeaders.ALLOW, allowed);
        sendError(req, res, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Only the following are allowed: " + allowed, null);
        return;
      }
    }
    sendError(req, res, HttpServletResponse.SC_NOT_FOUND, "Resource not found", null);
    LOGGER.debug("No matching service for: {}", req.getRequestURI());
  }

  /**
   * Writes the standard cache-control headers for a cacheable response: {@code Expires},
   * {@code Cache-Control}, and {@code ETag}. The cache-control value is taken from the service
   * definition when present, otherwise from the global configuration.
   */
  private static void applyCacheHeaders(HttpServletResponse res, BerliozConfig config, MatchingService match, String etag) {
    res.setDateHeader(HttpHeaders.EXPIRES, config.getExpiryDate());
    String cc = match.service().cache();
    if (cc.isEmpty()) cc = config.getCacheControl();
    res.setHeader(HttpHeaders.CACHE_CONTROL, toSafeHeader(cc));
    res.setHeader(HttpHeaders.ETAG, etag);
  }

  /**
   * Writes the standard cache-control headers that prevent caching.
   */
  private static void applyNoCacheHeaders(HttpServletResponse res) {
    res.setDateHeader(HttpHeaders.EXPIRES, 0);
    res.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
  }

  /**
   * Resolves the effective HTTP status code, allowing servlet error dispatches to override the
   * generator status without boxing the fallback value.
   */
  private static int resolveStatusCode(@Nullable Integer errorCode, int generatedStatusCode) {
    return errorCode != null ? errorCode : generatedStatusCode;
  }

  /**
   * Forwards an error to the client when a generator threw an exception and the global option
   * {@link BerliozOption#ERROR_GENERATOR_CATCH} is not set.
   *
   * @return {@code true} when an error was sent and the caller must return immediately.
   */
  private boolean checkAndSendError(HttpServletRequest req, HttpServletResponse res,
      int statusCode, @Nullable Exception error) {
    if (error == null || GlobalSettings.has(BerliozOption.ERROR_GENERATOR_CATCH)) return false;
    sendError(req, res, statusCode, "The service failed because of errors thrown by generators", error);
    return true;
  }

  /**
   * Sends a redirect (or a {@code 400} when the URL is unsafe) when the content status indicates
   * a redirect.
   *
   * @return {@code true} when a redirect was handled and the caller must return immediately.
   */
  private boolean handleRedirect(HttpServletRequest req, HttpServletResponse res,
      ContentStatus status, @Nullable String url) {
    if (!ContentStatus.isRedirect(status)) return false;
    if (HttpRequests.isSafeRedirectURL(url, req)) {
      LOGGER.debug("Redirecting to: {} with {}", url, status.code());
      res.reset();
      res.setStatus(status.code());
      res.setHeader("Location", res.encodeRedirectURL(url));
    } else {
      LOGGER.warn("Blocked unsafe redirect URL: {}", url);
      sendError(req, res, HttpServletResponse.SC_BAD_REQUEST, "Invalid redirect URL", null);
    }
    return true;
  }

  /**
   * Resolves the charset from the XSLT output encoding name, falling back to UTF-8 when the name
   * is not recognised by the JVM.
   */
  private static Charset resolveCharset(String encoding) {
    try {
      return Charset.forName(encoding);
    } catch (UnsupportedCharsetException ex) {
      LOGGER.warn("Unsupported encoding '{}' from XSLT output, falling back to UTF-8", encoding);
      return StandardCharsets.UTF_8;
    }
  }

  /**
   * Processes Berlioz control parameters from the request, applying any requested cache clearing,
   * reloading, or configuration reset, and returns the (possibly updated) profile flag.
   *
   * @param req     The HTTP servlet request.
   * @param config  The Berlioz configuration.
   * @param loader  The service loader (might be cleared on reload).
   * @param profile Whether profiling was already enabled via global settings.
   * @return {@code true} if profiling should be active for this request.
   */
  private boolean applyBerliozControl(HttpServletRequest req, BerliozConfig config, ServiceLoader loader, boolean profile) {
    if (!ControlAuthorization.hasControl(req)) return profile;

    // A "reload" triggers all the sub-operations below
    boolean reload = isTrue(req.getParameter("berlioz-reload"));

    // Clear the XSLT cache if requested
    if (reload || isTrue(req.getParameter("clear-xsl-cache"))) { XsltTransformer.clearAllCache(); }

    // Allow ETags to be reset so clients must revalidate
    if (reload || isTrue(req.getParameter("reset-etags"))) { config.resetETagSeed(); }

    // Reload the global configuration from disk
    if (reload) { GlobalSettings.load(); }

    // Clear the service configuration so it is re-read on the next request
    if (reload || isTrue(req.getParameter("reload-services"))) { loader.clear(); }

    // Profile flag can also be enabled per-request via URL parameter
    return profile || isTrue(req.getParameter("berlioz-profile"));
  }

  /**
   * Applies the XSLT transformer to the XML content or returns the raw XML when no transformer
   * is configured. Also records profiling and server-timing metrics when enabled.
   *
   * @param content     The XML content to transform.
   * @param req         The HTTP servlet request (passed through to the transformer).
   * @param xml         The XML response (provides the matched service and any problem details).
   * @param transformer The transformer to use, or {@code null} to pass XML through unchanged.
   * @param res         The HTTP servlet response (used for server-timing headers).
   * @param config      The Berlioz configuration (provides expected media type).
   * @param ctx         The processing context (provides profile and server-timing flags).
   * @return The transformed (or raw XML) output.
   */
  private BerliozOutput executeTransform(String content, HttpServletRequest req, XmlResponse xml,
                                         @Nullable XsltTransformer transformer, HttpServletResponse res,
                                         BerliozConfig config, ProcessingContext ctx) throws XsltTransformException {
    // Direct service with a problem response: return problem+xml, or apply failsafe stylesheet for
    // non-XML media types (e.g. text/html endpoints) so the client receives a rendered error page.
    if (transformer == null) {
      if (xml.getProblem() != null && !Xml.isXmlMediaType(config.getMediaType())) {
        URL failsafeUrl = ErrorHandlerServlet.resolveErrorStylesheet();
        if (failsafeUrl != null) {
          return new XmlContent(XsltTransformer.transformFailSafe(content, failsafeUrl), config.getMediaType());
        }
      }
      // Note we fall back on application/xml, because browsers don't show the response if
      // set to `application/problem+xml`
      return new XmlContent(content);
    }

    XsltTransformResult result = transformer.transformOrThrow(content, req, xml.getService());
    if (ctx.profile && LOGGER.isInfoEnabled()) {
      LOGGER.info("XSLT Transformation {} ms", ProfileFormat.format(result.time()));
    }
    if (ctx.serverTiming) {
      ServerTimingHeader.addMetricNano(res, "xslt", "XSLT Transform", result.time());
    }
    return result;
  }

  /** Gives application error handling one attempt, then uses the terminal built-in renderer. */
  private void handleTransformFailure(HttpServletRequest req, HttpServletResponse res, XsltTransformException ex) {
    Object depth = req.getAttribute(ErrorHandlerServlet.ERROR_RENDERING_DEPTH);
    boolean renderingError = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null
        || depth instanceof Integer && ((Integer) depth) > 0;
    if (renderingError) {
      LOGGER.error("XSLT failed while rendering an error; using terminal fail-safe", ex);
      Object originalAttribute = req.getAttribute(ErrorHandlerServlet.ORIGINAL_ERROR_EXCEPTION);
      if (!(originalAttribute instanceof Throwable)) {
        originalAttribute = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
      }
      Throwable original = originalAttribute instanceof Throwable ? (Throwable) originalAttribute : ex;
      req.setAttribute(ErrorHandlerServlet.ORIGINAL_ERROR_EXCEPTION, original);
      Object existingCode = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
      int status = existingCode instanceof Integer
          ? (Integer) existingCode : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
      Object existingMessage = req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
      String message = existingMessage instanceof String
          ? (String) existingMessage : "XSLT failed while rendering an error";
      ErrorHandlerServlet.prepareErrorAttributes(req, getBerliozConfig().getName(), status, message, original,
          getBerliozConfig().getMediaType());
      try {
        ErrorHandlerServlet.handleTerminal(req, res);
      } catch (IOException io) {
        LOGGER.error("Terminal XSLT error response failed", io);
      }
      return;
    }
    req.setAttribute(ErrorHandlerServlet.ERROR_RENDERING_DEPTH, 1);
    req.setAttribute(ErrorHandlerServlet.ORIGINAL_ERROR_EXCEPTION, ex);
    sendError(req, res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
        "The service failed during XSLT transformation", ex);
  }

  /**
   * Writes the response body, applying GZip compression when the configuration and the client
   * support it and the content meets {@link #COMPRESSION_THRESHOLD}. For HEAD requests
   * ({@code includeContent = false}) only the {@code Content-Length} header is written without a
   * body.
   *
   * @param req            The HTTP servlet request.
   * @param res            The HTTP servlet response.
   * @param result         The output to write.
   * @param etag           The current ETag (updated to its GZip variant when compressed), or {@code null}.
   * @param charset        The character set to use for encoding length calculations.
   * @param config         The Berlioz configuration (controls compression eligibility).
   * @param includeContent Whether to write a response body (false for HEAD requests).
   * @throws IOException For any I/O error while writing to the response.
   */
  private void writeOutput(HttpServletRequest req, HttpServletResponse res, BerliozOutput result,
      @Nullable String etag, Charset charset, BerliozConfig config, boolean includeContent) throws IOException {

    // Only attempt compression when the config enables it, the media type is compressible and the
    // content is large enough for compression to be worthwhile. Char count is a safe lower bound
    // on encoded byte length (every supported charset uses at least 1 byte per char), so it gates
    // the decision without a full encode pass.
    boolean compressible = config.enableCompression() && HttpResponses.isCompressible(result.getMediaType())
        && result.content().length() >= COMPRESSION_THRESHOLD;
    if (compressible && HttpRequests.acceptsGZipCompression(req)) {
      byte[] compressed = ResourceCompressor.compress(result.content(), charset);
      if (compressed.length > 0) {
        res.setIntHeader(HttpHeaders.CONTENT_LENGTH, compressed.length);
        res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        // ETag must reflect the encoding; replace it with the GZip variant
        if (etag != null) {
          res.setHeader(HttpHeaders.ETAG, ETags.getETagForGZip(etag));
        }
        if (includeContent) {
          ServletOutputStream out = res.getOutputStream();
          out.write(compressed);
          out.flush();
        }
        return; // Compressed output written; nothing more to do
      }
      // Compression produced no bytes (e.g. content was empty) — fall through to uncompressed
    }

    // Write uncompressed: body for GET, or just Content-Length for HEAD
    if (includeContent) {
      PrintWriter out = res.getWriter();
      out.print(result.content());
      out.flush();
    } else {
      // HEAD: report the length the client would receive without sending a body
      HttpResponses.setContentLength(res, result.content(), charset);
    }
  }

  /**
   * Handles the specified error.
   *
   * @param req     The HTTP Servlet request.
   * @param res     The HTTP Servlet response.
   * @param code    The HTTP status response code.
   * @param message The message for the message.
   * @param ex      Any caught exception (might be <code>null</code>).
   */
  private void sendError(HttpServletRequest req, HttpServletResponse res, int code, String message, @Nullable Exception ex) {
    // Is Berlioz already handling an error? (set by the servlet container per javax.servlet error dispatch contract)
    Integer error = (Integer) req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

    int statusCode = Math.max(100, Math.min(599, error != null ? error : code));
    prepareErrorAttributes(req, statusCode, message, ex);

    if (error == null && !GlobalSettings.has(BerliozOption.ERROR_HANDLER)) {
      logError(code, message, ex, "Berlioz sending error to Web container {} [{}]");
      try {
        res.sendError(statusCode, message);
      } catch (IOException e) {
        LOGGER.error("Failed to send error {} [{}] to client", message, code, e);
      }
      return;
    }

    // For JSON-configured servlets with ERROR_HANDLER=true, produce application/problem+json directly
    // instead of dispatching to ErrorHandlerServlet (which always renders XML/HTML). Unconditional:
    // there was never a legacy JSON representation, so the deprecated ERROR_PROBLEM_FORMAT=false
    // escape hatch (which only restores the legacy XML/HTML output) does not apply to JSON.
    if (error == null && Json.isJsonMediaType(getBerliozConfig().getMediaType())) {
      DetailLevel level = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL));
      ProblemDetails problem = Problems.forHttpError(code, message, extractErrorId(req, ex), ex, level);
      HttpException signal = HttpException.findIn(ex);
      Map<String, String> headers = signal != null ? signal.headers() : Map.of();
      logError(code, message, ex, "Berlioz sending problem JSON {} [{}]");
      writeProblemJson(res, problem, headers);
      return;
    }

    dispatchError(req, res, code, message, ex);
  }

  private void prepareErrorAttributes(HttpServletRequest req, int statusCode, String message, @Nullable Throwable ex) {
    ErrorHandlerServlet.prepareErrorAttributes(req, getBerliozConfig().getName(), statusCode, message, ex,
        getBerliozConfig().getMediaType());
  }

  /**
   * Returns the Berlioz error ID from the current exception or request attributes, if available.
   */
  private static @Nullable String extractErrorId(HttpServletRequest req, @Nullable Exception ex) {
    ErrorID id = ex instanceof BerliozException ? ((BerliozException) ex).id() : null;
    if (id != null) return id.id();
    Object attribute = req.getAttribute(ErrorHandlerServlet.BERLIOZ_ERROR_ID);
    return attribute instanceof String ? (String) attribute : null;
  }

  /**
   * Forwards the error to the registered handler or falls back to the built-in handler.
   */
  private void dispatchError(HttpServletRequest req, HttpServletResponse res, int code, String message, @Nullable Exception ex) {
    RequestDispatcher handler = this.errorHandler;
    try {
      if (handler != null) {
        logError(code, message, ex, "Berlioz forwarding error {} [{}] to handler");
        handler.forward(req, res);
      } else {
        logError(code, message, ex, "Berlioz handling error {} [{}] internally");
        new ErrorHandlerServlet().handle(req, res);
      }
    } catch (IOException | ServletException | RuntimeException e) {
      LOGGER.error("Failed to dispatch error response {} [{}]", message, code, e);
      if (!res.isCommitted()) {
        try {
          ErrorHandlerServlet.handleTerminal(req, res);
        } catch (IOException terminal) {
          LOGGER.error("Terminal error response also failed", terminal);
        }
      }
    }
  }

  /**
   * Writes a complete {@code application/problem+json} response directly to {@code res}.
   *
   * <p>{@code extraHeaders} (e.g. {@code Retry-After} from an {@link HttpException}) is applied
   * first so the framework-owned {@code Content-Type} set afterward always wins on a name clash.
   */
  private static void writeProblemJson(HttpServletResponse res, ProblemDetails problem, Map<String, String> extraHeaders) {
    try {
      extraHeaders.forEach(res::setHeader);
      res.setStatus(problem.status());
      res.setContentType("application/problem+json");
      res.setCharacterEncoding(StandardCharsets.UTF_8.name());
      PrintWriter out = res.getWriter();
      out.print(problem.toJson());
      out.flush();
    } catch (IOException e) {
      LOGGER.error("Failed to write problem JSON response for status {}", problem.status(), e);
    }
  }

  private void logError(int code, String message, @Nullable Exception ex, String format) {
    if (code >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
      LOGGER.error(format, message, code, ex);
    } else {
      LOGGER.warn(format, message, code, ex);
    }
  }

  /**
   * @param parameter the parameter value to check.
   * @return <code>true</code> if the parameter value is equal to "true";
   *         <code>false</code> for any other value.
   */
  private boolean isTrue(@Nullable String parameter) {
    return "true".equals(parameter);
  }

  private void logIOError(HttpServletRequest req, IOException ex) {
    LOGGER.debug("I/O error processing {} {} - likely a client disconnect", req.getMethod(), req.getRequestURI(), ex);
  }

  private BerliozConfig getBerliozConfig() {
    return Objects.requireNonNull(this.berliozConfig, "Berlioz is not configured!");
  }

  private ServiceRegistry getServiceRegistry() {
    return Objects.requireNonNull(this.serviceRegistry, "Berlioz services are not configured!");
  }

  // Private internal classes
  // ==============================================================================================

  /**
   * Bundles the per-request processing flags resolved once in {@link #process} and shared between
   * {@link #processJson} and {@link #processXml}, reducing the number of parameters on those
   * methods.
   */
  private static final class ProcessingContext {

    final MatchingService match;
    final HttpMethod method;
    final @Nullable Integer errorCode;
    final boolean profile;
    final boolean serverTiming;
    final boolean includeContent;

    ProcessingContext(MatchingService match, HttpMethod method, @Nullable Integer errorCode,
        boolean profile, boolean serverTiming, boolean includeContent) {
      this.match = match;
      this.method = method;
      this.errorCode = errorCode;
      this.profile = profile;
      this.serverTiming = serverTiming;
      this.includeContent = includeContent;
    }
  }

  /**
   * Provide simple entity information for the service.
   */
  private static final class ServiceInfo implements EntityInfo {

    /**
     * The wrapped ETag
     */
    private final String etag;

    /**
     * Creates a new service info instance.
     *
     * @param etag The etag.
     */
    public ServiceInfo(String etag) {
      this.etag = etag;
    }

    /**
     * @return the etag for this service.
     */
    @Override
    public String getETag() {
      return this.etag;
    }

    /**
     * @return Always "text/html".
     */
    @Override
    public String getMimeType() {
      return "text/html";
    }

    /**
     * @return Always -1 as we use the etag for caching.
     */
    @Override
    public long getLastModified() {
      return -1;
    }

  }

  private static String toSafeHeader(String value) {
    return value.replaceAll("[\\r\\n]", " ");
  }
}
