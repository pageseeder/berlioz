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

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
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
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.9 - 10 October 2012
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
      console("===============================================================");
      console("Initialing Berlioz "+GlobalSettings.getVersion()+"...");
      console("Application Base: "+webinfPath.getAbsolutePath());

      // Determine the mode (dev, production, etc...)
      String mode = getMode(config, configDir);

      // Checking that the 'config/services.xml' is there
      checkServices(configDir);

      // Configuring the logger
      configureLogger(configDir, mode);

      // Setup the global settings
      checkSettings(webinfPath, mode);

      // Invoke the lifecycle listener
      startListener(listener);

      // All done
      console("Done!");
      console("===============================================================");
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
      console("Mode: defined with init-parameter 'mode'");
    } else {
      // Try the system property
      mode = System.getProperty("berlioz.mode");
      if (mode != null) {
        console("Mode: defined with system property 'berlioz.mode'");
      } else {
        mode = guessMode(configDir);
        if (mode != null) {
          console("Mode: derived from XML configuration file.");
        } else {
          console("Mode: undefined, using "+GlobalSettings.DEFAULT_MODE);
          mode = GlobalSettings.DEFAULT_MODE;
        }
      }
    }
    // Report
    console("Mode: '"+mode+"'");
    console("Mode: OK ------------------------------------------------------");
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
      console("Services: found config/services.xml");
      console("Services: OK --------------------------------------------------");
    } else {
      console("(!) Could not find config/services.xml");
      console("Services: FAIL ------------------------------------------------");
    }
  }

  /**
   * Attempts to configure logger through reflection.
   *
   * <p>This method will look for logging configuration in the following order:
   * <ol>
   *   <li><code>logback-<i>[mode]</i>.xml</code></li>
   *   <li><code>logback.xml</code></li>
   *   <li><code>log4j-<i>[mode]</i>.prp</code></li>
   *   <li><code>log4j.prp</code></li>
   * </ol>
   *
   * @param config The directory containing the configuration files.
   * @param mode   The running mode.
   */
  private static void configureLogger(File config, String mode) {
    boolean configured = false;
    // Try specific logback first
    File file = new File(config, "logback-" + mode + ".xml");
    configured = configureLogback(file);
    if (configured) return;
    // Try generic logback
    file = new File(config, "logback.xml");
    configured = configureLogback(file);
    if (configured) return;
    // Try specific log4j
    file = new File(config, "log4j-"+mode+".prp");
    configured = configureLog4j(file);
    if (configured) return;
    // Try generic log4j
    file = new File(config, "log4j.prp");
    configured = configureLog4j(file);
    if (configured) return;
    // Unable to configure logging
    console("(!) Logging: no logging configured.");
    console("Logging: FAIL -------------------------------------------------");
  }

  /**
   * Attempts to configure logger through reflection.
   *
   * @param config The directory containing the configuration files.
   * @param mode   The running mode.
   */
  private static boolean configureLogback(File configuration) {
    boolean configured = false;
    // Look for LOGBACK first
    if (configuration.exists()) {
      console("Logging: found config/"+configuration.getName()+" [logback config file]");
      try {
        Class<?> joranClass = Class.forName("ch.qos.logback.classic.joran.JoranConfigurator");
        Class<?> contextClass = Class.forName("ch.qos.logback.core.Context");
        Object configurator = joranClass.newInstance();
        // Set the context
        ILoggerFactory context = LoggerFactory.getILoggerFactory();
        Method setContext = joranClass.getMethod("setContext", contextClass);
        setContext.invoke(configurator, contextClass.cast(context));
        // Reset the context
        try {
          Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
          Method reset = loggerContextClass.getMethod("reset", new Class<?>[]{});
          reset.invoke(context, new Object[]{});
          console("Logging: logger context reset successfully");
        } catch (Exception ex) {
          console("(!) Logging: Failed to  logger context - logging messages may appear twice");
          ex.printStackTrace();
        }
        // Invoke the configuration
        Method doConfigure = joranClass.getMethod("doConfigure", String.class);
        doConfigure.invoke(configurator, configuration.getAbsolutePath());
        configured = true;
        console("Logging: logback config file OK");
        console("Logging: OK ---------------------------------------------------");
      } catch (ClassNotFoundException ex) {
        console("(!) Logging: attempt to load logback configuration failed!");
        console("(!) Logging: logback could not be found on classpath!");
      } catch (Exception ex) {
        console("(!) Logging: attempt to load Logback configuration failed!");
        ex.printStackTrace();
      }
    } else {
      console("Logging: config/"+configuration.getName()+" not found");
    }
    return configured;
  }

  /**
   * Attempts to configure logger through reflection.
   *
   * @param config The directory containing the configuration files.
   * @param mode   The running mode.
   */
  private static boolean configureLog4j(File configuration) {
    boolean configured = false;
    if (configuration.exists()) {
      console("Logging: found config/"+configuration.getName()+" [log4j config file]");
      try {
        Class<?> configurator = Class.forName("org.apache.log4j.PropertyConfigurator");
        Method m = configurator.getDeclaredMethod("configure", String.class);
        m.invoke(null, configuration.getAbsolutePath());
        configured = true;
        console("Logging: log4j config file OK");
        console("Logging: OK ---------------------------------------------------");
      } catch (ClassNotFoundException ex) {
        console("(!) Logging: attempt to load Log4j configuration failed!");
        console("(!) Logging: Log4j could not be found on classpath!");
      } catch (Exception ex) {
        console("(!) Logging: attempt to load Log4j configuration failed!");
        ex.printStackTrace();
      }
    } else {
      console("(!) Logging: config/"+configuration.getName()+" not found");
    }
    return configured;
  }

  /**
   * Checking that the global setting are loaded properly.
   *
   * @param webinfPath The directory containing the configuration files.
   * @param mode       The mode
   */
  private static void checkSettings(File webinfPath, String mode) {
    console("Config: setting repository to Application Base");
    GlobalSettings.setRepository(webinfPath);
    if (mode != null) {
      GlobalSettings.setMode(mode);
    }
    File f = GlobalSettings.getPropertiesFile();
    if (f != null && f.exists()) {
      console("Config: found "+toRelPath(f, webinfPath));
      boolean loaded = GlobalSettings.load();
      if (loaded) {
        console("Config: loaded OK ("+GlobalSettings.countProperties()+" properties found)");
        console("Config: HTTP Compression = "+GlobalSettings.get(BerliozOption.HTTP_COMPRESSION));
        console("Config: HTTP Max Age = "+GlobalSettings.get(BerliozOption.HTTP_MAX_AGE));
        console("Config: XSLT Caching = "+GlobalSettings.get(BerliozOption.XSLT_CACHE));
        console("Config: XML Strict Parse = "+GlobalSettings.get(BerliozOption.XML_PARSE_STRICT));
        console("Config: OK ----------------------------------------------------");
      } else {
        console("(!) Unable to load global settings ");
        console("Config: FAIL --------------------------------------------------");
      }
    } else {
      console("(!) Could not find config/config-"+mode+".xml  or config/config-"+mode+".prp");
      console("Config: FAIL --------------------------------------------------");
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
        console("Lifecycle: Unable to find class for listener:");
        console("  "+listenerClass);
      } catch (ClassCastException ex) {
        console("Lifecycle: Class does not implement LifecycleListener:");
        console("  "+listenerClass);
      } catch (IllegalAccessException ex) {
        console("Lifecycle: Unable to access lifecycle listener:");
        console("  "+ex.getMessage());
      } catch (InstantiationException ex) {
        console("Lifecycle: Unable to instantiate lifecycle listener:");
        console("  "+ex.getMessage());
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
          console("Lifecycle: OK -------------------------------------------------");
        } else {
          console("(!) Unable to start Lifecycle listener");
          console("Lifecycle: FAIL -----------------------------------------------");
        }
      }

    } else {
      console("Lifecycle: OK (No listener)");
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
          console("Mode: multiple modes to choose from!");
          console("Mode: use 'berlioz.mode' or specify only 1 'config-[mode].xml'");
          // multiple config files: unable to choose.
          mode = null;
        }
      }
    }
    return mode;
  }

  /**
   * Log what this servlet is doing on the console.
   *
   * @param message the message to log.
   */
  private static void console(String message) {
    System.out.println("[BERLIOZ_INIT] "+message);
  }
}
