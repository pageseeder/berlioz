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

import org.pageseeder.berlioz.xslt.XsltCacheMode;
import org.pageseeder.berlioz.xslt.XsltErrorSensitivity;

/**
 * An enumerated list of the Berlioz options globally available.
 *
 * <p>Use this class to know which global setting can be used with Berlioz.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.8.4
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
   * the client does not accept responses compressed with GZip.
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
   * is textual.<br>For examples, scripts, CSS stylesheets, XML, and HTML are considered
   * compressible; most images and other media files are not.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>HTTP compression usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.compression</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   *  </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>HTTP compression recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>true</code></td></tr></tbody>
   * </table>
   * <p>HTTP compression is recommended for both development and production.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.11">HTTP/1.1 - 14.11 Content-Encoding</a>
   *
   * @since 0.7.0
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
   *   <caption>HTTP Get-Via-POST usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.get-via-post</code></td>
   *     <td><code>false</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>HTTP Get-Via-POST recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>false</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>Since this option goes against REST principles, it is recommended that it remain
   * {@code false} for most applications. It should not be enabled for a Web API.
   * Applications that relied on the old default of {@code true} must now set
   * {@code berlioz.http.get-via-post=true} explicitly.</p>
   *
   * @since 0.8.3
   * @deprecated Since 0.14.0; this option will be removed in 1.0 — POST requests will never
   *             fall back to GET. Applications must declare explicit POST-mapped services.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  HTTP_GET_VIA_POST("berlioz.http.get-via-post", Boolean.FALSE),

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
   *   <caption>HTTP Cache-Control usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.http.cache-control</code></td>
   *     <td><code>private, max-age=0, must-revalidate</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>HTTP Cache-Control recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>no-cache, no-store</code></td><td><code>N/A*</code></td></tr></tbody>
   * </table>
   * <p>* The value recommended for development or production depends on the nature of the data.
   *
   * <p><b>Use this value in preference to the <code>berlioz.http.max-age</code> option.</b> If
   * specified, this option will automatically override the max age option.
   *
   * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.9">HTTP/1.1 - 14.9 Cache-Control</a>
   *
   * @since 0.9.3
   */
  HTTP_CACHE_CONTROL("berlioz.http.cache-control", ""),

  /**
   * A global option to allow server timing information to be returned using the <code>Server-Timing</code>
   * header.
   *
   * @see <a href="https://www.w3.org/TR/server-timing/">W3: Server Timing</a>
   * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN: Server Timing</a>
   *
   * @since 0.11.5
   */
  HTTP_SERVER_TIMING("berlioz.http.server-timing", Boolean.FALSE),

  /**
   * A global option to show the Berlioz service in the <code>X-Berlioz-Service</code>
   * header.
   *
   * <p>It was enabled by default in the previous version; from 0.12.6, it is disabled
   * by default.
   *
   * @since 0.12.6
   */
  HTTP_SERVICE_HEADER("berlioz.http.service-header", Boolean.FALSE),

  /**
   * A boolean global option to indicate whether Berlioz should use its own error handler when
   * an error occurs.
   *
   * <p>If set to <code>true</code>, Berlioz will use fail-safe templates to display the error
   * details on screen for the user.</p>
   *
   * <p>If set to <code>false</code>, Berlioz invoke the <code>sendError</code> method on the
   * response causing the error to be caught by the error handling defined in the Web descriptor
   * (<code>web.xml</code>).</p>
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Errors handling usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.handle</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Errors handling recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>The default value should be set to <code>true</code> in development so that all error
   * details are returned to the developer. In production, it is preferable to either customize
   * the error handler or use the Web descriptor to redirect users to a more user-friendly page.
   *
   * @since 0.8.3
   */
  @Beta
  ERROR_HANDLER("berlioz.errors.handle", Boolean.TRUE),

  /**
   * A boolean global option to indicate whether errors thrown by generators should be caught
   * or thrown.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Generator errors usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.errors.generator-catch</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Generator errors recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>During the initial stages of development, it is better to let the errors thrown by
   * generators percolate through so that they can be identified and fixed. Later and in production,
   * it is generally preferable to let Berlioz catch the error, format it as XML and use XSLT to
   * produce the response.
   *
   * @since 0.8.3
   */
  @Beta
  ERROR_GENERATOR_CATCH("berlioz.errors.generator-catch", Boolean.TRUE),

  /**
   * A string global option to specify a custom XSLT stylesheet for rendering framework-generated
   * error responses.
   *
   * <p>When set, {@link org.pageseeder.berlioz.servlet.ErrorHandlerServlet} will attempt to load
   * the specified stylesheet from the {@code WEB-INF} directory before falling back to the
   * built-in failsafe template. This allows applications to apply their own branding and layout
   * to error pages while retaining Berlioz's standard error XML or RFC 9457 problem XML as the
   * source document.
   *
   * <p>The value must be a path relative to {@code WEB-INF}. For example, a value of
   * {@code xslt/error.xsl} resolves to {@code WEB-INF/xslt/error.xsl}. If the file does not
   * exist or cannot be read, Berlioz logs a warning and falls back to the built-in failsafe
   * template.
   *
   * <p>The fallback chain is:
   * <ol>
   *   <li>Custom stylesheet ({@code WEB-INF/<value>}) — if configured and the file exists</li>
   *   <li>Built-in failsafe classpath template</li>
   *   <li>Raw XML — if neither template can be loaded</li>
   * </ol>
   *
   * <p>The stylesheet receives the same error XML document that the failsafe template receives.
   * It must produce HTML output. If it does not produce output (e.g. the XSLT processor fails),
   * the raw XML is written instead with an appropriate content type.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Error stylesheet usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td>{@code berlioz.errors.stylesheet}</td>
   *     <td>{@code ""}<i>(empty — use built-in failsafe)</i></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Error stylesheet recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr>
   *     <td>{@code ""}<i>(empty)</i></td>
   *     <td>{@code xslt/error.xsl}<i>(or leave empty)</i></td>
   *   </tr></tbody>
   * </table>
   *
   * @since 0.13.5
   */
  @Beta
  ERROR_STYLESHEET("berlioz.errors.stylesheet", ""),

  /**
   * A boolean global option to opt in to RFC 9457 Problem Details format for framework-generated
   * error responses.
   *
   * <p>When set to {@code true}, {@link org.pageseeder.berlioz.servlet.ErrorHandlerServlet}
   * emits a {@code <problem>} XML document (aligned with {@code application/problem+xml}) instead
   * of the deprecated non-Problem-Details {@code <error http-class="...">} format. The failsafe
   * XSLT template handles both formats, so existing error rendering continues to work.</p>
   *
   * <p>The non-Problem-Details format is deprecated since 0.14.0. Applications whose XSLT
   * templates consume it can opt back in with {@code berlioz.errors.problem=false} while
   * migrating to the {@code <problem>} format. The older {@code <server-error>} and
   * {@code <client-error>} element names are not restored; both use the unified {@code <error>}
   * element in 0.14.0.</p>
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Problem format usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td>{@code berlioz.errors.problem}</td>
   *     <td>{@code true}</td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Problem format recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td>{@code true}</td><td>{@code true}</td></tr></tbody>
   * </table>
   *
   * @since 0.13.5
   * @deprecated Since 0.14.0; this option will be removed in 1.0 — Problem Details will always
   *             be used. The {@code berlioz.errors.problem=false} escape hatch is only supported
   *             during the 0.14.x migration window.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  ERROR_PROBLEM_FORMAT("berlioz.errors.problem", Boolean.TRUE),

  /**
   * A string global option to control how much diagnostic detail is included in legacy
   * framework-generated error responses (the non-Problem-Details {@code <error>} XML).
   *
   * <p>Three levels are available:
   * <ul>
   *   <li>{@code full} — full stack trace, HTTP request headers, and HTTP parameters are included.
   *       Recommended for development.</li>
   *   <li>{@code standard} — the exception class and message are included, but the stack trace,
   *       HTTP headers, and HTTP parameters are omitted.</li>
   *   <li>{@code minimal} — only the HTTP status, title, and error message are included; no
   *       exception information, headers, or parameters.</li>
   * </ul>
   *
   * <p>When {@link #ERROR_PROBLEM_FORMAT} is {@code true}, this option still applies: at
   * {@code standard} or {@code full} level, exception details are added to the RFC 9457
   * {@code <problem>} response as an {@code exception} extension member.</p>
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Error detail usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td>{@code berlioz.errors.detail}</td>
   *     <td>{@code minimal}</td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Error detail recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td>{@code full}</td><td>{@code minimal}</td></tr></tbody>
   * </table>
   * <p>The previous default of {@code full} exposed exception metadata and is not safe for
   * production. Set {@code berlioz.errors.detail=full} in development configurations.</p>
   *
   * @since 0.13.5
   */
  ERROR_DETAIL("berlioz.errors.detail", "minimal"),

  /**
   * A boolean global property to indicate whether Berlioz should record the time taken by each content generator
   * and by the transformer.
   *
   * @since 0.9.14
   */
  PROFILE("berlioz.profile", Boolean.FALSE),

  /**
   * A string global option to control the caching behavior of XSLT templates.
   *
   * <p>Three modes are available:
   * <ul>
   *   <li><code>no</code> — disable XSLT caching; templates are always recompiled from source.</li>
   *   <li><code>auto</code> — monitor XSLT source files for changes and automatically invalidate
   *       the cache when a change is detected. Recommended for development.</li>
   *   <li><code>manual</code> — only update the cache when explicitly cleared via the
   *       {@code clear-xsl-cache} control parameter. Recommended for production.</li>
   * </ul>
   *
   * <p>For backward compatibility, {@code true} is treated as {@code manual} and
   * {@code false} as {@code no}.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>XSLT cache usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xslt.cache</code></td>
   *     <td><code>manual</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>XSLT cache recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>auto</code></td><td><code>manual</code></td></tr></tbody>
   * </table>
   *
   * @see XsltCacheMode
   * @since 0.8.3
   */
  XSLT_CACHE("berlioz.xslt.cache", "manual"),

  /**
   * A string global option controlling which diagnostics reported by the JAXP XSLT
   * {@link javax.xml.transform.ErrorListener} make stylesheet compilation or execution fail.
   *
   * <p>Three sensitivity levels are available:
   * <ul>
   *   <li><code>fatal</code> — only fatal diagnostics fail the operation;</li>
   *   <li><code>error</code> — error and fatal diagnostics fail the operation;</li>
   *   <li><code>warning</code> — every warning, error, or fatal diagnostic fails the operation.</li>
   * </ul>
   *
   * <p>The terminal built-in fail-safe renderer does not use this setting, so it remains available
   * even when warning-level sensitivity is selected.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>XSLT error sensitivity</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xslt.sensitivity</code></td>
   *     <td><code>error</code></td>
   *   </tr>
   * </table>
   *
   * @see XsltErrorSensitivity
   * @since 0.14.0
   */
  XSLT_SENSITIVITY("berlioz.xslt.sensitivity", "error"),

  /**
   * Indicates the version of the XML header format berlioz should use.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>XML header version usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.header.version</code></td>
   *     <td><code>"1.0"</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>XML header version recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>1.0</code></td><td><code>1.0</code></td></tr></tbody>
   * </table>
   * <p>The legacy {@code "0.9"} header emits compatibility elements ({@code <group>},
   * {@code <service>}, {@code <path-info>}, {@code <host>}, {@code <port>}, {@code <url>},
   * {@code <query-string>}) that are absent in {@code "1.0"}. Applications whose XSLT templates
   * depend on those elements must migrate or set {@code berlioz.xml.header.version=0.9}
   * temporarily.</p>
   *
   * @since 0.9.26
   * @deprecated Since 0.14.0; this option will be removed in 1.0 — the 1.0 header format will
   *             always be used. The {@code berlioz.xml.header.version=0.9} escape hatch is only
   *             supported during the 0.14.x migration window.
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  XML_HEADER_VERSION("berlioz.xml.header.version", "1.0"),

  /**
   * A boolean global option to indicate whether to tolerate warnings or throw an error when they
   * are found in Berlioz XML files.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>XML strict parsing usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.xml.parse-strict</code></td>
   *     <td><code>true</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>XML strict parsing recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>true</code></td><td><code>false</code></td></tr></tbody>
   * </table>
   * <p>It is generally preferable to use the strict mode during development so that all potential
   * configuration issues are resolved early; it is generally not necessary to enable this option
   * in production.</p>
   *
   * @since 0.8.3
   */
  @Beta
  XML_PARSE_STRICT("berlioz.xml.parse-strict", Boolean.FALSE),

  /**
   * A string global option specifying the shared secret used by the key channel to authorize
   * Berlioz control parameters (e.g. {@code berlioz-reload}).
   *
   * <p>Independent of {@link #CONTROL_NETWORK}: whenever this is non-empty, a request carrying a
   * matching {@code Authorization: Berlioz <key>} request header is authorized, regardless of
   * {@code berlioz.control.network}. Empty (the default) means this channel is inert — it never
   * authorizes, not even a request whose header happens to be empty or absent. The
   * {@code berlioz-control} query parameter is not read for this purpose (query parameters and
   * URLs are more likely to be logged by servers and proxies along the way).
   *
   * <p>Typical usage: this is the channel expected for programmatic access against real,
   * non-dev deployments, as opposed to {@link #CONTROL_NETWORK}'s dev-convenience role.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Control key property</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.control.key</code></td>
   *     <td><code>""</code><i>(Empty string)</i></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Control key recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>dev</code><i>(or any simple key)</i></td><td><code>[a strong secret string]</code></td></tr></tbody>
   * </table>
   * <p>For development, set a simple key such as {@code dev}. In production, use a strong secret
   * string (for example, {@code 'd131dd02c5e6eec4693d96dacd436c91'}).</p>
   *
   * @see #CONTROL_NETWORK
   * @since 0.8.3
   */
  @Beta
  CONTROL_KEY("berlioz.control.key", ""),

  /**
   * A string global option describing what network position a direct HTTP caller must originate
   * from to be allowed to invoke Berlioz control parameters (e.g. {@code berlioz-reload},
   * {@code clear-xsl-cache}, {@code reset-etags}, {@code reload-services}, {@code berlioz-profile}).
   *
   * <p>This is re-evaluated independently on every request — there is no session or persisted
   * state, so being granted access on one request confers no standing for the next.
   *
   * <p>Independent of {@link #CONTROL_KEY} (the key channel) and the fixed, always-checked
   * delegated request-attribute channel documented on
   * {@link org.pageseeder.berlioz.security.ControlAuthorization#CONTROL_AUTHORIZED_ATTRIBUTE}. Any one of
   * the three being satisfied authorizes the request.
   *
   * <p>Three values are available:
   * <ul>
   *   <li>{@code off} (default) — the network channel never grants access; control parameters can
   *       never be authorized this way. Only the key or delegated channels can authorize them.
   *       This is the safe default and requires zero configuration.</li>
   *   <li>{@code loopback} — grants access when {@code req.getRemoteAddr()} is a loopback address
   *       ({@code 127.0.0.1} or {@code ::1}).</li>
   *   <li>{@code lan} — grants access when {@code req.getRemoteAddr()} is loopback or a
   *       private/site-local address, using {@link java.net.InetAddress#isSiteLocalAddress()}
   *       (RFC 1918 ranges for IPv4). <b>Known limitation:</b> this does not recognize IPv6 ULA
   *       ({@code fc00::/7}) — a JDK quirk, since that method predates ULA and targets the
   *       deprecated IPv6 site-local concept instead.</li>
   * </ul>
   *
   * <p>Unrecognized values are treated as {@code off} (fail closed).
   *
   * <p>Typical usage: this is a dev/local convenience (e.g. {@code loopback} while developing
   * against a local server), not intended for production use — see the reverse-proxy caveat below.
   *
   * <p><b>Reverse-proxy caveat:</b> {@code req.getRemoteAddr()} cannot be forged by a remote
   * attacker for a normal HTTP request — it reflects the actual TCP peer. But when a reverse
   * proxy fronts the app — especially one running on the same host as the servlet container, a
   * common production shape — {@code getRemoteAddr()} is the <i>proxy's</i> address for every
   * request, including external attackers' requests. {@code loopback} and {@code lan} are
   * dev-convenience settings, not production security settings, and must never be enabled on a
   * deployment where a same-host or otherwise untrusted-boundary reverse proxy is in front.
   * Selecting {@code loopback} or {@code lan} logs a warning once at configuration load time.
   *
   * <p><b>{@code X-Forwarded-For} safety net:</b> as a mistake-catcher for exactly the case above
   * (a {@code loopback}/{@code lan} config left on by accident in staging/production), {@code
   * req.getRemoteAddr()} alone is not enough: when an {@code X-Forwarded-For} header is present,
   * every hop it lists must <i>also</i> match {@code network} for the request to be authorized —
   * this only ever tightens the check (it can turn an authorization into a denial, never the
   * reverse), so it is safe to leave on unconditionally. It catches proxies that forward the
   * header, which many managed load balancers (e.g. cloud ALBs/CDNs) do unconditionally. It does
   * <b>not</b> catch a proxy that does not forward {@code X-Forwarded-For} at all — a common
   * default (e.g. a bare {@code proxy_pass} with no explicit header configuration) that is
   * indistinguishable from no proxy being present. The reverse-proxy caveat above still stands
   * regardless of this safety net.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Control network property</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.control.network</code></td>
   *     <td><code>"off"</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   * <table>
   *   <caption>Control network recommended value</caption>
   *   <tr><th>Development</th><th>Production</th></tr>
   *   <tbody><tr><td><code>loopback</code><i>(or rely on {@link #CONTROL_KEY} instead)</i></td><td><code>off</code><i>(rely on {@link #CONTROL_KEY} instead)</i></td></tr></tbody>
   * </table>
   *
   * @see #CONTROL_KEY
   * @see org.pageseeder.berlioz.security.ControlNetwork
   * @since 0.14.0
   */
  @Beta
  CONTROL_NETWORK("berlioz.control.network", "off"),

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
   *   <caption>Nonce attribute property</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.nonce.attribute</code></td>
   *     <td><code>"berlioz-nonce"</code></td>
   *   </tr>
   * </table>
   *
   * @since 0.12.5
   */
  NONCE_ATTRIBUTE("berlioz.nonce.attribute", "berlioz-nonce"),

  /**
   * A boolean global option to specify whether Berlioz should include a nonce in
   * the XML header.
   *
   * <p>If enabled, the nonce is always included in the response.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Control key usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.nonce.attribute</code></td>
   *     <td><code>"berlioz-nonce"</code></td>
   *   </tr>
   * </table>
   *
   * <h3>Recommended values</h3>
   *
   * @since 0.12.5
   */
  NONCE_ENABLE("berlioz.nonce.enable", Boolean.FALSE),

  /**
   * A string global option specifying a comma-separated list of external host names that are
   * permitted as redirect targets, in addition to the application's own host.
   *
   * <p>By default no external hosts are allowed. Add entries here for domains the application
   * may legitimately redirect to, such as an external identity provider or a partner site.
   *
   * <h3>Property</h3>
   * <table>
   *   <caption>Redirect allowed hosts usage</caption>
   *   <tr><th>Name</th><th>Value</th></tr>
   *   <tr>
   *     <td><code>berlioz.redirect.allowed-hosts</code></td>
   *     <td><code>""</code><i>(Empty — no external hosts allowed)</i></td>
   *   </tr>
   * </table>
   *
   * <p>Example: {@code berlioz.redirect.allowed-hosts=auth.example.com,partner.example.org}
   *
   * <p>For dynamic allowlists (e.g. OAuth clients loaded at runtime) implement
   * {@link org.pageseeder.berlioz.http.RedirectPolicy} and register it via
   * {@link java.util.ServiceLoader}.
   *
   * @since 0.13.2
   */
  REDIRECT_ALLOWED_HOSTS("berlioz.redirect.allowed-hosts", "");

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
   * {@inheritDoc}
   */
  @Override
  public final String toString() {
    return this.property;
  }
}
