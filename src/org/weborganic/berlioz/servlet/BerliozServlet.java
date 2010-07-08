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
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Calendar;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.util.EntityInfo;
import org.weborganic.berlioz.util.HttpHeaderUtils;
import org.weborganic.berlioz.util.HttpHeaders;
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
 * @author Christophe Lauret (Weborganic)
 * @version 8 July 2010
 */
public class BerliozServlet extends HttpServlet {

  /**
   * Name of the global property to use to enable HTTP compression using the 
   * <code>Content-Encoding</code> of compressible content.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  public static final String ENABLE_HTTP_COMPRESSION = "berlioz.http.compression";

  /**
   * Name of the global property to use to specify the max age of the <code>Cache-Control</code>
   * HTTP header of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   */
  public static final String HTTP_MAX_AGE = "berlioz.http.max-age";

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 2010070826180001L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozServlet.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * The Servlet configuration.
   */
  private ServletConfig servletConfig;

  /**
   * Set the content type.
   */
  private String contentType;

  /**
   * The environment. 
   */
  private transient Environment env;

  /**
   * The request dispatcher to forward to the error handler. 
   */
  private transient RequestDispatcher errorHandler;

  /**
   * The transformer factory to generate the templates
   */
  private transient XSLTransformer transformer;

// servlet methods --------------------------------------------------------------------------------

