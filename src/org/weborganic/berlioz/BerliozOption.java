/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

/**
 * An enumerated list of the Berlioz options globally available.
 *
 * <p>Use this class to know which global setting can be used with Berlioz.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.14 - 22 January 2013
 * @since Berlioz 0.8.4
 */
public enum BerliozOption {

  /**
   * A boolean global property to indicate whether Berlioz should enable HTTP compression.
   *
   * <p>If set to <code>true</code>, Berlioz will compress the content of the response using Gzip
   * and set the HTTP headers <code>Content-Encoding</code>, <code>Vary</code> and possibly
   * <code>Etag</code> appropriately.
   *
   * <p>Berlioz will not compress the response content if it is not considered compressible or if
   * the client does not accept response compressed with GZip.
   *
   * <p>When HTTP compression is enabled and possible, the headers are modified as:
   * <pre>
   *   Vary: Accept-Encoding
   *   Content-Length: <i>[Length of compressed content]</i>
   *   Content-Encoding: gzip
   *   Etag: "<i>[Uncompressed etag]</i>-gzip"
   * </pre>
   *
   * <p>Berlioz considers that the content is compressible if its content type indicates that it
   * is textual.<br>For examples, scripts, CSS stylesheets, XML and HTML are considered
   * compressible; most images and other media files are not.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.compression</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   *  </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>true</code></td></tr></tbody>
   * </table>
   * <p>HTTP compression is recommended for both development and production.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.11">HTTP/1.1 - 14.11 Content-Encoding</a>
   *
   * @since Berlioz 0.7.0
   */
  HTTP_COMPRESSION("berlioz.http.compression", Boolean.TRUE),

  /**
   * A boolean global option to indicate whether HTTP POST requests for which there is no specific
   * service should be processed as a GET request.
   *
   * <p>When this option is set to <code>true</code>, Berlioz will try to match a service using
   * GET if there is no matching service using POST.<br>This option is useful when a service needs
   * to accept both GET and POST requests such as searches.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.get-via-post</code></td>
   *     <td><code>true</code><super>*</super></td>
   *   </tr>
   * </table>
   * <p><super>*</super>The default value is set to <code>true</code> for legacy applications,
   * this may change in subsequent versions of Berlioz.</p>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>false</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>Since this option goes against REST principles, it is recommended that it is set to
   * <code>false</code> for most applications. It should not be enabled for a Web API.
   *
   * @since Berlioz 0.8.3
   */
  HTTP_GET_VIA_POST("berlioz.http.get-via-post", Boolean.TRUE),

  /**
   * A global option to specify the default cache control to use for cacheable content.
   *
   * <p>This option is used to define a default value the <code>Cache-Control</code> HTTP header
   * of cacheable responses when it has not been defined for a service.
   *
   * <p>For cacheable responses, Berlioz will return the following Headers:
   * <pre>
   *   Expires: <i>[Expiry date 1 year from now]</i>
   *   Cache-Control: [Cache control]
   *   Etag: <i>[Etag for generator]</i>
   * </pre>
   *
   * <p>Note: this option has no effect when the response is not cacheable or when a
   * <code>Cache-Control</code> HTTP header has been defined for the service.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.cache-control</code></td>
   *     <td><code>private, max-age=0, must-revalidate</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>no-cache, no-store</code></td><td><code>N/A*</code></td></tr></tbody>
   * </table>
   * <p>* The value recommended for development or production depends on the nature of the data.
   *
   * <p><b>Use this value in preference to the <code>berlioz.http.max-age</code> option. If
   * specified, this option will automatically override the max age option.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.9">HTTP/1.1 - 14.9 Cache-Control</a>
   *
   * @since Berlioz 0.9.3
   */
  HTTP_CACHE_CONTROL("berlioz.http.cache-control", ""),

  /**
   * An integer global option to specify the default maximum age in seconds of cacheable content.
   *
   * <p>This option is used to define a default value for the <code>max-age</code> directive of
   * the <code>Cache-Control</code> HTTP header of cacheable responses when it has not been defined
   * for a service.
   *
   * <p>For cacheable responses, Berlioz will return the following Headers:
   * <pre>
   *   Expires: <i>[Expiry date 1 year from now]</i>
   *   Cache-Control: max-age=<i>[max age in seconds]</i>, must-revalidate
   *   Etag: <i>[Etag for generator]</i>
   * </pre>
   *
   * <p>Note: this option has no effect when the response is not cacheable or when a
   * <code>Cache-Control</code> HTTP header has been defined for the service.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.max-age</code></td>
   *     <td><code>60</code> <i>(Time in seconds)</i></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>60</code></td><td><code>60</code></td></tr></tbody>
   * </table>
   * <p>The value recommended for development is a small as possible but not too small to allow
   * testing; in production mode, it should be as large as possible but not too large to allow
   * proper refreshing.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.9">HTTP/1.1 - 14.9 Cache-Control</a>
   *
   * @since Berlioz 0.7.0
   */
  HTTP_MAX_AGE("berlioz.http.max-age", Integer.valueOf(60)),

