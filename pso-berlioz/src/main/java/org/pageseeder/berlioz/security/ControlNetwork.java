/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.http.HttpHeaders;

/**
 * Describes what network position a direct HTTP caller must originate from to be allowed to
 * invoke Berlioz control parameters (e.g. {@code berlioz-reload}).
 *
 * <p>This is re-evaluated independently on every request — there is no session or persisted
 * state, so being granted access on one request confers no standing for the next.
 *
 * <p>This is the "network channel" of {@link ControlAuthorization#hasControl(javax.servlet.http.HttpServletRequest)};
 * see that method for how it combines with the independent key channel and the fixed delegated
 * (request-attribute) channel.
 *
 * <p>The string representation (lower-case enum name) is the value used in the
 * {@code berlioz.control.network} configuration property.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.0
 */
public enum ControlNetwork {

  /** The network channel never grants access. Safe default. */
  OFF,

  /**
   * Grants access when the caller's remote address is a loopback address, and every address
   * listed in the forwarding headers (see {@link #matches(HttpServletRequest)}) is also loopback.
   * Dev-only; see caveat below.
   */
  LOOPBACK,

  /**
   * Grants access when the caller's remote address is loopback or a private/site-local address,
   * and every address listed in the forwarding headers (see {@link #matches(HttpServletRequest)})
   * also is. Dev-only; see caveat below.
   */
  LAN;

  /**
   * A conservative character set for IP literals (IPv4 dotted-quad or IPv6 hex-and-colon), used to
   * reject non-literal input <em>before</em> it reaches {@link InetAddress#getByName(String)}.
   */
  private static final Pattern IP_LITERAL = Pattern.compile("[0-9a-fA-F.:]+");

  /**
   * Parses the config string value, returning {@link #OFF} for any unrecognised value so that
   * misconfiguration fails closed rather than open.
   *
   * @param value the raw config string (e.g. {@code "off"}, {@code "loopback"}, {@code "lan"})
   * @return the matching mode, or {@link #OFF} if the value is not recognised
   */
  public static ControlNetwork parse(String value) {
    switch (value.toLowerCase()) {
      case "loopback": return LOOPBACK;
      case "lan":      return LAN;
      default:         return OFF;
    }
  }

  /**
   * Indicates whether the request originates from this network position.
   *
   * <p>{@code req.getRemoteAddr()} must match this network and, when present, so must every
   * address listed in:
   * <ul>
   *   <li>{@code X-Forwarded-For} — every comma-separated hop;</li>
   *   <li>{@code Forwarded} (RFC 7239) — every {@code for=} parameter, with or without a port;</li>
   *   <li>{@code X-Real-IP}.</li>
   * </ul>
   * A value that is not an IP literal (e.g. a hostname, {@code unknown} or an obfuscated
   * {@code _hidden} identifier) fails the check. Literals are parsed, never resolved, so no DNS
   * lookup is ever made on header content.
   *
   * <p>The forwarding headers are a safety net, not a fix for the reverse-proxy caveat documented
   * on {@link org.pageseeder.berlioz.BerliozOption#CONTROL_NETWORK}: they can only turn an
   * authorization into a denial, never the reverse. They catch a {@code loopback}/{@code lan}
   * config mistakenly left on behind a same-host or private reverse proxy that forwards the
   * caller's address — since {@code req.getRemoteAddr()} is then the proxy's own address
   * regardless of who the real caller is. They do <b>not</b> help when the proxy forwards none of
   * these headers (e.g. a bare {@code proxy_pass}) — that case is indistinguishable from no proxy
   * being present. A local development proxy is unaffected, since loopback hops still match.
   *
   * @param req the request to check.
   * @return <code>true</code> if the request matches this network; always <code>false</code> for
   *         {@link #OFF} or when any address cannot be parsed.
   */
  public boolean matches(HttpServletRequest req) {
    if (this == OFF) return false;
    InetAddress remote = remoteAddress(req);
    if (remote == null || !matches(remote)) return false;
    return forwardedAddresses(req).allMatch(addr -> addr != null && matches(addr));
  }

