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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The configuration for the bundling for a given type.
 *
 * <p>Stores the bundles definitions and instantiate bundles.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public final class BundleConfig implements Serializable {

  /** Serializable */
  private static final long serialVersionUID = 5709906856099064344L;

  /**
   * Logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(BundleConfig.class);

  /**
   * The default bundle configuration.
   */
  private static final @NonNull String[] DEFAULT_BUNDLE_CONFIG = new String[]{ "global", "group", "service" };

  /**
   * The default JavaScript bundle definitions
   */
  private static final Map<String, BundleDefinition> DEFAULT_JS_BUNDLE = new HashMap<>();
  static {
    DEFAULT_JS_BUNDLE.put("global",  new BundleDefinition("global",  "global",    "/script/global.js"));
    DEFAULT_JS_BUNDLE.put("group",   new BundleDefinition("group",   "{GROUP}",   "/script/{GROUP}.js"));
    DEFAULT_JS_BUNDLE.put("service", new BundleDefinition("service", "{SERVICE}", "/script/{GROUP}/{SERVICE}.js"));
  }

  /**
   * The default CSS bundle definitions
   */
  private static final Map<String, BundleDefinition> DEFAULT_CSS_BUNDLE = new HashMap<>();
  static {
    DEFAULT_CSS_BUNDLE.put("global",  new BundleDefinition("global",  "global",    "/style/global.css"));
    DEFAULT_CSS_BUNDLE.put("group",   new BundleDefinition("group",   "{GROUP}",   "/style/{GROUP}.css"));
    DEFAULT_CSS_BUNDLE.put("service", new BundleDefinition("service", "{SERVICE}", "/style/{GROUP}/{SERVICE}.css"));
  }

  /** Where the bundled scripts should be located. */
  private static final String DEFAULT_BUNDLED_SCRIPTS = "/script/_/";

  /** Where the bundled styles should be located. */
  private static final String DEFAULT_BUNDLED_STYLES = "/style/_/";

  // Class attributes
  // ----------------------------------------------------------------------------------------------

  /**
   * The list of definitions in this configuration.
   */
  private final List<BundleDefinition> _definitions;

  /**
   * The list of bundle instances mapped to service IDs.
   */
  private final Map<String, List<BundleInstance>> _instances = new WeakHashMap<>();

  /**
   * The type of bundle config.
   */
  private final BundleType _type;

  /**
   * Whether the code should be minimized as part of bundling.
   */
  private final boolean _minimize;

  /**
   * Whether the code should be minimized as part of bundling.
   */
  private final String _location;

  /**
   * The root of the web application.
   */
  private final File _root;

  /**
   * The tool used for bundling JS.
   */
  private final WebBundleTool _bundler;

  /**
   * Create a new config - use factory method instead.
   */
  private BundleConfig(List<BundleDefinition> definitions, BundleType type, boolean minimize, String location, File root) {
    this._definitions = definitions;
    this._type = type;
    this._minimize = minimize;
    this._location = location;
    this._root = root;
    this._bundler = initBundler();
  }

  /**
   * @return bundle definitions.
   */
  public List<BundleDefinition> definitions() {
    return this._definitions;
  }

  /**
   * @return the type of bundle.
   */
  public BundleType type() {
    return this._type;
  }

  /**
   * @return <code>true</code> to minimize the code; <code>false</code> otherwise.
   */
  public boolean minimize() {
    return this._minimize;
  }

  /**
   * @return Path to where the bundles should be stored.
   */
  public String location() {
    return this._location;
  }

  /**
   * @return The root of the web application.
   */
  public File root() {
    return this._root;
  }

  /**
   * @return Where the bundles are stored
   */
  public File store() {
    return new File(this._root, this._location);
  }


  /**
   * @return The bundler.
   */
  public WebBundleTool bundler() {
    return this._bundler;
  }

  /**
   * @param service The service.
   * @return the list of bundles for this service
   */
  public List<File> getBundles(Service service) {
    List<BundleInstance> instances = getInstances(service);
    List<File> files = new ArrayList<>(instances.size());
    for (BundleInstance instance : instances) {
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
    List<BundleInstance> instances = getInstances(service);
    long lastModified = 0L;
    for (BundleInstance instance : instances) {
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
    List<BundleInstance> instances = getInstances(service);
    List<String> paths = new ArrayList<>();
    for (BundleInstance instance : instances) {
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
  public List<BundleInstance> getInstances(Service service) {
    List<BundleInstance> instances = this._instances.get(service.id());
    if (instances == null) {
      instances = instantiate(service);
      this._instances.put(service.id(), instances);
    }
    return instances;
  }

  // static helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Creates new instance of a bundle configuration.
   *
   * @param name The name of the config.
   * @param type The type "js" or "css".
   * @param root The root of the web application.
   *
   * @return the corresponding configuration.
   */
  public static BundleConfig newInstance(String name, BundleType type, File root) {
    String lctype = type.name().toLowerCase();
    @NonNull String[] names = getBundleNames("berlioz."+lctype+"bundler.configs."+name);
    Map<String, BundleDefinition> defaults = BundleType.JS == type? DEFAULT_JS_BUNDLE : DEFAULT_CSS_BUNDLE;
    List<BundleDefinition> definitions = loadDefinitions(names, "berlioz."+lctype+"bundler.bundles.", defaults);
    boolean minimize = GlobalSettings.get("berlioz."+lctype+"bundler.minimize", true);

    // Create the bundle store
    String defaultLocation = getDefaultLocation(type);
    String location = GlobalSettings.get("berlioz."+lctype+"bundler.location", defaultLocation);
    File store = new File(root, location);
    if (!store.exists()) {
      store.mkdirs();
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
    // Initialise the bundler
    WebBundleTool bundler = new WebBundleTool(new File(this._root, this._location));
    if (this._type == BundleType.CSS) {
      int threshold = GlobalSettings.get("berlioz.cssbundler.datauris.threshold", 4096);
      bundler.setDataURIThreshold(threshold);
    }
    return bundler;
  }

  /**
   * Create a bundle instance for each bundle definition for that service.
   *
   * @param service the service
   * @return the corresponding list of instances.
   */
  private List<BundleInstance> instantiate(Service service) {
    List<BundleInstance> instances = new ArrayList<>();
    for (BundleDefinition def : this._definitions) {
      BundleInstance instance = BundleInstance.instantiate(this, def, service);
      instances.add(instance);
    }
    return instances;
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
  private static List<BundleDefinition> loadDefinitions(@NonNull String[] names, String prefix, Map<String, BundleDefinition> defaults) {
    List<BundleDefinition> bundles = new ArrayList<>();
    for (String name : names) {
      BundleDefinition bc = defaults.get(name);
      // Same as the name if the 'filename' sub-property isn't defined
      String filename = GlobalSettings.get(prefix + name + ".filename", name);
      // The value of the property if the 'paths' sub-property isn't defined.
      String paths = GlobalSettings.get(prefix + name + ".include", GlobalSettings.get(prefix + name, ""));
      if (paths.length() > 0) {
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
  private static @NonNull String[] getBundleNames(String property) {
    String names = GlobalSettings.get(property);
    return names != null? names.split(",") : DEFAULT_BUNDLE_CONFIG;
  }

  /**
   * @param type JS or CSS.
   * @return the default location for the specified type.
   */
  private static String getDefaultLocation(BundleType type) {
    return BundleType.JS == type? DEFAULT_BUNDLED_SCRIPTS : DEFAULT_BUNDLED_STYLES;
  }

}
