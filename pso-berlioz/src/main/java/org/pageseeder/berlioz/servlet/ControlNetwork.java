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
package org.pageseeder.berlioz.servlet;

/**
 * Describes what network position a direct HTTP caller must originate from to be allowed to
 * invoke Berlioz control parameters (e.g. {@code berlioz-reload}).
 *
 * <p>This is re-evaluated independently on every request — there is no session or persisted
 * state, so being granted access on one request confers no standing for the next.
 *
 * <p>This is the "network channel" of {@link BerliozConfig#hasControl(javax.servlet.http.HttpServletRequest)};
 * see that method for how it combines with the independent key channel and the fixed delegated
 * (request-attribute) channel.
 *
 * <p>The string representation (lower-case enum name) is the value used in the
 * {@code berlioz.control.network} configuration property.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public enum ControlNetwork {

  /** The network channel never grants access. Safe default. */
  OFF,

  /** Grants access when the caller's remote address is a loopback address. Dev-only; see caveat below. */
  LOOPBACK,

  /** Grants access when the caller's remote address is loopback or a private/site-local address. Dev-only; see caveat below. */
  LAN;

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

}
