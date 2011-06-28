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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.topologi.diffx.xml.esc.XMLEscapeASCII;

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
   * The HTTP error code for internal server errors.
   */
  private static final int INTERNAL_SERVER_ERROR_CODE = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

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
//    res.setStatus(INTERNAL_SERVER_ERROR_CODE);

    // write the response
    PrintWriter out = res.getWriter();
    out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">");
    out.println("<html>");
    out.println("<head>");
    out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">");
    out.println("<title>Berlioz - Error</title>");
    out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + req.getContextPath()
        + "/css/default/berlioz.css\"/>");
    out.println("</head>");
    out.println("<body>");
    out.println("<div id=\"error-page\">");
    out.println("<h1>An error has occurred.</h1>");

    Object oex = req.getAttribute("exception");
    // an exception is attached
    if (oex instanceof Exception) {
      Exception ex = (Exception) oex;
      out.println("<p>The following message was returned:</p>");
      out.println("<p class=\"message\">" + ex.getMessage() + "</p>");
      if (ex.getCause() != null) {
        out.println("<p><i>Was caused by</i></p>");
        out.println("<p class=\"cause\">" + ex.getCause() + "</p>");
      }
      out.println("<p><i>Stack Trace:</i></p>");
      out.println("<pre class=\"stacktrace\">" + getStackTraceAsString(ex) + "</pre>");

      // no information available
    } else {
      out.println("<p>No information available.</p>");
    }

    // information about the XML
    if (req.getAttribute("extra") instanceof String) {
      String extra = (String) req.getAttribute("extra");
      out.println("<h4>Additional Information</h4>");
      out.println("<p class=\"extra\">");
      out.println(extra);
      out.println("</p>");
    }

    // information about the XML
    if (req.getAttribute("xml") instanceof String) {
      String xml = (String) req.getAttribute("xml");
      out.println("<h4>XML Generated</h4>");
      out.println("<pre class=\"xml-content\">");
      out.println(XMLEscapeASCII.ASCII_ESCAPE.toElementText(xml));
      out.println("</pre>");
    }

    // close tags
    out.println("</div>");
    out.println("</body>");
    out.println("</html>");
  }

  /**
   * Returns the stack trace of the specified exception as a string.
   * 
   * @param ex The exception
   * 
   * @return The stack trace of the exception as a string.
   */
  private String getStackTraceAsString(Exception ex) {
    StringWriter writer = new StringWriter();
    PrintWriter printer = new PrintWriter(writer);
    ex.printStackTrace(printer);
    printer.flush();
    return writer.toString();
  }
}
