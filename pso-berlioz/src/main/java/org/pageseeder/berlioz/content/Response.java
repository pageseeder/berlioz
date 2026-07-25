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
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.http.HttpResponses;

import java.util.*;

/**
 * An immutable value object returned by generators to describe the HTTP response outcome.
 *
 * <p>A {@code Response} carries the HTTP status code, optional redirect location, optional
 * RFC 9457 problem details, and any extra response headers the generator wants to set.
 * It does not carry the body — generators write the body directly to their writer.</p>
 *
 * <h2>Headers</h2>
 *
 * <p>{@link #header(String, String)} follows replace semantics: if the same name is set
 * twice within one {@code Response} chain, the last value wins. The same rule applies when
 * merging headers across multiple generators in a service.</p>
 *
 * <p>Only headers that a generator has a genuine reason to set belong here. Framework concerns
 * ({@code ETag}, {@code Cache-Control}, {@code Vary}, {@code Content-Encoding}, CORS headers,
 * {@code Set-Cookie}) are handled by other layers and will be flagged with a warning if set.
 * Use {@link #redirect(ContentStatus, String)} instead of setting {@code Location} directly.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * return Response.ok();
 * return Response.status(ContentStatus.NOT_FOUND);
 * return Response.redirect(ContentStatus.SEE_OTHER, "/login");
 * return Response.problem(ProblemDetails.of(NOT_FOUND).detail("Article 42 not found"));
 * return Response.ok().header("Content-Location", "/articles/42");
 * return Response.status(UNAUTHORIZED).header("WWW-Authenticate", "Bearer realm=\"api\"");
 * }</pre>
 *
 * <p>All methods return a new instance; this class is immutable.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.1
 * @since 0.13.2
 */
public final class Response {

  private final @Nullable ContentStatus status;
  private final int statusCode;
  private final @Nullable String redirectLocation;
  private final @Nullable ProblemDetails problem;
  private final Map<String, String> headers;

  private Response(@Nullable ContentStatus status, int statusCode, @Nullable String redirectLocation,
      @Nullable ProblemDetails problem, Map<String, String> headers) {
    this.status = status;
    this.statusCode = statusCode;
    this.redirectLocation = redirectLocation;
    this.problem = problem;
    this.headers = headers; // all callers guarantee immutability
  }

  // --- Factories -------------------------------------------------------------------------------

  /**
   * Creates a {@code 200 OK} response with no extra headers.
   *
   * @return a new {@code Response}
   */
  public static Response ok() {
    return new Response(ContentStatus.OK, ContentStatus.OK.code(), null, null, Map.of());
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
    return new Response(status, status.code(), null, null, Map.of());
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
    return new Response(status, status.code(), location, null, Map.of());
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
    ContentStatus cs = ContentStatus.forCode(problem.status());
    return new Response(cs, problem.status(), null, problem, Map.of());
  }

  // --- Fluent modifier -------------------------------------------------------------------------

  /**
   * Returns a copy of this response with the named header set to the given value.
   *
   * <p>If this name was already set, the previous value is replaced (last-call-wins).
   * The same rule applies when merging headers across generators in the same service.</p>
   *
   * <p>Only headers that a generator genuinely owns belong here — for example:
   * {@code Content-Location}, {@code Content-Disposition}, {@code WWW-Authenticate},
   * {@code Retry-After}, or application-specific {@code X-*} headers.
   * Headers managed by the framework ({@code ETag}, {@code Cache-Control}, {@code Vary},
   * {@code Set-Cookie}, etc.) should not be set here; the dispatch layer will log a warning
   * if it encounters them.</p>
   *
   * @param name  the header name
   * @param value the header value
   * @return a new {@code Response} with the header set
   * @throws IllegalArgumentException if the header name or value is not valid for HTTP
   */
  public Response header(String name, String value) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(value, "value");
    if (!HttpResponses.isValidHeaderName(name))
      throw new IllegalArgumentException("Invalid HTTP header name");
    if (!HttpResponses.isValidHeaderValue(value))
      throw new IllegalArgumentException("Invalid HTTP header value for: " + name);
    Map<String, String> copy = new LinkedHashMap<>(this.headers);
    copy.put(name, value);
    return new Response(this.status, this.statusCode, this.redirectLocation, this.problem, Map.copyOf(copy));
  }

  /**
   * Returns a copy of this response with all supplied headers merged into it.
   *
   * <p>The supplied headers replace existing values with the same name. All names and values are
   * validated before a new response is created.
   *
   * @param headers the headers to merge
   * @return a new {@code Response} containing the merged headers
   * @throws IllegalArgumentException if a header name or value is not valid for HTTP
   */
  public Response headers(Map<String, String> headers) {
    Objects.requireNonNull(headers, "headers");
    if (headers.isEmpty()) return this;
    headers.forEach((name, value) -> {
      Objects.requireNonNull(name, "name");
      Objects.requireNonNull(value, "value");
      if (!HttpResponses.isValidHeaderName(name))
        throw new IllegalArgumentException("Invalid HTTP header name");
      if (!HttpResponses.isValidHeaderValue(value))
        throw new IllegalArgumentException("Invalid HTTP header value for: " + name);
    });
    Map<String, String> copy = new LinkedHashMap<>(this.headers);
    copy.putAll(headers);
    return new Response(this.status, this.statusCode, this.redirectLocation, this.problem, Map.copyOf(copy));
  }

  // --- Accessors -------------------------------------------------------------------------------

  /**
   * Returns the response status as a {@link ContentStatus} when the code is represented by the
   * generator status enum.
   *
   * <p>Problem responses can carry any valid HTTP status. Use {@link #statusCode()} when the exact
   * wire-level HTTP status matters.</p>
   *
   * @return the content status view, or the nearest status class represented by
   *         {@link ContentStatus} when the exact code is not represented
   */
  public ContentStatus status() {
    return this.status != null ? this.status : ContentStatus.forCodeOrClass(this.statusCode);
  }

  /** @return the exact HTTP status code to send on the wire */
  public int statusCode() { return this.statusCode; }

  /** @return the redirect target URL, or {@code null} if this is not a redirect */
  public @Nullable String redirectLocation() { return this.redirectLocation; }

  /** @return the RFC 9457 problem details, or {@code null} if this is not a problem response */
  public @Nullable ProblemDetails problem() { return this.problem; }

  /**
   * Returns the response headers set by the generator.
   *
   * @return an unmodifiable map of header names to values; empty if none were set
   */
  public Map<String, String> headers() { return this.headers; }

  /** @return {@code true} if this response carries RFC 9457 problem details */
  public boolean isProblem() { return this.problem != null; }

  /** @return {@code true} if this is a redirect response */
  public boolean isRedirect() { return this.redirectLocation != null; }

}
