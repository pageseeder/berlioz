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
import org.pageseeder.berlioz.content.ContentStatus;

/**
 * Factory methods for all Berlioz {@link ProblemDetails} instances.
 *
 * <p>Every problem type produced by the framework is created here, ensuring the
 * {@code urn:berlioz:problem:*} URN namespace is assigned in one place and remains consistent
 * across generator-level and servlet-level error paths.</p>
 *
 * <p>Generator authors should call the appropriate named factory rather than constructing
 * {@link ProblemDetails} directly:</p>
 * <ul>
 *   <li>{@link #forInvalidParameter(InvalidParameterException)} — 400 Bad Request</li>
 *   <li>{@link #forUpstreamException(UpstreamException)} — 502 Bad Gateway</li>
 *   <li>{@link #forHttpException(HttpException)} — any code carried by an {@link HttpException} subclass</li>
 *   <li>{@link #forGeneratorError()} — 500 Internal Server Error</li>
 * </ul>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.5
 * @since 0.13.5
 */
public final class Problems {

  private Problems() {}

  // --- Generator-level problems ----------------------------------------------------------------

  /**
   * Creates a {@code 400 Bad Request} problem from a request parameter validation failure.
   *
   * @param ex the exception carrying the parameter name, value, and reason
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, {@code detail},
   *         {@code parameter}, and {@code reason} members set
   */
  public static ProblemDetails forInvalidParameter(InvalidParameterException ex) {
    return ProblemDetails.of(ContentStatus.BAD_REQUEST)
        .type("urn:berlioz:problem:invalid-parameter")
        .title("Invalid Request Parameter")
        .detail(ex.getMessage())
        .extension("parameter", ex.getParameterName())
        .extension("reason", reasonString(ex.getReason()));
  }

  /**
   * Creates a {@code 502 Bad Gateway} problem from an upstream service failure.
   *
   * <p>If the exception names the failing dependency via {@link UpstreamException#getUpstreamService()},
   * it is included as an {@code upstream-service} extension member.
   *
   * @param ex the upstream exception
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, and {@code detail} set
   */
  public static ProblemDetails forUpstreamException(UpstreamException ex) {
    ProblemDetails problem = ProblemDetails.of(ContentStatus.BAD_GATEWAY)
        .type("urn:berlioz:problem:upstream-error")
        .title("Upstream Service Error")
        .detail(ex.getMessage());
    String service = ex.getUpstreamService();
    return service != null ? problem.extension("upstream-service", service) : problem;
  }

  /**
   * Creates a problem from a developer-defined {@link HttpException} subclass.
   *
   * <p>The HTTP status code and message are taken directly from the exception. Use this as the
   * catch-all for {@link HttpException} subclasses that are not {@link InvalidParameterException}
   * or {@link UpstreamException}.
   *
   * @param ex the signal exception carrying the HTTP code and detail message
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, and {@code detail} set
   */
  public static ProblemDetails forHttpException(HttpException ex) {
    int code = ex.getHttpCode();
    ContentStatus status = ContentStatus.forCode(code);
    String title = status != null ? toTitle(status) : "HTTP " + code;
    return ProblemDetails.of(code)
        .type("urn:berlioz:problem:http-signal")
        .title(title)
        .detail(ex.getMessage());
  }

  /**
   * Creates a {@code 500 Internal Server Error} problem for an unhandled generator failure.
   *
   * <p>The error detail is intentionally omitted to avoid leaking internal information to clients.
   *
   * @return a new {@code ProblemDetails} with {@code type} and {@code title} set
   */
  public static ProblemDetails forGeneratorError() {
    return ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
        .type("urn:berlioz:problem:generator-error")
        .title("Internal Server Error");
  }

  // --- Framework-level problems ----------------------------------------------------------------

  /**
   * Creates a {@link ProblemDetails} for a framework-generated HTTP error, or {@code null} if
   * {@code code} does not map to a known {@link ContentStatus}.
   *
   * <p>Named types are assigned for the codes the framework commonly produces:
   * 400, 404, 405, and 503. All other valid codes receive the generic
   * {@code urn:berlioz:problem:error} type.
   *
   * @param code   an HTTP status code
   * @param detail a human-readable explanation of this specific occurrence
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an unrecognised code
   */
  public static @Nullable ProblemDetails forHttpError(int code, String detail) {
    ContentStatus status = ContentStatus.forCode(code);
    if (status == null) return null;
    String slug;
    if      (code == 400) slug = "bad-request";
    else if (code == 404) slug = "not-found";
    else if (code == 405) slug = "method-not-allowed";
    else if (code == 503) slug = "service-unavailable";
    else                  slug = "error";
    return ProblemDetails.of(status)
        .type("urn:berlioz:problem:" + slug)
        .title(toTitle(status))
        .detail(detail);
  }

  /**
   * Creates a {@link ProblemDetails} for a framework-generated HTTP error, optionally decorated
   * with exception detail according to the requested verbosity level.
   *
   * @param code        an HTTP status code
   * @param detail      a human-readable explanation of this specific occurrence
   * @param throwable   the exception that caused the error, or {@code null}
   * @param detailLevel controls how much diagnostic information is added as an {@code exception}
   *                    extension member
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an unrecognised code
   */
  public static @Nullable ProblemDetails forHttpError(int code, String detail,
      @Nullable Throwable throwable, DetailLevel detailLevel) {
    ProblemDetails base = forHttpError(code, detail);
    if (base == null || throwable == null || detailLevel == DetailLevel.MINIMAL) return base;
    boolean includeStackTrace = detailLevel == DetailLevel.FULL;
    return base.extension("exception", ExceptionDetail.of(throwable, includeStackTrace));
  }

  // --- Private helpers -------------------------------------------------------------------------

  private static String reasonString(InvalidParameterException.Reason reason) {
    return reason.name().toLowerCase().replace('_', '-');
  }

  /** Converts a {@link ContentStatus} name to a human-readable title, e.g. NOT_FOUND → "Not Found". */
  private static String toTitle(ContentStatus status) {
    String[] words = status.name().split("_");
    StringBuilder sb = new StringBuilder();
    for (String word : words) {
      if (sb.length() > 0) sb.append(' ');
      sb.append(Character.toUpperCase(word.charAt(0)));
      sb.append(word.substring(1).toLowerCase());
    }
    return sb.toString();
  }

}
