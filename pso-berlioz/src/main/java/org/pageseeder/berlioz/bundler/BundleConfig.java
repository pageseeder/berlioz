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
package org.pageseeder.berlioz.bundler;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration for the bundling for a given type.
 *
 * <p>Stores the bundle definitions and instantiate bundles.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.3
 * @since 0.9.32
 */
public final class BundleConfig implements Serializable {

  private static final String GLOBAL_LEVEL = "global";
  private static final String GROUP_LEVEL = "group";
  private static final String SERVICE_LEVEL = "service";

  /**
   * Name of the fallback configuration used when a requested name isn't explicitly configured.
   */
  static final String DEFAULT_CONFIG = "default";

  /** Serializable */
  private static final long serialVersionUID = 5709906856099064344L;

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleConfig.class);

  /**
   * The default bundle configuration.
   */
  private static final List<String> DEFAULT_BUNDLE_CONFIG = List.of(GLOBAL_LEVEL, GROUP_LEVEL, SERVICE_LEVEL);

  /**
   * The default JavaScript bundle definitions
   */
  private static final Map<String, BundleDefinition> DEFAULT_JS_BUNDLE = Map.of(
      GLOBAL_LEVEL,  new BundleDefinition(GLOBAL_LEVEL,  GLOBAL_LEVEL,   "/script/global.js"),
      GROUP_LEVEL,   new BundleDefinition(GROUP_LEVEL,   "{GROUP}",   "/script/{GROUP}.js"),
      SERVICE_LEVEL, new BundleDefinition(SERVICE_LEVEL, "{SERVICE}", "/script/{GROUP}/{SERVICE}.js"));

  /**
   * The default CSS bundle definitions
   */
  private static final Map<String, BundleDefinition> DEFAULT_CSS_BUNDLE = Map.of(
      GLOBAL_LEVEL,  new BundleDefinition(GLOBAL_LEVEL,  GLOBAL_LEVEL,    "/style/global.css"),
      GROUP_LEVEL,   new BundleDefinition(GROUP_LEVEL,   "{GROUP}",   "/style/{GROUP}.css"),
      SERVICE_LEVEL, new BundleDefinition(SERVICE_LEVEL, "{SERVICE}", "/style/{GROUP}/{SERVICE}.css"));

  /** Where the bundled scripts should be located. */
  private static final String DEFAULT_BUNDLED_SCRIPTS = "/script/_/";

  /** Where the bundled styles should be located. */
  private static final String DEFAULT_BUNDLED_STYLES = "/style/_/";

  // Class attributes
  // ----------------------------------------------------------------------------------------------

  /**
   * The list of definitions in this configuration.
   */
  private final List<BundleDefinition> definitions;

  /**
   * The list of bundle instances mapped to service IDs.
   */
  private final Map<String, List<BundleInstance>> instances = new ConcurrentHashMap<>();

  /**
   * The type of bundle config.
   */
  private final BundleType type;

  /**
   * Whether the code should be minimized as part of bundling.
   */
  private final boolean minimize;

  /**
   * Where the bundled code should be stored.
   */
  private final String location;

  /**
   * The root of the web application.
   */
  private final File root;

  /**
   * The tool used for bundling JS.
   */
  private transient @Nullable WebBundleTool bundler;

  /**
   * Create a new config - use factory method instead.
   */
  private BundleConfig(List<BundleDefinition> definitions, BundleType type, boolean minimize, String location, File root) {
    this.definitions = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
    this.type = Objects.requireNonNull(type, "type");
    this.minimize = minimize;
    this.location = Objects.requireNonNull(location, "location");
    this.root = Objects.requireNonNull(root, "root");
    this.bundler = initBundler();
  }

  /**
   * @return bundle definitions.
   */
  List<BundleDefinition> definitions() {
    return this.definitions;
  }

  /**
   * @return the type of bundle.
   */
  public BundleType type() {
    return this.type;
  }

  /**
   * @return <code>true</code> to minimize the code; <code>false</code> otherwise.
   */
  public boolean minimize() {
    return this.minimize;
  }

  /**
   * @return Path to where the bundles should be stored.
   */
  public String location() {
    return this.location;
  }

  /**
   * @return The root of the web application.
   */
  public File root() {
    return this.root;
  }

  /**
   * @return Where the bundles are stored
   */
  public File store() {
    return new File(this.root, this.location);
  }


  /**
   * @return The bundler.
   */
  public synchronized WebBundleTool bundler() {
    WebBundleTool tool = this.bundler;
    if (tool == null) {
      tool = initBundler();
      this.bundler = tool;
    }
    return tool;
  }

  /**
   * @param service The service.
   * @return the list of bundles for this service
   */
  public List<File> getBundles(Service service) {
    List<BundleInstance> bundles = getInstances(service);
    List<File> files = new ArrayList<>(bundles.size());
    for (BundleInstance instance : bundles) {
      File b = instance.getBundleFile(this);
      if (b != null) {
        files.add(b);
      }
    }
    return files;
  }

  /**
   * @param service The service.
   * @return the last modified bundle file for this service
   */
  public long getLastModifiedBundle(Service service) {
    List<BundleInstance> bundles = getInstances(service);
    long lastModified = 0L;
    for (BundleInstance instance : bundles) {
      File bundle = instance.getBundleFile(this);
      if (bundle != null && bundle.lastModified() > lastModified) {
        lastModified = bundle.lastModified();
      }
    }
    return lastModified;
  }

  /**
   * @param service The service.
   * @return the list of paths for this service
   */
  public List<String> getPaths(Service service) {
    List<BundleInstance> bundles = getInstances(service);
    List<String> paths = new ArrayList<>();
    for (BundleInstance instance : bundles) {
      instance.addToExistingPaths(paths);
    }
    return paths;
  }

  /**
   * Returns the list of bundle instance for the specific service.
   *
   * @param service the service for which the bundle instances are needed.
   *
   * @return the corresponding configuration.
   */
  private List<BundleInstance> getInstances(Service service) {
    return this.instances.computeIfAbsent(service.id(), id -> instantiate(service));
  }

  // static helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Caches, per bundle type, the configuration names explicitly declared in the global settings
   * (via <code>berlioz.&lt;type&gt;bundler.configs.&lt;name&gt;</code>).
   *
   * <p>Cleared whenever the global settings are (re)loaded so a reload is reflected without
   * requiring a restart.
   */
  private static final Map<BundleType, Set<String>> KNOWN_CONFIG_NAMES = new ConcurrentHashMap<>();

  static {
    GlobalSettings.registerListener(BundleConfig::clearKnownConfigNamesCache);
  }

  /**
   * Restricts a requested configuration name to one that an administrator has actually declared,
   * falling back to {@value #DEFAULT_CONFIG} otherwise.
   *
   * <p>An unrecognized name resolves to the same fallback bundle definitions as
   * {@value #DEFAULT_CONFIG} anyway (see {@link #getBundleNames(String)}), so collapsing it to
   * that name before it is used as a cache key changes no observable behavior. This keeps the
   * per-type configuration caches in {@code GetWebBundles} bounded to a finite, administrator
   * controlled set of names instead of letting an arbitrary request parameter value grow them
   * indefinitely.
   *
   * @param name The requested configuration name.
   * @param type The bundle type.
   *
   * @return <code>name</code> if it is declared for this type; {@value #DEFAULT_CONFIG} otherwise.
   */
  static String sanitizeConfigName(String name, BundleType type) {
    if (DEFAULT_CONFIG.equals(name) || knownConfigNames(type).contains(name)) return name;
    return DEFAULT_CONFIG;
  }

  /**
   * @param type The bundle type.
   * @return the configuration names declared for that type, computing and caching them if needed.
   */
  private static Set<String> knownConfigNames(BundleType type) {
    return KNOWN_CONFIG_NAMES.computeIfAbsent(type, BundleConfig::loadKnownConfigNames);
  }

  /**
   * Builds a global settings key of the form <code>berlioz.&lt;type&gt;bundler.&lt;suffix&gt;</code>.
   *
   * @param type   The bundle type.
   * @param suffix The part of the key following <code>bundler.</code>.
   *
   * @return the corresponding global settings key.
   */
  private static String bundlerKey(BundleType type, String suffix) {
    return "berlioz." + type.name().toLowerCase() + "bundler." + suffix;
  }

  /**
   * Scans the global settings for configuration names declared for the specified bundle type.
   *
   * @param type The bundle type.
   * @return the declared configuration names for that type.
   */
  private static Set<String> loadKnownConfigNames(BundleType type) {
    String prefix = bundlerKey(type, "configs.");
    return GlobalSettings.getAll().keySet().stream()
        .filter(key -> key.startsWith(prefix) && key.length() > prefix.length())
        .map(key -> key.substring(prefix.length()))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Clears the cached, per-type sets of declared configuration names.
   */
  private static void clearKnownConfigNamesCache() {
    KNOWN_CONFIG_NAMES.clear();
  }

  /**
   * Creates a new instance of a bundle configuration.
   *
   * @param name The name of the config.
   * @param type The type "js" or "css".
   * @param root The root of the web application.
   *
   * @return the corresponding configuration.
   */
  public static BundleConfig newInstance(String name, BundleType type, File root) {
    List<String> names = getBundleNames(bundlerKey(type, "configs." + name));
    Map<String, BundleDefinition> defaults = BundleType.JS == type? DEFAULT_JS_BUNDLE : DEFAULT_CSS_BUNDLE;
    List<BundleDefinition> definitions = loadDefinitions(names, bundlerKey(type, "bundles."), defaults);
    boolean minimize = GlobalSettings.get(bundlerKey(type, "minimize"), true);

    // Create the bundle store
    String defaultLocation = getDefaultLocation(type);
    String location = GlobalSettings.get(bundlerKey(type, "location"), defaultLocation);
    File store = new File(root, location);
    try {
      Files.createDirectories(store.toPath());
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to create bundle location: " + store, ex);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading bundle config:{} ({}) => {}", name, type, names);
      LOGGER.debug("Bundler settings minimize:{} location:{}", minimize, location);
      for (BundleDefinition d : definitions) {
        LOGGER.debug("Bundle definition:{} -> {} ({})", d.name(), d.filename(), d.paths());
      }
    }
    return new BundleConfig(definitions, type, minimize, location, root);
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  private WebBundleTool initBundler() {
    // Initialize the bundler
    WebBundleTool tool = new WebBundleTool(new File(this.root, this.location));
    tool.setRoot(this.root);
    if (this.type == BundleType.CSS) {
      int threshold = GlobalSettings.get(bundlerKey(BundleType.CSS, "datauris.threshold"), 4096);
      tool.setDataURIThreshold(threshold);
    }
    return tool;
  }

  /**
   * Create a bundle instance for each bundle definition for that service.
   *
   * @param service the service
   * @return the corresponding list of instances.
   */
  private List<BundleInstance> instantiate(Service service) {
    return this.definitions.stream()
        .map(def -> BundleInstance.instantiate(this, def, service))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  /**
   * Returns the list of bundle definitions for the specified names from the global settings
   * and falling back on the defaults defined in this class.
   *
   * @param names    the names of the bundle configuration to get.
   * @param prefix   the prefix in the global properties
   * @param defaults the default bundle configurations.
   *
   * @return The corresponding list.
   */
  private static List<BundleDefinition> loadDefinitions(List<String> names, String prefix, Map<String, BundleDefinition> defaults) {
    List<BundleDefinition> bundles = new ArrayList<>();
    for (String name : names) {
      BundleDefinition bc = defaults.get(name);
      // Same as the name if the 'filename' subproperty isn't defined
      String filename = GlobalSettings.get(prefix + name + ".filename", bc != null? bc.filename() : name);
      // The value of the property if the 'paths' subproperty isn't defined.
      String paths = GlobalSettings.get(prefix + name + ".include", GlobalSettings.get(prefix + name, ""));
      if (!paths.isBlank()) {
        bc = new BundleDefinition(name, filename, paths);
      }
      if (bc != null) {
        bundles.add(bc);
      } else {
        LOGGER.warn("Bundle '{}' is undefined", name);
      }
    }
    return bundles;
  }

  /**
   * Returns The configuration for the specified property.
   *
   * @param property the name of the property in the global config.
   *
   * @return The corresponding bundle names.
   */
  private static List<String> getBundleNames(String property) {
    String names = GlobalSettings.get(property);
    return names != null? parseCSV(names) : DEFAULT_BUNDLE_CONFIG;
  }

  /**
   * Parses a comma-separated property and ignores incidental whitespace.
   *
   * @param value the comma-separated property.
   *
   * @return the list of configured names.
   */
  private static List<String> parseCSV(String value) {
    return Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(name -> !name.isEmpty())
        .collect(Collectors.toList());
  }

  /**
   * @param type JS or CSS.
   * @return the default location for the specified type.
   */
  private static String getDefaultLocation(BundleType type) {
    return BundleType.JS == type? DEFAULT_BUNDLED_SCRIPTS : DEFAULT_BUNDLED_STYLES;
  }

}
