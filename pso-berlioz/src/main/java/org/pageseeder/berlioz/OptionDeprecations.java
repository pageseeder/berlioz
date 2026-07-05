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

import org.pageseeder.berlioz.security.ControlNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Checks for deprecated or otherwise risky {@link BerliozOption} values at configuration load
 * time and emits a single WARN-level log per violation.
 *
 * <p>This is the internal precursor to the declarative configuration validation layer planned
 * for Theme 5. Each entry maps an option to the specific raw value that constitutes deprecated
 * or risky usage and the message to emit when that value is found in the loaded configuration.
 *
 * @author Christophe Lauret
 * @version 0.14.0
 * @since 0.14.0
 */
final class OptionDeprecations {

  private static final Logger LOGGER = LoggerFactory.getLogger(OptionDeprecations.class);

  private OptionDeprecations() {}

  /**
   * Checks all known deprecated-value and risky-value constraints against the loaded
   * configuration and logs a warning for each violation. Called once per configuration load or
   * reload.
   *
   * @param properties the fully resolved configuration properties
   */
  @SuppressWarnings({"deprecation", "removal"})
  static void checkAll(Map<String, String> properties) {
    check(properties, BerliozOption.HTTP_GET_VIA_POST, "true",
        "Config berlioz.http.get-via-post=true is deprecated since 0.14.0 and will be removed in 1.0."
            + " POST requests will never fall back to GET."
            + " Declare explicit POST-mapped services and remove this property.");

    check(properties, BerliozOption.ERROR_PROBLEM_FORMAT, "false",
        "Config berlioz.errors.problem=false enables legacy error XML (<server-error>/<client-error>),"
            + " deprecated since 0.14.0 and will be removed in 1.0."
            + " Migrate XSLT error templates to use <problem> elements, then remove this property.");

    check(properties, BerliozOption.XML_HEADER_VERSION, "0.9",
        "Config berlioz.xml.header.version=0.9 enables the legacy XML header format,"
            + " deprecated since 0.14.0 and will be removed in 1.0."
            + " Migrate XSLT templates to the 1.0 header format and remove this property.");

    checkControlNetwork(properties);
  }

  /**
   * Warns once when {@code berlioz.control.network} is set to {@code loopback} or {@code lan} —
   * these authorize Berlioz control parameters based on {@code req.getRemoteAddr()}, which is
   * unsafe when a reverse proxy fronts the app on the same host or across an untrusted boundary.
   */
  private static void checkControlNetwork(Map<String, String> properties) {
    String raw = properties.get(BerliozOption.CONTROL_NETWORK.property());
    if (raw == null) return;
    ControlNetwork network = ControlNetwork.parse(raw);
    if (network == ControlNetwork.LOOPBACK || network == ControlNetwork.LAN) {
      LOGGER.warn("Config berlioz.control.network={} authorizes Berlioz control parameters"
          + " (berlioz-reload, etc.) based on req.getRemoteAddr(). This is a dev-convenience"
          + " setting, not a production security setting: if a reverse proxy fronts this app —"
          + " especially one on the same host — getRemoteAddr() is the proxy's address for every"
          + " request, including external attackers'. Do not enable it behind such a proxy.", raw);
    }
  }

  private static void check(Map<String, String> properties, BerliozOption option,
                             String deprecatedValue, String message) {
    String raw = properties.get(option.property());
    if (deprecatedValue.equalsIgnoreCase(raw)) {
      LOGGER.warn(message);
    }
  }
}
