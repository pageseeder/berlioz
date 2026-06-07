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
package org.pageseeder.berlioz.http;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

/**
 * Utility methods for working with HTTP servlet requests.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.13.0
 */
public final class HttpRequests {

  private HttpRequests() {}

  private static final String HTTP = "http";
  private static final String HTTPS = "https";

  /**
   * Returns {@code true} only for URLs that are safe to use as redirect targets.
   *
   * <p>A URL is considered safe when it is one of:
   * <ul>
   *   <li>A relative path with no host authority (e.g. {@code /some/path}, {@code ../other})</li>
   *   <li>An absolute URL whose host matches the effective request host, subject to port rules</li>
   *   <li>An absolute URL whose host is listed in {@code berlioz.redirect.allowed-hosts}</li>
   *   <li>An absolute URL accepted by a registered {@link RedirectPolicy}</li>
   * </ul>
   *
   * <p>Port matching for same-host redirects: ports must match, with one exception —
   * an HTTP-to-HTTPS scheme upgrade is always permitted even when the ports differ
   * (e.g. {@code http://host:8080} redirecting to {@code https://host:8443}).
   *
   * <p>When the application runs behind a reverse proxy, the effective origin is derived
   * from {@code X-Forwarded-Host} and {@code X-Forwarded-Proto} headers when present,
   * matching the behaviour of {@link org.pageseeder.berlioz.servlet.HttpLocation}.
   *
   * <p>Protocol-relative URLs ({@code //host/path}) are treated as absolute.
   *
   * @param url the redirect URL to validate (may be {@code null})
   * @param req the current request, used to determine the expected origin
   * @return {@code true} if the URL is safe to redirect to
   */
  public static boolean isSafeRedirectURL(@Nullable String url, HttpServletRequest req) {
    if (url == null) return false;
    // Reject CRLF to prevent HTTP response splitting
    if (url.indexOf('\r') >= 0 || url.indexOf('\n') >= 0) return false;
    try {
      URI uri = new URI(url);
      // Relative URL with no authority is always safe
      if (!uri.isAbsolute() && uri.getAuthority() == null) return true;
      String targetHost = uri.getHost();
      if (targetHost == null) return false;
      if (targetHost.equalsIgnoreCase(effectiveHost(req))) {
        return isSameHostPermitted(uri, req);
      }
      return isExternalHostPermitted(uri, req);
    } catch (URISyntaxException e) {
      return false;
    }
  }

  /**
   * Returns the effective server name for the request, honouring {@code X-Forwarded-Host}
   * when present (host part only, port stripped).
   */
  static String effectiveHost(HttpServletRequest req) {
    String forwarded = req.getHeader(HttpHeaders.X_FORWARDED_HOST);
    if (forwarded != null && !forwarded.isEmpty()) {
      int colon = forwarded.indexOf(':');
      return (colon > 0 ? forwarded.substring(0, colon) : forwarded).strip();
    }
    return req.getServerName();
  }

  /**
   * Returns the effective scheme for the request, honouring {@code X-Forwarded-Proto}
   * when it is {@code "http"} or {@code "https"}.
   */
  static String effectiveScheme(HttpServletRequest req) {
    String forwarded = req.getHeader(HttpHeaders.X_FORWARDED_PROTO);
    return (HTTP.equals(forwarded) || HTTPS.equals(forwarded)) ? forwarded : req.getScheme();
  }

  /**
   * Returns the effective server port for the request.
   *
   * <p>When {@code X-Forwarded-Proto} is present the port is taken from the
   * {@code host:port} part of {@code X-Forwarded-Host} (if supplied), or {@code -1}
   * to indicate "use the scheme default".
   */
  static int effectivePort(HttpServletRequest req) {
    String forwardedProto = req.getHeader(HttpHeaders.X_FORWARDED_PROTO);
    if (HTTP.equals(forwardedProto) || HTTPS.equals(forwardedProto)) {
      String forwardedHost = req.getHeader(HttpHeaders.X_FORWARDED_HOST);
      if (forwardedHost != null) {
        int colon = forwardedHost.indexOf(':');
        if (colon > 0) {
          int port = parsePort(forwardedHost.substring(colon + 1).strip());
          if (port != -1) return port;
        }
      }
      return -1;
    }
    return req.getServerPort();
  }

  // Private helpers -----------------------------------------------------------------------

  /**
   * Returns {@code true} if a same-host redirect is permitted.
   *
   * <p>Ports must match after normalising 80/443 to {@code -1}, with one exception:
   * an HTTP-to-HTTPS scheme upgrade is always allowed regardless of port.
   */
  private static boolean isSameHostPermitted(URI uri, HttpServletRequest req) {
    int uriPort = uri.getPort();
    if (uriPort == 80 || uriPort == 443) uriPort = -1;
    int reqPort = effectivePort(req);
    if (reqPort == 80 || reqPort == 443) reqPort = -1;
    if (uriPort == -1 || uriPort == reqPort) return true;
    // Allow HTTP→HTTPS upgrade even when the ports differ
    String uriScheme = uri.getScheme();
    return HTTPS.equalsIgnoreCase(uriScheme) && HTTP.equalsIgnoreCase(effectiveScheme(req));
  }

  /**
   * Returns {@code true} if an external-host redirect is permitted by either the
   * configured allowlist or a registered {@link RedirectPolicy}.
   */
  private static boolean isExternalHostPermitted(URI uri, HttpServletRequest req) {
    String host = uri.getHost().toLowerCase(Locale.ROOT);
    String allowedHosts = GlobalSettings.get(BerliozOption.REDIRECT_ALLOWED_HOSTS);
    if (!allowedHosts.isEmpty()) {
      for (String entry : allowedHosts.split(",")) {
        if (host.equals(entry.strip().toLowerCase(Locale.ROOT))) return true;
      }
    }
    for (RedirectPolicy policy : policies()) {
      if (policy.isPermitted(uri, req)) return true;
    }
    return false;
  }

  private static int parsePort(String portStr) {
    try {
      int port = Integer.parseInt(portStr);
      if (port > 0 && port <= 65535) return port;
    } catch (NumberFormatException ignored) {
      // invalid port string
    }
    return -1;
  }

  /** Returns the cached list of {@link RedirectPolicy} instances from {@link ServiceLoader}. */
  private static List<RedirectPolicy> policies() {
    return PoliciesHolder.INSTANCE;
  }

  private static final class PoliciesHolder {
    static final List<RedirectPolicy> INSTANCE;
    static {
      List<RedirectPolicy> loaded = new ArrayList<>();
      ServiceLoader.load(RedirectPolicy.class).forEach(loaded::add);
      INSTANCE = Collections.unmodifiableList(loaded);
    }
  }

}
