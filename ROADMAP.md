# Berlioz Roadmap

Berlioz is a lightweight Java web framework built around URI templates, generator-based content, XML, JSON, and XSLT rendering.

The 0.13.x releases have already delivered a large part of the previous roadmap: typed request parameters, safer XML parsing and XSLT caching, output-aware generators, direct JSON responses, explicit response values, and stronger redirect/filesystem protections.

This roadmap now focuses on the remaining work needed to make those foundations feel complete, predictable, and easy to adopt without turning Berlioz into a general-purpose application framework.

## Guiding Principles

- Keep Berlioz simple, explicit, and easy to reason about.
- Keep the core annotation-free. Annotation-based programming can be useful, but it should live in optional modules.
- Preserve URI templates as the primary request-matching model.
- Preserve XML and XSLT as first-class mechanisms for generating XML and HTML responses.
- Keep JSON support direct and practical without weakening the XML/XSLT pipeline.
- Prefer small extension points over large framework dependencies.
- Keep optional integrations optional.
- Maintain source compatibility for existing `ContentGenerator` applications where feasible.
- Treat security hardening as part of the core framework contract, not as an optional add-on.

## Recent Progress

### 0.13.1 Foundations

The 0.13.1 cycle completed much of the request and XML/XSLT foundation work:

- Added fluent typed parameter access via `Request#parameter(...)`.
- Added reusable `ParameterSpec<T>` definitions.
- Added validation helpers for required values, defaults, enums, ranges, clamps, regex matching, and custom parsers.
- Migrated XML parsing toward `Xml` and deprecated `XMLUtils`.
- Improved safe XML parser handling.
- Introduced `XsltTemplateCache` and `XsltCacheMode`.
- Improved deterministic XSLT cache ETags and failsafe template handling.
- Added test coverage across request, XML, XSLT, servlet, utility, and system generator areas.

### 0.13.2 Output And Response Model

The 0.13.2 cycle completed much of the output-aware generator model:

- Added the `BerliozGenerator` hierarchy:
  - `XmlGenerator`
  - `JsonGenerator`
  - `Generator`
  - `RawGenerator`
  - legacy `ContentGenerator` compatibility
- Added `Response` for generator status, redirects, headers, and problem values.
- Added `ProblemDetails` as an RFC 9457-shaped value object.
- Added direct JSON response handling through `JsonResponse`.
- Added JSON ETag and conditional request handling for cacheable generators.
- Added JSON profiling output.
- Added `JsonStringBuilder#fieldRaw(...)`.
- Added JSON/XML media type helpers.
- Added service schema support for `patch`, `<namespace>`, and `<handler>`.
- Added supported-output negotiation across service generators.
- Added warnings for disjoint generator output formats.
- Added `ContentStatus.PERMANENT_REDIRECT` for HTTP 308.
- Added `RedirectPolicy` and `berlioz.redirect.allowed-hosts`.
- Hardened redirects, relocations, stylesheet resolution, environment path resolution, web bundle CSS references, and response header validation.
- Added broad unit coverage for the new servlet, generator, response, service, JSON, and security behavior.

## Current Development Themes

### 1. Complete Problem Response Integration

`ProblemDetails` and `Response.problem(...)` now exist, but framework-generated errors still need a complete negotiated rendering model.

Next work:

- Render problem responses consistently for JSON, XML, and transformed HTML.
- Use `400 Bad Request` for invalid typed parameters.
- Use `404 Not Found` for unmatched services.
- Use `405 Method Not Allowed` when a URI matches but the method does not.
- Use `406 Not Acceptable` when the requested output format is unsupported.
- Use `500 Internal Server Error` for unexpected generator failures.
- Define Berlioz problem `type` URIs and decide whether they resolve to public documentation.
- Decide how parameter validation failures should expose field-level details.
- Decide whether request validation should fail immediately or collect all parameter errors first.

The goal is not a large exception-mapping framework. The goal is a small, predictable error model that works across the existing output pipeline.

### 2. Finish Direct Output And Raw Output Support

Direct JSON and direct XML services are now part of the pipeline. `RawGenerator` exists as an API shape, but servlet dispatch support is still future work.

Next work:

- Add a dedicated servlet path for `RawGenerator`.
- Define content type and charset behavior for raw output.
- Decide how cache headers and ETags apply to raw responses.
- Keep the direct service rule simple: one handler, one complete response body.
- Document how `<handler>` differs from `<generator>`.

### 3. Service Metadata And Diagnostics

Service inspection is becoming more important now that services can expose different output formats and response modes.

Potential metadata:

- Service identifier and group.
- Matched URI template.
- HTTP method.
- Supported output formats.
- Direct handler flag.
- Cache policy.
- Generator list, names, targets, and cacheability.
- Schema namespace resolution.
- Diagnostic warnings for development mode.

Potential output channels:

- Existing source or diagnostic views.
- XML metadata nodes.
- JSON metadata properties.
- HTTP headers where useful.
- Test helpers for service registry assertions.

This should make services easier to inspect, document, test, and debug without adding heavy runtime machinery.

### 4. Authentication And Authorization Guards

Berlioz should make it easy for applications to guard services and generators without becoming responsible for authentication itself.

Authentication should remain a separate concern. Applications, servlet containers, filters, reverse proxies, session systems, or identity providers can establish the current user or principal before Berlioz executes a service. Berlioz can then provide small authorization hooks to decide whether a service or generator may run.

Expected behavior:

- Return `401 Unauthorized` when authentication is required but no authenticated user is available.
- Return `403 Forbidden` when the user is authenticated but lacks the required permission, role, or capability.
- Use Problem Details for machine-readable authorization failures.
- Include `WWW-Authenticate` headers for authentication schemes that require them.

