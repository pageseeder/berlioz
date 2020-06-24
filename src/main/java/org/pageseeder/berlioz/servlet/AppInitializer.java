/*
 * Copyright 2016 Allette Systems (Australia)
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.LifecycleListener;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.servlet.Overlays.Overlay;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

/**
 * This class initializes a Berlioz application.
 *
 * @version Berlioz 0.11.5
 * @since Berlioz 0.11.0
 */
public abstract class AppInitializer {

  /**
   * The list of lifecycle listeners notified when Berlioz starts and stops.
   */
  private final List<LifecycleListener> _listeners;

  /**
   * The <code>WEB-INF</code> folder.
   */
  private final File _webinf;

  /**
   * Used for logging events.
   */
  private enum Phase {INIT, STOP}

  /**
   * Create a new application initializer using the
   *
   * @param webinf    The application WEB-INF configuration.
   * @param listeners The list of lifecycle listeners to use
   *
   * @throws NullPointerException If list of lifecycle listeners is <code>null</code>
   */
  public AppInitializer(File webinf, List<LifecycleListener> listeners) {
    this._webinf = Objects.requireNonNull(webinf, "Listeners must be specified");
    this._listeners = Objects.requireNonNull(listeners, "Listeners must be specified");
  }

  /**
   * Initializes the Berlioz application.
   *
   * <p>The following are configured in the order below:
   * <ol>
   *   <li>Application directory folder (<code>/WEB-INF</code>)</li>
   *   <li>Application data folder</li>
   *   <li>Berlioz mode</li>
   *   <li>Berlioz settings {@link GlobalSettings}</li>
   *   <li>Overlays deployment</li>
   *   <li>Logging</li>
   *   <li>Lifecycle listeners are started with {@link LifecycleListener#start()}</li>
   * </ol>
   */
  public final void init() {
    // Init message
    console(Phase.INIT, "===============================================================");
    console(Phase.INIT, "Initialing Berlioz "+GlobalSettings.getVersion()+"...");
    console(Phase.INIT, "Application base: "+this._webinf.getAbsolutePath());

    // Set the WEB-INF
    InitEnvironment env = InitEnvironment.create(this._webinf);

    // Setup config folder
    String configFolder = setupConfigFolder(this._webinf);
    env = env.configFolder(configFolder);

    // Determine the application data folder
    @Nullable File appData = configureAppData();
    if (appData != null) {
      env = env.appData(appData);
    }

    // Determine the mode (dev, production, etc...)
    String mode = configureMode(appData != null? appData : this._webinf, configFolder);
    if (mode != null) {
      env = env.mode(mode);
    }

    // Setup the Global settings
    GlobalSettings.setup(env);

    // Check for overlays
    deployOverlays();

    // Checking that the 'config/services.xml' is there
    checkServices(env);

    // Configuring the logger
    configureLogger(env);

    // Add the lifecycle listener
    addLifecycleListener();

    // Load the global settings (last so that listeners can be notified of the load)
    loadSettings();

    // Send 'start' event to lifecycle listener (after the configuration is loaded)
    startListeners();

    // All done
    console(Phase.INIT, "Done!");
    console(Phase.INIT, "===============================================================");
  }

  /**
   * Stop the Berlioz application.
   *
   * <p>The lifecycle listener are stopped with {@link LifecycleListener#stop()}.
   */
  public final void destroy() {
    console(Phase.STOP, "===============================================================");
    console(Phase.STOP, "Stopping Berlioz "+GlobalSettings.getVersion()+"...");
    console(Phase.STOP, "Application Base: "+this._webinf.getAbsolutePath());
    if (this._listeners.size() > 0) {
      console(Phase.STOP, "Lifecycle: Invoking listeners");
      for (LifecycleListener listener : this._listeners) {
        try {
          listener.stop();
        } catch (Exception ex) {
          System.out.println("[BERLIOZ_STOP] (!) Unable to stop Lifecycle listener: "+listener.getClass().getSimpleName());
        }
      }
      this._listeners.clear();
    } else {
      console(Phase.STOP, "Lifecycle: OK (No listener)");
    }

    console(Phase.STOP, "Bye now!");
    console(Phase.STOP, "===============================================================");
  }

  // Abstract initialization methods to implement
  // --------------------------------------------------------------------------

