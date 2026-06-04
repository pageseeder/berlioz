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
package org.pageseeder.berlioz.content;

import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * An immutable value object returned by generators to describe the HTTP response outcome.
 *
 * <p>A {@code Response} carries the HTTP status code, optional redirect location, optional
 * RFC 9457 problem details, and any extra response headers the generator wants to set.
 * It does not carry the body — generators write the body directly to their writer.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * // Success
 * return Response.ok();
 *
 * // Client error
 * return Response.status(ContentStatus.NOT_FOUND);
 *
 * // Redirect
 * return Response.redirect(ContentStatus.SEE_OTHER, "/login");
 *
 * // RFC 9457 problem
 * return Response.problem(ProblemDetails.of(NOT_FOUND).detail("Article 42 not found"));
 *
 * // With a custom header
 * return Response.ok().header("Cache-Control", "no-store");
 * }</pre>
 *
 * <p>All methods return a new instance; this class is immutable.</p>
 *
 * <p>When a service has multiple generators, Berlioz merges their {@code Response} objects:
 * the status code is resolved by the service's {@code ServiceStatusRule}; headers are
 * merged (last writer wins per header name); a problem response from any generator
 * overrides the combined status.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public final class Response {

  private final ContentStatus status;
  private final @Nullable String redirectLocation;
  private final @Nullable ProblemDetails problem;
  private final Map<String, List<String>> headers;

  private Response(ContentStatus status, @Nullable String redirectLocation,
      @Nullable ProblemDetails problem, Map<String, List<String>> headers) {
    this.status = status;
    this.redirectLocation = redirectLocation;
    this.problem = problem;
    this.headers = Collections.unmodifiableMap(headers);
  }

  /**
   * Creates a {@code 200 OK} response with no extra headers.
   *
   * @return a new {@code Response}
   */
  public static Response ok() {
    return new Response(ContentStatus.OK, null, null, Map.of());
  }

  /**
   * Creates a response with the given non-redirect status code.
   *
   * @param status the HTTP status; must not be a redirect code
   * @return a new {@code Response}
   * @throws IllegalArgumentException if {@code status} is a redirect code
   */
  public static Response status(ContentStatus status) {
    Objects.requireNonNull(status, "status");
    if (ContentStatus.isRedirect(status))
      throw new IllegalArgumentException("Use redirect() for redirect statuses: " + status);
    return new Response(status, null, null, Map.of());
  }

  /**
   * Creates a redirect response.
   *
   * @param status   the redirect status code
   * @param location the redirect target URL
   * @return a new {@code Response}
   * @throws IllegalArgumentException if {@code status} is not a redirect code
   */
  public static Response redirect(ContentStatus status, String location) {
    Objects.requireNonNull(status, "status");
    Objects.requireNonNull(location, "location");
    if (!ContentStatus.isRedirect(status))
      throw new IllegalArgumentException("Status is not a redirect code: " + status);
    return new Response(status, location, null, Map.of());
  }

  /**
   * Creates a problem response from RFC 9457 problem details.
   *
   * <p>The HTTP status is taken from {@link ProblemDetails#status()}.</p>
   *
   * @param problem the problem details; must not be {@code null}
   * @return a new {@code Response}
   */
  public static Response problem(ProblemDetails problem) {
    Objects.requireNonNull(problem, "problem");
    return new Response(problem.status(), null, problem, Map.of());
  }

  // --- Fluent modifier -------------------------------------------------------------------------

  /**
   * Returns a copy of this response with an additional HTTP response header.
   *
   * <p>If the header name already exists, the value is appended to the existing values
   * (multi-value header). Header names are case-sensitive in this API; the servlet layer
   * is responsible for case-insensitive handling.</p>
   *
   * @param name  the header name
   * @param value the header value
   * @return a new {@code Response} with the additional header
   */
  public Response header(String name, String value) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    Map<String, List<String>> copy = new LinkedHashMap<>(this.headers);
    copy.compute(name, (k, existing) -> {
      List<String> list = existing != null ? new ArrayList<>(existing) : new ArrayList<>();
      list.add(value);
      return Collections.unmodifiableList(list);
    });
    return new Response(this.status, this.redirectLocation, this.problem,
        Collections.unmodifiableMap(copy));
  }

  // --- Accessors -------------------------------------------------------------------------------

  /** @return the HTTP status code */
  public ContentStatus status() { return this.status; }

  /** @return the redirect target URL, or {@code null} if this is not a redirect */
  public @Nullable String redirectLocation() { return this.redirectLocation; }

  /** @return the RFC 9457 problem details, or {@code null} if this is not a problem response */
  public @Nullable ProblemDetails problem() { return this.problem; }

  /** @return an unmodifiable map of extra response headers; empty if none were set */
  public Map<String, List<String>> headers() { return this.headers; }

  /** @return {@code true} if this response carries RFC 9457 problem details */
  public boolean isProblem() { return this.problem != null; }

  /** @return {@code true} if this is a redirect response */
  public boolean isRedirect() { return this.redirectLocation != null; }

}