  /**
   * @return <code>true</code> if <code>addr</code> matches this network — loopback only for
   *         {@link #LOOPBACK}, loopback or private/site-local for {@link #LAN}.
   */
  private boolean matches(InetAddress addr) {
    switch (this) {
      case LOOPBACK: return addr.isLoopbackAddress();
      case LAN:      return addr.isLoopbackAddress() || addr.isSiteLocalAddress();
      case OFF:
      default:       return false;
    }
  }

  /**
   * Parses the forwarding headers into one {@link InetAddress} per listed address.
   *
   * <p>Unlike {@code req.getRemoteAddr()} (guaranteed literal by the servlet container, see
   * {@link #remoteAddress(HttpServletRequest)}), these values are attacker-controllable header
   * content. Anything that is not an IP literal resolves to {@code null} rather than being
   * skipped, since a malformed or non-IP value must fail the check rather than be silently ignored.
   *
   * @return a stream with one (possibly {@code null}) element per listed address; empty if none of
   *         the headers is present or they are all blank.
   */
  private static Stream<@Nullable InetAddress> forwardedAddresses(HttpServletRequest req) {
    Stream<@Nullable InetAddress> forwardedFor = headerValues(req, HttpHeaders.X_FORWARDED_FOR)
        .flatMap(header -> Arrays.stream(header.split(",")))
        .map(String::trim)
        .filter(hop -> !hop.isEmpty())
        .map(ControlNetwork::parseIpLiteral);
    Stream<@Nullable InetAddress> forwarded = headerValues(req, HttpHeaders.FORWARDED)
        .flatMap(header -> Arrays.stream(header.split("[,;]")))
        .map(String::trim)
        .filter(pair -> pair.regionMatches(true, 0, "for=", 0, 4))
        .map(pair -> parseForwardedNode(pair.substring(4).trim()));
    Stream<@Nullable InetAddress> realIp = headerValues(req, HttpHeaders.X_REAL_IP)
        .map(String::trim)
        .filter(ip -> !ip.isEmpty())
        .map(ControlNetwork::parseIpLiteral);
    return Stream.of(forwardedFor, forwarded, realIp).flatMap(s -> s);
  }

  /**
   * @return every value of the named header; empty if absent or if the container does not allow
   *         access to headers.
   */
  private static Stream<String> headerValues(HttpServletRequest req, String name) {
    Enumeration<String> values = req.getHeaders(name);
    return values == null ? Stream.empty() : Collections.list(values).stream();
  }

  /**
   * Parses the node of a {@code Forwarded} {@code for=} parameter (RFC 7239, section 6): an
   * optionally quoted IPv4 address or bracketed IPv6 address, either with an optional port.
   * An unbracketed IPv6 address is also accepted.
   *
   * @return the parsed address, or <code>null</code> if the node is not an IP literal (including
   *         {@code unknown} and obfuscated identifiers).
   */
  private static @Nullable InetAddress parseForwardedNode(String node) {
    String value = node.length() > 1 && node.startsWith("\"") && node.endsWith("\"")
        ? node.substring(1, node.length() - 1)
        : node;
    if (value.startsWith("[")) {
      int end = value.indexOf(']');
      return end < 0 ? null : parseIpLiteral(value.substring(1, end));
    }
    int colon = value.indexOf(':');
    boolean ipv4WithPort = colon >= 0 && colon == value.lastIndexOf(':');
    return parseIpLiteral(ipv4WithPort ? value.substring(0, colon) : value);
  }

  /**
   * Parses an IP literal, checking it against {@link #IP_LITERAL} first:
   * {@link InetAddress#getByName(String)} only skips DNS resolution for literal addresses.
   *
   * @return the parsed address, or <code>null</code> if it is not an IP literal.
   */
  private static @Nullable InetAddress parseIpLiteral(String value) {
    if (!IP_LITERAL.matcher(value).matches()) return null;
    try {
      return InetAddress.getByName(value);
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
    String remote = req.getRemoteAddr();
    // InetAddress.getByName(null) returns the loopback address: fail closed instead
    if (remote == null) return null;
    try {
      return InetAddress.getByName(remote);
    } catch (UnknownHostException ex) {
      return null;
    }
  }

}