  /**
   * @return the path to the application data folder if specified in the configuration.
   */
  @Nullable abstract String getAppDataPath();

  /**
   * @return the name of the folder within WEB-INF that contains the configuration.
   */
  abstract String getConfigFolder();

  /**
   * @return the mode if specified in the configuration.
   */
  @Nullable abstract String getMode();

  /**
   * Deploys the overlays
   */
  abstract void deployOverlays();

  /**
   * Adds a lifecycle listener defined in the configuration
   */
  abstract void addLifecycleListener();

  // Factory methods
  // --------------------------------------------------------------------------

  /**
   * Returns a new application initializer using only system properties.
   *
   * @return a new application initializer
   */
  public static AppInitializer newInstance(File root, List<LifecycleListener> listeners) {
    return new SystemInitializer(root, listeners);
  }

  /**
   * Returns a new application initializer using the servlet context.
   *
   * @return a new application initializer
   */
  public static AppInitializer newInstance(ServletContext context, List<LifecycleListener> listeners) {
    return new ServletContextInitializer(context, listeners);
  }

  /**
   * Returns a new application initializer using the servlet configuration.
   *
   * @return a new application initializer
   */
  public static AppInitializer newInstance(ServletConfig config, List<LifecycleListener> listeners) {
    return new ServletConfigInitializer(config, listeners);
  }


  // Implementations
  // --------------------------------------------------------------------------

  /**
   * This application initializer configures the application from a servlet
   * config.
   *
   * It inherits the configuration from the Servlet context.
   */
  private static class ServletConfigInitializer extends ServletContextInitializer {

    /**
     * The servlet context.
     */
    private final ServletConfig _config;

    public ServletConfigInitializer(ServletConfig config, List<LifecycleListener> listeners) {
      super(config.getServletContext(), listeners);
      this._config = Objects.requireNonNull(config, "Servlet config must be specified");
    }

    @Override String getConfigFolder() {
      String config = this._config.getInitParameter("config");
      if (config != null) {
        console(Phase.INIT, "Config folder: defined with servlet init-parameter 'config'");
        return config;
      }
      // Fall back on context initializer
      return super.getConfigFolder();
    }

    @Override
    @Nullable String getAppDataPath() {
      String appdata = this._config.getInitParameter("appdata");
      if (appdata != null) {
        console(Phase.INIT, "AppData: defined with servlet init-parameter 'appdata'");
        return appdata;
      }
      // Fall back on context initializer
      return super.getAppDataPath();
    }

    @Override
    @Nullable String getMode() {
      String mode = this._config.getInitParameter("mode");
      if (mode != null) {
        console(Phase.INIT, "Mode: defined with servlet init-parameter 'mode'");
        return mode;
      }
      // Fall back on context initializer
      return super.getMode();
    }

    @Override
    void addLifecycleListener() {
      String listenerClass = this._config.getInitParameter("lifecycle-listener");
      if (listenerClass != null) {
        registerListener(listenerClass);
      }
      // Fall back on context initializer
      super.addLifecycleListener();
    }

  }

  /**
   * This application initializer configures the application from the servlet
   * context
   *
   * It inherits the configuration from the system.
   */
  private static class ServletContextInitializer extends SystemInitializer {

    /**
     * The servlet context.
     */
    private final ServletContext _context;

    public ServletContextInitializer(ServletContext context, List<LifecycleListener> listeners) {
      super(new File(context.getRealPath("/")), listeners);
      this._context = Objects.requireNonNull(context, "Servlet context must be specified");
    }

    @Override String getConfigFolder() {
      String config = this._context.getInitParameter("berlioz.config");
      if (config != null) {
        console(Phase.INIT, "Config folder: defined with servlet init-parameter 'berlioz.config'");
        return config;
      }
      // Fallback on system
      return super.getConfigFolder();
    }

    @Override
    @Nullable String getAppDataPath() {
      // Check context level
      String appdata = this._context.getInitParameter("berlioz.appdata");
      if (appdata != null) {
        console(Phase.INIT, "AppData: defined with context init-parameter 'berlioz.appdata'");
        return appdata;
      }
      // Fallback on system
      return super.getAppDataPath();
    }

    @Override
    @Nullable String getMode() {
      String mode = this._context.getInitParameter("berlioz.mode");
      if (mode != null) {
        console(Phase.INIT, "Mode: defined with context init-parameter 'berlioz.mode'");
        return mode;
      }
      return super.getMode();
    }

