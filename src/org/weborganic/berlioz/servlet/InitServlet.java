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
 * The {@link InitServlet#init()} method performs a sanity check to inform the user about which 
 * version of Berlioz is running and which configuration files are used.
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
      File configDir = new File(webinfPath, "config");

      // Init message
      System.out.println("[BERLIOZ_INIT] ===============================================================");
      System.out.println("[BERLIOZ_INIT] Initialing Berlioz "+GlobalSettings.getVersion()+"...");

      // Determine the mode (dev, production, etc...)
      String mode = getMode(config);

      // Checking that the 'config/services.xml' is there
      checkServices(configDir);

      // Configuring the logger
      configureLog4j(configDir, mode);

      // Setup the global settings
      System.out.println("[BERLIOZ_INIT] Config: Setting repository to: "+webinfPath.getAbsolutePath());
      GlobalSettings.setRepository(webinfPath);
      if (mode != null)
        GlobalSettings.setConfig(mode);
      System.out.println("[BERLIOZ_INIT] Config: OK ---------------------------------------------------");

      // All done
      System.out.println("[BERLIOZ_INIT] Done!");
      System.out.println("[BERLIOZ_INIT] ====================================================");
    }
  }

  /**
   * Checking that the 'config/services.xml' is there
   * 
   * @param configDir The directory containing the configuration files. 
   */
  private void checkServices(File configDir) {
    File services = new File(configDir, "services.xml");
    if (services.exists()) {
      System.out.println("[BERLIOZ_INIT] Services: found config/services.xml");
      System.out.println("[BERLIOZ_INIT] Services: OK ------------------------------------------------");
    } else {
      System.out.println("[BERLIOZ_INIT] (!) Could not find config/services.xml");
      System.out.println("[BERLIOZ_INIT] Services: FAIL ----------------------------------------------");
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
    if (logProperties.exists()) {
      System.out.println("[BERLIOZ_INIT] Logging: Found log4j config file "+logProperties.getAbsolutePath());
      try {
        Class<?> configurator = Class.forName("org.apache.log4j.PropertyConfigurator");
        Method m = configurator.getDeclaredMethod("configure", String.class);
        m.invoke(null, logProperties.getAbsolutePath());
        System.out.println("[BERLIOZ_INIT] Logging: log4j config file OK");
        System.out.println("[BERLIOZ_INIT] Logging: OK ---------------------------------------------------");
      } catch (Exception ex) {
        System.out.println("[BERLIOZ_INIT] (!) Attempt to load Log4j configurator failed!");
        ex.printStackTrace();
        System.out.println("[BERLIOZ_INIT] Logging: FAIL -------------------------------------------------");
      }
    } else {
      System.out.println("[BERLIOZ_INIT] (!) Logging: config/"+logProperties.getName()+" not found - no logging configured.");
      System.out.println("[BERLIOZ_INIT] Logging: OK ---------------------------------------------------");
    }
  }

  /**
   * Attempts to configure Log4j through reflection.
   * 
   * @param config The directory containing the configuration files. 
   * @return The running mode.
   */
  private String getMode(ServletConfig config) {
    // Determine the mode (dev, production, etc...)
    String mode = config.getInitParameter("mode");
    if (mode != null) {
      System.out.println("[BERLIOZ_INIT] Mode: defined with init-parameter 'mode'");
    } else {
      // Try the system property
      mode = System.getProperty("berlioz.mode");
      if (mode != null) {
        System.out.println("[BERLIOZ_INIT] Mode: defined with system property 'berlioz.mode'");
      } else {
        // Try the legacy init-parameter
        mode = config.getInitParameter("config-name");
        if (mode != null) {
          System.out.println("[BERLIOZ_INIT] Mode: defined with init-parameter 'config-name'");
          System.out.println("[BERLIOZ_INIT] (!) Please change your web.xml to use 'mode' instead");
        } else {
          // Try the legacy system property
          mode = System.getProperty("berlioz.config");
          if (mode != null) {
            System.out.println("[BERLIOZ_INIT] Mode: defined with system property 'berlioz.config'");
            System.out.println("[BERLIOZ_INIT] (!) Please change your config file to use 'berlioz.mode' instead");
          } else {
            System.out.println("[BERLIOZ_INIT] Mode: undefined");
          }
        }
      }
    }
    // Report
    System.out.println("[BERLIOZ_INIT] Mode: '"+mode+"'");
    System.out.println("[BERLIOZ_INIT] Mode: OK ------------------------------------------------------");
    return mode;
  }

}
