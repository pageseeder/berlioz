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

import org.pageseeder.berlioz.Beta;

/**
 * Base class for exceptions that short-circuit generator execution with a specific HTTP status code.
 *
 * <p>These exceptions are flow-control signals, not programming errors. Stack trace capture is
 * suppressed so subclasses can be thrown on every request without the cost of walking the call stack.
 *
 * <p>The servlet layer catches any {@code HttpException} and maps it to the HTTP response code
 * returned by {@link #getHttpCode()}, bypassing the default {@code 500 Internal Server Error}.
 *
 * <p>Berlioz provides two built-in subclasses:
 * <ul>
 *   <li>{@link InvalidParameterException} — 400 Bad Request</li>
 *   <li>{@link UpstreamException} — 502 Bad Gateway</li>
 * </ul>
 *
 * <p>For other situations where short-circuiting is appropriate (e.g. 451, 431), extend this
 * class and call {@code super(message, code)} in the constructor:
 * <pre>{@code
 * public class LegalHoldException extends HttpException {
 *   public LegalHoldException(String reason) { super(reason, 451); }
 * }
 * }</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
@Beta
public abstract class HttpException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final int httpCode;

  /**
   * Creates an HTTP signal exception with the given message and HTTP status code.
   *
   * @param message  a description of the condition
   * @param httpCode the HTTP status code to send; should be 4xx or 5xx
   */
  protected HttpException(String message, int httpCode) {
    super(message);
    this.httpCode = httpCode;
  }

  /**
   * Creates an HTTP signal exception with the given message, HTTP status code, and underlying cause.
   *
   * @param message  a description of the condition
   * @param httpCode the HTTP status code to send; should be 4xx or 5xx
   * @param cause    the exception that triggered this signal
   */
  protected HttpException(String message, int httpCode, Throwable cause) {
    super(message, cause);
    this.httpCode = httpCode;
  }

  /**
   * Suppresses stack trace capture — this exception is a request-level signal, not a bug.
   */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  /**
   * @return the HTTP status code that the servlet layer should use for the response
   */
  public int getHttpCode() {
    return this.httpCode;
  }

}