    @Override
    void deployOverlays() {
      File contextPath = new File(this._context.getRealPath("/"));
      checkOverlays(contextPath);
    }

    @Override
    void addLifecycleListener() {
      String listenerClass = this._context.getInitParameter("berlioz.lifecycle-listener");
      if (listenerClass != null) {
        registerListener(listenerClass);
      }
      super.addLifecycleListener();
    }

  }

  /**
   *
   */
  private static class SystemInitializer extends AppInitializer {

    private final File _root;

    public SystemInitializer(File root, List<LifecycleListener> listeners) {
      super(new File(root, "WEB-INF"), listeners);
      this._root = root;
    }

    @Override String getConfigFolder() {
      // JVM property
      String config = System.getProperty("berlioz.config");
      if (config != null) {
        console(Phase.INIT, "Config folder: defined with system property 'berlioz.config'");
        return config;
      }

      // Environment variable
      config = System.getenv("BERLIOZ_CONFIG");
      if (config != null) {
        console(Phase.INIT, "Config folder: defined with environment variable 'BERLIOZ_CONFIG'");
        return config;
      }

      // No specified config folder
      return InitEnvironment.DEFAULT_CONFIG_DIRECTORY;
    }

    @Override
    @Nullable String getAppDataPath() {
      // JVM property
      String appdata = System.getProperty("berlioz.appdata");
      if (appdata != null) {
        console(Phase.INIT, "AppData: defined with system property 'berlioz.appdata'");
        return appdata;
      }

      // Environment variable
      appdata = System.getenv("BERLIOZ_APPDATA");
      if (appdata != null) {
        console(Phase.INIT, "AppData: defined with environment variable 'BERLIOZ_APPDATA'");
        return appdata;
      }

      // No specified appdata
      return null;
    }

    @Override
    @Nullable String getMode() {
      // JVM property
      String mode = System.getProperty("berlioz.mode");
      if (mode != null) {
        console(Phase.INIT, "Mode: defined with system property 'berlioz.mode'");
        return mode;
      }

      // Environment variable
      mode = System.getenv("BERLIOZ_MODE");
      if (mode != null) {
        console(Phase.INIT, "Mode: defined with environment variable 'BERLIOZ_MODE'");
        return mode;
      }

      // No specified mode
      return null;
    }

    @Override
    void deployOverlays() {
      checkOverlays(this._root);
    }

    @Override
    void addLifecycleListener() {
    }

  }


  // Application data folder
  // ----------------------------------------------------------------------------------------------

  /**
   * Configure the AppData directory from the current context.
   */
  private String setupConfigFolder(File webinf) {
    String configFolder = getConfigFolder();
    try {
      File config = new File(webinf, configFolder);

      // Sanity check on name
      if (!configFolder.matches("^[0-9a-zA-Z_]+$")) {
        console(Phase.INIT, "Config folder: (!) The specified config folder '"+configFolder+"' is not recommended");
      }

      // Check directory
      if (!config.exists()) {
        throw new IOException("The specified config folder '/WEB-INF/"+configFolder+"' does not exist.");
      } else if (!config.isDirectory()) {
        throw new IOException("The specified config folder '/WEB-INF/"+configFolder+"' is not a directory.");
      }

      // Report
      console(Phase.INIT, "Config folder: Using '"+configFolder+"'");

    } catch (IOException ex) {
      console(Phase.INIT, "(!) Unable to setup application config folder");
      console(Phase.INIT, "(!) "+ex.getMessage());
    }

    // Set and return application data folder that was set
    return configFolder;
  }


  // Application data folder
  // ----------------------------------------------------------------------------------------------

  /**
   * Configure the AppData directory from the current context.
   */
  private @Nullable File configureAppData() {
    @Nullable File appData = null;
    try {
      @Nullable String appDataPath = getAppDataPath();
      if (appDataPath != null) {
        appData = new File(appDataPath);

        // Check (and create) directory
        if (!appData.exists()) {
          Files.createDirectories(appData.toPath());
          console(Phase.INIT, "AppData: created specified appdata folder");
        } else if (!appData.isDirectory()) throw new IOException("The specified appdata folder "+appData+" is not a directory.");

        // Report
        console(Phase.INIT, "AppData: '"+appData.getAbsolutePath()+"'");

      } else {
        // Fallback on /WEB-INF
        console(Phase.INIT, "AppData: default to Web application /WEB-INF folder");
      }
      console(Phase.INIT, "AppData: OK ---------------------------------------------------");

    } catch (IOException ex) {
      appData = null;
      console(Phase.INIT, "(!) Unable to setup application data folder");
      console(Phase.INIT, "(!) "+ex.getMessage());
      console(Phase.INIT, "AppData: FAIL --------------------------------------------------");
    }

    // Set and return application data folder that was set
    return appData;
  }

