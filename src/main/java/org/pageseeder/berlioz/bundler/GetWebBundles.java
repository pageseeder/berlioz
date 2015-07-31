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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.servlet.HttpContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * This generator returns the list of timestamped scripts and styles for a given service.
 *
 * <p>It will assemble and minimize the scripts and styles together as much as possible and return the
 * list of bundles. Because the name each bundle includes unique stamp, they can be cached for a long periods.
 * When files included in a bundle are modified, this generator will automatically produce a new bundle
 * with a new stamp so that it results in a different URL.
 *
 * <h4>Bundling</h4>
 * <p>Scripts are simply concatenated. Styles are concatenated and import rules will automatically include
 * the imported styles into the main file.
 *
 * <h4>Minimization</h4>
 * <p>Both styles and scripts can be minimised after the bundling. Minimized bundles will be saved using the
 * following extensions <code>.min.js</code> and <code>.min.css</code>. If files to be bundled already use the
 * <code>*.min.*</code> extension, it will be considered to be already minimised and won't be minimised again.
 *
 * <h4>File naming</h4>
 * <p>Bundled files are automatically named as:</p>
 * <pre>[bundlename]-[date]-[etag].[ext]</pre>
 * <p>The <i>bundle name</i> is specified in the configuration; the <i>date</i> is the creation date of the
 * bundle; the etag is the 4-character alphanumerical stamp; and the extension depends on the MIME type and
 * minimization options.
 *
 *
 * <h3>Configuration</h3>
 * <p>This generator is highly configurable and the configuration properties are specific (but similar)
 * for styles and scripts.
 *
 * <p>Properties pertaining to scripts and styles are prefixed by respectively <code>berlioz.jsbundler</code>
 * and <code>berlioz.cssbundler</code>.
 *
 * <p>The <code>minimize</code> property can be used to control minimization of the code.
 *
 * <p>The <code>location</code> property can be used to define where the bundled files should be stored.
 *
 * <p>A bundle config defines the list of bundles to create. The "default" config is made of three bundles
 * 'global', 'group', and 'service'.
 *
 * <p>Each bundle is specified using the <code>bundles</code> property each bundle name is mapped to the list
 * of files to bundle, the {GROUP} and {SERVICE} values are automatically replaced by the Berlioz
 * service/group name in use.
 *
 * <h4>Default configuration</h4>
 * <p>The default configuration is the equivalent of:</p>
 *
 * <pre>{@code
 * <berlioz>
 *   <cssbundler minimize="true" location="/style/_/">
 *     <configs default="global,group,service"/>
 *     <bundles global="/style/global.css"
 *               group="/style/{GROUP}.css"
 *             service="/style/{GROUP}/{SERVICE}.css"/>
 *     <datauris threshold="4096"/>
 *   </cssbundler>
 *   <jsbundler minimize="true" location="/script/_/">
 *     <configs default="global,group,service"/>
 *     <bundles global="/script/global.js"
 *               group="/script/{GROUP}.js"
 *             service="/script/{GROUP}/{SERVICE}.js"/>
 *   </jsbundler>
 * </berlioz>
 * }</pre>
 *
 *
 * <h3>Parameters</h3>
 * <p>No parameters are required for this generator, but the bundling can be disabled by setting the
 * <code>berlioz-bundle</code> parameter to <code>true</code>
 *
 * <h3>Returned XML</h3>
 * <p>The XML returns the scripts and styles in the order in which they are defined.
 * <pre>{@code
 * <script src="[jslocation]/[bundle].js" bundled="[true|false]" minimized="[true|false]" />
 * ...
 * <style  src="[csslocation]/[bundle].css" bundled="[true|false]" minimized="[true|false]" />
 * ...
 * }</pre>
 *
 * <h4>Error handling</h4>
 *
 * <h3>Usage</h3>
 *
 * <h3>ETag</h3>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class GetWebBundles implements ContentGenerator, Cacheable {

  /**
   * The CSS bundle configuration - static as it is common to all generators.
   */
  private static final Map<String, BundleConfig> CSS_CONFIGS = new HashMap<String, BundleConfig>();

  /**
   * The JS bundle configuration - static as it is common to all generators.
   */
  private static final Map<String, BundleConfig> JS_CONFIGS = new HashMap<String, BundleConfig>();

  /**
   * Indicates whether the bundle can be written..
   */
  private static volatile Boolean isWritable = null;

  @Override
  public String getETag(ContentRequest req) {
    HttpContentRequest hreq = (HttpContentRequest)req;
    Service service = hreq.getService();
    Environment env = req.getEnvironment();
    String config = req.getParameter("config", "default");
    // Get the bundle configurations
    BundleConfig js = getConfig(config, BundleType.JS, env.getPublicFolder());
    BundleConfig css =  getConfig(config, BundleType.CSS, env.getPublicFolder());
    // Ensure that we can use bundles
    if (isWritable == null) {
      isWritable = Boolean.valueOf(js.store().exists() && js.store().canWrite());
    }
    boolean doBundle = canBundle(req);
    if (doBundle) {
      long etagJS = js.getLastModifiedBundle(service);
      long etagCSS = css.getLastModifiedBundle(service);
      return Long.toString(Math.max(etagJS, etagCSS));
    }
    return null;
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    HttpContentRequest hreq = (HttpContentRequest)req;
    Service service = hreq.getService();
    Environment env = req.getEnvironment();

    // Parameters
    boolean doBundle = canBundle(req);
    String config = req.getParameter("config", "default");

    // Scripts
    BundleConfig js = getConfig(config, BundleType.JS, env.getPublicFolder());
    if (doBundle) {
      List<File> bundles = js.getBundles(service);
      String location = js.location();
      for (File bundle :  bundles) {
        xml.openElement("script", false);
        xml.attribute("src", location+bundle.getName());
        xml.attribute("bundled", "true");
        xml.attribute("minimized", Boolean.toString(js.minimize()));
        xml.closeElement();
      }
    } else {
      List<String> paths = js.getPaths(service);
      for (String path : paths) {
        xml.openElement("script", false);
        xml.attribute("src", path);
        xml.attribute("bundled", "false");
        xml.attribute("minimized", "false");
        xml.closeElement();
      }
    }

    // Styles
    BundleConfig css = getConfig(config, BundleType.CSS, env.getPublicFolder());
    if (doBundle) {
      List<File> bundles = css.getBundles(service);
      String location = css.location();
      for (File bundle :  bundles) {
        xml.openElement("style", false);
        xml.attribute("src", location+bundle.getName());
        xml.attribute("bundled", "true");
        xml.attribute("minimized", Boolean.toString(css.minimize()));
        xml.closeElement();
      }
    } else {
      List<String> paths = css.getPaths(service);
      for (String path : paths) {
        xml.openElement("style", false);
        xml.attribute("src", path);
        xml.attribute("bundled", "false");
        xml.attribute("minimized", "false");
        xml.closeElement();
      }
    }
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * @param req content request
   * @return <code>true</code> if bundles folder is writable and "bundle-bundle" parameter is set to "false"
   */
  private static boolean canBundle(ContentRequest req) {
    return isWritable == Boolean.TRUE
        && !"false".equals(req.getParameter("berlioz-bundle", "true"));
  }

  /**
   * @param name The name of the config ("defaut")
   * @param type The type (JS or CSS)
   * @param root The root of the public folder
   *
   * @return the bundle config for the given type creating a new instance if necessary.
   */
  private BundleConfig getConfig(String name, BundleType type, File root) {
    Map<String, BundleConfig> configs = type == BundleType.JS? JS_CONFIGS : CSS_CONFIGS;
    BundleConfig config = configs.get(name);
    if (config == null) {
      config = BundleConfig.newInstance(name, type, root);
      configs.put(name, config);
    }
    return config;
  }

}
