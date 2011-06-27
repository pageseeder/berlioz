/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.ContentManager;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.ServiceRegistry;
import org.weborganic.berlioz.http.HttpHeaderUtils;
import org.weborganic.berlioz.http.HttpHeaders;
import org.weborganic.berlioz.http.HttpMethod;
import org.weborganic.berlioz.servlet.XSLTransformResult.Status;
import org.weborganic.berlioz.util.CharsetUtils;
import org.weborganic.berlioz.util.EntityInfo;
import org.weborganic.berlioz.util.MD5;
import org.weborganic.berlioz.util.ResourceCompressor;

/**
 * Default Berlioz servlet.
 * 
 * <p>A berlioz servlet can only generate one content type and use one set of XSLT templates, these are defined at 
 * initialisation. See {@link #init(ServletConfig)} for details.
 * 
 * <p>This servlet will pass on HTTP parameters to the underlying generators for the service it matches.
 * 
 * <h3>Compression</h3>
 * 
 * <p>The global property {@value ENABLE_HTTP_COMPRESSION} can be used to enable or disable HTTP compression using
 * the <code>Content-Encoding</code> HTTP Header.
 * 
 * <p>When HTTP compression is enabled and the HTTP client supports it, the headers are modified as:
 * <pre>
 *   Vary: Accept-Encoding
 *   Content-Length: <i>[Length of compressed content]</i>
 *   Content-Encoding: gzip
 *   Etag: "<i>[Uncompressed etag]</i>-gzip"
 * </pre>
 * 
 * <h3>XSLT Caching</h3>
 * 
 * <p>The XSLT templates are cached by default unless the {@value XSLTransformer#ENABLE_CACHE} property was set to 
 * <code>false</code>; in other words XSLT templates are parsed once and reused for each call. The special parameter
 * <code>clear-xsl-cache</code> can be used to clear the XSLT cache.
 *
 * <h3>HTTP Caching</h3>
 * 
 * <p>The response is considered cacheable if all the generators in the matching service are cacheable; that is if 
 * they implement the {@link Cacheable} interface).
 * 
 * <p>For cacheable responses, Berlioz will return the following Headers:
 * <pre>
 *   Expires: <i>[Expiry date 1 year from now]</i>
 *   Cache-Control: max-age=<i>[max age in seconds]</i>, must-revalidate
 *   Etag: <i>[Etag for generator]</i>
 * </pre>
 * 
 * <p>The global property {@value HTTP_MAX_AGE} can be used to define the maximum age used in the
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
 * @author Christophe Lauret (Weborganic)
 * @version 20 July 2010
 */
public class BerliozServlet extends HttpServlet {

  /**
   * Name of the global property to use to enable HTTP compression using the 
   * <code>Content-Encoding</code> of compressible content.
   * 
   * <p>The property value is <code>true</code> by default.
   * 
   * @deprecated Use BerliozConfig#ENABLE_HTTP_COMPRESSION
   */
  @Deprecated public static final String ENABLE_HTTP_COMPRESSION = BerliozConfig.ENABLE_HTTP_COMPRESSION;

  /**
   * Name of the global property to use to specify the max age of the <code>Cache-Control</code>
   * HTTP header of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   * 
   * @deprecated Use BerliozConfig#HTTP_MAX_AGE
   */
  @Deprecated public static final String HTTP_MAX_AGE = BerliozConfig.HTTP_MAX_AGE;

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2010071926180001L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozServlet.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * The transformer factory to generate the templates
   */
  private transient BerliozConfig _config;

  /**
   * The transformer factory to generate the templates
   */
  private transient XSLTransformer _transformer;

  /**
   * The services managed by this servlet.
   */
  private transient ServiceRegistry _services;

  /**
   * The request dispatcher to forward to the error handler. 
   */
  private transient RequestDispatcher _errorHandler;

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
  public final void init(ServletConfig servletConfig) throws ServletException {
    BerliozConfig config = new BerliozConfig(servletConfig);
    this._config = config;
    this._transformer = config.newTransformer();
    this._services = ContentManager.getDefaultRegistry();
    this._errorHandler = servletConfig.getServletContext().getNamedDispatcher("ErrorHandlerServlet");
    if (this._errorHandler == null) {
      LOGGER.warn("Error is not defined, using default error handler for the Web Application");
    }
  }