  // Berlioz mode
  // ----------------------------------------------------------------------------------------------

  /**
   * Attempts to compute the Berlioz mode
   *
   * @param appData      The servlet config.
   * @param configFolder The name of the directory containing the configuration files.
   *
   * @return The running mode.
   */
  private String configureMode(File appData, String configFolder) {
    // Determine the mode (dev, production, etc...)
    @Nullable String mode = getMode();
    if (mode == null) {
      mode = autoDetect(new File(appData, configFolder));
      if (mode != null) {
        console(Phase.INIT, "Mode: auto-detected modes configuration file.");
      } else {
        console(Phase.INIT, "Mode: defaulting to "+InitEnvironment.DEFAULT_MODE);
        mode = InitEnvironment.DEFAULT_MODE;
      }
    }
    // Report
    console(Phase.INIT, "Mode: '"+mode+"'");
    console(Phase.INIT, "Mode: OK ------------------------------------------------------");
    return mode;
  }

  /**
   * Tries to guess the mode based on the configuration files available in a directory.
   *
   * <p>This method look for a configuration file matching <code>"config-<i>[mode]</i>.xml"</code>.
   *
   * <p>If there is only one such file, this method will use this mode, otherwise this method will
   * return <code>null</code>.
   *
   * @param directory The configuration directory (<code>/WEB-INF/config</code>).
   * @return the mode if only one file.
   */
  private static @Nullable String autoDetect(File directory) {
    @Nullable String mode = null;
    @Nullable String[] filenames = directory.list();
    if (filenames != null) {
      for (String name : filenames) {
        if (name.startsWith("config-") && name.endsWith(".xml")) {
          if (mode == null) {
            // Found a config file
            final int prefix = 7;
            final int suffix = 4;
            mode = name.substring(prefix, name.length() - suffix);
          } else {
            console(Phase.INIT, "(!) Multiple modes to choose from!");
            console(Phase.INIT, "(!) Use 'berlioz.mode' or specify only 1 'config-[mode].xml'");
            // multiple config files: unable to choose.
            mode = null;
          }
        }
      }
    } else {
      console(Phase.INIT, "(!) Unable to list config files!");
    }
    return mode;
  }

  // Overlays
  // ----------------------------------------------------------------------------------------------

  /**
   * Check for overlays.
   *
   * @param contextPath the context path (root of the web application)
   */
  protected static void checkOverlays(File contextPath) {
    List<Overlay> overlays = Overlays.list(contextPath);
    console(Phase.INIT, "Overlays: found '"+overlays.size()+"' overlay(s)");
    @Nullable Overlay previous = null;
    // Check if there is already an overlay with the same name
    for (Overlay o : overlays) {
      if (previous != null && previous.name().equals(o.name())) {
        console(Phase.INIT, "(!) Multiple versions of the same overlay found!");
      }
      previous = o;
      try {
        File f  = o.getSource();
        console(Phase.INIT, "Overlays: unpacking '"+f.getName()+"'");
        int count = o.unpack(contextPath);
        console(Phase.INIT, "Overlays: '"+f.getName()+"' - "+count+" files unpacked");
      } catch (IOException ex) {
        console(Phase.INIT, "(!) Unable to unpack overlay: "+ex.getMessage());
      }
    }
  }

  // Services
  // ----------------------------------------------------------------------------------------------

  /**
   * Checking that the '[config]/services.xml' is there
   *
   * @param env The Web application init environment
   */
  private static void checkServices(InitEnvironment env) {
    Path services = env.webInf().toPath().resolve(env.configFolder()+"/services.xml");
    if (Files.exists(services)) {
      console(Phase.INIT, "Services: found "+env.configFolder()+"/services.xml");
      console(Phase.INIT, "Services: OK --------------------------------------------------");
    } else {
      console(Phase.INIT, "(!) Could not find "+env.configFolder()+"/services.xml");
      console(Phase.INIT, "Services: FAIL ------------------------------------------------");
    }
  }

