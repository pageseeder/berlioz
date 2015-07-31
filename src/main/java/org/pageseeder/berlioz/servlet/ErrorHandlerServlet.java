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
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.http.HttpStatusCodes;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.util.ISO8601;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * Servlet used to handle errors for a uniform response.
 *
 * <p>
 * This servlet always returns an error code.
 *
 * <p>
 * This servlet should be configured as:
 *
 * <pre>
 * &lt;!-- Handler for errors (this servlet does not need to be mapped to anything) --&gt;
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;ErrorHandlerServlet&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.pageseeder.berlioz.servlet.ErrorHandlerServlet&lt;/servlet-class&gt;
 *   &lt;load-on-startup&gt;2&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32 - 29 January 2015
 * @since Berlioz 0.6
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

  /** Exception thrown (Exception). */
  public static final String ERROR_EXCEPTION      = "javax.servlet.error.exception";

  /** Class of exception thrown (Class). */
  public static final String ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

  /** Any attached message (String). */
  public static final String ERROR_MESSAGE        = "javax.servlet.error.message";

  /** The offending request URI (String) .*/
  public static final String ERROR_REQUEST_URI    = "javax.servlet.error.request_uri";

  /** The name of offending servlet (String). */
  public static final String ERROR_SERVLET_NAME   = "javax.servlet.error.servlet_name";

  /** The HTTP Status code (Integer). */
  public static final String ERROR_STATUS_CODE    = "javax.servlet.error.status_code";

  /** The Berlioz error ID (String). */
  public static final String BERLIOZ_ERROR_ID     = "org.pageseeder.berlioz.error_id";

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
  private static Set<String> forwardExtensions = new HashSet<String>();

  /**
   * The extension to ignore.
   */
  private static Set<String> ignoreExtensions = new HashSet<String>();

  /**
   * The default extension to use for extensions which are neither preserved nor ignored.
   */
  private static String autoExtension = AUTO_EXTENSION;

  /**
   * The default extension to use for extensions which are neither preserved nor ignored.
   */
  private static String defaultExtension = DEFAULT_EXTENSION;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    String preserve = config.getInitParameter("forward-extensions");
    if (preserve == null) {
      preserve = FORWARD_EXTENSIONS;
    }
    for (String ext : preserve.split(",")) {
      forwardExtensions.add(ext);
    }
    String ignore = config.getInitParameter("ignore-extensions");
    if (ignore == null) {
      ignore = IGNORE_EXTENSIONS;
    }
    for (String ext : ignore.split(",")) {
      ignoreExtensions.add(ext);
    }
    defaultExtension = config.getInitParameter("forward-default");
    if (defaultExtension == null) {
      defaultExtension = DEFAULT_EXTENSION;
    }
  }

  /**
   * Handles a GET request.
   *
   * <p>
   * No parameter required.
   *
   * @param req The servlet request.
   * @param res The servlet response.
   *
   * @throws ServletException Should a servlet exception occur.
   * @throws IOException Should an I/O error occur.
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    // Handle the request
    handle(req, res);
  }

  /**
   * Handles a POST request.
   *
   * <p>
   * No parameter required.
   *
   * @param req The servlet request.
   * @param res The servlet response.
   *
   * @throws ServletException Should a servlet exception occur.
   * @throws IOException Should an I/O error occur.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    // Handle the request
    handle(req, res);
  }

  /**
   * Handle the errors using the fail safe options and templates.
   *
   * @param req The servlet request.
   * @param res The servlet response.
   *
   * @throws ServletException Should a servlet exception occur.
   * @throws IOException Should an I/O error occur.
   */
  public static void handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

    // Grab the status code (Default to 200 OK)
    int code  = getErrorCode(req);

    // Get URI of error handler
    String uri = req.getRequestURI();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Error handler for URI:{}", uri);
      LOGGER.debug(ERROR_MESSAGE+":{}", req.getAttribute(ERROR_MESSAGE));
      LOGGER.debug(ERROR_STATUS_CODE+":{}", req.getAttribute(ERROR_STATUS_CODE));
      LOGGER.debug(ERROR_SERVLET_NAME+":{}", req.getAttribute(ERROR_SERVLET_NAME));
      LOGGER.debug(ERROR_EXCEPTION+":{}", req.getAttribute(ERROR_EXCEPTION));
      LOGGER.debug(ERROR_REQUEST_URI+":{}", req.getAttribute(ERROR_REQUEST_URI));
      LOGGER.debug(BERLIOZ_ERROR_ID+":{}", req.getAttribute(BERLIOZ_ERROR_ID));
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
    if (uri.endsWith(autoExtension)) {

      // Check if we need to preserve the extension
      if (!forwardExtensions.contains(ext)) {
        ext = defaultExtension;
      }

      // Replace the .auto by the original extension (.html, .xml, .json, etc...)
      String to = replaceAutoURI(uri, ext, req.getContextPath());

      // If we do not detect a loop we forward the request
      if (!uri.equals(to)) {

        // Let's forward the request
        RequestDispatcher dispatcher = req.getRequestDispatcher(to);
        dispatcher.forward(req, res);
        return;

      }
    }

    // Generate error details as XML
    String xml = toXML(req);

    // Reset the response (in case the ETag, etc.. has been set...)
    res.reset();
    res.setCharacterEncoding("utf-8");
    res.setStatus(code);

    // Write to the output
    PrintWriter out = res.getWriter();

    // Try to format as HTML
    ClassLoader loader = ErrorHandlerServlet.class.getClassLoader();
    URL url = loader.getResource("org/pageseeder/berlioz/xslt/failsafe-error-html.xsl");
    if (url != null) {
      String html = XSLTransformer.transformFailSafe(xml, url);
      // FIXME if it fails uses the incorrect content type
      res.setContentType("text/html;charset=UTF-8");
      out.print(html);
      out.flush();
    } else {
      res.setContentType("application/xml;charset=UTF-8");
      out.print(xml);
      out.flush();
    }

  }

  /**
   * Handles HTTP error using the error requests attributes.
   *
   * @param req The HTTP servlet request will cause the error.
   *
   * @return the error details as XML
   */
  private static String toXML(HttpServletRequest req) {

    // Grab data from attributes
    String message = (String)req.getAttribute(ERROR_MESSAGE);
    int code = getErrorCode(req);
    String servlet = (String)req.getAttribute(ERROR_SERVLET_NAME);
    Throwable throwable = getErrorException(req);
    String requestURI = (String)req.getAttribute(ERROR_REQUEST_URI);
    String errorId = (String)req.getAttribute(BERLIOZ_ERROR_ID);

    // Write the XML
    StringWriter out = new StringWriter();
    try {
      XMLWriterImpl xml = new XMLWriterImpl(out, true);
      xml.xmlDecl();
      xml.openElement(getRootElementName(code));
      xml.attribute("http-code", code);
      xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));

      // If it has a Berlioz ID
      if (throwable instanceof BerliozException && ((BerliozException)throwable).id() != null) {
        xml.attribute("id", ((BerliozException)throwable).id().id());
      } else {
        xml.attribute("id", errorId != null? errorId : BerliozErrorID.UNEXPECTED.toString());
      }

      // Berlioz info
      xml.openElement("berlioz");
      xml.attribute("version", GlobalSettings.getVersion());
      xml.closeElement();

      // Other informational elements
      String title = HttpStatusCodes.getTitle(code);
      xml.element("title", title != null? title : "Berlioz Status");
      xml.element("message", message);
      xml.element("request-uri", requestURI != null? requestURI : req.getRequestURI());
      xml.element("servlet", servlet != null? servlet : "null");

      if (throwable != null) {
        Errors.toXML(throwable, xml, true);

        // If some errors were collected, let's include them
        if (throwable instanceof CompoundBerliozException) {
          xml.openElement("collected-errors");
          ErrorCollector<? extends Throwable> collector = ((CompoundBerliozException)throwable).getCollector();
          for (CollectedError<? extends Throwable> collected : collector.getErrors()) {
            collected.toXML(xml);
          }
          xml.closeElement();
        }

      }

      // HTTP Headers
      xml.openElement("http-headers");
      Enumeration<?> names = req.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = names.nextElement().toString();
        Enumeration<?> values = req.getHeaders(name);
        while (values.hasMoreElements()) {
          String value = values.nextElement().toString();
          xml.openElement("header");
          xml.attribute("name", name);
          xml.attribute("value", value);
          xml.closeElement();
        }
      }
      xml.closeElement();

      // HTTP parameters
      xml.openElement("http-parameters");
      Map<?, ?> parameters = req.getParameterMap();
      for (Entry<?, ?> entry : parameters.entrySet()) {
        String name = entry.getKey().toString();
        // Must be an array according to Servlet Specifications
        String[] values = (String[])entry.getValue();
        for (String value : values) {
          xml.openElement("parameters");
          xml.attribute("name", name);
          xml.attribute("value", value);
          xml.closeElement();
        }
      }
      xml.closeElement();

      xml.closeElement();
      xml.flush();

    } catch (IOException io) {
      LOGGER.warn("Unable to produce error details for error below:");
      LOGGER.error("An error occurred while transforming content", throwable);
    }

    return out.toString();
  }


  /**
   * Handles HTTP error using the error requests attributes.
   *
   * @param req The HTTP servlet request will cause the error.
   *
   * @return the error details as JSON
   */
  private static String toJSON(HttpServletRequest req) {

    // Grab data from attributes
    String message = (String)req.getAttribute(ERROR_MESSAGE);
    int code = getErrorCode(req);
    String servlet = (String)req.getAttribute(ERROR_SERVLET_NAME);
    Throwable throwable = getErrorException(req);
    String requestURI = (String)req.getAttribute(ERROR_REQUEST_URI);
    String errorId = (String)req.getAttribute(BERLIOZ_ERROR_ID);

    // Write the JSON
    StringBuilder json = new StringBuilder();
    json.append('{');
    appendJSONProperty(json, "http-code", code).append(',');
    appendJSONProperty(json, "datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME)).append(',');

    // If it has a Berlioz ID
    if (throwable instanceof BerliozException && ((BerliozException)throwable).id() != null) {
      appendJSONProperty(json, "id", ((BerliozException)throwable).id().id()).append(',');
    } else {
      appendJSONProperty(json, "id", errorId != null? errorId : BerliozErrorID.UNEXPECTED.toString()).append(',');
    }

    // Berlioz info
    appendJSONName(json, "berlioz");
    json.append('{');
    appendJSONProperty(json, "version", GlobalSettings.getVersion());
    json.append('}');
    json.append(',');

    // Other informational elements
    String title = HttpStatusCodes.getTitle(code);
    appendJSONProperty(json, "title",   title != null? title : "Berlioz Status").append(',');
    appendJSONProperty(json, "message", message).append(',');
    appendJSONProperty(json, "request-uri", requestURI != null? requestURI : req.getRequestURI()).append(',');
    appendJSONProperty(json, "servlet",  servlet != null? servlet : "null").append(',');

    // TODO: display error in json format
//      if (throwable != null) {
//        Errors.toXML(throwable, xml, true);
//
//        // If some errors were collected, let's include them
//        if (throwable instanceof CompoundBerliozException) {
//          xml.openElement("collected-errors");
//          ErrorCollector<? extends Exception> collector = ((CompoundBerliozException)throwable).getCollector();
//          for (CollectedError<? extends Exception> collected : collector.getErrors()) {
//            collected.toXML(xml);
//          }
//          xml.closeElement();
//        }
//
//      }

    // HTTP Headers
    appendJSONName(json, "http-headers");
    json.append('[');
    Enumeration<?> names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement().toString();
      Enumeration<?> values = req.getHeaders(name);
      while (values.hasMoreElements()) {
        String value = values.nextElement().toString();
        json.append('{');
        appendJSONProperty(json, "name", name);
        json.append(',');
        appendJSONProperty(json, "value", value);
        json.append('}');
      }
    }
    json.append(']');
    json.append(',');

    // HTTP parameters
    appendJSONName(json, "http-parameters");
    json.append('[');
    Map<?, ?> parameters = req.getParameterMap();
    for (Entry<?, ?> entry : parameters.entrySet()) {
      String name = entry.getKey().toString();
      // Must be an array according to Servlet Specifications
      String[] values = (String[])entry.getValue();
      for (String value : values) {
        json.append('{');
        appendJSONProperty(json, "name", name);
        json.append(',');
        appendJSONProperty(json, "value", value);
        json.append('}');
      }
    }
    json.append(']');
    json.append('}');

    return json.toString();
  }

