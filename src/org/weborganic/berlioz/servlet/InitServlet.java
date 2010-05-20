/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;
import org.weborganic.berlioz.GlobalSettings;

/**
 * Initialise a Berlioz-based application.
 * 
 * <p>
 * This servlet does not return any data, it is simply used to initialise some data, and therefore
 * only implements the {@link #init(ServletConfig)} method.
 * 
 * <p>
 * Typically this servlet should be configured in the Web Configuration (web.xml) as:
 * 
 * <pre>
 *  &lt;!-- Initialisation servlet --&gt;
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;Initialiser&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;org.weborganic.berlioz.servlet.InitServlet&lt;/servlet-class&gt;
 *   &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 * &lt;/servlet&gt;
 * </pre>
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 9 October 2009
 */
public final class InitServlet extends HttpServlet implements Servlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20061009261800002L;

  /**
   * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if (config != null) {
      ServletContext context = config.getServletContext();
      File contextPath = new File(context.getRealPath("/"));
      File webinfPath = new File(contextPath, "WEB-INF");
      String name = config.getInitParameter("config-name");
      if (name == null)
        name = System.getProperty("berlioz.config");

      // Configuring the logger
      System.err.println("Loading log configuration...");
      File configDir = new File(webinfPath, "config");
      File logProperties = new File(configDir, "log4j-" + name + ".prp");
      System.err.println(logProperties.getAbsolutePath());
      if (logProperties.exists()) {
        System.err.println("Using log4j config file " + logProperties.getAbsolutePath());
        PropertyConfigurator.configure(logProperties.getAbsolutePath());
      } else if (System.getProperty("berlioz.debug") != null) {
        System.err.println("Using basic Log4j configurator");
        BasicConfigurator.configure();
      }

      // Setup the global settings
      System.err.println("Initialising Global Settings...");
      GlobalSettings.setRepository(webinfPath);
      if (name != null)
        GlobalSettings.setConfig(name);
    }
  }

}
