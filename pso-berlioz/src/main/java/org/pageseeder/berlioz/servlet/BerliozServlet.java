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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
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
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.json.Json;
import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.http.*;
import org.pageseeder.berlioz.servlet.XsltTransformResult.Status;
import org.pageseeder.berlioz.util.*;
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
 * <p>For security, the Berlioz administration parameters can be secured using a Berlioz control key.
 * The control key is a string that must be supplied as a parameter whenever one of the admin
 * parameters is used. Use the initialization parameters to define a control key.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
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
   *   <li><code>berlioz-control</code> to specify the Berlioz control key to enable admin parameters.
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
    ServiceRegistry services = getServiceRegistry();
    String path = HttpRequestWrapper.getBerliozPath(req);
    List<String> methods = services.allows(path);
    res.setHeader(HttpHeaders.ALLOW, HttpHeaderUtils.allow(methods));
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
    req.setCharacterEncoding("utf-8");
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

    var service = match.service();

    // Include the service as a header for information
    if (serviceHeader) {
      res.setHeader("X-Berlioz-Service", toSafeHeader(service.id()));
    }
    LOGGER.debug("{} -> {}", path, service);

    // Is Berlioz used to handle an error?
    Integer code = (Integer)req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);

    // Detect whether a direct JSON response is appropriate:
    // the servlet is configured with a JSON media type AND the service supports direct JSON output.
    boolean jsonRequest = Json.isJsonMediaType(config.getMediaType());
    boolean serviceSupportsJson = jsonRequest && service.supported().contains(OutputType.JSON);

    ProcessingContext ctx = new ProcessingContext(match, method, code, profile, serverTiming, includeContent);
    if (serviceSupportsJson) {
      processJson(req, res, config, ctx);
    } else {
      processXml(req, res, config, ctx);
    }
  }

  /**
   * Handles requests whose service supports direct JSON output, bypassing the XSLT pipeline.
   */
  @SuppressWarnings("java:S3776") // sequential HTTP protocol steps; splitting would harm readability
  private void processJson(HttpServletRequest req, HttpServletResponse res, BerliozConfig config,
      ProcessingContext ctx) throws IOException {

    JsonResponse json = new JsonResponse(req, res, config, ctx.match, ctx.profile);

    // Indicate that the representation may vary depending on the encoding
    if (config.enableCompression()) {
      res.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
    }

    // Compute the ETag for the request if cacheable and method is GET or HEAD
    String etag = null;
    boolean cacheable = ctx.errorCode == null && ctx.match.isCacheable()
        && (ctx.method == HttpMethod.GET || ctx.method == HttpMethod.HEAD);
    if (cacheable) {
      String etagJSON = json.getEtag();
      if (etagJSON != null) {
        etag = '"' + SHA256.hash(config.getETagSeed() + "~" + etagJSON) + '"';
        applyCacheHeaders(res, config, ctx.match, etag);

        // Check conditional request headers (may return 304 without generating content)
        if (!HttpHeaderUtils.checkIfHeaders(req, res, new ServiceInfo(etag))) return;

      } else {
        cacheable = false;
      }
    }

    if (!cacheable) applyNoCacheHeaders(res);

    // Generate JSON content
    long start = System.nanoTime();
    String content = json.generate();
    long end = System.nanoTime();
    if (ctx.profile && LOGGER.isInfoEnabled()) {
      LOGGER.info("JSON content generated in {} ms", ProfileFormat.format(end - start));
    }
    if (ctx.serverTiming) {
      ServerTimingHeader.addMetricNano(res, "json", "JSON Response", end - start);
    }

    // Examine status
    ContentStatus status = json.getStatus();
    res.setStatus(Objects.requireNonNullElseGet(ctx.errorCode, status::code));

    // If errors occurred and should percolate
    if (checkAndSendError(req, res, status, json.getError())) return;

    // Redirection
    if (handleRedirect(req, res, status, json.getRedirectURL())) return;

    // Apply generator response headers
    json.getHeaders().forEach(res::setHeader);

    // Write JSON — with optional GZip compression
    res.setContentType("application/json;charset=UTF-8");
    res.setCharacterEncoding("UTF-8");
    BerliozOutput jsonOutput = new BerliozOutput() {
      public CharSequence content() { return content; }
      public String getMediaType() { return "application/json"; }
      public String getEncoding() { return "UTF-8"; }
    };
    writeOutput(req, res, jsonOutput, etag, StandardCharsets.UTF_8, config, ctx.includeContent);
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
    boolean cacheable = ctx.errorCode == null && ctx.match.isCacheable();
    if (cacheable && (ctx.method == HttpMethod.GET || ctx.method == HttpMethod.HEAD)) {
      String etagXML = xml.getEtag();
      if (etagXML != null) {
        String etagXSL = transformer != null? transformer.getEtag() : null;
        etag = '"'+ SHA256.hash(config.getETagSeed()+"~"+etagXML+"--"+etagXSL)+'"';

        // Update the headers (they should also be included in case of redirect)
        applyCacheHeaders(res, config, ctx.match, etag);

        // Check if the conditions specified in the optional If headers are satisfied.
        if (!HttpHeaderUtils.checkIfHeaders(req, res, new ServiceInfo(etag))) return;

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
    res.setStatus(Objects.requireNonNullElseGet(ctx.errorCode, status::code));

    // If errors occurred and should percolate
    if (checkAndSendError(req, res, status, xml.getError())) return;

    // Redirection (Beta)
    if (handleRedirect(req, res, status, xml.getRedirectURL())) return;

    // Apply response headers set by generators (last-writer-wins per name).
    xml.getHeaders().forEach(res::setHeader);

    // Produce the output (XSLT transform or pass through raw XML)
    BerliozOutput result = executeTransform(content, req, xml, transformer, res, ctx.profile, ctx.serverTiming);

    // Resolve and validate encoding from XSLT output; canonical name is safe for HTTP headers
    Charset charset = resolveCharset(result.getEncoding());

    // Update content type from XSLT transform result (MUST be specified before the output is requested)
    String ctype = result.getMediaType()+";charset="+charset.name();
    res.setContentType(ctype);
    res.setCharacterEncoding(charset.name());
    if (!config.getContentType().equals(ctype)) {
      LOGGER.info("Updating content type to {}", ctype);
      config.setContentType(ctype);
    }

    // Write the response body, applying GZip compression when appropriate
    writeOutput(req, res, result, etag, charset, config, ctx.includeContent);
  }

  /**
   * Returns the service matching {@code path} and {@code method}, falling back to GET when the
   * global option {@link BerliozOption#HTTP_GET_VIA_POST} allows POST requests to be treated as GET
   * (backward compatibility).
   */
  private static @Nullable MatchingService findMatch(ServiceRegistry services, String path, HttpMethod method) {
    // No matching service (backward compatibility)
    MatchingService match = services.get(path, method);
    if (match == null && method == HttpMethod.POST && GlobalSettings.has(BerliozOption.HTTP_GET_VIA_POST)) {
      match = services.get(path, HttpMethod.GET);
    }
    return match;
  }

  /**
   * Sends the appropriate error response when no service matches the request. For non-GET/HEAD
   * methods Berlioz first checks whether the path is known for other methods and replies with
   * {@code 405 Method Not Allowed} instead of {@code 404} when it is.
   */
  private void handleNoMatch(HttpServletRequest req, HttpServletResponse res,
      ServiceRegistry services, String path, HttpMethod method) {
    // If the method is different from GET or HEAD, look if it matches any other URL (just in case)
    if (!(method == HttpMethod.HEAD || method == HttpMethod.GET)) {
      List<String> methods = services.allows(path);
      if (!methods.isEmpty()) {
        String allowed = HttpHeaderUtils.allow(methods);
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
   * Forwards an error to the client when a generator threw an exception and the global option
   * {@link BerliozOption#ERROR_GENERATOR_CATCH} is not set.
   *
   * @return {@code true} when an error was sent and the caller must return immediately.
   */
  private boolean checkAndSendError(HttpServletRequest req, HttpServletResponse res,
      ContentStatus status, @Nullable Exception error) {
    if (error == null || GlobalSettings.has(BerliozOption.ERROR_GENERATOR_CATCH)) return false;
    sendError(req, res, status.code(), "The service failed because of errors thrown by generators", error);
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
    if (!config.hasControl(req)) return profile;

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
   * @param content       The XML content to transform.
   * @param req           The HTTP servlet request (passed through to the transformer).
   * @param xml           The XML response (provides the matched service).
   * @param transformer   The transformer to use, or {@code null} to pass XML through unchanged.
   * @param res           The HTTP servlet response (used for server-timing headers).
   * @param profile       Whether to log profiling information.
   * @param serverTiming  Whether to add a Server-Timing header entry.
   * @return The transformed (or raw XML) output.
   */
  private BerliozOutput executeTransform(String content, HttpServletRequest req, XmlResponse xml,
                                         @Nullable XsltTransformer transformer, HttpServletResponse res, boolean profile, boolean serverTiming) {
    if (transformer == null) return new XmlContent(content);

    XsltTransformResult xslresult = transformer.transform(content, req, xml.getService());
    if (profile && LOGGER.isInfoEnabled()) {
      LOGGER.info("XSLT Transformation {} ms", ProfileFormat.format(xslresult.time()));
    }
    if (serverTiming) {
      ServerTimingHeader.addMetricNano(res, "xslt", "XSLT Transform", xslresult.time());
    }
    // Signal the client that the service is temporarily unavailable when the transform failed
    if (xslresult.status() == Status.ERROR) {
      res.reset();
      res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }
    return xslresult;
  }

  /**
   * Writes the response body, applying GZip compression when both the configuration and the
   * client support it. For HEAD requests ({@code includeContent = false}) only the
   * {@code Content-Length} header is written without a body.
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

    // Only attempt compression when the config enables it and the media type is compressible
    boolean compressible = config.enableCompression() && HttpHeaderUtils.isCompressible(result.getMediaType());
    if (compressible && HttpHeaderUtils.acceptsGZipCompression(req)) {
      byte[] compressed = ResourceCompressor.compress(result.content(), charset);
      if (compressed.length > 0) {
        res.setIntHeader(HttpHeaders.CONTENT_LENGTH, compressed.length);
        res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        // ETag must reflect the encoding; replace it with the GZip variant
        if (etag != null) {
          res.setHeader(HttpHeaders.ETAG, HttpHeaderUtils.getETagForGZip(etag));
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
      res.setIntHeader(HttpHeaders.CONTENT_LENGTH, CharsetUtils.length(result.content(), charset));
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
    Integer error = (Integer) req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);

    if (error == null && !GlobalSettings.has(BerliozOption.ERROR_HANDLER)) {
      logError(code, message, ex, "Berlioz sending error to Web container {} [{}]");
      try {
        res.sendError(code, message);
      } catch (IOException e) {
        LOGGER.error("Failed to send error {} [{}] to client", message, code, e);
      }
      return;
    }

    // Preserve the original error code when already handling an error; clamp to valid HTTP range to satisfy taint analysis
    int statusCode = Math.max(100, Math.min(599, error != null ? error : code));
    req.setAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE, statusCode);
    req.setAttribute(ErrorHandlerServlet.ERROR_MESSAGE, message);
    req.setAttribute(ErrorHandlerServlet.ERROR_REQUEST_URI, req.getRequestURI());
    req.setAttribute(ErrorHandlerServlet.ERROR_SERVLET_NAME, getBerliozConfig().getName());

    if (ex != null) {
      req.setAttribute(ErrorHandlerServlet.ERROR_EXCEPTION, ex);
      req.setAttribute(ErrorHandlerServlet.ERROR_EXCEPTION_TYPE, ex.getClass());
    }

    dispatchError(req, res, code, message, ex);
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
    } catch (IOException | ServletException e) {
      LOGGER.error("Failed to dispatch error response {} [{}]", message, code, e);
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