  // Standard HTTP Methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Handles a HEAD request.
   * 
   * {@inheritDoc}
   */
  @Override public final void doHead(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, false);
  }

  /**
   * Handles a GET request.
   * 
   * {@inheritDoc}
   */
  @Override public final void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, true);
  }

  /**
   * Handles a POST request.
   * 
   * {@inheritDoc}
   */
  @Override public final void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, true);
  }

  /**
   * Handles a PUT request.
   * 
   * {@inheritDoc}
   */
  @Override public final void doPut(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, true);
  }

  /**
   * Handles a DELETE request.
   * 
   * {@inheritDoc}
   */
  @Override public final void doDelete(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res, true);
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
  protected final void process(HttpServletRequest req, HttpServletResponse res, boolean includeContent)
      throws ServletException, IOException {

    // Use Berlioz config locally
    BerliozConfig config = this._config;

    // Setup and ensure that we use UTF-8 to read data
    req.setCharacterEncoding("utf-8");
    res.setContentType(config.getContentType());

    // Notify the client not to attempt a range request if it does attempt to do so
    if (req.getHeader(HttpHeaders.RANGE) != null)
      res.setHeader(HttpHeaders.ACCEPT_RANGES, "none");

    // Determine the method in use.
    HttpMethod method = HttpMethod.valueOf(req.getMethod());
    
    // Berlioz Control
    if (config.hasControl(req)) {

      // Clear the cache and reload the services
      boolean reload = "true".equals(req.getParameter("berlioz-reload"));

      // Clear the XSLT cache if requested
      boolean clearCache = reload || "true".equals(req.getParameter("clear-xsl-cache"));
      if (clearCache && this._transformer != null) { this._transformer.clearCache(); }

      // Allow ETags to be reset
      boolean resetEtags = reload || "true".equals(req.getParameter("reset-etags"));
      if (resetEtags) { config.resetETagSeed(); }

      // Clear the service configuration
      boolean clearServices = reload || "true".equals(req.getParameter("reload-services"));
      if (clearServices) { ContentManager.clear(); }
    }

    // Start handling XML content
    long t0 = System.currentTimeMillis();
    String path = HttpRequestWrapper.getBerliozPath(req);
    MatchingService match = this._services.get(path, method);

    // No matching service (backward compatibility)
    if (match == null && method == HttpMethod.POST && GlobalSettings.get("berlioz.http.getviapost", true)) {
      match = ContentManager.getService(path, "GET");
    }

    // Still no matching service
    if (match == null) {
      // If the method is different from GET or HEAD, look if it matches any other URL (just in case)
      if (!(method == HttpMethod.HEAD || method == HttpMethod.GET)) {
        List<String> methods = this._services.allows(path);
        if (methods.size() > 0) {
          res.setHeader(HttpHeaders.ALLOW, HttpHeaderUtils.allow(methods));
          res.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, req.getRequestURI());
          return;
        }
      }
      res.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
      LOGGER.debug("No matching service for: " + req.getRequestURI());
      return;
    }

    // Prepare the XML Response
    XMLResponse xml = new XMLResponse(req, res, config.getEnvironment(), match);

    // Include the service as a header for information
    res.setHeader("X-Berlioz-Service", match.service().id());

    // Compute the ETag for the request if cacheable and method GET or HEAD
    String etag = null;
    if (match.isCacheable() && (method == HttpMethod.GET || method == HttpMethod.HEAD)) {
      String etagXML = xml.getEtag();
      String etagXSL = this._transformer != null? this._transformer.getEtag() : null;
      etag = '"'+MD5.hash(config.getETagSeed()+"~"+etagXML+"--"+etagXSL)+'"';

      // Check if the conditions specified in the optional If headers are satisfied.
      ServiceInfo info = new ServiceInfo(etag);
      if (!HttpHeaderUtils.checkIfHeaders(req, res, info)) {
        return;
      }

      // Update the headers 
      res.setDateHeader(HttpHeaders.EXPIRES, config.getExpiryDate());
      String cc = xml.getService().cache();
      if (cc == null) cc = config.getCacheControl();
      res.setHeader(HttpHeaders.CACHE_CONTROL, cc);
      res.setHeader(HttpHeaders.ETAG, etag);

    // Prevents caching
    } else {
      res.setDateHeader(HttpHeaders.EXPIRES, 0);
      res.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
    }

    // Generate the XML content
    String content = xml.generate();
    long t1 = System.currentTimeMillis();
    LOGGER.debug("Content generated in {} ms", (t1 - t0));

    // Redirect if required - Phase this feature out
    String url = req.getParameter("redirect-url");
    if (url == null)
      url = (String)req.getAttribute("redirect-url");
    if (url != null && !"".equals(url)) {
      LOGGER.warn("Redirecting URL using deprecated 'redirect-url' - will be removed in future releases.");
      res.sendRedirect(url);

    // Produce the output
    } else {

      // setup the output
      BerliozOutput result = null;
      if (this._transformer != null) {
        XSLTransformResult xslresult = this._transformer.transform(content, req, xml.getService());
        LOGGER.debug("XSLT Transformation {} ms", xslresult.time());
        result = xslresult;
        if (xslresult.status() == Status.ERROR) {
          res.reset();
          res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
      boolean isCompressed = HttpHeaderUtils.isCompressible(result.getMediaType()) && config.enableCompression();
      if (isCompressed) {

        // Indicate that the representation may vary depending on the encoding
        res.setHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
        if (HttpHeaderUtils.acceptsGZipCompression(req)) {
          byte[] compressed = ResourceCompressor.compress(result.content(), Charset.forName(result.getEncoding()));
          if (compressed.length > 0) {
            res.setIntHeader(HttpHeaders.CONTENT_LENGTH, compressed.length);
            res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
            if (etag != null)
              res.setHeader(HttpHeaders.ETAG, HttpHeaderUtils.getETagForGZip(etag));
            if (includeContent) {
              ServletOutputStream out = res.getOutputStream();
              out.write(compressed);
              out.flush();
            }
          } else isCompressed = false; // Compression failed
        } else isCompressed = false; // Client does not accept Compression
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
  }

  // Private internal class =======================================================================

  /**
   * Provide a simple entity information for the service.
   * 
   * @author Christophe Lauret
   * @version 19 July 2010
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
    public String getETag() {
      return this._etag;
    }

    /**
     * {@inheritDoc}
     * @return Always "text/html".
     */
    public String getMimeType() {
      return "text/html";
    }

    /**
     * {@inheritDoc}
     * @return Always -1 as we use the etag for caching.
     */
    public long getLastModified() {
      return -1;
    }

  }

}
