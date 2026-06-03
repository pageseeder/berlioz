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
package org.pageseeder.berlioz;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.config.ConfigException;
import org.pageseeder.berlioz.config.GlobalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Berlioz global settings.
 *
 * <p>This class provides global access to the settings for the application.
 * It needs to be set up prior to using most of the classes used in this
 * application.
 *
 * <p>This class uses two main access points:
 * <ul>
 *   <li><var>webinf</var>: is the Web application directory that contains all
 *   things related to the application that aren't in the document root of the
 *   application. It normally corresponds to the <code>WEB-INF</code>
 *   folder.</li>
 *
 *   <li><var>appdata</var>: is the directory that contains the application
 *   data that is not part of the application distribution such as a WAR file
 *   and that needs to survive redeployments and updates. This folder defaults
 *   to the same as the <i>webinf</i> folder.</li>
 *
 *   <li><var>configuration</var>: is the file that contains all the global properties used
 *   in this application; it is always located in the <i>/config</i> directory of the
 *   repository; the default name of this file 'config.xml'.</li>
 * </ul>
 *
 * <p>The <var>appdata</var> and <var>config</var> may be specified using System properties,
 * respectively <code>berlioz.appdata</code> and <code>berlioz.config</code>.
 *
 * <p>Since Berlioz 0.10, the <code>berlioz.repository</code> is no longer supported.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.6
 */
public final class GlobalSettings {

  /**
   * Errors about loading the properties are reported here.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSettings.class);

  /**
   * The format of configuration used.
   */
  private enum Format { XML_CONFIG, PROPERTIES }

  // constants
  // --------------------------------------------------------------------------

  /**
   * Name of the directory containing the configuration files for Berlioz,
   * including the global settings, services, logging, etc...
   *
   * @deprecated since 0.11.4, use {@link InitEnvironment#DEFAULT_CONFIG_DIRECTORY} instead
   */
  @Deprecated(since = "0.11.4")
  public static final String CONFIG_DIRECTORY = "config";

  /**
   * Name of the default configuration to use.
   */
  public static final String DEFAULT_MODE = "default";

  // static variables
  // --------------------------------------------------------------------------

  /**
   * The Web application directory.
   *
   * <p>This should always be the <code>WEB-INF</code> folder of the Web
   * application.
   */
  private static final AtomicReference<@Nullable InitEnvironment> ENV = new AtomicReference<>();

  /**
   * The global properties.
   */
  private static final AtomicReference<@Nullable Map<String, String>> SETTINGS = new AtomicReference<>();

  /**
   * Maps properties to nodes that have been processed.
   */
  private static final AtomicReference<@Nullable Map<String, Properties>> NODES = new AtomicReference<>();

  /**
   * The list of listeners to invoke when the global settings have been reloaded.
   */
  private static final List<ConfigListener> LISTENERS = new ArrayList<>();

  // Constructor
  // ---------------------------------------------------------------------------------

  /**
   * Prevents the creation of instances.
   */
  private GlobalSettings() {
    // empty constructor
  }

  private static @Nullable InitEnvironment env() { return ENV.get(); }

  private static InitEnvironment requireEnv() {
    InitEnvironment e = ENV.get();
    if (e == null) throw new IllegalStateException("GlobalSettings has not been set up");
    return e;
  }

  // General static methods
  // --------------------------------------------------------------------------

  /**
   * Returns the main repository or <code>null</code> if it has not been set up.
   *
   * @return The directory used as a repository or <code>null</code>.
   */
  public static @Nullable File getWebInf() {
    InitEnvironment e = env();
    return e != null ? e.webInf() : null;
  }

  /**
   * Returns the application data folder.
   *
   * <p>It can be the same as the Web application directory (<code>WEB-INF</code>),
   * but may different in cases where the data needs to be persistent and
   * separate from the application itself.
   *
   * @return The Web application data folder or <code>null</code>.
   */
  public static @Nullable File getAppData() {
    InitEnvironment e = env();
    return e != null ? e.appData() : null;
  }

  /**
   * note: If the appData is different of webInf then maybe the mode config could be in another folder.
   * Therefore, in this case it should be worth checking in {appData}/config
   *
   * @return The configuration directory containing all configuration files for Berlioz.
   */
  public static @Nullable File getConfig() {
    InitEnvironment e = env();
    return e != null ? e.webInf().toPath().resolve(e.configFolder()).toFile() : null;
  }

