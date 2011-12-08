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

import org.weborganic.berlioz.BerliozOption;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.LifecycleListener;

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
 * @version Berlioz 0.9.3 - 9 December 2011
 * @since Berlioz 0.7
 */
public final class InitServlet extends HttpServlet implements Servlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20061009261800002L;

  /**
   * As per requirement for the Serializable interface.
   */
  private LifecycleListener _listener = null;

  /**
   * Initialises Berlioz.
   * 
   * <p>This method displays the configuration setting and does the following in order:
   * <ol>
   *  <li>Determines the Berlioz mode;</li>
   *  <li>Checks that the <code>services.xml</code> is available;</li>
   *  <li>Configures <code>Log4j</code> if possible;</li>
   *  <li>Loads and checks the global config;</li>
   *  <li>Invokes the <code>start</code> method of the <code>LifecycleListener</code>.</li>
   * </ol>
   * 
   * {@inheritDoc}
   * 
   * @param config The servlet configuration.
   * 
   * @throws ServletException Should an exception occur.
   */
  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    if (config != null) {
      ServletContext context = config.getServletContext();
      File contextPath = new File(context.getRealPath("/"));
      File webinfPath = new File(contextPath, "WEB-INF");
      File configDir = new File(webinfPath, "config");
      String listener = config.getInitParameter("lifecycle-listener");

      // Init message
      System.out.println("[BERLIOZ_INIT] ===============================================================");
      System.out.println("[BERLIOZ_INIT] Initialing Berlioz "+GlobalSettings.getVersion()+"...");
      System.out.println("[BERLIOZ_INIT] Application Base: "+webinfPath.getAbsolutePath());

      // Determine the mode (dev, production, etc...)
      String mode = getMode(config, configDir);

      // Checking that the 'config/services.xml' is there
      checkServices(configDir);

      // Configuring the logger
      configureLog4j(configDir, mode);

      // Setup the global settings
      checkSettings(webinfPath, mode);

      // Invoke the lifecycle listener
      startListener(listener);

      // All done
      System.out.println("[BERLIOZ_INIT] Done!");
      System.out.println("[BERLIOZ_INIT] ===============================================================");
    }
  }

  /**
   * Reset the initialisation Berlioz.
   * 
   * <p>This method unload the configuration setting and does the following in order:
   * <ol>
   *  <li>Invokes the <code>stop</code> method of the <code>ConfigListener</code>.</li>
   * </ol>
   * 
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    super.destroy();
    System.out.println("[BERLIOZ_STOP] ===============================================================");
    System.out.println("[BERLIOZ_STOP] Stopping Berlioz "+GlobalSettings.getVersion()+"...");
    System.out.println("[BERLIOZ_STOP] Application Base: "+GlobalSettings.getRepository().getAbsolutePath());
    if (this._listener != null) {
      System.out.println("[BERLIOZ_STOP] Lifecycle: Invoking listener");
      try {
        this._listener.stop();
      } catch (Exception ex) {
        System.out.println("[BERLIOZ_STOP] (!) Unable to stop Lifecycle listener");
      }
      this._listener = null;
    } else {
      System.out.println("[BERLIOZ_STOP] Lifecycle: OK (No listener)");
    }

    System.out.println("[BERLIOZ_STOP] Bye now!");
    System.out.println("[BERLIOZ_STOP] ===============================================================");
  }

  /**
   * @return "Berlioz Initialisation Servlet"
   */
  @Override
  public String getServletInfo() {
    return "Berlioz Initialisation Servlet";
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Attempts to configure Log4j through reflection.
   * 
   * @param config The servlet config.
   * @param configDir The directory containing the configuration files. 
   * @return The running mode.
   */
  private static String getMode(ServletConfig config, File configDir) {
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
        mode = guessMode(configDir);
        if (mode != null) {
          System.out.println("[BERLIOZ_INIT] Mode: derived from XML configuration file.");
        } else {
          System.out.println("[BERLIOZ_INIT] Mode: undefined, using "+GlobalSettings.DEFAULT_MODE);
          mode = GlobalSettings.DEFAULT_MODE;
        }
      }
    }
    // Report
    System.out.println("[BERLIOZ_INIT] Mode: '"+mode+"'");
    System.out.println("[BERLIOZ_INIT] Mode: OK ------------------------------------------------------");
    return mode;
  }

  /**
   * Checking that the 'config/services.xml' is there
   * 
   * @param configDir The directory containing the configuration files. 
   */
  private static void checkServices(File configDir) {
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
   * Attempts to configure Log4j through reflection.
   * 
   * @param config The directory containing the configuration files. 
   * @param mode   The running mode.
   */
  private static void configureLog4j(File config, String mode) {
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
   * Checking that the global setting are loaded properly.
   * 
   * @param webinfPath The directory containing the configuration files.
   * @param mode       The mode
   */
  private static void checkSettings(File webinfPath, String mode) {
    System.out.println("[BERLIOZ_INIT] Config: Setting repository to Application Base");
    GlobalSettings.setRepository(webinfPath);
    if (mode != null) {
      GlobalSettings.setConfig(mode);
    }
    File f = GlobalSettings.getPropertiesFile();
    if (f != null && f.exists()) {
      System.out.println("[BERLIOZ_INIT] Config: found "+toRelPath(f, webinfPath));
      boolean loaded = GlobalSettings.load();
      if (loaded) {
        System.out.println("[BERLIOZ_INIT] Config: loaded OK");
        System.out.println("[BERLIOZ_INIT] Config: HTTP Compression = "+GlobalSettings.get(BerliozOption.HTTP_COMPRESSION));
        System.out.println("[BERLIOZ_INIT] Config: HTTP Max Age = "+GlobalSettings.get(BerliozOption.HTTP_MAX_AGE));
        System.out.println("[BERLIOZ_INIT] Config: XSLT Caching = "+GlobalSettings.get(BerliozOption.XSLT_CACHE));
        System.out.println("[BERLIOZ_INIT] Config: XML Strict Parse = "+GlobalSettings.get(BerliozOption.XML_PARSE_STRICT));
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
   * Checking that the global setting are loaded properly.
   * 
   * @param listenerClass The lifecycle listener class.
   */
  private void startListener(String listenerClass) {
    if (listenerClass != null) {
      LifecycleListener listener = null;
      // Instantiate
      try {
        Class<?> c = Class.forName(listenerClass);
        listener = (LifecycleListener)c.newInstance();
      } catch (ClassNotFoundException ex) {
        System.out.println("[BERLIOZ_INIT] Lifecycle: Unable to find class for listener:");
        System.out.println("[BERLIOZ_INIT]   "+listenerClass);
      } catch (ClassCastException ex) {
        System.out.println("[BERLIOZ_INIT] Lifecycle: Class does not implement LifecycleListener:");
        System.out.println("[BERLIOZ_INIT]   "+listenerClass);
      } catch (IllegalAccessException ex) {
        System.out.println("[BERLIOZ_INIT] Lifecycle: Unable to access lifecycle listener:");
        System.out.println("[BERLIOZ_INIT]   "+ex.getMessage());
      } catch (InstantiationException ex) {
        System.out.println("[BERLIOZ_INIT] Lifecycle: Unable to instantiate lifecycle listener:");
        System.out.println("[BERLIOZ_INIT]   "+ex.getMessage());
      }
      // Start
      if (listener != null) {
        this._listener = listener;
        boolean ok = false;
        try {
          ok = listener.start();
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        if (ok) {
          System.out.println("[BERLIOZ_INIT] Lifecycle: OK -------------------------------------------------");
        } else {
          System.out.println("[BERLIOZ_INIT] (!) Unable to start Lifecycle listener");
          System.out.println("[BERLIOZ_INIT] Lifecycle: FAIL -----------------------------------------------");
        }
      }

    } else {
      System.out.println("[BERLIOZ_INIT] Lifecycle: OK (No listener)");
    }
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
    if (p.startsWith(b) && p.length() > b.length()) return p.substring(b.length()+1);
    else
      return p;
  }

  /**
   * Tries to guess the mode based on the configuration files in the config directory.
   *
   * <p>This method look for a configuration file matching <code>"config-<i>[mode]</i>.xml"</code>.
   * <p>If there is only one such file, this method will use this mode, otherwise, this method will 
   * return <code>null</code>. 
   * 
   * @param config The configuration directory (<code>/WEB-INF/config</code>).
   * @return the mode if only one file.
   */
  private static String guessMode(File config) {
    if (config == null) return null;
    String mode = null;
    for (String name : config.list()) {
      if (name.startsWith("config-") && name.endsWith(".xml")) {
        if (mode == null) {
          // Found a config file
          final int prefix = 7;
          final int suffix = 4;
          mode = name.substring(prefix, name.length() - suffix);
        } else {
          System.out.println("[BERLIOZ_INIT] Mode: multiple modes to choose from!");
          System.out.println("[BERLIOZ_INIT] Mode: use 'berlioz.mode' or specify only 1 'config-[mode].xml'");
          // multiple config files: unable to choose.
          mode = null;
        }
      }
    }
    return mode;
  }

}
