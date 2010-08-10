/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic servlet to redirect to a specific address.
 * 
 * <p>See {@link #init(ServletConfig)} for details for configuration options.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 11 August 2010
 */
public final class RedirectorServlet extends HttpServlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 25684657834543L;

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectorServlet.class);

// class attributes -------------------------------------------------------------------------------

  /**
   * Where this servlet should redirect to.
   */
  private String _target;

// servlet methods --------------------------------------------------------------------------------

  /**
   * Initialises the Redirector Servlet.
   * 
   * <p>This servlet accepts the following init parameters:
   * <ul>
   *   <li><code>target</code> Where this servlet should redirect all requests.
   * </ul>
   * 
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  public void init(ServletConfig config) throws ServletException {
    ServletContext context = config.getServletContext();
    this._target = context.getInitParameter("target");
  }

  /**
   * Redirects to the the target URL.
   * 
   * {@inheritDoc}
   */
  @Override public void doGet(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    redirect(req, res);
  }

  /**
   * Redirects to the the target URL.
   * 
   * {@inheritDoc}
   */
  @Override public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    redirect(req, res);
  }

  /**
   * Redirects to the the target URL.
   * 
   * @param req The servlet request.
   * @param res The servlet response.
   * 
   * @throws IOException If thrown by the {@link HttpServletResponse#sendRedirect(String)} method.
   */
  private void redirect(HttpServletRequest req, HttpServletResponse res) throws IOException {
    LOGGER.debug("Redirecting from {} to {}", req.getRequestURL(), this._target);
    res.sendRedirect(this._target);
  }

}