  /**
   * Returns the number of properties defined in the file.
   *
   * @return The number of properties defined in the file.
   */
  public static int countProperties() {
    return ensureSettings().size();
  }

  /**
   * Returns the build version of Berlioz.
   *
   * @return the Berlioz version.
   */
  public static String getVersion() {
    Package p = GlobalSettings.class.getPackage();
    String v = p != null ? p.getImplementationVersion() : "unknown";
    return v != null ? v : "unknown";
  }

  /**
   * @return The Berlioz mode in use.
   */
  public static String getMode() {
    InitEnvironment e = env();
    return e != null ? e.mode() : InitEnvironment.DEFAULT_MODE;
  }

  /**
   * Returns the properties file to use externally.
   *
   * @return The properties file to load or <code>null</code>.
   */
  public static @Nullable File getPropertiesFile() {
    if (env() == null) return null;
    File f = getModeConfigFile();
    if (f == null) {
      f = getDefaultConfigFile();
    }
    return f;
  }

  /**
   * Returns the properties file to use externally.
   *
   * @return The properties file to load or <code>null</code>.
   */
  public static @Nullable File getModeConfigFile() {
    InitEnvironment e = env();
    if (e == null) return null;
    File appDataConfigDirectory = e.appData().toPath().resolve(e.configFolder()).toFile();
    File f = getModeConfigFile(appDataConfigDirectory);
    if (f == null || !f.exists()) {
      File webInfConfigDirectory = e.webInf().toPath().resolve(e.configFolder()).toFile();
      f = getModeConfigFile(webInfConfigDirectory);
    }
    return f;
  }

  /**
   * Returns the properties file to use externally.
   *
   * @return The properties file to load or <code>null</code>.
   */
  public static @Nullable File getDefaultConfigFile() {
    InitEnvironment e = env();
    if (e == null) return null;
    return getDefaultConfigFile(e.webInf().toPath().resolve(e.configFolder()).toFile());
  }

  // Properties methods
  // --------------------------------------------------------------------------

  /**
   * Return the requested property.
   *
   * <p>Returns <code>null</code> if the property is not found or defined.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}
   * method before returning an <code>Enumeration</code>.
   *
   * @param name  the name of the property
   *
   * @return  the property value or <code>null</code>.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static @Nullable String get(String name) throws IllegalStateException {
    return ensureSettings().get(name);
  }

  /**
   * Return the property value for the specified Berlioz option.
   *
   * <p>Returns the <code>default</code> value if the property is not found or defined.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}
   * method before returning an <code>Enumeration</code>.
   *
   * @param option  the name of the property
   *
   * @return  the property value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static String get(BerliozOption option) throws IllegalStateException {
    return get(option.property(), option.defaultTo().toString());
  }

  /**
   * Indicates whether the property value for the specified Berlioz option is enabled.
   *
   * <p>Returns the <code>default</code> value if the property is not found or defined.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}
   * method.
   *
   * @param option the name of the property
   *
   * @return whether the specified option is set to <code>true</code> or not.
   *
   * @throws IllegalStateException    If this class has not been setup properly.
   * @throws IllegalArgumentException If this class has not been setup properly.
   * @throws NullPointerException     If the specified option is <code>null</code>.
   */
  public static boolean has(BerliozOption option) {
    Objects.requireNonNull(option, "No Berlioz option specified");
    String value = ensureSettings().get(option.property());
    Object def = option.defaultTo();
    if (option.isBoolean()) return value != null? Boolean.parseBoolean(value) : (Boolean) def;
    else
      throw new IllegalArgumentException("Trying to get non-boolean option '"+option.property()+"' as boolean.");
  }

