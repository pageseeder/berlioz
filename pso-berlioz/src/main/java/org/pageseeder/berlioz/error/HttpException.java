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
import org.pageseeder.berlioz.http.HttpStatusCodes;

import java.util.Objects;

/**
 * Base class for exceptions that short-circuit generator execution with a specific HTTP status code.
 *
 * <p>These exceptions are flow-control signals, not programming errors. Stack trace capture is
 * suppressed so subclasses can be thrown on every request without the cost of walking the call stack.
 *
 * <p>The servlet layer catches any {@code HttpException}, calls {@link #toProblem()} to obtain the
 * RFC 9457 problem representation, then appends diagnostic detail (class, message, stack trace)
 * according to the {@code berlioz.errors.detail} configuration — never based on anything the
 * exception itself decides.
 *
 * <p>Berlioz provides two built-in subclasses for common cases:
 * <ul>
 *   <li>{@link InvalidParameterException} — 400 Bad Request</li>
 *   <li>{@link UpstreamException} — 502 Bad Gateway</li>
 * </ul>
 *
 * <h3>Registry pattern (recommended for application-level errors)</h3>
 * <p>Define typed {@link ProblemDetails} constants in a registry class and throw them via
 * {@link #of(ProblemDetails)}. No subclassing is needed and the framework controls all
 * diagnostic output:
 * <pre>{@code
 * // One-time definitions
 * static final ProblemDetails QUOTA_EXCEEDED = ProblemDetails.of(429)
 *     .type("urn:myapp:problem:quota-exceeded")
 *     .title("Rate Limit Exceeded");
 *
 * // In a generator
 * throw HttpException.of(QUOTA_EXCEEDED.detail("Daily limit reached for user " + userId));
 * }</pre>
 *
 * <h3>Subclassing (escape hatch)</h3>
 * <p>When the problem type and title are fixed for a given exception class, override
 * {@link #toProblem()} instead:
 * <pre>{@code
 * public class LegalHoldException extends HttpException {
 *   public LegalHoldException(String reason) { super(reason, 451); }
 *
 *   {@literal @}Override
 *   public ProblemDetails toProblem() {
 *     return ProblemDetails.of(451)
 *         .type("urn:myapp:problem:legal-hold")
 *         .title("Unavailable For Legal Reasons")
 *         .detail(getMessage());
 *   }
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
   * @param httpCode the HTTP status code to send; must be in the range 400–599
   * @throws IllegalArgumentException if {@code httpCode} is outside 400–599
   */
  protected HttpException(String message, int httpCode) {
    super(message);
    this.httpCode = requireErrorCode(httpCode);
  }

  /**
   * Creates an HTTP signal exception with the given message, HTTP status code, and underlying cause.
   *
   * @param message  a description of the condition
   * @param httpCode the HTTP status code to send; must be in the range 400–599
   * @param cause    the exception that triggered this signal
   * @throws IllegalArgumentException if {@code httpCode} is outside 400–599
   */
  protected HttpException(String message, int httpCode, Throwable cause) {
    super(message, cause);
    this.httpCode = requireErrorCode(httpCode);
  }

  private static int requireErrorCode(int code) {
    if (code < 400 || code > 599) throw new IllegalArgumentException("HTTP signal code must be 400–599, got: " + code);
    return code;
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

  /**
   * Returns the RFC 9457 problem representation for this exception.
   *
   * <p>The default implementation produces a generic {@code urn:berlioz:problem:http-signal}
   * problem using the HTTP status code and exception message. Subclasses should override this
   * to provide a domain-specific {@code type} URI and {@code title}.
   *
   * <p>Do not add an {@code exception} extension here — the framework appends diagnostic detail
   * (class name, message, stack trace) automatically based on {@code berlioz.errors.detail}.
   *
   * @return a {@link ProblemDetails} describing this error; never {@code null}
   */
  public ProblemDetails toProblem() {
    String title = HttpStatusCodes.getTitle(this.httpCode);
    ProblemDetails problem = ProblemDetails.of(this.httpCode)
        .type("urn:berlioz:problem:http-signal")
        .title(title != null ? title : "HTTP " + this.httpCode);
    String detail = getMessage();
    return detail != null ? problem.detail(detail) : problem;
  }

  // --- Registry pattern factory ----------------------------------------------------------------

  /**
   * Creates an {@code HttpException} that carries the given problem definition.
   *
   * <p>Use this with a registry of pre-defined {@link ProblemDetails} constants when no custom
   * subclass is needed. The HTTP status code is taken from the problem's {@code status} field.
   * The framework controls how much diagnostic detail (class, message, stack trace) is appended
   * to the response — the problem definition itself is always safe to pre-define and reuse.
   *
   * @param problem the RFC 9457 problem describing this error; must not be {@code null}
   * @return a new {@code HttpException} whose {@link #toProblem()} returns {@code problem}
   */
  public static HttpException of(ProblemDetails problem) {
    return new ProblemException(Objects.requireNonNull(problem, "problem"));
  }

  /**
   * Creates an {@code HttpException} that carries the given problem definition and wraps a cause.
   *
   * @param problem the RFC 9457 problem describing this error; must not be {@code null}
   * @param cause   the exception that triggered this signal
   * @return a new {@code HttpException} whose {@link #toProblem()} returns {@code problem}
   */
  public static HttpException of(ProblemDetails problem, Throwable cause) {
    return new ProblemException(Objects.requireNonNull(problem, "problem"), cause);
  }

  private static final class ProblemException extends HttpException {

    private static final long serialVersionUID = 1L;

    private final ProblemDetails problem;

    ProblemException(ProblemDetails problem) {
      super(effectiveMessage(problem), problem.status());
      this.problem = problem;
    }

    ProblemException(ProblemDetails problem, Throwable cause) {
      super(effectiveMessage(problem), problem.status(), cause);
      this.problem = problem;
    }

    @Override
    public ProblemDetails toProblem() {
      return this.problem;
    }

    private static String effectiveMessage(ProblemDetails p) {
      if (p.detail() != null) return p.detail();
      if (p.title() != null) return p.title();
      return "HTTP " + p.status();
    }
  }

}
