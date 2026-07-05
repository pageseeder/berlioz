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
package org.pageseeder.berlioz.security;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.http.HttpHeaders;

/**
 * Determines whether a request is authorized to invoke Berlioz control parameters (e.g.
 * {@code berlioz-reload}, {@code clear-xsl-cache}, {@code reset-etags}, {@code reload-services},
 * {@code berlioz-profile}).
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class ControlAuthorization {

  private ControlAuthorization() {
  }

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
        .map(ControlAuthorization::parseIpLiteral);
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

}
