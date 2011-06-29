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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.util.ISO8601;

import com.topologi.diffx.xml.XMLWriter;
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
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
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

  /** Exception thrown (Exception) */
  public static final String  ERROR_EXCEPTION      = "javax.servlet.error.exception";

  /** Class of exception thrown (Class). */
  public static final String  ERROR_EXCEPTION_TYPE = "javax.servlet.error.exception_type";

  /** Any attached message (String). */
  public static final String  ERROR_MESSAGE        = "javax.servlet.error.message";

  /** The offending request URI (String) .*/
  public static final String  ERROR_REQUEST_URI    = "javax.servlet.error.request_uri";

  /** The name of offending servlet (String). */
  public static final String  ERROR_SERVLET_NAME   = "javax.servlet.error.servlet_name";

  /** The HTTP Status code (Integer). */
  public static final String  ERROR_STATUS_CODE    = "javax.servlet.error.status_code";

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
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,
      IOException {

    // set the headers of the response
    res.setCharacterEncoding("utf-8");
    res.setContentType("text/html;charset=UTF-8");

    // Grab the data
    Integer codeAttr  = (Integer)req.getAttribute(ERROR_STATUS_CODE);

    // Default to 200 OK
    int code = codeAttr != null? codeAttr.intValue() : HttpServletResponse.SC_OK;

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
   * Handles error send via requests attributes.
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
    Exception exception = (Exception)req.getAttribute(ERROR_EXCEPTION);
    Object type = (Object)req.getAttribute(ERROR_EXCEPTION_TYPE);
    String requestURI = (String)req.getAttribute(ERROR_REQUEST_URI);

    // Write the XML 
    StringWriter out = new StringWriter();
    try {
      XMLWriterImpl xml = new XMLWriterImpl(out);
      xml.xmlDecl();
      xml.openElement("error");
      xml.attribute("code", code != null? code.intValue() : 200);
      xml.attribute("request-uri", requestURI != null? requestURI : "");
      xml.element("message", message);
      xml.element("servlet", servlet != null? servlet : "null");

      if (exception != null) {
        xml.openElement("exception");
        if (type != null) {
          xml.attribute("type", type.toString());
        }
        toXML(exception, xml);
      }
      // TODO Add headers and parameters

      xml.closeElement();
      xml.flush();
      xml.flush();
    } catch (IOException io) {
      LOGGER.warn("Unable to produce error details for error below:");
      LOGGER.error("An error occurred while transforming content", exception);
    }

    return out.toString();
  }

  /**
   * Handles transformation errors - to be used in catch blocks.
   * 
   * @param ex         An error occurring during an XSLT transformation.
   * @param source     The XML source being transformed
   * @param parameters The XSLT parameters passed to the transformer
   * @return the error details as XML
   */
  private static String toXML(BerliozException ex) {
    // Remove all double dash so that it may be inserted in the XML comment
    StringWriter out = new StringWriter();
    try {
      XMLWriter xml = new XMLWriterImpl(out);
      toXML(ex, xml);
      xml.flush();
    } catch (IOException io) {
      LOGGER.warn("Unable to produce error details for error below:");
      LOGGER.error("An error occurred while transforming content", ex);
    }

    return out.toString();
  }

  /**
   * Handles transformation errors - to be used in catch blocks.
   * 
   * @param ex  A Berlioz exception
   * @param xml The XML writer.
   * 
   * @return the error details as XML
   */
  private static void toXML(Exception ex, XMLWriter xml) throws IOException {
    xml.openElement("berlioz-exception");
    xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));
    xml.attribute("berlioz-version", GlobalSettings.getVersion());

      // Copy the errors collected here
      // TODO get info from collectors
//      XSLTErrorListener collector = wrapper.collector();
//      xml.writeXML(collector.xml.toString());

    // Exception
    xml.openElement("exception");
    // TODO unwrap exception for better messages
    xml.attribute("class", ex.getClass().getName());
    xml.element("message", ex.getMessage());
    xml.element("stack-trace", XSLTransformer.getStackTrace(ex, true));
    xml.closeElement();

    // Any cause ?
    Throwable cause = ex.getCause();
    if (cause != null) {
      // Transform Exception
      xml.openElement("cause");
      xml.attribute("class", cause.getClass().getName());
      xml.element("message", cause.getMessage());
      xml.element("stack-trace", XSLTransformer.getStackTrace(cause, true));
      xml.closeElement();
    }

    xml.closeElement();
  }

}