  /**
   * Returns the requested property or it default value.
   *
   * <p>The given default value is returned only if the property is not found.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}
   * method before returning an <code>Enumeration</code>.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static String get(String name, String def) throws IllegalStateException {
    String value = ensureSettings().get(name);
    return value == null? def : value;
  }

  /**
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static int get(String name, int def) throws IllegalStateException {
    try {
      String value = ensureSettings().get(name);
      return value == null? def : Integer.parseInt(value);
    } catch (NumberFormatException ex) {
      return def;
    }
  }

  /**
   * Returns the requested property or it default value.
   *
   * <p>The given default value is returned only if the property is not found.
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static boolean get(String name, boolean def) throws IllegalStateException {
    String value = ensureSettings().get(name);
    if (value == null) return def;
    return def? !"false".equals(value) : "true".equals(value);
  }

  /**
   * Returns the requested property as a file or its default file value.
   *
   * <p>The given default value is returned if:
   * <ul>
   *   <li>the property is not found;</li>
   *   <li>the file corresponding to the property does not exist;</li>
   *   <li>there is an error.</li>
   * </ul>
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name  The name of the property.
   * @param def   A default value for the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static File get(String name, File def) throws IllegalStateException {
    File file = getFileProperty(name);
    return file != null? file : def;
  }

  /**
   * Returns the requested property as a file.
   *
   * <p>The given default value is returned if:
   * <ul>
   *   <li>the property is not found;</li>
   *   <li>the file corresponding to the property does not exist;</li>
   *   <li>there is an error</li>
   * </ul>
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name  The name of the property.
   *
   * @return  the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static @Nullable File getDirProperty(String name) throws IllegalStateException {
    File file = getFileProperty(name);
    if (file != null && file.isDirectory())
      return file;
    else return null;
  }

  /**
   * Returns the requested property as a file.
   *
   * <p>The given default value is returned if:
   * <ul>
   *   <li>the property is not found;</li>
   *   <li>the file corresponding to the property does not exist;</li>
   *   <li>there is an error
   * </ul>
   *
   * <p>If the properties file has not been loaded, this method will invoke the {@link #load()}.
   *
   * @param name  The name of the property.
   *
   * @return the property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static @Nullable File getFileProperty(String name) throws IllegalStateException {
    String filepath = ensureSettings().get(name);
    if (filepath != null) {
      InitEnvironment e = requireEnv();
      // try appData first
      File file = new File(e.appData(), filepath);
      try {
        if (file.exists()) return file.getCanonicalFile();
      } catch (IOException ex) {
        LOGGER.warn("Unable to generate canonical file: {}", ex.getMessage());
      }
      // fall back on webinf
      if (e.appData() != e.webInf()) {
        file = new File(e.webInf(), filepath);
        try {
          if (file.exists()) return file.getCanonicalFile();
        } catch (IOException ex) {
          LOGGER.warn("Unable to generate canonical file: {}", ex.getMessage());
        }
      }
    }
    return null;
  }

  /**
   * Returns the entries for the specified node as <code>Properties</code>.
   *
   * @param name The name of the database.
   *
   * @return The property value or the default value.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static @Nullable Properties getNode(String name) {
    Map<String, String> s = ensureSettings();
    Map<String, Properties> cache = NODES.get();
    if (cache == null) return null;
    return cache.computeIfAbsent(name, k -> {
      Properties node = new Properties();
      String prefix = k + '.';
      for (Entry<String, String> e : s.entrySet()) {
        String key = e.getKey();
        if (key.startsWith(prefix) && key.indexOf('.', prefix.length()) < 0) {
          node.setProperty(key.substring(prefix.length()), e.getValue());
        }
      }
      return node;
    });
  }

  /**
   * Enumerates the properties in the global settings.
   *
   * @return An enumeration of the property names.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  public static Enumeration<String> propertyNames() throws IllegalStateException {
    return Collections.enumeration(ensureSettings().keySet());
  }

  /**
   * Returns all the global properties as an unmodifiable map.
   *
   * @return An unmodifiable map of the properties.
   *
   * @throws IllegalStateException If this class has not been setup properly.
   *
   * @since 0.11.2
   */
  public static Map<String, String> getAll() throws IllegalStateException {
    return Collections.unmodifiableMap(ensureSettings());
  }

  // Setup methods
  // -------------------------------------------------------------------------------


  /**
   * Sets the initial environment
   *
   * @param environment The environment to use.
   */
  public static void setup(InitEnvironment environment) {
    ENV.set(environment);
    SETTINGS.set(null);
    NODES.set(null);
  }