  // Logging
  // ----------------------------------------------------------------------------------------------

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
   * @param env  The initial environment
   */
  private static void configureLogger(InitEnvironment env) {
    File appDataConfig = new File(env.appData(), env.configFolder());
    File webInfConfig = new File(env.webInf(), env.configFolder());
    boolean configured;
    // Try specific logback first
    File file = new File(appDataConfig, "logback-" + env.mode() + ".xml");
    configured = configureLogback(file);
    if (configured) return;
    // Try specific logback first in WEB-INF
    if (env.hasAppData()) {
      file = new File(webInfConfig, "logback-" + env.mode() + ".xml");
      configured = configureLogback(file);
      if (configured) return;
    }
    // Try generic logback
    file = new File(webInfConfig, "logback.xml");
    configured = configureLogback(file);
    if (configured) return;
    // Try specific log4j
    file = new File(appDataConfig, "log4j-"+env.mode()+".properties");
    configured = configureLog4j(file);
    if (configured) return;
    // Try specific log4j in WEB-INF
    if (env.hasAppData()) {
      // Try specific log4j
      file = new File(webInfConfig, "log4j-"+env.mode()+".properties");
      configured = configureLog4j(file);
      if (configured) return;
    }
    // Try generic log4j
    file = new File(webInfConfig, "log4j.properties");
    configured = configureLog4j(file);
    if (configured) return;
    // Unable to configure logging
    console(Phase.INIT, "Logging: no logging configured.");
    console(Phase.INIT, "Logging: DONE -------------------------------------------------");
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
      console(Phase.INIT, "Logging: found config/"+configuration.getName()+" [logback config file]");
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
          Method reset = loggerContextClass.getMethod("reset");
          reset.invoke(context);
          console(Phase.INIT, "Logging: logger context reset successfully");
        } catch (Exception ex) {
          console(Phase.INIT, "(!) Logging: Failed to  logger context - logging messages may appear twice");
          ex.printStackTrace();
        }
        // Invoke the configuration
        Method doConfigure = joranClass.getMethod("doConfigure", String.class);
        doConfigure.invoke(configurator, configuration.getAbsolutePath());
        configured = true;
        console(Phase.INIT, "Logging: logback config file OK");
        console(Phase.INIT, "Logging: OK ---------------------------------------------------");
      } catch (ClassNotFoundException ex) {
        console(Phase.INIT, "(!) Logging: attempt to load logback configuration failed!");
        console(Phase.INIT, "(!) Logging: logback could not be found on classpath!");
      } catch (Exception ex) {
        console(Phase.INIT, "(!) Logging: attempt to load Logback configuration failed!");
        ex.printStackTrace();
      }
    } else {
      console(Phase.INIT, "Logging: config/"+configuration.getName()+" not found");
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
      console(Phase.INIT, "Logging: found config/"+configuration.getName()+" [log4j config file]");
      try {
        Class<?> configurator = Class.forName("org.apache.log4j.PropertyConfigurator");
        Method m = configurator.getDeclaredMethod("configure", String.class);
        m.invoke(null, configuration.getAbsolutePath());
        configured = true;
        console(Phase.INIT, "Logging: log4j config file OK");
        console(Phase.INIT, "Logging: OK ---------------------------------------------------");
      } catch (ClassNotFoundException ex) {
        console(Phase.INIT, "(!) Logging: attempt to load Log4j configuration failed!");
        console(Phase.INIT, "(!) Logging: Log4j could not be found on classpath!");
      } catch (Exception ex) {
        console(Phase.INIT, "(!) Logging: attempt to load Log4j configuration failed!");
        ex.printStackTrace();
      }
    } else {
      console(Phase.INIT, "Logging: config/"+configuration.getName()+" not found");
    }
    return configured;
  }

  // Global settings
  // ----------------------------------------------------------------------------------------------

  /**
   * Checking that the global setting are loaded properly.
   */
  private static void loadSettings() {
    @Nullable File modeConfigFile = GlobalSettings.getModeConfigFile();
    @Nullable File defaultConfigFile = GlobalSettings.getDefaultConfigFile();
    String mode = GlobalSettings.getMode();
    if (modeConfigFile != null || defaultConfigFile != null) {
      @Nullable File appdata = GlobalSettings.getAppData();
      @Nullable File webinf = GlobalSettings.getWebInf();
      if (modeConfigFile != null && appdata != null) {
        console(Phase.INIT, "Config: found [appdata]/"+toRelPath(modeConfigFile, appdata));
      }
      if (defaultConfigFile != null && webinf != null) {
        console(Phase.INIT, "Config: found [webinf]/"+toRelPath(defaultConfigFile, webinf));
      }
      boolean loaded = GlobalSettings.load();
      if (loaded) {
        console(Phase.INIT, "Config: loaded OK ("+GlobalSettings.countProperties()+" properties found)");
        console(Phase.INIT, "Config: HTTP Compression = "+GlobalSettings.get(BerliozOption.HTTP_COMPRESSION));
        console(Phase.INIT, "Config: XSLT Caching = "+GlobalSettings.get(BerliozOption.XSLT_CACHE));
        console(Phase.INIT, "Config: XML Strict Parse = "+GlobalSettings.get(BerliozOption.XML_PARSE_STRICT));
        console(Phase.INIT, "Config: XML Header Version = "+GlobalSettings.get(BerliozOption.XML_HEADER_VERSION));
        console(Phase.INIT, "Config: OK ----------------------------------------------------");
      } else {
        console(Phase.INIT, "(!) Unable to load global settings ");
        console(Phase.INIT, "Config: FAIL --------------------------------------------------");
      }
    } else {
      console(Phase.INIT, "(!) Could not find config.xml, config.properties, config-"+mode+".xml or config-"+mode+".properties");
      console(Phase.INIT, "Config: FAIL --------------------------------------------------");
    }
  }

  // Lifecycle listeners
  // ----------------------------------------------------------------------------------------------

  /**
   * Checking that the global setting are loaded properly.
   *
   * @param listenerClass The lifecycle listener class.
   */
  protected void registerListener(String listenerClass) {
    @Nullable LifecycleListener listener = null;
    // Instantiate
    try {
      Class<?> c = Class.forName(listenerClass);
      listener = (LifecycleListener)c.newInstance();
    } catch (ClassNotFoundException ex) {
      console(Phase.INIT, "Lifecycle: (!) Unable to find class for listener:");
      console(Phase.INIT, "  "+listenerClass);
    } catch (ClassCastException ex) {
      console(Phase.INIT, "Lifecycle: (!) Class does not implement LifecycleListener:");
      console(Phase.INIT, "  "+listenerClass);
    } catch (IllegalAccessException ex) {
      console(Phase.INIT, "Lifecycle: (!) Unable to access lifecycle listener:");
      console(Phase.INIT, "  "+ex.getMessage());
    } catch (InstantiationException ex) {
      console(Phase.INIT, "Lifecycle: (!) Unable to instantiate lifecycle listener:");
      console(Phase.INIT, "  "+ex.getMessage());
    }
    // Start
    if (listener != null) {
      this._listeners.add(listener);
    }
  }

  /**
   * Checking that the global setting are loaded properly.
   */
  private void startListeners() {
    // Start
    if (this._listeners.size() > 0) {
      boolean ok = true;
      for (LifecycleListener listener : this._listeners) {
        try {
          ok = ok && listener.start();
          console(Phase.INIT, "Lifecycle: started "+listener.getClass().getSimpleName());
        } catch (Exception ex) {
          ok = false;
          ex.printStackTrace();
        }
      }
      if (ok) {
        console(Phase.INIT, "Lifecycle: OK -------------------------------------------------");
      } else {
        console(Phase.INIT, "(!) Unable to start Lifecycle listener");
        console(Phase.INIT, "Lifecycle: FAIL -----------------------------------------------");
      }

    } else {
      console(Phase.INIT, "Lifecycle: OK (No listeners)");
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
    if (p.startsWith(b) && p.length() > b.length()) return p.substring(b.length()+1).replace('\\', '/');
    else
      return p;
  }

  /**
   * Log what the initializer is doing on the console.
   *
   * @param phase   the initialization phase.
   * @param message the message to log.
   */
  private static void console(Phase phase, String message) {
    System.out.println("[BERLIOZ_"+phase+"] "+message);
  }

}
