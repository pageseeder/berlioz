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
import org.pageseeder.berlioz.http.HttpStatusCodes;

import java.util.regex.Pattern;

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
 * @version 0.13.5
 * @since 0.13.5
 */
public final class Problems {

  /**
   *
   */
  private static final Pattern PROBLEM_SLUG =
      Pattern.compile("\\A(?=.{1,128}\\z)[a-z0-9]++(?:-[a-z0-9]++)*+\\z");

  private Problems() {
  }

  // --- Generator-level problems ----------------------------------------------------------------

  /**
   * Creates a {@code 400 Bad Request} problem from a request parameter validation failure.
   *
   * @param ex the exception carrying the parameter name, value, and reason
   * @return a new {@code ProblemDetails} with {@code type}, {@code title}, {@code detail},
   * {@code parameter}, and {@code reason} members set
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
   * Creates a {@code 400 Bad Request} problem with optional exception detail.
   *
   * @param ex    the exception carrying the parameter name, value, and reason
   * @param level controls how much diagnostic information is added as an {@code exception} member
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails forInvalidParameter(InvalidParameterException ex, DetailLevel level) {
    return withExceptionDetail(forInvalidParameter(ex), ex, level);
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
   * Creates a {@code 502 Bad Gateway} problem with optional exception detail.
   *
   * @param ex    the upstream exception
   * @param level controls how much diagnostic information is added as an {@code exception} member
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails forUpstreamException(UpstreamException ex, DetailLevel level) {
    return withExceptionDetail(forUpstreamException(ex), ex, level);
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
    String title = titleFor(code, status);
    return ProblemDetails.of(code)
        .type("urn:berlioz:problem:http-signal")
        .title(title)
        .detail(ex.getMessage());
  }

  /**
   * Creates a problem from a developer-defined {@link HttpException} with optional exception detail.
   *
   * @param ex    the signal exception carrying the HTTP code and detail message
   * @param level controls how much diagnostic information is added as an {@code exception} member
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails forHttpException(HttpException ex, DetailLevel level) {
    return withExceptionDetail(forHttpException(ex), ex, level);
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

  /**
   * Creates a {@code 500 Internal Server Error} problem with optional exception detail.
   *
   * <p>At {@link DetailLevel#MINIMAL} the result is identical to {@link #forGeneratorError()}.
   * At {@code STANDARD} or {@code FULL} an {@code exception} extension member is added, which
   * preserves source location when {@code ex} is a {@link org.xml.sax.SAXParseException} or a
   * {@link javax.xml.transform.TransformerException}.
   *
   * @param ex    the unhandled exception thrown by the generator
   * @param level controls how much diagnostic information is added as an {@code exception} member
   * @return a new {@code ProblemDetails} instance
   */
  public static ProblemDetails forGeneratorError(Throwable ex, DetailLevel level) {
    return withExceptionDetail(forGeneratorError(), ex, level);
  }

  // --- Framework-level problems ----------------------------------------------------------------

  /**
   * Creates a {@link ProblemDetails} for a framework-generated HTTP error, or {@code null} if
   * {@code code} is outside the valid HTTP status range.
   *
   * <p>Named types are assigned for the codes the framework commonly produces:
   * 400, 404, 405, and 503. All other valid codes receive the generic
   * {@code urn:berlioz:problem:error} type.
   *
   * @param code   an HTTP status code
   * @param detail a human-readable explanation of this specific occurrence
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an invalid code
   */
  public static ProblemDetails forHttpError(int code, String detail) {
    return forHttpError(code, detail, null);
  }

