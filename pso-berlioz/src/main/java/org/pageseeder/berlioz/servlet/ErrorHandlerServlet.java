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
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.ErrorID;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.error.DetailLevel;
import org.pageseeder.berlioz.error.LegacyError;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.error.Problems;
import org.pageseeder.berlioz.http.HttpHeaders;
import org.pageseeder.berlioz.http.HttpResponses;
import org.pageseeder.berlioz.xml.XmlStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.pageseeder.berlioz.xslt.XsltTransformException;

/**
 * Servlet used to handle errors for a uniform response.
 *
 * <p>This servlet intercepts errors forwarded by the servlet container via the
 * {@code <error-page>} mechanism and renders them as either an RFC 9457
 * {@code <problem>} XML document (default since 0.14.0) or the legacy Berlioz
 * {@code <error>} XML format (opt-in via {@code berlioz.errors.problem=false}).
 *
 * <p>For {@code .html} requests the XML is further transformed by an XSLT stylesheet
 * (custom via {@link BerliozOption#ERROR_STYLESHEET}, or the built-in failsafe template).
 * For other extensions ({@code .xml}, {@code .src}, etc.) raw XML is returned directly.
 * Static resource errors ({@code .jpg}, {@code .png}, {@code .css}, {@code .js}) are
 * answered with an empty body and the original status code.
 *
 * <p>This servlet should be configured as:
 *
 * <pre>{@code
 * <!-- Handler for errors (this servlet does not need to be mapped to anything) -->
 * <servlet>
 *   <servlet-name>ErrorHandlerServlet</servlet-name>
 *   <servlet-class>org.pageseeder.berlioz.servlet.ErrorHandlerServlet</servlet-class>
 *   <load-on-startup>2</load-on-startup>
 * </servlet>
 * }</pre>
 *
 * @author Christophe Lauret
 * @version 0.14.0
 * @since 0.6
 */