  /**
   * A boolean global option to indicate whether Berlioz should use its own error handler when
   * an error occurs.
   *
   * <p>If set to <code>true</code>, Berlioz will use fail safe templates to display the error
   * details on screen for the user.</p>
   *
   * <p>If set to <code>false</code>, Berlioz invoke the <code>sendError</code> method on the
   * response causing the error to be caught by the error handling defined in the Web descriptor
   * (<code>web.xml</code>).</p>
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.handle</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>The default value should be set to <code>true</code> in development so that all error
   * details are returned to the developer. In production, it is preferable to either customise
   * the error handler or use the Web descriptor to redirect users to a more user friendly page.
   *
   * @since Berlioz 0.8.3
   */
  @Beta
  ERROR_HANDLER("berlioz.errors.handle", Boolean.TRUE),

  /**
   * A boolean global option to indicate whether errors thrown by generators should be caught
   * or thrown.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.generator-catch</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>During the initial stages of development, it is better to let the errors thrown by
   * generators percolate through so that they can be identified and fixed. Later and in production,
   * it is generally preferable to let Berlioz catch the error, format it as XML and use XSLT to
   * produce the response.
   *
   * @since Berlioz 0.8.3
   */
  @Beta
  ERROR_GENERATOR_CATCH("berlioz.errors.generator-catch", Boolean.TRUE),

  /**
   * A boolean global property to indicate whether Berlioz should record the time taken by each content generator
   * and by the transformer.
   *
   * @since Berlioz 0.9.14
   */
  PROFILE("berlioz.profile", Boolean.FALSE),

  /**
   * A boolean global option to indicate whether to enable the caching of XSLT templates.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xslt.cache</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>false</code></td><td><code>true</code></td></tr></tbody>
   * </table>
   * <p>It is easier to test the XSLT files during development when caching is disabled; caching
   * should be enabled in production mode.</p>
   *
   * @since Berlioz 0.8.3
   */
  XSLT_CACHE("berlioz.xslt.cache", Boolean.TRUE),

  /**
   * A boolean global option to indicate whether the header should use the header format used prior
   * to version 1.0.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.header.compatibility</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>false</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>This option will be default to <code>false</code> for Berlioz 1.0</p>
   *
   * @since Berlioz 0.9.2
   */
  @Beta
  XML_HEADER_COMPATIBILITY("berlioz.xml.header.compatibility", Boolean.TRUE),

  /**
   * A boolean global option to indicate whether to tolerate warnings or throw an error when they
   * are found in Berlioz XML files.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.parse-strict</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>It is generally preferable to use the strict mode during development so that all potential
   * configuration issues are resolved early; it is generally not necessary to enable this option
   * in production.</p>
   *
   * @since Berlioz 0.8.3
   */
  @Beta
  XML_PARSE_STRICT("berlioz.xml.parse-strict", Boolean.FALSE),

  /**
   * A string global option to specify a key to use enable the control parameters to reload the
   * configuration and XSLT or reset the Etag seed.
   *
   * <p>If the control key is empty, then the control parameters can be used directly.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.control-key</code></td>
   *     <td><code>""</code><i>(Empty string)</i></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>""</code><i>(Empty string)</i></td><td><code>[a complex string]</code></td></tr></tbody>
   * </table>
   * <p>No control key is required for development, however in production the a string such as
   * an MD5 hash value should be specified to secure the application
   * (for example: 'd131dd02c5e6eec4693d96dacd436c91').</p>
   *
   * @since Berlioz 0.8.3
   */
  @Beta
  XML_CONTROL_KEY("berlioz.control-key", "");

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
  public String property() {
    return this._property;
  }

  /**
   * The value this property defaults to.
   *
   * @return The property in the global settings.
   */
  public Object defaultTo() {
    return this._default;
  }

  /**
   * Indicates whether the type of this property is boolean.
   *
   * <p>Implementation note: this is based on the class of the default value.
   *
   * @return <code>true</code> if this property is of type boolean;
   *         <code>false</code> otherwise.
   */
  public boolean isBoolean() {
    return this._default.getClass() == Boolean.class;
  }

  /**
   * Returns the same as the <code>property()</code> method.
   *
   * {@inheritDoc}
   */
  @Override
  public final String toString() {
    return this._property;
  }
}
