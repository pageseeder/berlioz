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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentManager;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Service;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * Servlets that only returns XML.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 8 October 2009
 */
public final class QuickContentServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20060104256100002L;

  /**
   * The environment. 
   */
  private transient Environment env;

// servlet methods ----------------------------------------------------------------------

  /**
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  public void init(ServletConfig config) throws ServletException {
    ServletContext context = config.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    this.env = new HttpEnvironment(contextPath, webinfPath);
  }

  /**
   * Handles a GET request.
   * 
   * @param req The servlet request.
   * @param res The servlet response.
   * 
   * @throws ServletException Should a servlet exception occur.
   * @throws IOException Should an I/O error occur.
   */
  public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {

    // setup and ensure that we use UTF-8
    req.setCharacterEncoding("utf-8");
    res.setCharacterEncoding("utf-8");
    res.setContentType("text/xml;charset=UTF-8");

    // Generate the XML content
    PrintWriter out = res.getWriter();
    XMLWriter xml = new XMLWriterImpl(out);
    xml.xmlDecl();
    xml.openElement("envelope");

    // get the content generator
    MatchingService match = ContentManager.getInstance(req.getPathInfo());

    // if the generator exists
    if (match != null) {
      Service service = match.service();
      for (ContentGenerator generator : service.generators()) {
        try {
          ContentRequest wrapper = new HttpRequestWrapper(req, res, this.env);
          generator.process(wrapper, xml);

        // an error occurred, do not elaborate 500 is enough.
        } catch (Exception ex) {
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }      
      }

    // the generator does not exist.
    } else res.sendError(HttpServletResponse.SC_NOT_FOUND);

    xml.closeElement();
    xml.flush();
    res.flushBuffer();
  }

  /**
   * Same as <code>doGet</code>.
   * 
   * {@inheritDoc}
   * 
   * @see HttpServlet#doPost(HttpServletRequest, HttpServletResponse)
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    doGet(req, res);
  }

}
