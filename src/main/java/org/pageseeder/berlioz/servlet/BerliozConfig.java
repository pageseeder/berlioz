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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the configuration uses by a a Berlioz Servlet.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.8.1
 */
public final class BerliozConfig {

  /** Logger for this class */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozConfig.class);

  /**
   * Stores all the berlioz config here.
   */
  private static final Map<String, BerliozConfig> CONFIGS = new ConcurrentHashMap<>();

  /**
   * Used to generate ETag Seeds.
   */
  private static final Random RANDOM = new Random();

  /**
   * At what level is the XML transformer allocated.
   */
  private enum TransformAllocation {
    /** No transformation */
    NIL,

    /** One global transformer */
    GLOBAL,

    /** One transformer per group of services */
    GROUP,

    /** One transformer per service */
    SERVICE
  }

  // Class attributes -----------------------------------------------------------------------------

  /**
   * The Servlet configuration.
   */
  private final ServletConfig _servletConfig;

  /**
   * Set the default content type for this Berlioz instance.
   */
  private String _contentType;

  /**
   * Set the default cache control for this Berlioz instance.
   */
  private final String _cacheControl;

  /**
   * Set the Berlioz control key.
   */
  private final String _controlKey;

  /**
   * The relative path to the XSLT stylesheet to use.
   */
  private final String _stylePath;

  /**
   * The fallback stylesheet (points to a URL)
   */
  private final String _fallbackStyleSheet;

  /**
   * Indicates whether the Berlioz instance should use HTTP compression (when possible)
   */
  private final boolean _compression;

  /**
   * The environment.
   */
  private final Environment _env;

  /**
   * How is the XSLT allocated for this configuration.
   */
  private final TransformAllocation _allocation;

  /**
   * The XSLT Transformers to user.
   *
   * <p>The key depends on how the transformers are allocated.
   */
  private final Map<String, XSLTransformer> _transformers;

  /**
   * A seed to use for the calculation of etags (allows them to be reset)
   */
  private volatile long etagSeed = 0L;

  /**
   * Create a new Berlioz configuration.
   * @param servletConfig The servlet configuration.
   */
  private BerliozConfig(ServletConfig servletConfig) {
    this._servletConfig = servletConfig;
    // get the WEB-INF directory
    ServletContext context = servletConfig.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    this._stylePath = this.getInitParameter("stylesheet", "IDENTITY");
    this._allocation = toAllocation(this._stylePath);
    this._fallbackStyleSheet = this.getInitParameter("fallback-stylesheet", "");
    this._transformers = this._allocation != TransformAllocation.NIL? new ConcurrentHashMap<>() : Collections.emptyMap();
    this._contentType = this.getInitParameter("content-type", "text/html;charset=utf-8");
    if ("IDENTITY".equals(this._stylePath) && !this._contentType.contains("xml")) {
      LOGGER.warn("Servlet {} specified content type {} but output is XML", servletConfig.getServletName(), this._contentType);
    }
    String cacheControl = GlobalSettings.get(BerliozOption.HTTP_CACHE_CONTROL);
    if (cacheControl.isEmpty()) {
      cacheControl = "no-cache";
    }
    this._cacheControl = this.getInitParameter("cache-control", cacheControl);
    this._controlKey = this.getInitParameter("berlioz-control", GlobalSettings.get(BerliozOption.XML_CONTROL_KEY));
    this._compression = this.getInitParameter("http-compression", GlobalSettings.has(BerliozOption.HTTP_COMPRESSION));
    this._env = new HttpEnvironment(contextPath, webinfPath, this._cacheControl);
    this.etagSeed = getEtagSeed();
  }

  /**
   * Returns the name of this configuration, usually the servlet name.
   * @return the name of this configuration, usually the servlet name.
   */
  public String getName() {
    return this._servletConfig.getServletName();
  }

  /**
   * Returns the environment.
   * @return the environment.
   */
  public Environment getEnvironment() {
    return this._env;
  }

  /**
   * Return the ETag Seed.
   * @return the ETag Seed.
   */
  public long getETagSeed() {
    return this.etagSeed;
  }

  /**
   * Resets the ETag Seed.
   */
  public void resetETagSeed() {
    this.etagSeed = newEtagSeed();
  }

  /**
   * Expiry date is a year from now.
   * @return One year into the future.
   */
  public long getExpiryDate() {
    Calendar calendar = Calendar.getInstance();
    calendar.roll(Calendar.YEAR, 1);
    return calendar.getTimeInMillis();
  }

  /**
   * Returns the default cache control instruction.
   *
   * @return the cache control.
   */
  public String getCacheControl() {
    return this._cacheControl;
  }

  /**
   * Returns the content type.
   *
   * @return the content type.
   */
  public String getContentType() {
    return this._contentType;
  }

  /**
   * Indicates whether HTTP compression is enabled for the Berlioz configuration.
   *
   * @return <code>true</code> to enable HTTP compression;
   *         <code>false</code> otherwise.
   */
  public boolean enableCompression() {
    return this._compression;
  }

  /**
   * Sets the content type.
   * @param contentType the content type.
   */
  public void setContentType(String contentType) {
    this._contentType = contentType;
  }

  /**
   * Indicates whether this configuration can be controlled by the user.
   *
   * @param req the request including the control key is specified as a request parameter
   * @return <code>true</code> if no key has been configured or the <code>berlioz-control</code> matches
   *         the control key; false otherwise.
   */
  public boolean hasControl(HttpServletRequest req) {
    return hasControl(req, this._controlKey);
  }

  /**
   * Indicates whether this configuration can be controlled by the user.
   *
   * @param req the request including the control key is specified as a request parameter
   * @param controlKey the control key
   *
   * @return <code>true</code> if no key has been configured or the <code>berlioz-control</code> matches
   *         the control key; false otherwise.
   */
  public static boolean hasControl(HttpServletRequest req, String controlKey) {
    if (controlKey == null || "".equals(controlKey)) return true;
    if (controlKey.equals(req.getParameter("berlioz-control"))) return true;
    // TODO Check if this is appropriate!!
    @Nullable Enumeration<String> e = req.getHeaders("Authorization");
    if (e != null) {
      while (e.hasMoreElements()) {
        String auth = e.nextElement();
        if (auth.startsWith("Berlioz ") && auth.endsWith(controlKey))
          return true;
      }
    }
    return false;
  }

  /**
   * Returns the XSLT transformer for the specified service.
   *
   * @param service the service which requires a transformer.
   * @return the corresponding XSLT transformer.
   */
  public @Nullable XSLTransformer getTransformer(Service service) {
    switch (this._allocation) {
      case NIL:     return null;
      case GLOBAL:  return getTransformer(service, "global");
      case GROUP:   return getTransformer(service, service.group());
      case SERVICE: return getTransformer(service, service.id());
      // Should never happen, but...
      default: return null;
    }
  }

  /**
   * Creates a new config for a given Servlet config.
   *
   * @param servletConfig The servlet configuration.
   * @return A new Berlioz config.
   */
  public static synchronized BerliozConfig newConfig(ServletConfig servletConfig) {
    BerliozConfig config = new BerliozConfig(servletConfig);
    String name = servletConfig.getServletName();
    CONFIGS.put(name, config);
    return config;
  }

  /**
   * Creates a new config for a given Servlet config.
   *
   * @param config The Berlioz configuration to unregister.
   * @return <code>true</code> if the config was unregistered;
   *         <code>false</code> otherwise.
   */
  public static synchronized boolean unregister(BerliozConfig config) {
    String name = config._servletConfig.getServletName();
    return CONFIGS.remove(name) != null;
  }

  /**
   * @param listener the listener to set
   */
  @Beta
  public static synchronized void setListener(GeneratorListener listener) {
    XMLResponse.setListener(listener);
  }

  /**
   * @return the listener currently in use.
   */
  @Beta
  public static synchronized @Nullable GeneratorListener getListener() {
    return XMLResponse.getListener();
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Returns the value for the specified init parameter name.
   *
   * <p>If <code>null</code> returns the default value.
   *
   * @param name The name of the init parameter.
   * @param def  The default value if the parameter value is <code>null</code>
   *
   * @return The values for the specified init parameter name.
   */
  private String getInitParameter(String name, String def) {
    String value = this._servletConfig.getInitParameter(name);
    return (value != null)? value : def;
  }

  /**
   * Returns the value for the specified init parameter name.
   *
   * <p>If <code>null</code> returns the default value.
   *
   * @param name The name of the init parameter.
   * @param def  The default value if the parameter value is <code>null</code>
   *
   * @return The values for the specified init parameter name.
   */
  private boolean getInitParameter(String name, boolean def) {
    String value = this._servletConfig.getInitParameter(name);
    return (value != null)? "true".equals(value) : def;
  }

  /**
   * Expiry date is a year from now.
   * @return One year into the future.
   */
  private long getEtagSeed() {
    long seed = 0L;
    File f = this._env.getPrivateFile("berlioz.etag");
    if (f.exists() && f.length() < 100) {
      try (Scanner scanner = new Scanner(f)) {
        String etag = scanner.useDelimiter("\\Z").next();
        etag = etag.replaceAll("[^a-zA-Z0-9-]", "");
        seed = Long.parseLong(etag, 36);
        LOGGER.info("Loading the etag seed {}", etag);
      } catch (IOException ex) {
        LOGGER.warn("Unable to load the etag seed", ex);
      } catch (NumberFormatException ex) {
        LOGGER.warn("Unable to load the etag seed", ex);
      }
    }
    return seed;
  }

  /**
   * Expiry date is a year from now.
   * @return One year into the future.
   */
  private long newEtagSeed() {
    Long seed = RANDOM.nextLong();
    LOGGER.info("Generating new ETag Seed: {}", Long.toString(seed.longValue(), 36));
    File f = this._env.getPrivateFile("berlioz.etag");
    File p = f.getParentFile();
    if (f.exists() && f.canWrite() || p != null && p.canWrite()) {
      // NB. We don't care about encoding
      try (FileOutputStream os = new FileOutputStream(f)) {
        for (char c : Long.toString(seed.longValue(), 36).toCharArray()) {
          os.write(c);
        }
      } catch (IOException ex) {
        LOGGER.warn("Unable to save the etag seed", ex);
      }
    }
    return seed;
  }

  /**
   * Returns the XSLT transformer for the specified service.
   *
   * <p>This method will create and cache the transformer if necessary.
   *
   * @param service the service which requires a transformer.
   * @param key the key to use to store the transformer.
   * @return the corresponding XSLT transformer.
   */
  private @Nullable XSLTransformer getTransformer(Service service, String key) {
    XSLTransformer transformer = this._transformers.get(key);
    if (transformer == null) {
      transformer = newTransformer(service);
      this._transformers.put(key, transformer);
    }
    return transformer;
  }

  /**
   * Returns a new XSLT transformer for the specified service.
   *
   * <p>This method creates a new transform from the style path configuration and replaces the
   * <code>{GROUP}</code> and <code>{SERVICE}</code> tokens by the corresponding service attributes.
   *
   * @param service The service
   * @return a new XSLT transformer from the style path configuration for the service.
   *
   * @throws NullPointerException if the service is <code>null</code>.
   */
  private XSLTransformer newTransformer(Service service) {
    String path = this._stylePath;
    path = path.replaceAll("\\{GROUP}", service.group());
    path = path.replaceAll("\\{SERVICE}", service.id());
    File styleSheet = this._env.getPrivateFile(path);
    return new XSLTransformer(styleSheet, toURL(this._fallbackStyleSheet));
  }

  /**
   * Returns the value for the specified init parameter name.
   *
   * <p>If <code>null</code> returns the default value.
   *
   * @param stylePath The path to the stylesheet to use.
   *
   * @return The values for the specified init parameter name.
   */
  private static TransformAllocation toAllocation(String stylePath) {
    if ("IDENTITY".equals(stylePath) || stylePath == null) return TransformAllocation.NIL;
    else if (stylePath.contains("{SERVICE}")) return TransformAllocation.SERVICE;
    else if (stylePath.contains("{GROUP}")) return TransformAllocation.GROUP;
    else return TransformAllocation.GLOBAL;
  }

  /**
   * Returns the URL instance from the specified path.
   *
   * If the path starts with "resource:", the XSLT will be loaded from a resource
   * using the same class loader as Berlioz.
   *
   * @param path the path to create the URL
   * @return the corresponding URL.
   */
  private @Nullable URL toURL(String path) {
    if (path.length() == 0) return null;
    URL url = null;
    if (path.startsWith("resource:")) {
      ClassLoader loader = BerliozConfig.class.getClassLoader();
      url = loader.getResource(path.substring("resource:".length()));
      if (url == null) {
        LOGGER.warn("Unable to load {} as fallback templates", path);
      }
    } else {
      File file = this._env.getPrivateFile(path);
      try {
        url = file.toURI().toURL();
      } catch (MalformedURLException ex) {
        LOGGER.warn("Unable to load {} as fallback templates", path, ex);
      }
    }
    return url;
  }

}
