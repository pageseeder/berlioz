/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;
import java.lang.reflect.Method;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

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
 * <pre>{@code
 *  <!-- Initialisation servlet -->
 * <servlet>
 *   <servlet-name>Initialiser</servlet-name>
 *   <servlet-class>org.weborganic.berlioz.servlet.InitServlet</servlet-class>
 *   <load-on-startup>1</load-on-startup>
 * </servlet>
 * }</pre>
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 25 May 2010
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
      configureLog4j(configDir, name);

      // Setup the global settings
      System.err.println("Initialising Global Settings...");
      GlobalSettings.setRepository(webinfPath);
      if (name != null)
        GlobalSettings.setConfig(name);
    }
  }

  /**
   * Attempts to configure Log4j through reflection.
   * 
   * @param config The directory containing the configuration files. 
   * @param mode   The running mode.
   */
  private void configureLog4j(File config, String mode) {
    // Configuring the logger
    File logProperties = new File(config, "log4j-" + mode + ".prp");
    System.err.println(logProperties.getAbsolutePath());
    if (logProperties.exists()) {
      System.err.println("Using log4j config file " + logProperties.getAbsolutePath());
      try {
        Class<?> configurator = Class.forName("org.apache.log4j.PropertyConfigurator");
        Method m = configurator.getDeclaredMethod("configure", String.class);
        m.invoke(null, logProperties.getAbsolutePath());
      } catch (Exception ex) {
        System.err.println("Attempt to load Log4j configurator failed:");
        ex.printStackTrace();
      }
    }
  }

}
