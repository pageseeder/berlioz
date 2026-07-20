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
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the configuration used by a Berlioz servlet.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.8.1
 */
public final class BerliozConfig {

  /** Logger for this class */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozConfig.class);

  private static final String IDENTITY_TRANSFORM = "IDENTITY";

  /**
   * Stores all the berlioz config here.
   */
  private static final Map<String, BerliozConfig> CONFIGS = new ConcurrentHashMap<>();

  /**
   * Used to generate ETag Seeds.
   */
  private static final SecureRandom RANDOM = new SecureRandom();

  /**
   * At what level the XML transformer is allocated.
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
  private final ServletConfig servletConfig;

  /**
   * The media type (without parameters) for responses from this Berlioz instance.
   */
  private String mediaType;

  /**
   * The character set for responses from this Berlioz instance.
   *
   * <p>Of the parameters allowed in a {@code Content-Type} header (RFC 2045), only {@code charset}
   * is relevant for Berlioz responses. {@code boundary} applies to multipart types that Berlioz
   * never produces; all other parameters ({@code format}, {@code type}, etc.) are type-specific
   * and not applicable here.</p>
   */
  private Charset charset;

  /**
   * Set the default cache control for this Berlioz instance.
   */
  private final String cacheControl;

  /**
   * The relative path to the XSLT stylesheet to use.
   */
  private final String stylePath;

  /**
   * The fallback stylesheet (points to a URL)
   */
  private final String fallbackStyleSheet;

  /**
   * Indicates whether the Berlioz instance should use HTTP compression (when possible)
   */
  private final boolean compression;

  /**
   * The environment.
   */
  private final Environment env;

  /**
   * How the XSLT allocated for this is configuration.
   */
  private final TransformAllocation allocation;

  /**
   * The XSLT transformers to use.
   *
   * <p>The key depends on how the transformers are allocated.
   */
  private final Map<String, XsltTransformer> transformers;

  /**
   * A seed to use for the calculation of etags (allows them to be reset)
   */
  private volatile long etagSeed;

  /**
   * Create a new Berlioz configuration.
   * @param servletConfig The servlet configuration.
   */
  private BerliozConfig(ServletConfig servletConfig) {
    this.servletConfig = servletConfig;
    // get the WEB-INF directory
    ServletContext context = servletConfig.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    this.stylePath = this.getInitParameter("stylesheet", IDENTITY_TRANSFORM);
    this.allocation = toAllocation(this.stylePath);
    this.fallbackStyleSheet = this.getInitParameter("fallback-stylesheet", "");
    this.transformers = this.allocation != TransformAllocation.NIL? new ConcurrentHashMap<>() : Map.of();
    String rawContentType = this.getInitParameter("content-type", "text/html;charset=utf-8");
    this.mediaType = parseMediaType(rawContentType);
    this.charset = parseCharset(rawContentType);
    if (IDENTITY_TRANSFORM.equals(this.stylePath) && !this.mediaType.contains("xml")) {
      LOGGER.warn("Servlet {} specified content type {} but output is XML", servletConfig.getServletName(), this.mediaType);
    }
    String defaultCacheControl = GlobalSettings.get(BerliozOption.HTTP_CACHE_CONTROL);
    if (defaultCacheControl.isEmpty()) {
      defaultCacheControl = "no-cache";
    }
    this.cacheControl = this.getInitParameter("cache-control", defaultCacheControl);
    this.compression = this.getInitParameter("http-compression", GlobalSettings.has(BerliozOption.HTTP_COMPRESSION));
    this.env = new HttpEnvironment(contextPath, webinfPath, this.cacheControl);
    this.etagSeed = loadEtagSeed();
  }

  /**
   * Returns the name of this configuration, usually the servlet name.
   * @return the name of this configuration, usually the servlet name.
   */
  public String getName() {
    return this.servletConfig.getServletName();
  }

  /**
   * Returns the environment.
   * @return the environment.
   */
  public Environment getEnvironment() {
    return this.env;
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
   * The expiry date is a year from now.
   * @return One year into the future.
   */
  public long getExpiryDate() {
    return Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli();
  }

  /**
   * Returns the default cache control instruction.
   *
   * @return the cache control.
   */
  public String getCacheControl() {
    return this.cacheControl;
  }

  /**
   * Returns the media type without parameters (e.g. {@code text/html}).
   *
   * @return the media type.
   */
  public String getMediaType() {
    return this.mediaType;
  }

  /**
   * Returns the character set for responses.
   *
   * @return the charset, never {@code null}; defaults to UTF-8.
   */
  public Charset getCharset() {
    return this.charset;
  }

  /**
   * Returns the full content type as a {@code type;charset=NAME} string suitable for use in
   * the {@code Content-Type} HTTP response header.
   *
   * @return the content type.
   */
  public String getContentType() {
    return this.mediaType + ";charset=" + this.charset.name();
  }

  /**
   * Indicates whether HTTP compression is enabled for the Berlioz configuration.
   *
   * @return <code>true</code> to enable HTTP compression;
   *         <code>false</code> otherwise.
   */
  public boolean enableCompression() {
    return this.compression;
  }

  /**
   * Updates the configured content type.
   *
   * <p>Response processing no longer mutates shared servlet configuration when an XSLT result
   * declares a different media type or charset. Use {@link #getMediaType()}, {@link #getCharset()},
   * or the servlet response itself instead of changing this object at runtime.</p>
   *
   * @param contentType the new content type, which may include a {@code charset} parameter.
   *
   * @deprecated Mutating shared Berlioz configuration during request processing causes
   *             cross-request side effects.
   */
  @Deprecated(forRemoval = true, since = "0.13.5")
  public void setContentType(String contentType) {
    this.mediaType = parseMediaType(contentType);
    this.charset = parseCharset(contentType);
  }

  /**
   * Returns the XSLT transformer for the specified service.
   *
   * @param service the service that requires a transformer.
   * @return the corresponding XSLT transformer.
   */
  public @Nullable XsltTransformer getTransformer(Service service) {
    switch (this.allocation) {
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
    String name = config.servletConfig.getServletName();
    return CONFIGS.remove(name) != null;
  }

  /**
   * @param listener the listener to set
   */
  @Beta
  public static synchronized void setListener(GeneratorListener listener) {
    XmlResponse.setListener(listener);
    JsonResponse.setListener(listener);
  }

  /**
   * @return the listener currently in use.
   */
  @Beta
  public static synchronized @Nullable GeneratorListener getListener() {
    return XmlResponse.getListener();
  }

  // private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Extracts the bare media type from a {@code Content-Type} value, discarding parameters.
   *
   * <p>For example, {@code "text/html;charset=utf-8"} returns {@code "text/html"}.</p>
   */
  private static String parseMediaType(String contentType) {
    int semi = contentType.indexOf(';');
    return (semi < 0 ? contentType : contentType.substring(0, semi)).trim();
  }

  /**
   * Extracts the {@code charset} parameter from a {@code Content-Type} value.
   *
   * <p>Returns {@link StandardCharsets#UTF_8} when the parameter is absent or the named charset
   * is not supported. Quoted-string values (RFC 2045 §5.1) are unquoted before lookup.</p>
   */
  private static Charset parseCharset(String contentType) {
    int idx = contentType.toLowerCase(Locale.ROOT).indexOf("charset=");
    if (idx < 0) return StandardCharsets.UTF_8;
    int start = idx + 8;
    int end = contentType.indexOf(';', start);
    String name = (end < 0 ? contentType.substring(start) : contentType.substring(start, end)).trim();
    if (name.startsWith("\"") && name.endsWith("\"") && name.length() > 1) {
      name = name.substring(1, name.length() - 1);
    }
    try {
      return Charset.forName(name);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
      LOGGER.warn("Unknown charset '{}' in content type '{}', defaulting to UTF-8", name, contentType);
      return StandardCharsets.UTF_8;
    }
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
  private String getInitParameter(String name, String def) {
    String value = this.servletConfig.getInitParameter(name);
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
    String value = this.servletConfig.getInitParameter(name);
    return (value != null)? "true".equals(value) : def;
  }

  /**
   * The expiry date is a year from now.
   * @return One year into the future.
   */
  private long loadEtagSeed() {
    long seed = 0L;
    File f = this.env.getPrivateFile("berlioz.etag");
    if (f.exists() && f.length() < 100) {
      try (Scanner scanner = new Scanner(f)) {
        String etag = scanner.useDelimiter("\\Z").next();
        etag = etag.replaceAll("[^a-zA-Z0-9-]", "");
        seed = Long.parseLong(etag, 36);
        LOGGER.info("Loading the etag seed {}", etag);
      } catch (IOException | NumberFormatException ex) {
        LOGGER.warn("Unable to load the etag seed", ex);
      }
    }
    return seed;
  }

  /**
   * The expiry date is a year from now.
   * @return One year into the future.
   */
  private long newEtagSeed() {
    long seed = RANDOM.nextLong();
    String seedAsString = Long.toString(seed, 36);
    LOGGER.info("Generating new Etag Seed: {}", seedAsString);
    File f = this.env.getPrivateFile("berlioz.etag");
    File p = f.getParentFile();
    if (f.exists() && f.canWrite() || p != null && p.canWrite()) {
      // NB. We don't care about encoding
      try (FileOutputStream os = new FileOutputStream(f)) {
        for (char c : seedAsString.toCharArray()) {
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
   * @param service the service that requires a transformer.
   * @param key the key to use to store the transformer.
   * @return the corresponding XSLT transformer.
   */
  private XsltTransformer getTransformer(Service service, String key) {
    return this.transformers.computeIfAbsent(key, k -> newTransformer(service));
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
  private XsltTransformer newTransformer(Service service) {
    String path = this.stylePath
        .replace("{GROUP}", service.group())
        .replace("{SERVICE}", service.id());
    try {
      Path styleSheet = this.env.getPrivateFile(path).toPath();
      return new XsltTransformer(styleSheet, toURL(this.fallbackStyleSheet));
    } catch (IllegalArgumentException ex) {
      LOGGER.warn("Stylesheet '{}' for service '{}' resolves outside private folder — using fallback", path, service.id());
      return new XsltTransformer(this.env.getPrivateFolder().toPath(), toURL(this.fallbackStyleSheet));
    }
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
    if (IDENTITY_TRANSFORM.equals(stylePath)) return TransformAllocation.NIL;
    else if (stylePath.contains("{SERVICE}")) return TransformAllocation.SERVICE;
    else if (stylePath.contains("{GROUP}")) return TransformAllocation.GROUP;
    else return TransformAllocation.GLOBAL;
  }

  /**
   * Returns the URL instance from the specified path.
   *
   * <p>If the path starts with "resource:", the XSLT will be loaded from a resource
   * using the same class loader as Berlioz.
   *
   * @param path the path to create the URL
   * @return the corresponding URL.
   */
  private @Nullable URL toURL(String path) {
    if (path.isEmpty()) return null;
    URL url = null;
    if (path.startsWith("resource:")) {
      ClassLoader loader = BerliozConfig.class.getClassLoader();
      url = loader.getResource(path.substring("resource:".length()));
      if (url == null) {
        LOGGER.warn("Unable to load {} as fallback templates", path);
      }
    } else {
      File file = this.env.getPrivateFile(path);
      try {
        url = file.toURI().toURL();
      } catch (MalformedURLException ex) {
        LOGGER.warn("Unable to load {} as fallback templates", path, ex);
      }
    }
    return url;
  }

}
