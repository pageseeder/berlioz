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

import javax.servlet.http.HttpServletRequest;

import org.jspecify.annotations.Nullable;

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

  /**
   * Returns {@code true} only for relative paths and same-origin absolute URLs, blocking open redirects.
   * Protocol-relative URLs ({@code //host/path}) are treated as absolute.
   *
   * @param url the redirect URL to validate (might be {@code null})
   * @param req the current request, used to determine the expected origin
   * @return {@code true} if the URL is safe to redirect to
   */
  public static boolean isSafeRedirectURL(@Nullable String url, HttpServletRequest req) {
    if (url == null) return false;
    // Reject CRLF characters to prevent HTTP response splitting
    if (url.indexOf('\r') >= 0 || url.indexOf('\n') >= 0) return false;
    try {
      URI uri = new URI(url);
      // Relative URL with no authority is safe (e.g., /some/path or ../other)
      if (!uri.isAbsolute() && uri.getAuthority() == null) return true;
      // Absolute or protocol-relative: must match the same host and port
      String host = uri.getHost();
      if (host == null || !host.equalsIgnoreCase(req.getServerName())) return false;
      int uriPort = uri.getPort();
      int reqPort = req.getServerPort();
      // Treat standard ports as unspecified
      if (uriPort == 80 || uriPort == 443) uriPort = -1;
      if (reqPort == 80 || reqPort == 443) reqPort = -1;
      return uriPort == -1 || uriPort == reqPort;
    } catch (URISyntaxException e) {
      return false;
    }
  }

}
