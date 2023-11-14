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

/**
 * An enumerated list of the Berlioz options globally available.
 *
 * <p>Use this class to know which global setting can be used with Berlioz.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.3
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
   * <table summary="HTTP compression usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.compression</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   *  </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="HTTP compression recommended value">
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
   * <table summary="HTTP Get-Via-POST usage">
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
   * <table summary="HTTP Get-Via-POST recommended value">
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
   * <table summary="HTTP Cache-Control usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.cache-control</code></td>
   *     <td><code>private, max-age=0, must-revalidate</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="HTTP Cache-Control recommended value">
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
   * A global option to allow server timing information to be to returned using the <code>Server-Timing</code>
   * header.
   *
   * @see <a href="https://www.w3.org/TR/server-timing/">W3: Server Timing</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN: Server Timing</a>
   *
   * @since Berlioz 0.11.5
   */
  HTTP_SERVER_TIMING("berlioz.http.server-timing", Boolean.FALSE),

  /**
   * A global option to show the Berlioz service in the <code>X-Berlioz-Service</code>
   * header.
   *
   * <p>It was enabled by default in previous version, from 0.12.6, it is disabled
   * by default.
   *
   * @since Berlioz 0.12.6
   */
  HTTP_SERVICE_HEADER("berlioz.http.service-header", Boolean.FALSE),

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
   * <table summary="Errors handling usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.handle</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="Errors handling recommended value">
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
   * <table summary="Generator errors usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.generator-catch</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="Generator errors recommended value">
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
   * <table summary="XSLT cache usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xslt.cache</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="XSLT cache recommended value">
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
   * Indicates the version of the XML header format  berlioz should use.
   *
   * <h3>Property</h3>
   * <table summary="XML header version usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.header.version</code></td>
   *     <td><code>"0.9"</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="XML header version recommended value">
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>1.0</code></td><td><code>1.0</code></td></tr></tbody>
   * </table>
   * <p>This option will default to <code>1.0</code> from Berlioz 1.0.</p>
   *
   * @since Berlioz 0.9.26
   */
  @Beta
  XML_HEADER_VERSION("berlioz.xml.header.version", "0.9"),

  /**
   * A boolean global option to indicate whether to tolerate warnings or throw an error when they
   * are found in Berlioz XML files.
   *
   * <h3>Property</h3>
   * <table summary="XML strict parsing usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.parse-strict</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="XML strict parsing recommended value">
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
   * <table summary="Control key usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.control-key</code></td>
   *     <td><code>""</code><i>(Empty string)</i></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table summary="Control key recommended value">
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
  XML_CONTROL_KEY("berlioz.control-key", ""),

  /**
   * A string global option to specify the name of the HTTP request attribute for the nonce.
   *
   * <p>This option has no effect unless <code>berlioz.nonce.enable</code> is also set to
   * true.</p>
   *
   * <p>Berlioz first tries to retrieve the nonce from the attribute. If it does not exist,
   * Berlioz generates a nonce and saves it in the request attribute.</p>
   *
   * <p>If the value is empty, the nonce is not stored in an attribute, it is generated by Berlioz
   * only returned in the response.
   *
   * <h3>Property</h3>
   * <table>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.nonce.attribute</code></td>
   *     <td><code>"berlioz-nonce"</code></td>
   *   </tr>
   * </table>
   *
   * @since Berlioz 0.12.5
   */
  NONCE_ATTRIBUTE("berlioz.nonce.attribute", "berlioz-nonce"),

  /**
   * A boolean global option to specify whether Berlioz should include a nonce in
   * the XML header.
   *
   * <p>If enabled, the nonce is always included in the response.
   *
   * <h3>Property</h3>
   * <table summary="Control key usage">
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.nonce.attribute</code></td>
   *     <td><code>"berlioz-nonce"</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   *
   * @since Berlioz 0.12.5
   */
  NONCE_ENABLE("berlioz.nonce.enable", Boolean.FALSE);

  /**
   * The name of the property in the global settings.
   */
  private final String property;

  /**
   * The default value for the property.
   */
  private final Object defaultValue;

  /**
   * Creates a new berlioz option.
   *
   * @param property  The name of the property in the global settings.
   * @param defaultTo The default value for this option.
   */
  BerliozOption(String property, Object defaultTo) {
    this.property = property;
    this.defaultValue = defaultTo;
  }

  /**
   * Returns a string representation of this error code.
   *
   * @return The property in the global settings.
   */
  public String property() {
    return this.property;
  }

  /**
   * The value this property defaults to.
   *
   * @return The property in the global settings.
   */
  public Object defaultTo() {
    return this.defaultValue;
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
    return this.defaultValue.getClass() == Boolean.class;
  }

  /**
   * Returns the same as the <code>property()</code> method.
   *
   * {@inheritDoc}
   */
  @Override
  public final String toString() {
    return this.property;
  }
}