  /**
   * Creates a {@link ProblemDetails} for a framework-generated HTTP error, using the Berlioz
   * error ID (when present) to select a specific {@code type} URI rather than the generic
   * status-code-based fallback.
   *
   * <p>When {@code berliozErrorId} starts with {@code "berlioz-"}, the slug after that prefix
   * becomes the type URI suffix — e.g. {@code "berlioz-transform-not-found"} yields
   * {@code "urn:berlioz:problem:transform-not-found"}. This allows the failsafe XSLT to match
   * the same contextual help templates it uses for the legacy error format.
   *
   * @param code           an HTTP status code
   * @param detail         a human-readable explanation of this specific occurrence
   * @param berliozErrorId a Berlioz error ID string (e.g. {@code "berlioz-transform-not-found"}),
   *                       or {@code null} to fall back to status-code-based type selection
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an invalid code
   */
  public static ProblemDetails forHttpError(int code, String detail, @Nullable String berliozErrorId) {
    if (code < 100 || code > 599) throw new IllegalArgumentException("HTTP status code out of range: " + code);
    ContentStatus status = ContentStatus.forCode(code);
    return ProblemDetails.of(code)
        .type("urn:berlioz:problem:" + typeSlug(code, berliozErrorId))
        .title(titleFor(code, status))
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
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an invalid code
   */
  public static ProblemDetails forHttpError(int code, String detail,
                                            @Nullable Throwable throwable, DetailLevel detailLevel) {
    return forHttpError(code, detail, null, throwable, detailLevel);
  }

  /**
   * Creates a {@link ProblemDetails} for a framework-generated HTTP error, using the Berlioz
   * error ID for type selection and optionally decorating with exception detail.
   *
   * @param code           an HTTP status code
   * @param detail         a human-readable explanation of this specific occurrence
   * @param berliozErrorId a Berlioz error ID string, or {@code null}
   * @param throwable      the exception that caused the error, or {@code null}
   * @param detailLevel    controls how much diagnostic information is added as an {@code exception}
   *                       extension member
   * @return a fully populated {@code ProblemDetails}, or {@code null} for an invalid code
   */
  public static ProblemDetails forHttpError(int code, String detail,
                                            @Nullable String berliozErrorId,
                                            @Nullable Throwable throwable,
                                            DetailLevel detailLevel) {
    ProblemDetails base = forHttpError(code, detail, berliozErrorId);
    if (throwable == null || detailLevel == DetailLevel.MINIMAL) return base;
    boolean includeStackTrace = detailLevel == DetailLevel.FULL;
    return base.extension("exception", ExceptionDetail.of(throwable, includeStackTrace));
  }

  // --- Private helpers -------------------------------------------------------------------------

  /**
   * Returns {@code base} with an {@code exception} extension added when {@code level} is not
   * {@link DetailLevel#MINIMAL}. Source location (line, column, system-id) is preserved
   * automatically for {@link org.xml.sax.SAXParseException} and
   * {@link javax.xml.transform.TransformerException} instances.
   */
  private static ProblemDetails withExceptionDetail(ProblemDetails base, Throwable ex, DetailLevel level) {
    if (level == DetailLevel.MINIMAL) return base;
    return base.extension("exception", ExceptionDetail.of(ex, level == DetailLevel.FULL));
  }

  /**
   * Returns the type URI slug for a framework error.
   *
   * <p>When {@code berliozErrorId} starts with {@code "berlioz-"}, strips that prefix and uses
   * the remainder as the slug — e.g. {@code "berlioz-transform-not-found"} →
   * {@code "transform-not-found"}. Otherwise falls back to a slug derived from the HTTP status
   * code for the four codes the framework commonly produces specifically.
   */
  private static String typeSlug(int code, @Nullable String berliozErrorId) {
    if (berliozErrorId != null && berliozErrorId.startsWith("berlioz-")) {
      String slug = berliozErrorId.substring("berlioz-".length());
      if (PROBLEM_SLUG.matcher(slug).matches()) return slug;
    }
    if (code == 400) return "bad-request";
    if (code == 404) return "not-found";
    if (code == 405) return "method-not-allowed";
    if (code == 503) return "service-unavailable";
    return "error";
  }

  private static String reasonString(InvalidParameterException.Reason reason) {
    return reason.name().toLowerCase().replace('_', '-');
  }

  /**
   * Converts a {@link ContentStatus} name to a human-readable title, e.g. NOT_FOUND → "Not Found".
   */
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

  private static String titleFor(int code, @Nullable ContentStatus status) {
    String title = HttpStatusCodes.getTitle(code);
    if (title != null) return title;
    return status != null ? toTitle(status) : "HTTP " + code;
  }

}
