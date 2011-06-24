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
 * <pre>
 * {@code <!-- Initialisation servlet -->
 * <servlet>
 *   <servlet-name>Initialiser</servlet-name>
 *   <servlet-class>org.weborganic.berlioz.servlet.InitServlet</servlet-class>
 *   <load-on-startup>1</load-on-startup>
 * </servlet> }
 * </pre>
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
      System.out.println("[BERLIOZ_INIT] Application Base: "+webinfPath.getAbsolutePath());

      // Determine the mode (dev, production, etc...)
      String mode = getMode(config);

      // Checking that the 'config/services.xml' is there
      checkServices(configDir);

      // Configuring the logger
      configureLog4j(configDir, mode);

      // Setup the global settings
      checkSettings(webinfPath, mode);

      // All done
      System.out.println("[BERLIOZ_INIT] Done!");
      System.out.println("[BERLIOZ_INIT] ===============================================================");
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
      System.out.println("[BERLIOZ_INIT] Services: OK --------------------------------------------------");
    } else {
      System.out.println("[BERLIOZ_INIT] (!) Could not find config/services.xml");
      System.out.println("[BERLIOZ_INIT] Services: FAIL ------------------------------------------------");
    }
  }

  /**
   * Checking that the global setting are loaded properly.
   * 
   * @param webinfPath The directory containing the configuration files.
   * @param mode       The mode
   */
  private void checkSettings(File webinfPath, String mode) {
    System.out.println("[BERLIOZ_INIT] Config: Setting repository to Application Base");
    GlobalSettings.setRepository(webinfPath);
    if (mode != null)
      GlobalSettings.setConfig(mode);
    File f = GlobalSettings.getPropertiesFile();
    if (f.exists()) {
      System.out.println("[BERLIOZ_INIT] Config: found "+toRelPath(f, webinfPath));
      boolean loaded = GlobalSettings.load();
      if (loaded) {
        System.out.println("[BERLIOZ_INIT] Config: loaded OK");
        System.out.println("[BERLIOZ_INIT] Config: HTTP Compression = "+GlobalSettings.get(BerliozConfig.ENABLE_HTTP_COMPRESSION));
        System.out.println("[BERLIOZ_INIT] Config: HTTP Max Age = "+GlobalSettings.get(BerliozConfig.HTTP_MAX_AGE, 60));
        System.out.println("[BERLIOZ_INIT] Config: OK ----------------------------------------------------");
      } else {
        System.out.println("[BERLIOZ_INIT] (!) Unable to load global settings ");
        System.out.println("[BERLIOZ_INIT] Config: FAIL --------------------------------------------------");
      }
    } else {
      System.out.println("[BERLIOZ_INIT] (!) Could not find config/config-"+mode+".xml  or config/config-"+mode+".prp");
      System.out.println("[BERLIOZ_INIT] Config: FAIL --------------------------------------------------");
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
      System.out.println("[BERLIOZ_INIT] Logging: found config/"+logProperties.getName()+" [log4j config file]");
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
            System.out.println("[BERLIOZ_INIT] Mode: undefined, using "+GlobalSettings.DEFAULT_MODE);
          }
        }
      }
    }
    // Report
    System.out.println("[BERLIOZ_INIT] Mode: '"+mode+"'");
    System.out.println("[BERLIOZ_INIT] Mode: OK ------------------------------------------------------");
    return mode;
  }

  /**
   * Returns the relative path to the given file if possible.
   *
   * @param file The file.
   * @param base The base file (ancestor folder).
   * @return the relative path if the file path starts with the path; the full path otherwise.
   */
  private static String toRelPath(File file, File base) {
    String p = file.getPath();
    String b = base.getPath();
    if (p.startsWith(b) && p.length() > b.length()) {
      return p.substring(b.length()+1);
    } else {
      return p;
    }
  }
}
