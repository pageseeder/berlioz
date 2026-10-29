# 0005. Use RFC 9457 Problem Details for errors

- **Date:** 2026-07-21 (0.14.0; opt-in since 0.13.5)
- **Status:** Accepted

## Context

Berlioz reported errors in its own XML formats (`<server-error>`, `<client-error>`), which JSON clients and API tooling could not consume. Generators had no standard way to short-circuit a request with an HTTP error and structured details.

## Decision

Berlioz uses RFC 9457 Problem Details for framework and generator errors, in both XML (`application/problem+xml`) and JSON (`application/problem+json`):

- Generators signal errors by throwing an `HttpException` subclass (such as `InvalidParameterException` or `UpstreamException`) or returning `Response.problem(…)`.
- Direct services return the problem as the whole response. Envelope services embed it inline, and the service's response-code rule decides the final status, which allows partial failure.
- `urn:berlioz:problem:*` type URIs are reserved for Berlioz; applications use their own scheme.
- `berlioz.errors.detail` controls framework-internal diagnostics (stack traces, exception class, headers, parameters). The `detail` member is always the message passed to the problem, which for framework errors is the HTTP status phrase, so it is safe at `minimal`.
- JSON errors are always Problem Details; there was never a legacy JSON format.

Problem Details became the default in 0.14.0 ([0004](0004-secure-defaults-with-deprecated-opt-outs.md)). The legacy XML format is deprecated and uses a unified `<error http-class="…">` element until it is removed.

## Consequences

- One error model for XML, JSON and HTML output, rendered to HTML by the failsafe or a custom error stylesheet.
- `HttpException` suppresses stack trace capture so that it can be thrown on every bad request without cost.
- Error stylesheets written for the legacy elements must be migrated.