public final class ErrorHandlerServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = -2993007522046978323L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandlerServlet.class);

  // Attributes set for error handlers.
  // ---------------------------------------------------------------------------------------------

  /**
   * Exception thrown (Exception).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_EXCEPTION} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_EXCEPTION = RequestDispatcher.ERROR_EXCEPTION;

  /**
   * Class of exception thrown (Class).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_EXCEPTION_TYPE} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_EXCEPTION_TYPE = RequestDispatcher.ERROR_EXCEPTION_TYPE;

  /**
   * Any attached message (String).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_MESSAGE} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_MESSAGE = RequestDispatcher.ERROR_MESSAGE;

  /**
   * The offending request URI (String).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_REQUEST_URI} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_REQUEST_URI = RequestDispatcher.ERROR_REQUEST_URI;

  /**
   * The name of the offending servlet (String).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_SERVLET_NAME} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_SERVLET_NAME = RequestDispatcher.ERROR_SERVLET_NAME;

  /**
   * The HTTP Status code (Integer).
   *
   * @deprecated Use {@link RequestDispatcher#ERROR_STATUS_CODE} instead; this duplicates the
   *             standard servlet API constant.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public static final String ERROR_STATUS_CODE = RequestDispatcher.ERROR_STATUS_CODE;

  /**
   * The Berlioz error ID (String).
   */
  public static final String BERLIOZ_ERROR_ID = "org.pageseeder.berlioz.error_id";

  static final String ERROR_RENDERING_DEPTH = "org.pageseeder.berlioz.error.rendering-depth";

  static final String ORIGINAL_ERROR_EXCEPTION = "org.pageseeder.berlioz.error.original-exception";

  /**
   * The default list of extensions to preserve.
   */
  private static final String FORWARD_EXTENSIONS = ".html,.xml";

  /**
   * The default list of extensions to ignore.
   */
  private static final String IGNORE_EXTENSIONS = ".jpg,.png,.css,.js";

  /**
   * The default extension to use for extensions which are neither preserved nor ignored.
   */
  private static final String AUTO_EXTENSION = ".auto";

  /**
   * The default extension to use for extensions which are neither preserved nor ignored.
   */
  private static final String DEFAULT_EXTENSION = ".html";

  // servlet methods ----------------------------------------------------------------------

  /**
   * The extension to preserve.
   */
  private final Set<String> forwardExtensions = new HashSet<>();

  /**
   * The extension to ignore.
   */
  private final Set<String> ignoreExtensions = new HashSet<>();

  /**
   * The default extension to use for extensions which are neither preserved nor ignored.
   */
  private String defaultExtension = DEFAULT_EXTENSION;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    String preserve = config.getInitParameter("forward-extensions");
    if (preserve == null) {
      preserve = FORWARD_EXTENSIONS;
    }
    Collections.addAll(forwardExtensions, preserve.split(","));
    String ignore = config.getInitParameter("ignore-extensions");
    if (ignore == null) {
      ignore = IGNORE_EXTENSIONS;
    }
    Collections.addAll(ignoreExtensions, ignore.split(","));
    String defExt = config.getInitParameter("forward-default");
    defaultExtension = defExt != null ? defExt : DEFAULT_EXTENSION;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {
      handle(req, res);
    } catch (IOException | ServletException ex) {
      LOGGER.error("Failed to handle error for {}", req.getRequestURI(), ex);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) {
    try {
      handle(req, res);
    } catch (IOException | ServletException ex) {
      LOGGER.error("Failed to handle error for {}", req.getRequestURI(), ex);
    }
  }

  /**
   * Handle the errors using the fail-safe options and templates.
   *
   * @param req The servlet request.
   * @param res The servlet response.
   * @throws ServletException Should a servlet exception occur.
   * @throws IOException      Should an I/O error occur.
   */
  public void handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    // Grab the status code (Default to 200 OK)
    int code = getErrorCode(req);

    // Get URI of error handler
    String uri = req.getRequestURI();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Error handler for URI:{}", uri);
      LOGGER.debug(RequestDispatcher.ERROR_MESSAGE + ":{}", req.getAttribute(RequestDispatcher.ERROR_MESSAGE));
      LOGGER.debug(RequestDispatcher.ERROR_STATUS_CODE + ":{}", req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE));
      LOGGER.debug(RequestDispatcher.ERROR_SERVLET_NAME + ":{}", req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME));
      LOGGER.debug(RequestDispatcher.ERROR_EXCEPTION + ":{}", req.getAttribute(RequestDispatcher.ERROR_EXCEPTION));
      LOGGER.debug(RequestDispatcher.ERROR_REQUEST_URI + ":{}", req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
      LOGGER.debug(BERLIOZ_ERROR_ID + ":{}", req.getAttribute(BERLIOZ_ERROR_ID));
    }

    // Fetch original URI and its extension
    String original = getOriginalURI(req);
    String ext = getExtension(original);

    // Check if we should just ignore it
    if (ignoreExtensions.contains(ext)) {
      res.reset();
      res.setStatus(code);
      res.setContentType("text/plain;charset=UTF-8");
      ServletOutputStream o = res.getOutputStream();
      o.close();
      res.setIntHeader("Content-Length", 0);
      res.flushBuffer();
      return;
    }

    // When processing an .auto URI
    if (uri.endsWith(AUTO_EXTENSION)) {

      // Check if we need to preserve the extension
      if (!forwardExtensions.contains(ext)) {
        ext = defaultExtension;
      }

      // Replace the '.auto' by the original extension (.html, .xml, .json, etc...)
      String to = replaceAutoURI(uri, ext, req.getContextPath());
      to = Paths.get(to).normalize().toString();

      // If we do not detect a loop, we forward the request
      if (!uri.equals(to)) {

        // Let's forward the request
        RequestDispatcher dispatcher = req.getRequestDispatcher(to);
        dispatcher.forward(req, res);
        return;

      }
    }

    // Generate error details as XML
    String xml = toXml(req);

    // Reset the response (in case the ETag, etc. has been set...)
    res.reset();
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    res.setStatus(code);

    // Write to the output
    String fallbackType = "application/xml";

    // For non-HTML extensions (.xml, .src, etc.) return XML error directly, without the HTML
    // failsafe stylesheet. This respects the content type whether dispatched via the .auto
    // error-page mechanism or forwarded directly from BerliozServlet (ERROR_HANDLER=true).
    if (!DEFAULT_EXTENSION.equals(ext) && !ext.isEmpty()) {
      writeResponse(res, xml, fallbackType);
      return;
    }

    // Resolve stylesheet: custom → built-in failsafe → raw XML
    URL url = resolveErrorStylesheet();
    if (url != null) {
      String html = XsltTransformer.transformFailSafe(xml, url);
      if (Objects.equals(html, xml)) html = XsltTransformer.transformBuiltInFailSafe(xml);
      writeResponse(res, html, !Objects.equals(html, xml) ? "text/html" : fallbackType);
    } else {
      writeResponse(res, xml, fallbackType);
    }
  }

  /** Writes the non-dispatching terminal response using only module-owned resources. */
  static void handleTerminal(HttpServletRequest req, HttpServletResponse res) throws IOException {
    if (res.isCommitted()) return;
    int code = getErrorCode(req);
    String xml = toXml(req);
    res.reset();
    res.setStatus(code);
    String ext = getExtension(getOriginalURI(req));
    if (DEFAULT_EXTENSION.equals(ext) || ext.isEmpty()) {
      String html = XsltTransformer.transformBuiltInFailSafe(xml);
      writeResponse(res, html, !Objects.equals(html, xml) ? "text/html" : "application/xml");
    } else {
      writeResponse(res, xml, "application/xml");
    }
  }

  static void prepareErrorAttributes(HttpServletRequest req, String servletName, int statusCode,
                                     String message, @Nullable Throwable ex) {
    req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, statusCode);
    req.setAttribute(RequestDispatcher.ERROR_MESSAGE, message);
    if (req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI) == null) {
      req.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, req.getRequestURI());
    }
    if (req.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME) == null) {
      req.setAttribute(RequestDispatcher.ERROR_SERVLET_NAME, servletName);
    }
    if (ex != null) {
      req.setAttribute(RequestDispatcher.ERROR_EXCEPTION, ex);
      req.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, ex.getClass());
      ErrorID id = ex instanceof BerliozException ? ((BerliozException) ex).id() : null;
      if (id != null) req.setAttribute(BERLIOZ_ERROR_ID, id.id());
    }
  }

  private static void writeResponse(HttpServletResponse res, String content, String mediaType) throws IOException {
    res.setContentType(mediaType);
    res.setCharacterEncoding(StandardCharsets.UTF_8.name());
    HttpResponses.setContentLength(res, content, StandardCharsets.UTF_8);
    res.setDateHeader(HttpHeaders.DATE, System.currentTimeMillis());
    res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    PrintWriter out = res.getWriter();
    out.print(content);
    out.flush();
  }

  /**
   * Resolves the XSLT stylesheet URL to use for error rendering.
   *
   * <p>Implements the fallback chain:
   * <ol>
   *   <li>Custom ({@code berlioz.errors.stylesheet} relative to {@code WEB-INF}) if configured</li>
   *   <li>Built-in failsafe classpath template</li>
   *   <li>{@code null} — raw XML is written with an appropriate content type</li>
   * </ol>
   *
   * @return the URL to use, or {@code null} if no stylesheet is available
   */
  static @Nullable URL resolveErrorStylesheet() {
    String configured = GlobalSettings.get(BerliozOption.ERROR_STYLESHEET);
    if (!configured.isEmpty()) {
      File webInf = GlobalSettings.getWebInf();
      if (webInf != null) {
        File xsl = new File(webInf, configured);
        if (xsl.isFile() && xsl.canRead()) {
          try {
            return xsl.toURI().toURL();
          } catch (MalformedURLException ex) {
            LOGGER.warn("Cannot convert custom error stylesheet path to URL: {}", xsl, ex);
          }
        } else {
          LOGGER.warn("Custom error stylesheet not found or not readable: {} (falling back to built-in)", xsl);
        }
      } else {
        LOGGER.warn("berlioz.errors.stylesheet is configured but WEB-INF is not initialised — falling back to built-in");
      }
    }
    ClassLoader loader = ErrorHandlerServlet.class.getClassLoader();
    return loader.getResource("org/pageseeder/berlioz/xslt/failsafe-error-html.xsl");
  }

  /**
   * Handles an HTTP error using servlet error request attributes.
   *
   * @param req The HTTP servlet request that caused the error.
   * @return the error details as XML
   */
  @SuppressWarnings("removal") // both removed in 1.0; legacy fallback guarded here until then
  private static String toXml(HttpServletRequest req) {
    if (GlobalSettings.has(BerliozOption.ERROR_PROBLEM_FORMAT)) {
      int code = getErrorCode(req);
      String message = (String) req.getAttribute(RequestDispatcher.ERROR_MESSAGE);
      Throwable throwable = getErrorException(req);
      return toProblemXml(code, message, extractErrorId(req, throwable), throwable);
    }
    return toLegacyXml(req);
  }

  /**
   * Serializes the error as an RFC 9457 {@code <problem>} XML document.
   */
  private static String toProblemXml(int code, @Nullable String message,
                                     @Nullable String berliozErrorId, @Nullable Throwable throwable) {
    DetailLevel level = DetailLevel.parse(GlobalSettings.get(BerliozOption.ERROR_DETAIL));
    XmlStringBuilder xml = new XmlStringBuilder();
    try {
      xml.declaration();
      ProblemDetails problem;
      if (throwable instanceof XsltTransformException) {
        XsltTransformException xslt = (XsltTransformException) throwable;
        problem = Problems.forXsltError(code, berliozErrorId, xslt.transformerException(), level);
      } else {
        problem = Problems.forHttpError(code, message != null ? message : "", berliozErrorId, throwable, level);
      }
      xml.asXml(problem);
      xml.flush();
    } catch (Exception ex) {
      // ProblemDetails should not cause problem, but custom problem extensions might...
      LOGGER.warn("Unable to produce problem details XML for status {}", code, ex);
      return fallbackProblemXml(code);
    }
    return xml.toString();
  }

  /**
   * Returns the Berlioz error ID for the current request, or {@code null} if unavailable.
   *
   * <p>Priority: {@link BerliozException#id()} from the throwable (when the throwable is a
   * {@link BerliozException}), then the {@link #BERLIOZ_ERROR_ID} request attribute set by
   * the servlet that detected the error.
   */
  private static @Nullable String extractErrorId(HttpServletRequest req, @Nullable Throwable throwable) {
    ErrorID eid = throwable instanceof BerliozException ? ((BerliozException) throwable).id() : null;
    if (eid != null) return eid.id();
    return (String) req.getAttribute(BERLIOZ_ERROR_ID);
  }

  /**
   * Serializes the error in the legacy Berlioz error XML format.
   */
  @SuppressWarnings("deprecation") // LegacyError removed in 1.0; intentional use until GetErrorDetails is retired
  private static String toLegacyXml(HttpServletRequest req) {
    LegacyError error = LegacyError.of(req);
    return new XmlStringBuilder().declaration().asXml(error).toString();
  }

  /**
   * Returns the extension of the specified URI including the dot.
   *
   * @param uri The URI
   * @return the extension or empty string
   */
  private static String getExtension(String uri) {
    int dot = uri.lastIndexOf('.');
    return dot >= 0 ? uri.substring(dot) : "";
  }

  /**
   * Returns the original URI from <code>javax.servlet.error.request_uri</code>.
   *
   * @param req The HTTP servlet request
   * @return The original URI or this URI if it is the original.
   */
  private static String getOriginalURI(HttpServletRequest req) {
    Object original = req.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    if (original instanceof String) return (String) original;
    return req.getRequestURI();
  }

  /**
   * Returns the error code from the request attribute '<code>javax.servlet.error.status_code</code>'.
   *
   * @param req the servlet request
   * @return the error code.
   */
  private static int getErrorCode(ServletRequest req) {
    Object o = req.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    if (o == null) return HttpServletResponse.SC_OK;
    else if (o instanceof Integer) {
      return (Integer) o;
    } else {
      LOGGER.error("The 'javax.servlet.error.status_code' must contain an Integer, but was of type: {}", o.getClass().getSimpleName());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Returns the exception from the request attribute '<code>javax.servlet.error.exception</code>'.
   *
   * @param req the servlet request
   * @return the exception, or {@code null} when unavailable.
   */
  private static @Nullable Throwable getErrorException(ServletRequest req) {
    Object o = req.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
    if (o == null) return null;
    else if (o instanceof Throwable) return (Throwable) o;
    else {
      LOGGER.error("The 'javax.servlet.error.exception' must contain a Throwable, but was of type: {}", o.getClass().getSimpleName());
      return null;
    }
  }

  /**
   * Replace the '.auto' by the original extension (.html, .xml, .json, etc...)
   *
   * <p>The application context is removed from the request URI as the {@link RequestDispatcher}
   * will automatically add it.
   *
   * @param uri     The original request URI
   * @param ext     The extension to map the .auto to
   * @param context The application context
   * @return The path to forward to.
   */
  private static String replaceAutoURI(String uri, String ext, String context) {
    String to = uri.substring(context.length());
    int dot = to.lastIndexOf('.');
    to = (dot >= 0 ? to.substring(0, dot) : uri) + ext;
    LOGGER.debug("Auto forward: {} to {}", uri, to);
    return to;
  }

  private static String fallbackProblemXml(int code) {
    XmlStringBuilder xml = new XmlStringBuilder();
    xml.declaration();
    ProblemDetails.of(code)
        .type("urn:berlioz:problem:error")
        .title("HTTP " + code)
        .detail("Unable to serialize problem details.")
        .toXml(xml);
    xml.flush();
    return xml.toString();
  }

}
