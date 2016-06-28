/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.LifecycleListener;
import org.pageseeder.berlioz.servlet.Overlays.Overlay;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

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
 *   <servlet-class>org.pageseeder.berlioz.servlet.InitServlet</servlet-class>
 *   <load-on-startup>1</load-on-startup>
 * </servlet> }
 * </pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.30 - 8 January 2015
 * @since Berlioz 0.7
 */
public final class InitServlet extends HttpServlet implements Servlet {

  /**
   * As per requirement for the Serializable interface.
   */
  private static final long serialVersionUID = 20061009261800002L;

  /**
   * The lifecycle listener notified when Berlioz starts and stops.
   */
  private static List<LifecycleListener> listeners = new ArrayList<LifecycleListener>();

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

      // Check for overlays
      checkOverlays(contextPath);

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
    if (listeners.size() > 0) {
      System.out.println("[BERLIOZ_STOP] Lifecycle: Invoking listeners");
      for (LifecycleListener listener : listeners) {
        try {
          listener.stop();
        } catch (Exception ex) {
          System.out.println("[BERLIOZ_STOP] (!) Unable to stop Lifecycle listener: "+listener.getClass().getSimpleName());
        }
      }
      listeners.clear();
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

  /**
   * Add a listener to invoke when the Berlioz starts or stops.
   *
   * @param listener The listener to register.
   */
  @Beta
  public static void registerListener(LifecycleListener listener) {
    listeners.add(listener);
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
   * Check for overlays.
   *
   * @param contextPath the context path (root of the web application)
   */
  private static void checkOverlays(File contextPath) {
    List<Overlay> overlays = Overlays.list(contextPath);
    console("Overlays: found '"+overlays.size()+"' overlay(s)");
    Overlay previous = null;
    // Check if there is already an overlay with the same name
    for (Overlay o : overlays) {
      if (previous != null && previous.name().equals(o.name())) {
        console("(!) Multiple versions of the same overlay found!");
      }
      previous = o;
      try {
        File f  = o.getSource();
        console("Overlays: unpacking '"+f.getName()+"'");
        int count = o.unpack(contextPath);
        console("Overlays: '"+f.getName()+"' - "+count+" files unpacked");
      } catch (IOException ex) {
        console("(!) Unable to unpack overlay: "+ex.getMessage());
      }
    }
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
   * @param configuration The logback configuration file.
   * @return <code>true</code> if configuration was successful;
   *         <code>false</code> in case of any error.
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
   * @param configuration The log4j configuration file.
   * @return <code>true</code> if configuration was successful;
   *         <code>false</code> in case of any error.
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
        listeners.add(listener);
      }
    }
    // Start
    if (listeners.size() > 0) {
      boolean ok = true;
      for (LifecycleListener listener : listeners) {
        try {
          ok = ok && listener.start();
        } catch (Exception ex) {
          ok = false;
          ex.printStackTrace();
        }
      }
      if (ok) {
        console("Lifecycle: OK -------------------------------------------------");
      } else {
        console("(!) Unable to start Lifecycle listener");
        console("Lifecycle: FAIL -----------------------------------------------");
      }

    } else {
      console("Lifecycle: OK (No listeners)");
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
    String[] filenames = config.list();
    if (filenames != null) {
      for (String name : filenames) {
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
    } else {
      console("Mode: unable to list config files!");
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
