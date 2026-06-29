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
package org.pageseeder.berlioz.error;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;

/**
 * Thrown when a generator cannot reach an upstream service it depends on.
 *
 * <p>This exception is a flow-control signal for infrastructure failures, not a programming
 * error. Stack trace capture is suppressed so it can be thrown on every upstream failure
 * without the cost of walking the call stack.
 *
 * <p>The servlet layer catches this exception and maps it to a {@code 502 Bad Gateway}
 * response instead of the default {@code 500 Internal Server Error}.
 *
 * <p>The optional {@code upstreamService} field names the dependency that failed, which
 * appears in diagnostic log messages.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.3
 */
@Beta
public final class UpstreamException extends HttpException {

  private static final long serialVersionUID = 1L;

  /**
   * The name of the upstream service that failed, or {@code null} if not specified.
   */
  private final @Nullable String upstreamService;

  /**
   * Creates an exception for an unnamed upstream failure.
   *
   * @param message a description of the failure
   */
  public UpstreamException(String message) {
    super(message, 502);
    this.upstreamService = null;
  }

  /**
   * Creates an exception for an unnamed upstream failure, preserving the original cause.
   *
   * @param message a description of the failure
   * @param cause   the underlying exception
   */
  public UpstreamException(String message, Throwable cause) {
    super(message, 502, cause);
    this.upstreamService = null;
  }

  /**
   * Creates an exception naming the upstream service that failed.
   *
   * @param message         a description of the failure
   * @param upstreamService a short name identifying the failing dependency (e.g. {@code "PageSeeder"}, {@code "search-api"})
   */
  public UpstreamException(String message, String upstreamService) {
    super(message, 502);
    this.upstreamService = upstreamService;
  }

  /**
   * Creates an exception naming the upstream service that failed, preserving the original cause.
   *
   * @param message         a description of the failure
   * @param upstreamService a short name identifying the failing dependency
   * @param cause           the underlying exception
   */
  public UpstreamException(String message, String upstreamService, Throwable cause) {
    super(message, 502, cause);
    this.upstreamService = upstreamService;
  }

  /**
   * @return the name of the upstream service that failed, or {@code null} if not specified.
   */
  public @Nullable String getUpstreamService() {
    return this.upstreamService;
  }

}
