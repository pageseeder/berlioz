package org.weborganic.berlioz;

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
   * 
   * @since Berlioz 0.7.0
   */
  HTTP_ENABLE_COMPRESSION("berlioz.http.compression", Boolean.TRUE),

  /**
   * A integer global property to specify the max age of the <code>Cache-Control</code> HTTP header
   * of cacheable content.
   * 
   * <p>The property value is <code>60</code> (seconds) by default.
   * 
   * @since Berlioz 0.7.0
   */
  HTTP_MAX_AGE("berlioz.http.max-age", Integer.valueOf(60)),

  /**
   * A global property to indicate how errors should be handled.
   * 
   * <p>The values currently supported are:
   * <ul>
   *   <li>"berlioz": Use the Berlioz fail safe templates to display the error details</li>
   *   <li>"web-descriptor": invoke the <code>sendError</code> method on the response (the error 
   *   will be caught by the error handling defined in the Web descriptor.</li>
   * </ul>
   * 
   * @since Berlioz 0.8.3
   */
  @Beta
  HTTP_ERROR_HANDLER("berlioz.errors.handler", "berlioz"),

  /**
   * A boolean global property to indicate whether errors thrown by generators should be caught
   * or thrown.
   * 
   * <p>The property value is <code>true</code> by default.
   * 
   * @since Berlioz 0.8.3
   */
  @Beta
  HTTP_ERROR_GENERATOR_CATCH("berlioz.errors.generator-catch", "true"),

  /**
   * A boolean global property to indicate whether to enable caching of XSLT.
   * 
   * <p>The property value is <code>true</code> by default.
   * 
   * @since Berlioz 0.7.0
   */
  XSLT_ENABLE_CACHE("berlioz.cache.xslt", Boolean.TRUE),

  /**
   * A boolean global property to indicate whether to tolerate warnings or throw an error when they
   * are found in Berlioz XML files.
   * 
   * <p>The property value is <code>false</code> by default.
   * 
   * @since Berlioz 0.8.3
   */
  @Beta
  XML_ERROR_PARSE_STRICT("berlioz.xml.strict-parsing", Boolean.FALSE);

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
