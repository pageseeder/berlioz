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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.logging.ZLogger;
import org.weborganic.berlioz.logging.ZLoggerFactory;

/**
 * Servlets that only returns XML.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public final class XMLContentServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20060104256100003L;

  /**
   * Displays debug information.
   */
  private static final ZLogger LOGGER = ZLoggerFactory.getLogger(XMLContentServlet.class);

// servlet methods ----------------------------------------------------------------------

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
    long t0 = System.currentTimeMillis();
    String content = new XMLResponse().generate(req, res);
    long t1 = System.currentTimeMillis();
    LOGGER.debug("Content generated in "+(t1 - t0)+" ms");

    // redirect if required
    String redirectURL = req.getParameter("redirect-url");
    if (redirectURL == null)
      redirectURL = (String)req.getAttribute("redirect-url");
    if (redirectURL != null && !"".equals(redirectURL)) {
      res.sendRedirect(redirectURL);

    } else {
      // setup the output
      PrintWriter out = res.getWriter();
      out.println(content);
      out.flush();
    }
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
