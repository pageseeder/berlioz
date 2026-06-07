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

import javax.servlet.http.HttpServletRequest;

/**
 * SPI for controlling which external redirect targets are permitted.
 *
 * <p>Implementations are discovered at runtime via {@link java.util.ServiceLoader}.
 * They are consulted when a redirect target does not match the application's own host
 * and is not covered by the static {@code berlioz.redirect.allowed-hosts} setting.
 *
 * <p>Register an implementation by placing its fully qualified class name in
 * {@code META-INF/services/org.pageseeder.berlioz.http.RedirectPolicy}.
 *
 * <p>All registered policies are loaded once on first use and cached for the lifetime
 * of the application.
 *
 * @see org.pageseeder.berlioz.BerliozOption#REDIRECT_ALLOWED_HOSTS
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.13.0
 */
public interface RedirectPolicy {

  /**
   * Returns {@code true} if redirecting to the given target URI is permitted for this request.
   *
   * <p>Implementations should be stateless and fast — this method is called inline
   * during request processing.
   *
   * @param target  the absolute redirect target URI (never {@code null}, always has a host)
   * @param request the current HTTP request
   * @return {@code true} if the redirect is permitted; {@code false} otherwise
   */
  boolean isPermitted(URI target, HttpServletRequest request);

}
