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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Environment;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.http.HttpHeaders;
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
   * The fixed request attribute name for the delegated channel of
   * {@link #hasControl(HttpServletRequest)}: an in-process, already-authenticated host
   * application (e.g. an admin UI) sets this attribute to {@link Boolean#TRUE} to authorize
   * Berlioz control parameters (e.g. {@code berlioz-reload}), with no secret ever reaching the
   * client.
   *
   * <p>Always checked — not a {@link BerliozOption}/config setting. Request attributes can only be
   * set by trusted in-process code (filters/servlets running earlier in the chain), never by an
   * HTTP client, so always checking this does not relax the secure-by-default guarantee: no
   * external caller can trigger it. Unlike {@link BerliozOption#NONCE_ATTRIBUTE}, there's no
   * third-party framework that would ever spontaneously set an attribute with this meaning, so
   * there's no interop reason to make the name configurable — whoever wires this always has to
   * write {@code request.setAttribute(CONTROL_AUTHORIZED_ATTRIBUTE, Boolean.TRUE)} in their own
   * auth filter regardless. The name is deliberately fully-qualified so it can't collide with an
   * unrelated attribute an application already sets for a different purpose.
   *
   * <p>Berlioz does not interpret sessions, roles, CSRF tokens, HTTP methods, or filter ordering
   * for this attribute — that is entirely the host application's responsibility.
   *
   * @see #hasControl(HttpServletRequest)
   */
  public static final String CONTROL_AUTHORIZED_ATTRIBUTE = "org.pageseeder.berlioz.control.authorized";

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
    this.transformers = this.allocation != TransformAllocation.NIL? new ConcurrentHashMap<>() : Collections.emptyMap();
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
   * Indicates whether the request is authorized to invoke Berlioz control parameters (e.g.
   * {@code berlioz-reload}, {@code clear-xsl-cache}, {@code reset-etags}, {@code reload-services},
   * {@code berlioz-profile}).
   *
   * <p>Authorization is granted by any one of three independent channels:
   * <ol>
   *   <li>the delegated channel — {@link #CONTROL_AUTHORIZED_ATTRIBUTE}, a fixed request-attribute
   *       handoff from the host application's own auth layer; or</li>
   *   <li>the key channel — {@link BerliozOption#CONTROL_KEY}, a shared secret presented via an
   *       {@code Authorization: Berlioz <key>} header; or</li>
   *   <li>the network channel — {@link BerliozOption#CONTROL_NETWORK}, describing what network
   *       position a direct HTTP caller must originate from (loopback or LAN).</li>
   * </ol>
   * None depends on the others. Re-evaluated independently on every request; there is no session
   * or persisted state.
   *
   * <p>By default (no key configured, network {@code off}, attribute unset), none of the three
   * channels authorizes and this always returns {@code false}.
   *
   * @param req the request to check.
   * @return <code>true</code> if the request is authorized via any channel; <code>false</code> otherwise.
   */
  public static boolean hasControl(HttpServletRequest req) {
    if (Boolean.TRUE.equals(req.getAttribute(CONTROL_AUTHORIZED_ATTRIBUTE))) return true;

    if (matchesAuthorizationHeader(req, GlobalSettings.get(BerliozOption.CONTROL_KEY))) return true;

    ControlNetwork network = ControlNetwork.parse(GlobalSettings.get(BerliozOption.CONTROL_NETWORK));
    switch (network) {
      case LOOPBACK:
      case LAN:      return matchesNetwork(req, network);
      case OFF:
      default:       return false;
    }
  }

  /**
   * @param req     the request to check.
   * @param network either {@link ControlNetwork#LOOPBACK} or {@link ControlNetwork#LAN} — never
   *                {@link ControlNetwork#OFF}.
   * @return <code>true</code> if {@code req.getRemoteAddr()} matches <code>network</code> and,
   *         when an {@code X-Forwarded-For} header is present, every hop it lists also matches;
   *         <code>false</code> otherwise, including when any address cannot be parsed.
   *
   * <p>The {@code X-Forwarded-For} check is a safety net, not a fix for the reverse-proxy caveat
   * documented on {@link BerliozOption#CONTROL_NETWORK}: it only tightens the existing
   * {@code req.getRemoteAddr()} check (it can turn an authorization into a denial, never the
   * reverse), so it cannot grant access the plain address check would not already grant. It catches
   * a {@code loopback}/{@code lan} config mistakenly left on behind a same-host or private reverse
   * proxy that forwards the header — since {@code req.getRemoteAddr()} is then always the proxy's
   * own address regardless of who the real caller is, requiring every forwarded hop to also match
   * closes that specific gap. It does <b>not</b> help when the proxy does not forward
   * {@code X-Forwarded-For} at all (a common default — e.g. a bare {@code proxy_pass} with no
   * explicit header configuration) — that case is indistinguishable from no proxy being present.
   */
  private static boolean matchesNetwork(HttpServletRequest req, ControlNetwork network) {
    InetAddress remote = remoteAddress(req);
    if (remote == null || !isAuthorizedAddress(remote, network)) return false;
    return forwardedForAddresses(req).allMatch(addr -> addr != null && isAuthorizedAddress(addr, network));
  }

  /**
   * @return <code>true</code> if <code>addr</code> matches <code>network</code> — loopback only
   *         for {@link ControlNetwork#LOOPBACK}, loopback or private/site-local for
   *         {@link ControlNetwork#LAN}.
   */
  private static boolean isAuthorizedAddress(InetAddress addr, ControlNetwork network) {
    return network == ControlNetwork.LOOPBACK
        ? addr.isLoopbackAddress()
        : addr.isLoopbackAddress() || addr.isSiteLocalAddress();
  }

  /**
   * A conservative character set for IP literals (IPv4 dotted-quad or IPv6 hex-and-colon), used to
   * reject non-literal input <em>before</em> it reaches {@link InetAddress#getByName(String)}.
   */
  private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F.:]+");

  /**
   * Parses the {@code X-Forwarded-For} header, if any, into one {@link InetAddress} per
   * comma-separated hop.
   *
   * <p>Unlike {@code req.getRemoteAddr()} (guaranteed literal by the servlet container, see
   * {@link #remoteAddress(HttpServletRequest)}), each hop here is attacker-controllable header
   * content. {@link InetAddress#getByName(String)} only skips DNS resolution for literal
   * addresses — a non-literal value (e.g. a hostname) would otherwise trigger a real DNS lookup
   * against attacker-supplied input. Each token is therefore checked against {@link #IP_LITERAL}
   * first; anything that doesn't match, or that fails to parse, resolves to {@code null} rather
   * than being skipped, since a malformed or non-IP hop must fail the authorization check in
   * {@link #matchesNetwork} rather than be silently ignored.
   *
   * @return a stream with one (possibly {@code null}) element per comma-separated hop; empty if
   *         the header is absent or blank.
   */
  private static Stream<@Nullable InetAddress> forwardedForAddresses(HttpServletRequest req) {
    String header = req.getHeader(HttpHeaders.X_FORWARDED_FOR);
    if (header == null || header.isBlank()) return Stream.empty();
    return Arrays.stream(header.split(","))
        .map(String::trim)
        .filter(hop -> !hop.isEmpty())
        .map(BerliozConfig::parseIpLiteral);
  }

  private static @Nullable InetAddress parseIpLiteral(String hop) {
    if (!IP_LITERAL.matcher(hop).matches()) return null;
    try {
      return InetAddress.getByName(hop);
    } catch (UnknownHostException ex) {
      return null;
    }
  }

  /**
   * Resolves {@code req.getRemoteAddr()} as an {@link InetAddress}.
   *
   * <p>The value is a literal IP address supplied by the servlet container, so this never
   * triggers a DNS lookup.
   *
   * @return the parsed address, or <code>null</code> if it cannot be parsed.
   */
  private static @Nullable InetAddress remoteAddress(HttpServletRequest req) {
    try {
      return InetAddress.getByName(req.getRemoteAddr());
    } catch (UnknownHostException ex) {
      return null;
    }
  }

  /**
   * @param req        the request to check.
   * @param controlKey the shared secret configured via {@link BerliozOption#CONTROL_KEY}.
   * @return <code>true</code> if a non-empty <code>controlKey</code> is configured and the request
   *         carries a matching <code>Authorization: Berlioz &lt;key&gt;</code> header;
   *         <code>false</code> otherwise.
   */
  private static boolean matchesAuthorizationHeader(HttpServletRequest req, String controlKey) {
    // An unset key must never match — otherwise an unconfigured key channel would be satisfied
    // by a bare "Authorization: Berlioz " header, reopening the exact bug being fixed.
    if (controlKey.isEmpty()) return false;
    // NB: use equals (not endsWith) to prevent a suffix like "Berlioz xyzSECRET" from matching "SECRET"
    Enumeration<String> headers = req.getHeaders("Authorization");
    while (headers.hasMoreElements()) {
      if (headers.nextElement().equals("Berlioz " + controlKey)) return true;
    }
    return false;
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
