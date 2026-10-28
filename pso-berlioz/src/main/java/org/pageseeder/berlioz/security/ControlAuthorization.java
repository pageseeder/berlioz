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

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;

/**
 * Determines whether a request is authorized to invoke Berlioz control parameters (e.g.
 * {@code berlioz-reload}, {@code berlioz-profile}), see
 * {@link org.pageseeder.berlioz.servlet.ControlParameter}.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
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
   * {@code berlioz-reload}, {@code berlioz-profile}).
   *
   * <p>Authorization is granted by any one of three independent channels:
   * <ol>
   *   <li>the delegated channel — {@link #CONTROL_AUTHORIZED_ATTRIBUTE}, a fixed request-attribute
   *       handoff from the host application's own auth layer; or</li>
   *   <li>the key channel — {@link BerliozOption#CONTROL_KEY}, a shared secret presented via an
   *       {@code Authorization: Berlioz <key>} header; or</li>
   *   <li>the network channel — {@link BerliozOption#CONTROL_NETWORK}, describing what network
   *       position a direct HTTP caller must originate from (loopback or LAN), see
   *       {@link ControlNetwork#matches(HttpServletRequest)}.</li>
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

    return ControlNetwork.parse(GlobalSettings.get(BerliozOption.CONTROL_NETWORK)).matches(req);
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