// TODO Find a mechanism to automate that process
//  /**
//   * Allows Berlioz to automatically add the extensions.
//   * @param ext the extension to add.
//   */
//  static void addForwardExtension(String ext) {
//    forwardExtensions.add(ext);
//  }

  /**
   * Return the root element name based on the status code.
   *
   * @param code the HTTP status code.
   * @return the root element name based on the HTTP status code or "unknown-status";
   */
  private static String getRootElementName(Integer code) {
    String element = HttpStatusCodes.getClassOfStatus(code);
    return (element != null)? element.toLowerCase().replace(' ', '-') : "unknown-status";
  }

  /**
   * Returns the extension of the specified URI including the dot.
   *
   * @param uri The URI
   * @return the extension or empty string
   */
  private static String getExtension(String uri) {
    int dot = uri.lastIndexOf('.');
    return dot >= 0? uri.substring(dot) : "";
  }

  /**
   * Returns the original URI from <code>javax.servlet.error.request_uri</code>.
   *
   * @param req The HTTP servlet request
   * @return The original URI or this URI if it is the original.
   */
  private static String getOriginalURI(HttpServletRequest req) {
    Object original = req.getAttribute("javax.servlet.error.request_uri");
    if (original != null && original instanceof String) return (String)original;
    return req.getRequestURI();
  }

  /**
   * Returns the error code from the request attribute '<code>javax.servlet.error.status_code</code>'.
   *
   * @param req the servlet request
   *
   * @return the error code.
   */
  private static int getErrorCode(ServletRequest req) {
    Object o = req.getAttribute(ERROR_STATUS_CODE);
    if (o == null) return HttpServletResponse.SC_OK;
    else if (o instanceof Integer) {
      Integer code  = (Integer)o;
      return code.intValue();
    } else {
      LOGGER.error("The 'javax.servlet.error.status_code' must contain an Integer, but was of type: {}", o.getClass().getSimpleName());
      return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Returns the error code from the request attribute '<code>javax.servlet.error.exception</code>'.
   *
   * @param req the servlet request
   *
   * @return the error code.
   */
  private static Throwable getErrorException(ServletRequest req) {
    Object o = req.getAttribute(ERROR_EXCEPTION);
    if (o == null) return null;
    else if (o instanceof Throwable) return (Throwable)o;
    else {
      LOGGER.error("The 'javax.servlet.error.exception' must contain a Throwable, but was of type: {}", o.getClass().getSimpleName());
      return null;
    }
  }


  /**
   * Replace the .auto by the original extension (.html, .xml, .json, etc...)
   *
   * <p>The application context is removed from the request URI as the RequestDiscpatcher will automatically add it.
   *
   * @param uri     The original request URI
   * @param ext     The extension to map the .auto to
   * @param context The application context
   *
   * @return THe path to forward to.
   */
  private static String replaceAutoURI(String uri, String ext, String context) {
    String to = uri.substring(context.length());
    int dot = to.lastIndexOf('.');
    to = (dot >= 0? to.substring(0, dot) : uri)+ext;
    LOGGER.debug("Auto forward: {} to {}", uri, to);
    return  to;
  }


  private static StringBuilder appendJSONName(StringBuilder json, String name) {
    return json.append('"').append(name).append('"').append(':');
  }

  private static StringBuilder appendJSONProperty(StringBuilder json, String name, String value) {
    json.append('"').append(name).append('"').append(':');
    json.append('"').append(value).append('"');
    return json;
  }

  private static StringBuilder appendJSONProperty(StringBuilder json, String name, int value) {
    return json;
  }
}
