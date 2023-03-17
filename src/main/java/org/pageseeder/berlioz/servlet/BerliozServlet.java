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
import java.util.List;
import java.util.Objects;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.http.*;
import org.pageseeder.berlioz.servlet.XSLTransformResult.Status;
import org.pageseeder.berlioz.util.CharsetUtils;
import org.pageseeder.berlioz.util.EntityInfo;
import org.pageseeder.berlioz.util.MD5;
import org.pageseeder.berlioz.util.ProfileFormat;
import org.pageseeder.berlioz.util.ResourceCompressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Berlioz servlet.
 *
 * <p>A berlioz servlet can only generate one content type and use one set of XSLT templates, these are defined at
 * initialisation. See {@link #init(ServletConfig)} for details.
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
 * they implement the {@link org.pageseeder.berlioz.content.Cacheable} interface).
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
 * <p>Non cacheable responses, always return:
 * <pre>
 *   Expires: 0
 *   Cache-Control: no-cache
 * </pre>
 *
 * <p>For security, the Berlioz administration parameters can be secures using a Berlioz control key.
 * The control key is a string that must be supplied as a parameter whenever one of the admin
 * parameters is used. Use the initialisation parameters to define a control key.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
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
   * Initialises the Berlioz Servlet.
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
  protected void service(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    try {
      HttpMethod method = HttpMethod.valueOf(req.getMethod());
      if (method == HttpMethod.OPTIONS) {
        doOptions(req, res);
      } else {
        process(req, res, method, method != HttpMethod.HEAD);
      }

    } catch (IllegalArgumentException ex) {
      sendError(req, res, HttpServletResponse.SC_NOT_IMPLEMENTED, "Unsupported HTTP method", null);
    }
  }

  @Override
  public void doHead(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, HttpMethod.HEAD, false);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, HttpMethod.GET, true);
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, HttpMethod.POST, true);
  }

  @Override
  public void doPut(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, HttpMethod.PUT, true);
  }

  @Override
  public void doDelete(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, HttpMethod.DELETE, true);
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
   * @throws ServletException To wrap any non IO exception.
   * @throws IOException For any IO exception.
   */
  private void process(HttpServletRequest req, HttpServletResponse res, HttpMethod method, boolean includeContent)
      throws ServletException, IOException {

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

    // Berlioz Control
    if (config.hasControl(req)) {

      // Clear the cache and reload the services
      boolean reload = isTrue(req.getParameter("berlioz-reload"));

      // Clear the XSLT cache if requested
      boolean clearCache = reload || isTrue(req.getParameter("clear-xsl-cache"));
      if (clearCache) { XSLTransformer.clearAllCache(); }

      // Allow ETags to be reset
      boolean resetEtags = reload || isTrue(req.getParameter("reset-etags"));
      if (resetEtags) { config.resetETagSeed(); }

      // Reload the global configuration
      if (reload) { GlobalSettings.load(); }

      // Clear the service configuration
      boolean clearServices = reload || isTrue(req.getParameter("reload-services"));
      if (clearServices) { loader.clear(); }

      // If profile specified on URL
      profile = profile || isTrue(req.getParameter("berlioz-profile"));
    }

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
    MatchingService match = services.get(path, method);

    // No matching service (backward compatibility)
    if (match == null && method == HttpMethod.POST && GlobalSettings.has(BerliozOption.HTTP_GET_VIA_POST)) {
      match = services.get(path, HttpMethod.GET);
    }

    // Still no matching service
    if (match == null) {
      // If the method is different from GET or HEAD, look if it matches any other URL (just in case)
      if (!(method == HttpMethod.HEAD || method == HttpMethod.GET)) {
        List<String> methods = services.allows(path);
        if (methods.size() > 0) {
          res.setHeader(HttpHeaders.ALLOW, HttpHeaderUtils.allow(methods));
          String message = "Only the following are allowed: "+HttpHeaderUtils.allow(methods);
          sendError(req, res, HttpServletResponse.SC_METHOD_NOT_ALLOWED, message, null);
          return;
        }
      }
      sendError(req, res, HttpServletResponse.SC_NOT_FOUND, "Unable to find "+req.getRequestURI(), null);
      LOGGER.debug("No matching service for: {}", req.getRequestURI());
      return;
    }

    // Prepare the XML Response
    XMLResponse xml = new XMLResponse(req, res, config, match, profile);
    if (serverTiming) xml.enableServerTiming();

    // Include the service as a header for information
    res.setHeader("X-Berlioz-Service", toSafeHeader(match.service().id()));
    LOGGER.debug("{} -> {}", path, match.service());

    // Is Berlioz used to handle an error?
    Integer code = (Integer)req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);

    // Identify the transformer
    XSLTransformer transformer = config.getTransformer(match.service());
    long start = System.nanoTime();

    // Indicate that the representation may vary depending on the encoding
    if (config.enableCompression()) {
      res.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
    }

    // Compute the ETag for the request if cacheable and method GET or HEAD
    String etag = null;
    boolean cacheable = code == null && match.isCacheable();
    if (cacheable && (method == HttpMethod.GET || method == HttpMethod.HEAD)) {
      String etagXML = xml.getEtag();
      if (etagXML != null) {
        String etagXSL = transformer != null? transformer.getEtag() : null;
        etag = '"'+MD5.hash(config.getETagSeed()+"~"+etagXML+"--"+etagXSL)+'"';

        // Update the headers (they should also be included in case of redirect)
        res.setDateHeader(HttpHeaders.EXPIRES, config.getExpiryDate());
        String cc = xml.getService().cache();
        if (cc.isEmpty()) {
          cc = config.getCacheControl();
        }
        res.setHeader(HttpHeaders.CACHE_CONTROL, toSafeHeader(cc));
        res.setHeader(HttpHeaders.ETAG, etag);

        // Check if the conditions specified in the optional If headers are satisfied.
        ServiceInfo info = new ServiceInfo(etag);
        if (!HttpHeaderUtils.checkIfHeaders(req, res, info)) return;

      } else {
        cacheable = false;
      }
    }

    // Prevents caching
    if (!cacheable) {
      res.setDateHeader(HttpHeaders.EXPIRES, 0);
      res.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    }

    // Generate the XML content
    String content = xml.generate();
    long end = System.nanoTime();
    if (profile) {
      LOGGER.info("Content generated in {} ms", ProfileFormat.format(end - start));
    }
    if (serverTiming) {
      ServerTimingHeader.addMetricNano(res,"xml", "XML Response", end - start);
    }

    // Examine the status
    ContentStatus status = xml.getStatus();
    if (code != null) {
      res.setStatus(code);
    } else {
      res.setStatus(status.code());
    }

    // If errors occurred and should percolate
    if (xml.getError() != null && !GlobalSettings.has(BerliozOption.ERROR_GENERATOR_CATCH)) {
      sendError(req, res, status.code(), "The service failed because of errors thrown by generators", xml.getError());
      return;
    }

    // Redirection (Beta)
    if (ContentStatus.isRedirect(status)) {
      String url = xml.getRedirectURL();
      LOGGER.debug("Redirecting to: {} with {}", url, status.code());
      res.reset();
      res.sendRedirect(url);
      res.setStatus(status.code());
      return;
    }

    // Produce the output
    BerliozOutput result;
    if (transformer != null) {
      XSLTransformResult xslresult = transformer.transform(content, req, xml.getService());
      if (profile) {
        LOGGER.info("XSLT Transformation {} ms", ProfileFormat.format(xslresult.time()));
      }
      if (serverTiming) {
        ServerTimingHeader.addMetricNano(res, "xslt", "XSLT Transform", xslresult.time());
      }
      result = xslresult;
      if (xslresult.status() == Status.ERROR) {
        res.reset();
        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      }
    } else {
      result = new XMLContent(content);
    }

    // Update content type from XSLT transform result (MUST be specified before the output is requested)
    String ctype = result.getMediaType()+";charset="+result.getEncoding();
    res.setContentType(ctype);
    res.setCharacterEncoding(result.getEncoding()); // TODO check with different encoding
    if (!config.getContentType().equals(ctype)) {
      LOGGER.info("Updating content type to {}", ctype);
      config.setContentType(ctype);
    }

    // Apply Compression if necessary
    boolean isCompressed = config.enableCompression() && HttpHeaderUtils.isCompressible(result.getMediaType());
    if (isCompressed) {

      if (HttpHeaderUtils.acceptsGZipCompression(req)) {
        byte[] compressed = ResourceCompressor.compress(result.content(), Charset.forName(result.getEncoding()));
        if (compressed.length > 0) {
          res.setIntHeader(HttpHeaders.CONTENT_LENGTH, compressed.length);
          res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
          if (etag != null) {
            res.setHeader(HttpHeaders.ETAG, HttpHeaderUtils.getETagForGZip(etag));
          }
          if (includeContent) {
            ServletOutputStream out = res.getOutputStream();
            out.write(compressed);
            out.flush();
          }
        } else {
          isCompressed = false; // Compression failed
        }
      } else {
        isCompressed = false; // Client does not accept Compression
      }
    }

    // Copy the uncompressed version if needed
    if (!isCompressed) {
      if (includeContent) {
        PrintWriter out = res.getWriter();
        out.print(result.content());
        out.flush();
      } else {
        // We need to calculate when we don't include the content
        res.setIntHeader(HttpHeaders.CONTENT_LENGTH, CharsetUtils.length(result.content(), Charset.forName(result.getEncoding())));
      }
    }

  }

  /**
   * Handles the specified error.
   *
   * @param req     The HTTP Servlet request.
   * @param res     The HTTP Servlet response.
   * @param code    The HTTP status response code.
   * @param message The message for the message.
   * @param ex      Any caught exception (may be <code>null</code>).
   *
   * @throws IOException      The HTTP Servlet Request.
   * @throws ServletException Should any error occur at this point.
   */
  private void sendError(HttpServletRequest req, HttpServletResponse res, int code, String message, @Nullable Exception ex)
      throws IOException, ServletException {

    // Is Berlioz already handling an error?
    Integer error = (Integer)req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);

    // Handle internally
    if (error != null || GlobalSettings.has(BerliozOption.ERROR_HANDLER)) {
      req.setAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE, error != null? error : code);
      req.setAttribute(ErrorHandlerServlet.ERROR_MESSAGE, message);
      req.setAttribute(ErrorHandlerServlet.ERROR_REQUEST_URI, req.getRequestURI());
      req.setAttribute(ErrorHandlerServlet.ERROR_SERVLET_NAME, getBerliozConfig().getName());
      // TODO: also add Berlioz specific data

      // If an exception has occurred
      if (ex != null) {
        req.setAttribute(ErrorHandlerServlet.ERROR_EXCEPTION, ex);
        req.setAttribute(ErrorHandlerServlet.ERROR_EXCEPTION_TYPE, ex.getClass());
      }
      // Use the error handler if defined, otherwise use the default error handling options
      RequestDispatcher handler = this.errorHandler;
      if (handler != null) {
        String format = "Berlioz forwarding error {} [{}] to handler";
        if (code >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
          LOGGER.error(format, message, code, ex);
        } else {
          LOGGER.warn(format, message, code, ex);
        }
        handler.forward(req, res);
      } else {
        String format = "Berlioz handling error {} [{}] to handler";
        if (code >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
          LOGGER.error(format, message, code, ex);
        } else {
          LOGGER.warn(format, message, code, ex);
        }
        ErrorHandlerServlet.handle(req, res);
      }
    } else {
      String format = "Berlioz sending error to Web container {} [{}] to handler";
      if (code >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
        LOGGER.error(format, message, code, ex);
      } else {
        LOGGER.warn(format, message, code, ex);
      }
      res.sendError(code, req.getRequestURI());
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

  private BerliozConfig getBerliozConfig() {
    return Objects.requireNonNull(this.berliozConfig, "Berlioz is not configured!");
  }

  private ServiceRegistry getServiceRegistry() {
    return Objects.requireNonNull(this.serviceRegistry, "Berlioz services are not configured!");
  }

  // Private internal class
  // ==============================================================================================

  /**
   * Provide a simple entity information for the service.
   */
  private static final class ServiceInfo implements EntityInfo {

    /**
     * The wrapped ETag
     */
    private final String _etag;

    /**
     * Creates a new service info instance.
     *
     * @param etag The etag.
     */
    public ServiceInfo(String etag) {
      this._etag = etag;
    }

    /**
     * @return the etag for this service.
     */
    @Override
    public @NonNull String getETag() {
      return this._etag;
    }

    /**
     * @return Always "text/html".
     */
    @Override
    public @NonNull String getMimeType() {
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
