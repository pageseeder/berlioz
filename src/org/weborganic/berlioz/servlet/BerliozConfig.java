/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.File;
import java.util.Calendar;
import java.util.Random;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Environment;

/**
 * Defines the configuration use by a a Berlioz Servlet.
 * 
 * @author Christophe Lauret
 * @version 24 June 2011
 */
public final class BerliozConfig {

  /** Logger for this class */
  private static final Logger LOGGER = LoggerFactory.getLogger(BerliozConfig.class);

  /**
   * Name of the global property to use to enable HTTP compression using the 
   * <code>Content-Encoding</code> of compressible content.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  public static final String ENABLE_HTTP_COMPRESSION = "berlioz.http.compression";

  /**
   * Name of the global property to use to specify the max age of the <code>Cache-Control</code>
   * HTTP header of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   */
  public static final String HTTP_MAX_AGE = "berlioz.http.max-age";

  // Class attributes -----------------------------------------------------------------------------

  /**
   * The Servlet configuration.
   */
  private ServletConfig _servletConfig;

  /**
   * Set the default content type for this Berlioz instance.
   */
  private String _contentType;

  /**
   * Set the default cache control for this Berlioz instance.
   */
  private String _cacheControl;

  /**
   * Set the Berlioz control key.
   */
  private String _controlKey;

  /**
   * The relative path to the XSLT stylesheet to use.
   */
  private String _stylePath;

  /**
   * The environment. 
   */
  private final Environment _env;

  /**
   * A seed to use for the calculation of etags (allows them to be reset)
   */
  private volatile long _etagSeed = 0L;

  /**
   * Create a new Berlioz configuration.
   * @param servletConfig The servlet configuration.
   */
  protected BerliozConfig(ServletConfig servletConfig) {
    this._servletConfig = servletConfig;
    // get the WEB-INF directory
    ServletContext context = servletConfig.getServletContext();
    File contextPath = new File(context.getRealPath("/"));
    File webinfPath = new File(contextPath, "WEB-INF");
    this._stylePath = this.getInitParameter("stylesheet", "/xslt/html/global.xsl");
    this._contentType = this.getInitParameter("content-type", "text/html;charset=utf-8");
    if ("IDENTITY".equals(this._stylePath) && !this._contentType.contains("xml")) {
      LOGGER.warn("Servlet {} specified content type {} but output is XML", servletConfig.getServletName(), this._contentType);
    }
    this._cacheControl = this.getInitParameter("cache-control", "max-age="+GlobalSettings.get(HTTP_MAX_AGE, 60)+", must-revalidate");
    this._controlKey  = this.getInitParameter("berlioz-control", null);
    this._env = new HttpEnvironment(contextPath, webinfPath);
    this._etagSeed = newEtagSeed();
  }

  /**
   * Returns the environment.
   * @return the environment.
   */
  public Environment getEnvironment() {
    return this._env;
  }

  /**
   * Return the ETag Seed
   * @return the ETag Seed
   */
  public long getETagSeed() {
    return this._etagSeed;
  }

  /**
   * Return the ETag Seed
   * @return the ETag Seed
   */
  public void resetETagSeed() {
    this._etagSeed = newEtagSeed();
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
   * Returns the content type.
   * 
   * @return the content type.
   */
  public boolean enableCompression() {
    return GlobalSettings.get(ENABLE_HTTP_COMPRESSION, true);
  }

  /**
   * Sets the content type.
   * @return contentType the content type.
   */
  public void setContentType(String contentType) {
    this._contentType = contentType;
  }

  /**
   * Indicates whether this configuration can be controlled by the user.
   * 
   * @param the control key is specified as a request parameter 
   * @return <code>true</code> if no key has been configured or the <code>berlioz-control</code> matches
   *         the control key; false otherwise.
   */
  public boolean hasControl(ServletRequest req) {
    if (this._controlKey == null) return true;
    return this._controlKey.equals(req.getParameter("berlioz-control"));
  }

  /**
   * Returns a new XSLT transformer from the stylepath configuration.
   * 
   * @return a new XSLT transformer from the stylepath configuration.
   */
  protected XSLTransformer newTransformer() {
    if ("IDENTITY".equals(this._stylePath) || this._stylePath == null) return null;
    File styleSheet = this._env.getPrivateFile(this._stylePath);
    return new XSLTransformer(styleSheet);
  }

//private helpers --------------------------------------------------------------------------------

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
   * Expiry date is a year from now.
   * @return One year into the future.
   */
  private static long newEtagSeed() {
    Long seed = new Random().nextLong();
    LOGGER.info("Generating new ETag Seed: {}", seed);
    return seed;
  }
}