  /**
   * Initialises the Berlioz Servlet.
   * 
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>content-type</code> to specify the content type used by this Berlioz instance.
   *   <li><code>stylesheet</code> to specify the XSLT stylesheet to use for this Berlioz instance.
   * </ul>
   * 
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  public void init(ServletConfig config) throws ServletException {
    this.servletConfig = config;
    // get the WEB-INF directory
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    this.contentType = this.getInitParameter("content-type", "text/html;charset=utf-8");
    String stylePath = this.getInitParameter("stylesheet", "/xslt/html/global.xsl");
    File styleSheet = new File(webinfPath, stylePath);
    this.transformer = new XSLTransformer(styleSheet);
    // used to dispatch
    this.errorHandler = context.getNamedDispatcher("ErrorHandlerServlet");
    if (this.errorHandler == null)
      throw new ServletException("The error handler must be configured and named ErrorHandlerServlet.");
    this.env = new HttpEnvironment(contextPath, webinfPath);
  }

  /**
   * Handles a GET request.
   * 
   * {@inheritDoc}
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res);
  }

  /**
   * Handles a POST request.
   * 
   * {@inheritDoc}
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    process(req, res);
  }

  /**
   * Handles requests.
   * 
   * {@inheritDoc}
   */
  protected void process(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    // Setup and ensure that we use UTF-8
    req.setCharacterEncoding("utf-8");
    res.setContentType(this.contentType);

    // Notify the client not to attempt a range request if it does attempt to do so
    if (req.getHeader(HttpHeaders.RANGE) != null)
      res.setHeader(HttpHeaders.ACCEPT_RANGES, "none");

    // Clear the cache if requested
    boolean clearCache = "true".equals(req.getParameter("clear-xsl-cache"));
    if (clearCache) {
      this.transformer.clearCache();
    }

    // Start handling XML content
    long t0 = System.currentTimeMillis();
    XMLResponse xml = new XMLResponse(req, res, this.env);

    // No matching service
    if (xml.getService() == null) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND, req.getRequestURI());
      LOGGER.debug("No Matching service for: " + req.getRequestURI());
      return;
    }

    // Compute the ETag for the request if cacheable and method GET
    String etag = null;
    if (xml.isCacheable() && "get".equalsIgnoreCase(req.getMethod())) {
      String XMLEtag = xml.getEtag();
      String XSLEtag = this.transformer.getEtag();
      etag = '"'+MD5.hash(XMLEtag+"--"+XSLEtag)+'"';

      // Check if the conditions specified in the optional If headers are satisfied.
      ServiceInfo info = new ServiceInfo(etag);
      if (!HttpHeaderUtils.checkIfHeaders(req, res, info)) {
        return;
      }

      // Update the headers 
      res.setDateHeader(HttpHeaders.EXPIRES, getExpiryDate());
      res.setHeader(HttpHeaders.CACHE_CONTROL, "max-age="+GlobalSettings.get(HTTP_MAX_AGE, 60)+", must-revalidate");
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

    // Redirect if required
    String url = req.getParameter("redirect-url");
    if (url == null)
      url = (String)req.getAttribute("redirect-url");
    if (url != null && !"".equals(url)) {
      res.sendRedirect(url);

    // produce the output
    } else {
      
      // setup the output
//      ByteArrayOutputStream errors = new ByteArrayOutputStream();

      try {
        XSLTransformResult result = this.transformer.transform(content, req, xml.getService());
        LOGGER.debug("XSLT Transformation {} ms", result.time());

        // Update content type from XSLT transform result 
        res.setContentType(result.getContentType()+";charset="+result.getEncoding());

        boolean isCompressed = HttpHeaderUtils.isCompressible(result.getContentType())
                            && GlobalSettings.get(ENABLE_HTTP_COMPRESSION, true);
        if (isCompressed) {

          // Indicate that the representation may vary depending on the encoding
          res.setHeader("Vary", "Accept-Encoding");
          if (HttpHeaderUtils.acceptsGZipCompression(req)) {
            byte[] compressed = ResourceCompressor.compress(result.content(), Charset.forName(result.getEncoding()));
            if (compressed.length > 0) {
              res.setIntHeader(HttpHeaders.CONTENT_LENGTH, compressed.length);
              res.setHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
              if (etag != null)
                res.setHeader(HttpHeaders.ETAG, HttpHeaderUtils.getETagForGZip(etag));
              ServletOutputStream out = res.getOutputStream();
              out.write(compressed);
            } else isCompressed = false;
          } else isCompressed = false;
        }

        // Copy the uncompressed version if needed
        if (!isCompressed) {
          PrintWriter out = res.getWriter();
          out.print(result.content());        
        }

      // very likely to be an error in the XML or a dynamic error
      } catch (Exception ex) {
        /*
        if (!res.isCommitted()) {
          res.resetBuffer();
          req.setAttribute("exception", ex);
          if (errors.size() > 0)
            req.setAttribute("extra", errors.toString("utf-8"));
          req.setAttribute("xml", content);
          this.errorHandler.forward(req, res);
        } else {
          LOGGER.error("A dynamic XSLT error was caught", ex);
//TODO          res.sendRedirect(req.getServletPath()+"/error/500");
        }
        */
      } finally {
        /*
        if (errors.size() > 0)
          LOGGER.debug(errors.toString("utf-8"));
          */
      }
    }
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Returns the value for the specified init parameter name.
   * 
   * <p>If <code>null</code> returns the default value.
   * 
   * @param name The name of the init parameter.
   * @param def  The default value if the parameter value is <code>null</code> 
   * 
   * @return The values for the specified init parameter name.
   */
  private String getInitParameter(String name, String def) {
    String value = this.servletConfig.getInitParameter(name);
    return (value != null)? value : def;
  }

  /**
   * Expiry date is a year from now.
   */
  private static long getExpiryDate() {
    Calendar calendar = Calendar.getInstance();
    calendar.roll(Calendar.YEAR, 1);
    return calendar.getTimeInMillis();
  }

  /**
   * Provide a simple entity information for the service.
   * 
   * @author Christophe Lauret
   * @version 31 May 2010
   */
  private static class ServiceInfo implements EntityInfo {

    private final String _etag;
    
    public ServiceInfo(String etag) {
      this._etag = etag;
    }

    public String getETag() {
      return this._etag;
    }

    public String getMimeType() {
      return "text/html";
    }

    public long getLastModified() {
      return -1;
    }

  }

}