  /**
   * Sets the initial environment
   *
   * @param webInf The Web application folder to use.
   */
  public static void setup(File webInf) {
    ENV.set(InitEnvironment.create(webInf));
    SETTINGS.set(null);
    NODES.set(null);
  }

  /**
   * Sets the application directory to the specified file if it exists and is a directory.
   *
   * <p>The application directory corresponds to the <code>/WEB-INF</code> folder in your
   * application context root.
   *
   * @param dir The directory to use.
   *
   * @throws NullPointerException If the specified file is <code>null</code>.
   * @throws IllegalArgumentException If the specified file is not a valid repository.
   *
   * @deprecated since 0.11.4, use {@link InitEnvironment#webInf(File)} instead
   */
  @Deprecated(since = "0.11.4")
  public static void setWebInf(File dir) {
    ENV.updateAndGet(e -> e != null ? e.webInf(dir) : InitEnvironment.create(dir));
    SETTINGS.set(null);
    NODES.set(null);
  }

  /**
   * Sets the application data directory to the specified file if it exists and is a directory.
   *
   * <p>The application data directory is used to store persistent data
   *
   * @param dir The directory to use for application data
   *
   * @throws NullPointerException If the specified file is null.
   * @throws IllegalArgumentException If the specified file is not a valid repository.
   *
   * @deprecated since 0.11.4, use {@link InitEnvironment#appData(File)} instead
   */
  @Deprecated(since = "0.11.4")
  public static void setAppData(File dir) {
    ENV.updateAndGet(e -> e != null ? e.appData(dir) : e);
  }

  /**
   * Sets the configuration mode to use.
   *
   * @param name The name of the mode to use.
   *
   * @throws NullPointerException If the name of the mode is <code>null</code>.
   *
   * @deprecated since 0.11.4, use {@link InitEnvironment#mode(String)} instead
   */
  @Deprecated(since = "0.11.4")
  public static void setMode(String name) {
    ENV.updateAndGet(e -> e != null ? e.mode(name) : e);
  }

  /**
   * Loads the properties.
   *
   * <p>There are several mechanism to load the properties.
   *
   * <p>First, this method will try to use the properties file for the mode.
   *
   * <p>If all of the above fail, the properties will remain empty, this method will return
   * <code>false</code> to indicate that the properties could not be loaded.
   *
   * <p>Errors will only be reported to the <code>System</code> error output, the complete stack
   * trace will be available.
   *
   * <p>In all cases, the file must be conform to the java properties specifications.
   *
   * @see java.util.Properties
   * @see System#getProperty(java.lang.String)
   * @see ClassLoader#getResourceAsStream(java.lang.String)
   *
   * @return <code>true</code> if the properties were loaded; <code>false</code> otherwise.
   *
   * @throws IllegalStateException If this class has not been set up properly.
   */
  public static synchronized boolean load() throws IllegalStateException {
    boolean loaded = false;

    // make sure we have a repository
    Map<String, String> properties = new HashMap<>();

    try {
      requireEnv();
      // Try to load the default config file
      File defaultConfig = getDefaultConfigFile();
      if (defaultConfig != null) {
        loadInto(defaultConfig, properties);
      }

      // Try to override with the mode-specific config file
      File modeConfig = getModeConfigFile();
      if (modeConfig != null) {
        loadInto(modeConfig, properties);
      }

      // Considered loaded if any file was loaded without error
      loaded = defaultConfig != null || modeConfig != null;

    } catch (Exception ex) {
      console("[BERLIOZ_CONFIG] (!) An error occurred whilst trying to read the properties file.");
      LOGGER.warn("Unable to load the configuration file", ex);
      properties.clear(); // Let's not load dirty properties
    } finally {
      // Reset after loading
      SETTINGS.set(properties);
      NODES.set(new ConcurrentHashMap<>());
    }

    // Notify the listeners
    if (loaded) {
      for (ConfigListener listener : LISTENERS) {
        try {
          listener.load();
        } catch (Exception ex) {
          // MUST NOT throw an exception, silently ignore and report in logs
          LOGGER.warn("Listener threw an exception", ex);
        }
      }
    }

    return loaded;
  }

  /**
   * Add a listener to invoke when the settings are being loaded or reloaded.
   *
   * @param listener The listener to register.
   */
  @Beta
  public static void registerListener(ConfigListener listener) {
    LISTENERS.add(listener);
  }