Possible guard models:

- Programmatic generator guards, for example `Guard.authenticated().and(canWrite(document))`.
- Generator interfaces such as `GuardedGenerator`, `RequiresAuthorization`, or `PermissionCheck`.
- Service-level requirements in `services.xml`, with application-defined meanings for roles and permissions.
- An application-provided `AuthorizationManager` that evaluates declared requirements.
- Lightweight interceptors that run before services or generators.
- Optional annotation support in a separate module, for example `@RequiresRole` or `@RequiresPermission`.

The core model should not force role-based, permission-based, attribute-based, or application-specific policy. It should provide a small result model:

- Allowed.
- Authentication required.
- Forbidden.
- Custom problem response.

### 5. Interceptors And Observability

`GeneratorListener` already provides a focused generator timing hook. The next step is deciding whether Berlioz needs a broader interceptor model.

Possible hooks:

- Before service execution.
- After successful service execution.
- Around individual generator execution.
- On generator errors.
- Around XSLT transformation.
- Around authorization checks.

Potential uses:

- Logging.
- Timing, including XSLT transformation performance.
- Metrics.
- Tracing.
- Security checks.
- Development diagnostics.

Natural optional integrations:

- Micrometer.
- OpenTelemetry.
- Application-specific audit logging.

The core should expose a small lifecycle model. Metrics and tracing dependencies should stay in optional modules.

### 6. Optional Integration Modules

Keep Berlioz core small, but provide integration points for applications that already use other Java frameworks.

Possible modules:

- Spring integration for resolving generators from an `ApplicationContext`.
- Spring integration for using `ConversionService` or `Validator`.
- Spring Security integration for evaluating authorization requirements where applications already use it.
- Jakarta CDI integration for resolving generators as CDI beans.
- Optional annotation-based routing, metadata, or authorization module.
- Optional adapters for metrics and tracing.

These integrations should support Berlioz rather than replace its URI template, generator, and XSLT model.

### 7. Jakarta Servlet Support

Move Berlioz to the Jakarta Servlet namespace for modern servlet containers when the application migration window is clear.

This migration remains intentionally deferred while many existing applications remain on `javax.servlet`. The `javax` line should continue as the active release until a migration plan is agreed.

Topics to resolve:

- Whether Jakarta support should be a breaking major release.
- Whether to maintain a legacy `javax.servlet` line for existing applications.
- Whether servlet-specific code should be isolated enough to support parallel `javax` and `jakarta` artifacts.
- How the mock and kickstart modules should track the migration.

Likely outcome:

- A Jakarta-based Berlioz line for current containers.
- Clear migration notes for applications moving from `javax.servlet` to `jakarta.servlet`.
- A defined maintenance policy for the `javax.servlet` line.

## Possible Milestones

### Milestone 1: 0.13.2 Release Polish

- Finalize 0.13.2 release notes.
- Align Javadoc `@since` and `@version` values for new APIs.
- Document new generator interfaces with examples.
- Document `<namespace>`, `<handler>`, `patch`, and redirect allowlists.
- Verify source compatibility for existing `ContentGenerator` applications.
- Confirm which `RawGenerator` behavior is intentionally future-facing.

### Milestone 2: Problem Response Completion

- Wire `ProblemDetails` into framework-generated errors.
- Define negotiated problem response rendering for JSON, XML, and HTML.
- Add tests for `400`, `404`, `405`, `406`, and generator failure behavior.
- Decide immediate versus collected parameter validation failures.
- Define problem type URI conventions.

### Milestone 3: Metadata And Diagnostics

- Add supported-output metadata to source or diagnostic output.
- Expose direct handler and generator capability information.
- Improve service registry diagnostic warnings.
- Add tests for metadata stability.

### Milestone 4: Raw Output

- Implement servlet dispatch for `RawGenerator`.
- Define raw content type and cache behavior.
- Add tests for raw response status, headers, ETags, and body writing.
- Document raw output constraints.

### Milestone 5: Authorization And Interceptors

- Define a small authorization result model.
- Add programmatic guard support for services or generators.
- Define how authorization failures map to Problem Details.
- Decide whether interceptor hooks belong in core or an optional module.
- Add tests for `401` and `403` early-return behavior.

### Milestone 6: Integration And Instrumentation

- Add optional Spring or CDI generator resolution.
- Add optional Spring Security or application authorization adapters.
- Add optional metrics/tracing integration.
- Keep integration modules separate from the core runtime.

### Milestone 7: Jakarta Migration

- Decide final Jakarta migration strategy.
- Release a Jakarta-based line with migration notes.
- Determine whether the `javax.servlet` line continues as maintenance-only.

## Open Questions

- Should the Jakarta migration be released as Berlioz 1.0?
- Should the `javax.servlet` line become maintenance-only immediately, or remain active until a specific application migration threshold is reached?
- Should framework-generated Problem Details use public Berlioz documentation URIs?
- Should invalid parameter handling fail on the first invalid value or collect all parameter errors?
- Should service-level metadata be part of normal output, diagnostic output, or both?
- Should direct services support only one handler forever, or is there a future aggregation model?
- How should raw output interact with cache headers, ETags, and content negotiation?
- Should authorization requirements be declared primarily by generator interfaces, service configuration, or both?
- What minimum user/principal abstraction should Berlioz expose without taking ownership of authentication?
- Should authorization checks run at service level, generator level, or both?
- Should interceptor hooks be added to core, or should they wait for optional observability modules?
- Which integrations are valuable enough to maintain as official modules?
