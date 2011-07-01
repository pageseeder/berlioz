package org.weborganic.berlioz;

import org.weborganic.berlioz.util.CollectedError.Level;

/**
 * An enumerated list of the Berlioz options globally available.
 * 
 * <p>Use this class to know which global setting can be used with Berlioz.
 * 
 * @author Christophe Lauret
 * @version 1 July 2011
 */
public enum BerliozOption {

  /**
   * A boolean global property to indicate whether Berlioz should use to enable HTTP compression 
   * using the <code>Content-Encoding</code> of compressible content.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  HTTP_ENABLE_COMPRESSION("berlioz.http.compression", Boolean.TRUE),

  /**
   * A integer global property to specify the max age of the <code>Cache-Control</code> HTTP header
   * of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   */
  HTTP_MAX_AGE("berlioz.http.max-age", Integer.valueOf(60)),

  /**
   * A boolean global property to indicate whether to enable caching of XSLT.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  XSLT_ENABLE_CACHE("berlioz.cache.xslt", Boolean.TRUE),

  /**
   * A boolean global property to indicate whether to enable caching of XSLT.
   * 
   * <p>The property value is <code>true</code> by default.
   */
  @Beta XML_ERROR_THRESHOLD("berlioz.xml.error.threshold", Level.ERROR);

  /**
   * The name of the property in the global settings.
   */
  private final String _property;

  /**
   * The default value for the property.
   */
  private final Object _default;

  /**
   * Creates a new berlioz option.
   * 
   * @param property  The name of the property in the global settings.
   * @param defaultTo The default value for this option.
   */
  private BerliozOption(String property, Object defaultTo) {
    this._property = property;
    this._default = defaultTo;
  }

  /**
   * Returns a string representation of this error code.
   * 
   * @return The property in the global settings.
   */
  public final String property() {
    return this._property;
  }

  /**
   * The value this property defaults to.
   * 
   * @return The property in the global settings.
   */
  public final Object defaultTo() {
    return this._default;
  }

  /**
   * Returns the same as the <code>property()</code> method.
   * 
   * {@inheritDoc}
   */
  public final String toString() {
    return this._property;
  }
}
