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
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozErrorID;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.http.HttpStatusCodes;
import org.weborganic.berlioz.util.CollectedError;
import org.weborganic.berlioz.util.CompoundBerliozException;
import org.weborganic.berlioz.util.ErrorCollector;
import org.weborganic.berlioz.util.Errors;
import org.weborganic.berlioz.util.ISO8601;

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
 *   &lt;servlet-class&gt;org.weborganic.berlioz.servlet.ErrorHandlerServlet&lt;/servlet-class&gt;
 *   &lt;load-on-startup&gt;2&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.2 - 29 November 2011
 * @since Berlioz 0.6
 */
public final class ErrorHandlerServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20060910261800002L;

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
  public static final String BERLIOZ_ERROR_ID     = "org.weborganic.berlioz.error_id";

  // servlet methods ----------------------------------------------------------------------

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
    Integer code  = (Integer)req.getAttribute(ERROR_STATUS_CODE);
    if (code == null) {
      code = Integer.valueOf(HttpServletResponse.SC_OK);
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
    URL url = loader.getResource("org/weborganic/berlioz/xslt/failsafe-error-html.xsl");
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
    Integer code   = (Integer)req.getAttribute(ERROR_STATUS_CODE);
    String servlet = (String)req.getAttribute(ERROR_SERVLET_NAME);
    Throwable throwable = (Throwable)req.getAttribute(ERROR_EXCEPTION);
    String requestURI = (String)req.getAttribute(ERROR_REQUEST_URI);
    String errorId = (String)req.getAttribute(BERLIOZ_ERROR_ID);

    // Ensure we have a status code
    if (code == null) {
      code = Integer.valueOf(HttpServletResponse.SC_OK);
    }

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
          ErrorCollector<? extends Exception> collector = ((CompoundBerliozException)throwable).getCollector();
          for (CollectedError<? extends Exception> collected : collector.getErrors()) {
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
   * Return the root element name based on the status code.
   *
   * @param code the HTTP status code.
   * @return the root element name based on the HTTP status code or "unknown-status";
   */
  private static String getRootElementName(Integer code) {
    String element = HttpStatusCodes.getClassOfStatus(code);
    return (element != null)? element.toLowerCase().replace(' ', '-') : "unknown-status";
  }

}