  /**
   * Removes all the listeners from the global settings.
   */
  @Beta
  public static void removeAllListeners() {
    LISTENERS.clear();
  }

  // private helpers
  // --------------------------------------------------------------------------

  /**
   * Loads the specified configuration file into the specific map.
   *
   * @param file The file to load
   * @param properties  The map containing the global settings
   *
   * @throws IOException Should an I/O error occur while reading the properties.
   * @throws ConfigException Should a parse error occur while reading the properties.
   */
  private static void loadInto(File file, Map<String, String> properties) throws IOException, ConfigException {
    Format format = file.getName().endsWith(".xml")? Format.XML_CONFIG : Format.PROPERTIES;
    LOGGER.debug("Loading Berlioz config {} as {}", file.getName(), format);
    switch (format) {
      case XML_CONFIG:
        loadXMLConfig(file, properties);
        break;
      case PROPERTIES:
        loadProperties(file, properties);
        break;
    }
  }

  /**
   * Loads the settings from the specified XML config file.
   *
   * @param file The XML config file to load.
   * @param map  The properties to load into
   *
   * @throws IOException Should an error be reported by the parser.
   */
  private static void loadProperties(File file, Map<String, String> map) throws IOException {
    Properties p = new Properties();
    try (InputStream in = new FileInputStream(file)){
      p.load(in);
      // Load the values into the map
      for (Entry<Object, Object> e : p.entrySet()) {
        map.put(e.getKey().toString(), e.getValue().toString());
      }
    }
  }

  /**
   * Loads the settings from the specified XML config file.
   *
   * @param file The XML config file to load.
   * @param map  The properties to load into
   *
   * @throws IOException If an I/O error occur while reading the file.
   * @throws ConfigException If a parse error occur while reading the file.
   */
  private static void loadXMLConfig(File file, Map<String, String> map) throws IOException, ConfigException {
    GlobalConfig config = new GlobalConfig(map);
    Path path = checkPath(file);
    try (InputStream in = Files.newInputStream(path)) {
      config.load(in);
    }
  }

  /**
   * Returns the properties file to use externally.
   *
   * @return The properties file to load or <code>null</code>.
   */
  private static @Nullable File getModeConfigFile(File dir) {
    if (!dir.isDirectory()) return null;
    // try as an XML file
    File xml = new File(dir, "config-"+getMode()+".xml");
    if (xml.canRead()) return xml;
    // otherwise try as a properties file
    File prp = new File(dir, "config-"+getMode()+".properties");
    if (prp.canRead()) return prp;
    return null;
  }

  /**
   * Returns the properties file to use externally.
   *
   * @return The properties file to load or <code>null</code>.
   */
  private static @Nullable File getDefaultConfigFile(File dir) {
    if (!dir.isDirectory()) return null;
    // try as an XML file
    File xml = new File(dir, "config.xml");
    if (xml.canRead()) return xml;
    // otherwise try as a properties file
    File prp = new File(dir, "config.properties");
    if (prp.canRead()) return prp;
    return null;
  }

  /**
   * Returns the settings map and attempts to load if needed.
   *
   * @return The settings map if load
   *
   * @throws IllegalStateException If this class has not been setup properly.
   */
  private static Map<String, String> ensureSettings() throws IllegalStateException {
    if (SETTINGS.get() == null) {
      load();
    }
    Map<String, String> s = SETTINGS.get();
    return s != null ? s : Map.of();
  }

  /**
   * Prevent path traversal by checking that file is within webinf or appdata.
   */
  private static Path checkPath(File file) throws IOException {
    String path = file.getCanonicalPath();
    InitEnvironment e = requireEnv();
    if (path.startsWith(e.appData().getCanonicalPath())
     || path.startsWith(e.webInf().getCanonicalPath())) {
      return file.toPath();
    }
    throw new IOException("Config file must be located within webinf or appdata folder");
  }

  /**
   * Log any global config error on the error console.
   *
   * @param message the message to log.
   */
  @SuppressWarnings("java:S106") // Writing to System.out is intentional here, logging isn't configured yet!
  private static void console(String message) {
    System.err.println("[BERLIOZ_CONFIG] "+message);
  }

}
