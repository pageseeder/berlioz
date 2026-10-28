# Berlioz Roadmap

Berlioz is a lightweight Java web framework built around URI templates, generator-based content, XML, JSON, and XSLT rendering.

The 0.13.x releases delivered the complete roadmap for that cycle: typed request parameters, safer XML parsing and XSLT caching, output-aware generators, direct JSON responses, explicit response values, RFC 9457 Problem Details, stronger redirect/filesystem protections, and the full configurable error handling pipeline. **0.13.5 was released on 2026-07-03 as the final 0.13.x release.**

**0.14.0 was released on 2026-07-21.** It moved the framework to secure defaults, closed administration controls by default, strengthened the XSLT error pipeline, and modernized the built-in diagnostic generators.

**0.14.1 was released on 2026-07-25.** It delivered the runtime service-diagnostics model, first-class HTTP `QUERY` support, and hardened Content-Type/header handling across the servlet layer.

**0.14.2 was released on 2026-09-04 as a hotfix.** It prevented unintended session creation and updated dependencies. The hotfix shifted the planned overlay release to 0.14.3 and configuration validation to 0.14.4.

**0.14.3 was released on 2026-10-16.** It delivered classpath overlay discovery, classpath XSLT resolution, transactional service loading, and deployed servlet diagnostics.

**0.14.4 covers small features and updates**, starting with stub-free group stylesheet overlays. 

**0.14.5 focuses on small security improvements, the scoped `berlioz-reload`, and feedback from Berlioz Admin.** Configuration requirements and validation, previously planned for 0.14.5, are deferred to a later 0.14.x release; its implementation plan is in [`PLAN-0.14.5.md`](PLAN-0.14.5.md).

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

## Release Plan

### 0.14.0: Secure Defaults ✓ Released 2026-07-21

0.14.0 was the secure-defaults migration release. It retained migration escape hatches while moving out-of-the-box behavior to the production-oriented side.

Completed release changes:

- Made RFC 9457 Problem Details, minimal error detail, no GET-via-POST fallback, and XML 1.0 headers the defaults.
- Closed administration/control parameters by default and added explicit authorization through a control key, network policy, or delegated request attribute.
- Deprecated legacy non-Problem-Details error XML while retaining the unified `<error http-class="...">` migration format.
- Routed XSLT failures through the standard HTTP 500 error pipeline and added sensitivity-aware XSLT diagnostics.
- Modernized built-in system and diagnostic generators, including direct JSON output where supported.
- Improved response efficiency and correctness with configurable gzip thresholds and UTF-8-aware content lengths.

### 0.14.1: Runtime Introspection And HTTP QUERY ✓ Released 2026-07-25

0.14.1 delivered the runtime diagnostics model, core HTTP `QUERY` dispatch, and the smaller output/error contracts. The overlay and configuration-lifecycle work stays deferred to 0.14.3/0.14.5.

Completed:

- Added the explicit supported-output set to every generator entry in `Service.writeTo()`, including custom `BerliozGenerator` implementations whose capabilities cannot be inferred from the type label alone.
- Migrated service metadata serialization from XML-only `Service.toXml()` to format-agnostic `Service.writeTo(OutputWriter, ...)`, and converted `GetLiveServices` and `GetMatchingService` to `Generator` implementations with equivalent, stable XML and JSON shapes (plural JSON array keys: `generators`, `parameters`, `urls`).
- Fixed a pre-existing bug where the service `cache-control` diagnostic attribute reported the configured string's length instead of its value.
- Gave `GetMatchingService` a single stable root shape (`matched` boolean field) instead of two different root elements for the found/not-found cases, so JSON output is unambiguous.
- Kept `GetServices` as the raw source-configuration view; runtime-derived capabilities remain confined to the live diagnostic generators.
- Extracted the disjoint-output-set and invalid-direct-service warning conditions into testable static methods (`Service.disjointOutputWarning()`, `Service.invalidDirectWarning()`), and extended the invalid-direct check to also reject a direct service whose single generator supports no output format (previously registered silently).
- Improved the service-registry override warning (`ServiceRegistry.overrideWarning()`) to name both the replaced and replacing service, not just the replaced one.
- Added test coverage that didn't exist before: per-generator/service supported-output schema tests (XML and JSON), duplicate-pattern-within-file registration behavior, and registry-level mapping-override behavior.
- Added `QUERY` as a first-class mappable `HttpMethod`, so `ServiceRegistry`, `ServicesHandler10` (`method="query"` in `services.xml`, DTD updated), `GetLiveServices`, and `GetMatchingService` all support it with no special-casing beyond the enum constant; `BerliozServlet`'s generic `service()` dispatch already routes it to the same `process()` path as the other methods since there is no `HttpServlet.doQuery()` to override.
- Treated `QUERY` as safe/idempotent alongside `GET`/`HEAD` in `BerliozServlet.handleNoMatch()`: an unmatched `QUERY` request always gets a plain `404` rather than probing other registered methods for a `405`.
- Excluded `QUERY` from ETag/conditional-request caching in both `processJson()` and `processXml()` pending a body-aware cache key design. While fixing this, found and fixed a pre-existing bug in `processXml()`: for any non-GET/HEAD method against a cacheable service, the `cacheable` flag never got reset to `false` when the ETag block was skipped, so neither cache headers nor the `no-cache` fallback were written; `processJson()` did not have this bug. Both methods now compute `cacheable` (including the method check) up front.
- Added test coverage for `QUERY` dispatch/matching, the safe-method 404-vs-405 behavior, `Allow`/`OPTIONS` including `QUERY`, and the no-ETag/no-cache behavior for `QUERY` against a cacheable service.
- Added `application/x-www-form-urlencoded` body-to-parameter emulation for `QUERY` (new `QueryBodyParameters` class) so generators can read `QUERY` payloads through the normal `Request` parameter API. Unconditional rather than a `BerliozOption`: since `QUERY` dispatch is new, no existing generator can depend on the body being left alone, and the engine-detection check (comparing `getParameterMap()` against a plain parse of the URL query string) is itself the cheap, self-limiting guard — the class becomes a no-op the moment a container adds native `QUERY` support, with no configuration to revisit.
- Aligned dispatch with RFC 10008's media-type requirements: a matched `QUERY` request without `Content-Type` now receives `400 Bad Request` before generator invocation, malformed form or URI-query encoding receives `400`, and an emulated form body over the one-megabyte buffer limit or 1,000 field occurrences receives `413 Payload Too Large`. The form decoder scans fields incrementally without `String.split()` or an intermediate multi-value map, and caps its initial buffer allocation independently of the untrusted `Content-Length`. Unknown routes continue to receive `404`; support for a present media type remains resource-specific and is validated by the generator.
- Passed the resolved expected media type to the error pipeline instead of relying only on URL-extension inference: `BerliozServlet` now records the matched servlet mapping's configured `BerliozConfig.getMediaType()` onto the request as `ErrorHandlerServlet.BERLIOZ_ERROR_MEDIA_TYPE` (via the shared `prepareErrorAttributes()`) before forwarding or handling an error, and `ErrorHandlerServlet.handle()` prefers that attribute over guessing from the URL extension. When an exception escapes before the attribute can be set, the handler uses the standard originating servlet-name attribute to retrieve the exact `BerliozServlet` registration and its configured content type; extension inference remains the fallback for non-Berlioz errors. Default `.auto` forwarding is likewise derived from the extension mappings of deployed Berlioz servlets, so the standard `.json` and `.src` mappings and application-defined formats are recognized without a hard-coded list; an explicit `forward-extensions` parameter remains authoritative. Also added the JSON branch that `ErrorHandlerServlet` was missing entirely: a resolved or inferred JSON expectation now always emits `application/problem+json`, unconditionally — there was never a legacy JSON representation to fall back to, so the deprecated `berlioz.errors.problem=false` escape hatch (which only restores the legacy XML/HTML output) does not apply to JSON. Aligned `BerliozServlet`'s direct-error `application/problem+json` shortcut with the same rule for consistency.
- Modernized static-asset error suppression and media-type lookup for common web formats including WebP, AVIF, APNG, HEIC/HEIF, JPEG XL, WOFF/WOFF2, WebAssembly, ES modules, web manifests, WebM, FLAC, Opus, and M4A. Unknown non-asset document extensions now receive the useful HTML failsafe page rather than raw XML; `.xml` and `.src` retain the raw XML response.
- Kept Problem Details body-only and added `HttpException.header(name, value)`/`headers()` as the transport for response headers such as `Retry-After` that belong on the response itself rather than the RFC 9457 body. Threaded through all three paths that can turn an `HttpException` into a response: `GeneratorFailure` now folds the exception's headers onto the `Response` it builds (reaching the client through the existing `Response.header()`/`GeneratorDispatch.accumulateHeaders()` mechanism generators already use), and both `BerliozServlet.writeProblemJson()` and `ErrorHandlerServlet`'s `writeResponse()` apply them directly for the framework's own synthetic error paths. Added `HttpException.findIn(Throwable)` to recover the signal even after it has been wrapped in a `BerliozException` on the way to a servlet-level handler, so headers survive that indirection too. Extracted the header name/value validation `Response.header()` already had into `HttpResponses.isValidHeaderName()`/`isValidHeaderValue()` so `HttpException.header()` enforces the same CRLF-injection guard rather than duplicating it.
- Added a `ContentType` class (`org.pageseeder.berlioz.http`) for RFC-conformant `Content-Type` parsing and parameter/charset handling, replacing ad hoc string splitting in `BerliozConfig`, `ErrorHandlerServlet`, and `QueryBodyParameters` with one shared, tested implementation.
- Unified response-header handling behind `Response.setHeaders()`/`HttpResponses.headersIn()`, removing duplicated header-copying logic between `BerliozServlet`, `ErrorHandlerServlet`, and `GeneratorFailure`.
- Added a warnings pass for misconfigured service registrations, surfaced through `GetServices` alongside the existing source-configuration view.
- Limited exception cause-chain traversal to 100 levels in `HttpException` and `XsltTransformException`, guarding against cyclic or pathologically deep cause chains during error rendering.

Retained decision (not a task): preserve the XML/XSLT fallback for JSON-configured requests throughout 0.14.x; revisit only with content negotiation in 1.0.

### 0.14.2: Session Creation Hotfix ✓ Released 2026-09-04

Completed release changes:

- Used `getSession(false)` to prevent unintended session creation in request wrappers.
- Updated Jackson to 2.22.2 and JSpecify to 1.0.1.

### 0.14.3: Classpath Overlay Discovery ✓ Released 2026-10-16

0.14.3 delivered reusable overlays as ordinary JAR dependencies. Release verification covered `./gradlew test`, the packaged `overlayIntegrationTest`, `./gradlew build`, and Javadoc generation under the Java 11 toolchain across `pso-berlioz`, `pso-berlioz-mock`, `pso-berlioz-kickstart`, and `samples:minimal-gretty`. The released version is recorded in `version.txt` and tagged `v0.14.3`.

Completed release changes:

- Discovered exactly one addressable `META-INF/berlioz/services.xml` resource per contributing JAR via `getResources(...)` on the web application's context classloader (falling back to the classloader that loaded Berlioz), including exploded development classpath directories; an explicit index is reserved for a future multiple-document-per-JAR need.
- Enabled discovery by default: adding the overlay dependency is the only application action required.
- Loaded classpath service sources before filesystem sources, so filesystem mappings deterministically take final precedence; same-pattern declarations with different HTTP methods never conflict.
- Defined conflicts by HTTP method and normalized URI pattern, with override warnings naming both the replaced and replacing declaration and their safe origins (contributing JAR/resource path, or filesystem path relative to the configuration directory).
- Made service loading transactional: each `ServiceSource` parses into an isolated candidate, candidates merge in source order, and the complete result publishes atomically through `ServiceRegistry`. A malformed or unreachable source rejects the whole candidate; a failed *reload* keeps serving the last successfully published registry, including its warnings, source metadata, version, and content ETags, with retries after a 30-second delay (or on demand).
- Generalized `ServiceLoader` from `File` inputs to `ServiceSource`/`ServiceOrigin` descriptors carrying a URL and safe, loggable origin metadata; `load(File)` and `listServiceFiles()` remain source-compatible.
- Loaded overlay generator classes through the resolved application classloader instead of `Class.forName(...)`, so overlay-owned generators resolve correctly even from a child web-application classloader.
- Added format-agnostic `GetBerliozConfig` and `GetErrorHandlerConfig` diagnostic generators reporting deployed servlet configuration — mappings, media type/charset, stylesheet/fallback allocation (`identity`/`global`/`group`/`service`), cache policy, compression, and XSLT cache mode for Berlioz servlets; mappings, Problem Details mode, detail level, forward-extension source/list, ignored extensions, and custom/built-in/none stylesheet selection for the error handler — with stable, equivalent, allowlisted XML/JSON schemas and request-scoped, content-derived SHA-256 ETags.
- Added `classpath:` as the canonical primary/fallback XSLT prefix, retaining `resource:` as a **deprecated** compatibility alias (both accept an optional leading slash, resolved identically); `resource:` is scheduled for removal at 1.0.
- Added a `StylesheetLocation` abstraction shared by `BerliozConfig`, `XsltTransformer`, and `XsltTemplateCache`, and a constrained XSLT `URIResolver` allowing relative `xsl:import`/`xsl:include` within the same JAR plus explicit cross-artifact `classpath:`/`resource:` references, while continuing to deny every network protocol.
- Treated classpath/JAR-backed templates as immutable under `manual` and `auto` cache modes (recompiled only under `no`); cache keys and ETags distinguish identically named resources contributed by different JARs or application classloaders.
- Verified the complete deployment model — discovery, precedence/conflict warnings, classpath XSLT imports/includes, static resources under `META-INF/resources/`, `web-fragment.xml` servlet/filter registration, and both new diagnostics — with a real packaged overlay JAR deployed into a Jetty 10 container (`tests/overlay-fixture`, `tests/overlay-deployment`, `./gradlew overlayIntegrationTest`).
- Renamed `GetLiveServices` to `ListRuntimeServices`, retaining `GetLiveServices` as a `@Deprecated` forwarding alias with identical behavior and output until removal at 1.0.
- Documented overlay packaging and precedence, classpath/filesystem XSLT resolution and cache-mode behavior, and migration notes for existing filesystem-only applications in `README.md`.

### 0.14.4: Small Features And Updates

Implemented: filesystem group stylesheet fallback to `META-INF/berlioz/xslt/` overlays,
enabled by default only for absent files. Filesystem entries take precedence, including errors.
Classpath lookup preserves JAR import bases and uses application classloader precedence.
Source transitions invalidate cached selections and expose the selected origin through
`XsltTransformer.location()`. Acceptance tests cover missing resources, valid/invalid/unreadable
overrides, nested imports, duplicate resources, and all cache modes. `./gradlew test` and
`./gradlew build publish` passed. Minimum version for stub-free overlays: **0.14.4**.
Cobble's classpath documentation support remains separate.

### 0.14.5: Security Improvements, Scoped Reload And Admin Feedback — Next Planned Release

0.14.5 should stay small and additive: close the remaining low-risk security gaps, replace the separate reload parameters with a scoped `berlioz-reload`, and deliver what Berlioz Admin (bzadmin) needs to drop its workarounds. Configuration requirements and validation move to a later 0.14.x release (see below).

Small security improvements:

- ✓ Redact and sort `GetSystemProperties` output (Theme 18, item 1): implemented; now a `Generator` with XML/JSON output, values redacted via `Redaction.redact`, sorted by name.
- ✓ Check `Forwarded` and `X-Real-IP` like `X-Forwarded-For` in the control network channel, and expose the check publicly (Theme 18, item 7): implemented as `ControlNetwork.matches(HttpServletRequest)`, used by `ControlAuthorization.hasControl`.
- Minimal default XML exposure: rewrite the kickstart `fallback-xml.xsl` as an allowlist and restrict the `*.src` mapping (Theme 15).

Scoped `berlioz-reload` (Theme 17):

- Accept a comma-separated list of scopes (`config`, `services`, `xslt`, `etags`, possibly `redirects`); `true` keeps meaning everything.
- Keep `reload-services`, `clear-xsl-cache` and `reset-etags` as deprecated aliases of their scope until 1.0.
- Settle the open points listed in Theme 17 (scope order, unknown scopes, implied scopes, configuration failures, filters, reporting) before implementation.

Berlioz Admin feedback:

- Admin diagnostics, items 1–7 (Theme 16): configuration sources, load diagnostics, service source and overrides, value provenance, option catalog, `GetBerliozConfig` completeness, and a public secure XSLT resolver.
- Admin overlay upstreaming, items 2–6 and 8–9 (Theme 18): non-blocking CPU sample, bounded thread diagnostics, JSON generator statistics, public control parameters and reload scopes, `BerliozOption` metadata, XSLT processor and container identity, and stylesheet listing.

Items that turn out larger than expected can move to a later 0.14.x release without holding back the security fixes.

### Later 0.14.x: Configuration Requirements And Validation — Deferred From 0.14.5

This release should add application-facing configuration validation with explicit property provenance and safe diagnostics. The implementation sequence, proposed XML contract, lifecycle rules, and acceptance criteria are in [`PLAN-0.14.5.md`](PLAN-0.14.5.md), written when this work targeted 0.14.5; implementation has not started.

Planned release changes:

- Read requirements from a separate `WEB-INF/config/config-requirements.xml` file after variable resolution.
- Preserve whether each resolved property was explicitly specified or inherited from a framework default.
- Support exact and simple wildcard property matching and presence checks for `defined`, `specified`, and `has-value`.
- Treat zero wildcard matches as a violation only when the requirement's presence rule requires a match.
- Implement built-in constraints for boolean, integer, number, regex, enum, URL, URI, port, hostname, and path values.
- Add a validation report model with severity, requirement identity, matched property name, and safe messages that omit raw values by default.
- Default to report-and-continue behavior, with an option to fail startup or reload on error-level violations.
- Validate reload candidates before publication so a failed reload retains the last valid configuration.
- Add an opt-in XML/JSON diagnostic generator and tests for matching, presence, constraints, provenance, reload, fail-fast behavior, and redaction.

After configuration validation ships, assess readiness for a 1.0.0 release and confirm which remaining core capabilities below are required for 1.0.0 versus later 1.0.x releases.

Reusable format-neutral output snapshots (Theme 14) are also planned; the target release remains to be decided.

A stable `ListRuntimeServices` (Theme 16, item 8) and the service-level authorisation hook (Theme 18, item 10) remain planned for 1.0.

### 1.0.x: Roadmap Themes 6-9

The 1.0.x cycle should complete the remaining core runtime capabilities without expanding Berlioz into a broad application framework.

Theme 6, Raw Output:

- Implement servlet dispatch for `RawGenerator`.
- Define raw content type and cache behavior.
- Add tests for raw response status, headers, ETags, and body writing.
- Document raw output constraints.

Theme 7, Content Negotiation:

- Support `Accept` header content negotiation.
- Use `406 Not Acceptable` when the requested format is not supported.
- Define interaction with extension-based format selection.

Theme 8, Authentication And Authorization Guards:

- Define a small authorization result model.
- Add programmatic guard support for services or generators.
- Define how authorization failures map to Problem Details.
- Add tests for `401` and `403` early-return behavior.

Theme 9, Interceptors And Observability:

- Decide whether interceptor hooks belong in core or should wait for optional observability modules.
- Add lifecycle hooks only where they support clear core use cases.
- Keep metrics and tracing dependencies in optional modules.

### 1.1+: Roadmap Themes 10-12

The 1.1 and later line should focus on optional integrations, additional output formats, and platform migration work.

Theme 10, Optional Integration Modules:

- Add optional Spring or CDI generator resolution.
- Add optional Spring Security or application authorization adapters.
- Add optional metrics/tracing integration.
- Keep integration modules separate from the core runtime.

Theme 11, Binary Serialization Module:

- Evaluate MessagePack as the first optional binary serialization module.
- Defer schema-based formats such as Protocol Buffers, Avro, and FlatBuffers until the generator contract is clearer.

Theme 12, Jakarta Servlet Support:

- Decide final Jakarta migration strategy.
- Release a Jakarta-based line with migration notes when the application migration window is clear.
- Determine whether the `javax.servlet` line continues as maintenance-only.

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

### 0.13.3–0.13.5 Problem Response Integration

The 0.13.3–0.13.5 cycle completed RFC 9457 Problem Details integration across the request pipeline:

- Added `HttpException` as the base class for generator-level HTTP short-circuiting (400–599); stack trace capture suppressed so it can be thrown on every bad request without cost.
- Added `InvalidParameterException` (400 Bad Request) with `parameter` and `reason` extensions; immediate-fail semantics — one exception per request, no collector.
- Added `UpstreamException` (502 Bad Gateway) with optional `upstream-service` extension.
- Added `Problems` factory producing typed `ProblemDetails` from all framework exception types.
- `XmlResponse` and `JsonResponse` both catch all `HttpException` subtypes and map them to `Response.problem(...)`.
- For direct services, problem responses are promoted to top-level with `application/problem+json` or `application/problem+xml` content type.
- For envelope services, generator problems are embedded inline; the final HTTP status is governed by the service's response-code rule, supporting partial-failure semantics.
- Moved problem APIs into `org.pageseeder.berlioz.error`: `ProblemDetails`, `InvalidParameterException`, and `UpstreamException`.
- Added `ProblemExtension` for structured problem extension members and `ExceptionDetail` for optional exception metadata.
- Added support for custom problem HTTP status codes through `ProblemDetails.of(int)`, `Response.problem(...)`, and `GeneratorOutcome`.
- Added `ERROR_PROBLEM_FORMAT` option to opt in to RFC 9457 `<problem>` XML in `ErrorHandlerServlet` (default: legacy format).
- Added `ERROR_DETAIL` option (`minimal` / `standard` / `full`) controlling diagnostic verbosity in error responses; at `standard` or `full`, exception details are added to RFC 9457 problem responses as an `exception` extension member.
- Updated failsafe XSLT with a `<problem>` template for consistent HTML rendering of problem responses.
- Problem type URIs: `urn:berlioz:problem:*` reserved for Berlioz-internal types; application developers own their own scheme.

### 0.13.5 Error Handling Pipeline

The 0.13.5 cycle completed the remaining configurable error handling options:

- Added `ERROR_STYLESHEET` option (`berlioz.errors.stylesheet`) — a path relative to `WEB-INF/` for a custom error XSLT; `ErrorHandlerServlet` tries the custom file first, falls back to the built-in failsafe, then falls back to raw XML with an appropriate content type.
- Decided: the custom error stylesheet is a single global template (not per-group), consistent with how error handling bypasses the normal XSLT resolution path.
- Added sensitive-value redaction for full-detail HTTP headers and parameters in `ErrorHandlerServlet`.
- Added XSLT 2.0 runtime detection and a static diagnostic page for environments missing an XSLT 2.0 processor.
- Added direct JSON problem output for JSON-configured servlet errors when Berlioz handles the error.
- Added per-generator `Server-Timing` metrics for direct JSON services, matching the XML service path.
- Added test coverage for stylesheet resolution: default failsafe, non-existent path fallback, custom file, and end-to-end `handle()` with custom output.

### 0.13.5: Final 0.13 Release ✓ Released 2026-07-03

0.13.5 was the final 0.13.x release. It consolidated the 0.13 cycle and completed the Problem Details and error handling pipeline while keeping compatibility-oriented defaults.

Release posture:

- Keep legacy Berlioz error XML as the default for `ErrorHandlerServlet`.
- Keep RFC 9457 Problem Details available through `berlioz.errors.problem=true`.
- Keep detailed error output available for development, but support `berlioz.errors.detail=minimal` for production.
- Preserve source compatibility where feasible for existing `ContentGenerator` applications.

### 0.14.0: Secure Defaults And Diagnostics ✓ Released 2026-07-21

The 0.14.0 release completed the secure-defaults migration and expanded the framework's production diagnostics:

- Made RFC 9457 Problem Details the default framework error format and minimal error detail the default exposure level.
- Disabled GET-via-POST fallback, made XML 1.0 headers the default, and added deprecation warnings for compatibility settings.
- Closed request-level administration controls by default and centralized authorization across key, network, and delegated channels.
- Unified legacy error XML under `<error>`, deprecated the legacy mode, and routed XSLT failures through the standard error pipeline.
- Added structured, sensitivity-aware XSLT diagnostics and modernized built-in system generators with direct JSON support.
- Added configurable gzip thresholds, corrected UTF-8 content-length handling, and completed several thread-safety and utility cleanups.
- Preserved migration paths for applications that still require the former defaults.

## Current Development Themes

### 2. Error Handling Pipeline ✓ Completed In 0.14.0

The error handling pipeline is complete. Applications can control diagnostic verbosity, use RFC 9457 Problem Details, supply a custom error stylesheet, and the built-in failsafe template renders cleanly across all error types.

Done:

- `BerliozErrorID` classifies errors semantically (transform not found, invalid, dynamic error, malformed source XML, etc.).
- `XsltErrorCollector` collects warnings, errors, and fatals during XSLT processing.
- `XsltTransformer` catches both `TransformerConfigurationException` (static) and `TransformerException` (dynamic) and falls back to the failsafe template, with distinct `BerliozErrorID` values for each.
- `ERROR_DETAIL` option controls diagnostic verbosity in both legacy XML and RFC 9457 problem responses.
- `ERROR_PROBLEM_FORMAT` makes `ErrorHandlerServlet` emit RFC 9457 `<problem>` XML by default. The deprecated legacy format remains available as a temporary migration option.
- `ERROR_STYLESHEET` option lets applications supply a custom error XSLT, with automatic fallback to the built-in failsafe.
- Modernized failsafe template: CSS variables, dark-mode support, responsive layout, collapsible stack traces via `<details>`/`<summary>`, structured `<exception>` rendering inside Problem Details responses, and `http-headers`/`http-parameters` diagnostic blocks in full-detail mode.
- Sensitive values in diagnostic request headers and parameters are redacted before serialization.
- Runtime XSLT 2.0 probing reports a clear static diagnostic when no XSLT 2.0 processor is available.

The result is predictable, configurable error presentation that helps developers during development while protecting production environments from information leakage. Problem Details became the default in 0.14.0 and the legacy non-Problem-Details format is now deprecated. It should be removed only after a clear migration window; until then it uses the unified `<error>` element rather than the older `<server-error>` / `<client-error>` names.

### 3. Service Metadata And Diagnostics ✓ Completed In 0.14.1

Service inspection is becoming more important now that services can expose different output formats and response modes.

Already done:

- `Service.writeTo(OutputWriter, ...)` exposes service identifier, group, method, URI templates, direct mode, cacheability, cache policy, response-code rule, and both the service's supported-output intersection and each generator's own explicit supported-output set (including custom `BerliozGenerator` implementations, whose capabilities cannot be inferred from the type label alone).
- Generator entries expose class, name, target, type, cacheability, status participation, and configured parameters.
- `GetLiveServices` and `GetMatchingService` are format-agnostic `Generator` implementations that serialize the effective in-memory registry with equivalent, stable XML and JSON shapes; `GetServices` deliberately continues to copy the source configuration and carries no runtime-derived attributes.
- `GetMatchingService` reports a single stable root shape (`matched` boolean) rather than two different root elements for the found/not-found cases, keeping its JSON output unambiguous.
- Diagnostic warnings for disjoint output sets and invalid direct services are computed by testable static methods (`Service.disjointOutputWarning()`, `Service.invalidDirectWarning()`); the invalid-direct check also rejects a direct service whose single generator supports no output format. The registry override warning (`ServiceRegistry.overrideWarning()`) now names both the replaced and replacing service.
- Stable-schema XML/JSON tests exist for `Service.writeTo()`, `GetLiveServices`, and `GetMatchingService`, plus registration-behavior tests for duplicate patterns within a file and mapping overrides across registrations.

This makes services easier to inspect, document, test, and debug without adding metadata to every normal response or introducing heavy runtime machinery.

### 4. Classpath Overlay Discovery ✓ Completed In 0.14.3

The 0.14.0–0.14.2 lines required service configuration and primary XSLT templates on the filesystem under `WEB-INF/`, so reusable admin or utility overlays had to be distributed as WAR overlays — ZIP archives merged into the host application at build time. 0.14.3 allows a self-contained overlay — XSLT templates, service configuration, static assets via `META-INF/resources/`, and servlet components via `web-fragment.xml` — to be distributed and consumed as a single ordinary JAR dependency, with no filesystem merge step.

Existing foundations this built on:

- `BerliozConfig.toURL()` already supported a `resource:` prefix for loading fallback and kickstart XSLT templates from the classpath.
- The failsafe error stylesheet was already loaded from the classpath via `ClassLoader.getResource()`.
- `web-fragment.xml` in `pso-berlioz-kickstart` already registered servlet mappings without requiring changes to the host application's `web.xml`.
- `java.util.ServiceLoader` was already used for `RedirectPolicy` discovery via `META-INF/services/`, establishing a workable SPI pattern.

Done: classpath `META-INF/berlioz/services.xml` discovery with filesystem precedence and origin-aware conflict reporting; transactional candidate parsing and atomic registry publication with reload-failure retention; `classpath:` XSLT resolution (with `resource:` kept as a deprecated alias until 1.0) with immutable classpath caching and a secure import/include resolver; the `GetBerliozConfig`/`GetErrorHandlerConfig` diagnostic generators; the `ListRuntimeServices` rename (`GetLiveServices` kept as a deprecated alias); and full verification against a real packaged overlay JAR in a Jetty container. See the **0.14.3: Classpath Overlay Discovery** entry under Release Plan above for the complete list of released changes and the Open Questions section for resolved decisions.

### 5. Configuration Requirements And Validation

Many Berlioz applications depend on application-specific global properties being present and well-formed. Berlioz should be able to validate those requirements when global configuration is loaded, report violations consistently, and expose the result through a diagnostic generator.

Already done (groundwork):

- `OptionDeprecations` (package-private) holds the deprecated-value checks for the three 0.14.0 deprecated options: `berlioz.http.get-via-post=true`, `berlioz.errors.problem=false`, and `berlioz.xml.header.version=0.9`. `GlobalSettings.load()` calls it once after a successful load or reload; no warning is ever emitted per request.
- `BerliozOption` is unchanged — deprecation metadata does not live on the enum.
- `OptionDeprecations` is the internal precursor to Theme 5's `ConfigurationValidator`: the check-and-warn pattern it establishes will generalize into the declarative, application-facing validation layer.

Planned shape for a later 0.14.x release (deferred from 0.14.5):

- Add a separate declarative `WEB-INF/config/config-requirements.xml` file, evaluated after global properties are fully resolved.
- Retain property provenance so validation can distinguish an explicitly specified value from a framework default.
- Match requirements by property name, including simple wildcard patterns such as `test.*.url`.
- Distinguish presence checks:
  - `defined`: the resolved property key exists.
  - `specified`: the application supplied the property rather than only inheriting a framework default.
  - `has-value`: the resolved value is non-null and non-blank after trimming.
- Treat zero wildcard matches as a violation only when the applicable presence rule requires at least one match.
- Support built-in value constraints for booleans, integers, numbers, regular expressions, enumerated values, URLs, URIs, ports, hostnames, and paths.
- Record violations with severity (`error`, `warning`, `info`) and enough metadata to identify the requirement, matched property, and failing constraint.
- Omit raw values from diagnostic output by default, especially for secret-like keys.
- Use report-and-continue behavior by default, with an option to fail application startup or configuration reload when `error` violations are present.
- Validate a reload candidate before publishing it; failed validation retains the last valid configuration.

Potential core model:

- `ConfigurationRequirement`
- `ConfigurationConstraint`
- `ConfigurationViolation`
- `ConfigurationValidationReport`
- `ConfigurationValidator`

The first implementation should keep the constraint language small and declarative. A later extension point, such as `ServiceLoader<ConfigurationConstraintProvider>`, can allow applications or modules to contribute custom constraints without turning the core into a general validation framework.

The diagnostic generator should emit the current validation report as XML, with JSON available through the normal output pipeline. It should be opt-in or intended for admin-only services.

### 13. HTTP QUERY Method Support ✓ Completed In 0.14.1

Several servlet containers are adding support for the HTTP `QUERY` method (a safe, idempotent method with a request body, intended to replace GET-with-oversized-query-string and ad-hoc POST-as-GET tunneling). Berlioz supports `QUERY` as a first-class mappable method.

Done:

- Added `QUERY` to `HttpMethod` as mappable (`HttpMethod.java`), alongside `GET`, `POST`, `PUT`, `PATCH`, `DELETE`; `ServiceRegistry` and `ServicesHandler10` were already keyed off `HttpMethod` generically, so registering and matching `QUERY` services required no structural change beyond the enum constant (plus allowing `query` in the `services-1.0.dtd` `method` attribute).
- In `BerliozServlet`, `QUERY` is treated as safe/idempotent for `405`-vs-`404` handling (`handleNoMatch`): an unmatched `QUERY` request always gets a plain `404` rather than probing other registered methods for a `405`, the same treatment as `GET`/`HEAD`. The current Servlet 4.0 API has no `HttpServlet.doQuery()` method to override; Berlioz's generic `service()` dispatch already routes `QUERY` through the same `process()` path as the other methods.
- Did **not** extend the existing `GET`/`HEAD` ETag caching path (`processJson`/`processXml`) to `QUERY`: the current ETag is derived from the URL alone, so two different `QUERY` bodies against the same URL would incorrectly share a cached response. Caching `QUERY` will require hashing the request body into the ETag seed, deferred until that design exists.
- Added test coverage for `QUERY` dispatch, matching, 404-vs-405 behavior, the `Allow`/`OPTIONS` headers, and the no-ETag/no-cache behavior against a cacheable service.
- Added `application/x-www-form-urlencoded` body-to-parameter emulation. `QueryBodyParameters` (in `org.pageseeder.berlioz.http`) checks whether the servlet engine already exposes body parameters through `getParameterMap()` — by comparing it against a plain parse of the URL query string alone — and only reads and parses the body itself when the engine clearly has not; a container with native `QUERY` support is left untouched. No `BerliozOption` gate: since `QUERY` dispatch is new in this release, no existing generator can be relying on the body being left alone, and the engine-detection check is itself the safety net, so an on/off switch would only add configuration surface without adding safety. Wired into `HttpRequestWrapper.toParameters()` (lowest precedence, below the URL query string and URI template variables) so the parsed values reach generators through the existing `Request`/`ContentRequest` parameter API with no new generator-facing surface.
- Enforced RFC 10008's request media-type requirement after routing and before generator invocation: matched `QUERY` requests without `Content-Type` receive `400`, malformed encoded form input receives `400`, and form bodies beyond the one-megabyte emulation limit or 1,000 field occurrences receive `413`. The indexed decoder avoids attacker-sized `split()` arrays and intermediate repeated-value lists, while body buffering rejects oversized declared lengths before reading and never preallocates from an untrusted large `Content-Length`. A present but resource-unsupported media type remains the generator's responsibility and should produce `415`.

Still deferred:

- A body-aware ETag/cache key design for `QUERY` — see above.

### 6. Finish Direct Output And Raw Output Support

Direct JSON and direct XML services are now part of the pipeline. `RawGenerator` exists as an API shape, but servlet dispatch support is still future work.

Next work:

- Add a dedicated servlet path for `RawGenerator`.
- Define content type and charset behavior for raw output.
- Decide how cache headers and ETags apply to raw responses.
- Keep the direct service rule simple: one handler, one complete response body.
- Document how `<handler>` differs from `<generator>`.

### 7. Content Negotiation

Berlioz currently determines the output format from the URL extension (`.xml`, `.json`, `.html`) via servlet mappings. Adding support for the `Accept` header would allow a single endpoint to serve multiple formats based on client preference.

Next work:

- Support `Accept` header content negotiation as an alternative to extension-based format selection.
- Use `406 Not Acceptable` when the requested format is not supported by the service.
- Decide how `Accept`-based negotiation interacts with extension-based format selection (precedence, override, fallback).
- Decide whether content negotiation should be opt-in per service or enabled globally.
- Revisit `BerliozServlet.process()`'s handling of a matched service that can't produce the requested output format (JSON unsupported and no XML+XSLT fallback available): it currently returns `404` rather than `406`, since 406 is conventionally tied to `Accept`-header negotiation and extension-based selection doesn't clearly fit that model yet.

### 8. Authentication And Authorization Guards

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

### 9. Interceptors And Observability

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

### 10. Optional Integration Modules

Keep Berlioz core small, but provide integration points for applications that already use other Java frameworks.

Possible modules:

- Spring integration for resolving generators from an `ApplicationContext`.
- Spring integration for using `ConversionService` or `Validator`.
- Spring Security integration for evaluating authorization requirements where applications already use it.
- Jakarta CDI integration for resolving generators as CDI beans.
- Optional annotation-based routing, metadata, or authorization module.
- Optional adapters for metrics and tracing.
- Binary serialization module for high-performance output formats (see §11).

These integrations should support Berlioz rather than replace its URI template, generator, and XSLT model.

### 11. Binary Serialization Module

Berlioz's generator hierarchy (`XmlGenerator`, `JsonGenerator`, `RawGenerator`) already supports multiple output formats dispatched by URL extension. A future optional module could extend this to high-performance binary serialization formats, following the same pattern as the existing `aeson` JSON adapters.

Recommended first candidate:

- **MessagePack** — schema-less and structurally similar to JSON, so the generator API can mirror `JsonGenerator` closely. A `pso-berlioz-msgpack` module would provide a `MessagePackGenerator` base class and wire the `*.msgpack` extension into servlet dispatch.

Other candidates to evaluate later:

- **Protocol Buffers** — requires pre-defined `.proto` schemas compiled to Java classes. Higher friction for users but strong ecosystem support and compact wire format.
- **Apache Avro** — schema-based with optional schema evolution. Natural fit for data interchange but heavier setup than MessagePack.
- **FlatBuffers** — zero-copy deserialization for performance-critical paths. Niche use case within a web framework.

Schema-based formats (Protobuf, Avro, FlatBuffers) would need a different generator contract where the user returns a pre-built typed message rather than writing fields dynamically, so they represent a larger API design decision.

No core changes are expected: the existing extension-based dispatch and generator hierarchy should accommodate binary formats without modification.

### 12. Jakarta Servlet Support

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

### 14. Reusable Format-Neutral Output Snapshots

Generators that parse and aggregate complex sources should be able to capture their output once and cache an immutable snapshot for reuse as either XML or JSON. This avoids repeating source processing or building an application-specific object model solely for serialization.

Proposed API:

- `OutputBuffer`: a mutable builder that captures structural output operations while sources are parsed and aggregated.
- `OutputFragment`: an immutable `OutputWritable` snapshot that replays those operations through the destination `OutputWriter`, using the existing XML and JSON adapters.

Next work:

- Preserve operation order, object/array boundaries, names, scalar types, nulls, multi-value fields, and `FieldOption`/`ContextOption` values. Apply format-specific options during replay.
- Copy mutable inputs and consume iterables during capture so completed fragments are safe to share across requests without retaining source parsers or mutable application objects.
- Validate balanced contexts, define finalization behavior, and document valid insertion contexts, including XML attribute ordering and whether a fragment represents one complete object or a sequence of children.
- Resolve capture-time format detection: the destination writer knows its format during replay, but a neutral buffer cannot choose both branches of Java code using `getType()`, `isXml()`, or `isJson()`. Evaluate a shared structural writing interface without format queries versus an `OutputWriter`-compatible buffer that rejects those queries explicitly.
- Keep cache keys, expiry, and invalidation application-controlled; one fragment should serve both XML and JSON without separate serialized cache entries.
- Verify equivalent direct and replayed XML/JSON output, snapshot isolation, concurrent replay, validation failures, and that replay does not close or flush the destination writer.
- Measure capture time, retained memory, and replay throughput before optimizing storage. Start with typed operations; defer a navigable tree API and serialized per-format caches until demonstrated needs justify them.

Target release: to be decided. Keep the implementation compatible with Java 11.

### 15. Minimal Default XML Exposure

The kickstart `fallback-xml.xsl` copies the source document verbatim (`<xsl:sequence select="."/>`), so every `*.xml` request for a group without its own `/xslt/xml/{GROUP}.xsl` publishes the full internal envelope. JSON is unaffected because `fallback-json.xsl` returns `{}`. The envelope discloses:

- Framework fingerprint and environment: `header/berlioz/@version` and `@mode`.
- Arbitrary application settings: `header/app` (every `berlioz.app.*` property).
- The CSP nonce, which is meaningful only in HTML: `header/security/@nonce`.
- Internal topology behind a reverse proxy, and reflected `Host` values: `header/location`, `header/path`, and the deprecated `host`, `port`, `scheme`, `url`, and `context-path` elements.
- Reflected request input: `header/http-parameters` and `header/uri-parameters`.
- Implementation details: `content/@generator` (fully qualified class names), `content/@etag` (internal cache keys), `content/@profile*` (timings, when `berlioz.profile` is enabled), `content/@deprecated`, and `root/@flags`.

Planned changes:

- Rewrite `fallback-xml.xsl` as an allowlist rather than a denylist, so future envelope additions stay private unless explicitly exposed: drop `<header>` entirely, keep `root/@service` and `@group`, keep `content/@name`, `@target`, and `@status`, and copy the generator payload unchanged. Error content is already governed by `berlioz.errors.detail`.
- Restrict the kickstart `*.src` mapping (`BerliozSource`, `stylesheet=IDENTITY`), which returns the raw envelope for every service with no mode, network, or key guard and is hard for applications to remove from a web fragment. Options: serve it only in `dev` mode, guard it with the existing `berlioz.control.network`/`berlioz.control.key` authorization, or remove it from kickstart and require an explicit opt-in mapping. Changing the `.xml` fallback alone leaves the same data reachable through `.src`.
- Document in `README.md` and the kickstart README that application `xml/{GROUP}.xsl` templates copying the whole source document expose the same data, and show a safe pattern.
- Add tests asserting the fallback output contains no `header`, `@generator`, `@etag`, `@profile*`, or `@flags`, and that `.src` is denied outside the permitted mode or authorization.
- Release-note the behavior change: clients reading `header/…` (for example URI parameters or the Berlioz version) from the default `.xml` output must use an application template instead.

Target release: 0.14.5.

### 16. Admin Diagnostics (Requested By Berlioz Admin)

Berlioz Admin (bzadmin) displays Berlioz's own diagnostic generators directly. It only ships an overlay generator where Berlioz has no equivalent. The items below let it drop those workarounds and show information that is currently marked "not available". Unless stated otherwise, each item only adds new elements or attributes to existing output; existing fields keep their meaning.

0.14.x (additive):

1. **Configuration sources in `ListRuntimeServices`**
   - List the service configuration sources in load order, after the services: `<source kind="classpath|filesystem" name="bzadmin-0.14.0-beta.jar!META-INF/berlioz/services.xml"/>` (from `ServiceLoader.discoverSources()`; JSON: `sources` array).
   - If discovery fails, write an element without details instead of failing the generator.
   - Acceptance: a host with an overlay JAR, `services.xml` and `services!x.xml` lists all three, classpath sources first.
2. **Load diagnostics in `ListRuntimeServices`**
   - The warnings of the last successful load (`getLastLoadWarnings()`): `<warning line="…" source="…">message</warning>`. Include `line` only when `berlioz.errors.detail` is not `minimal`, as `GetServices` does.
   - When a reload fails, its cause (`<load-failure source="…" line="…">message</load-failure>`), kept until the next successful load. Today the failure is only logged, and the previous warnings are kept unchanged.
   - Timestamps: `loaded` (last successful load), `failed` (last failed attempt) and `retry-after` (when Berlioz will try again), as ISO 8601 attributes on the root element.
   - Acceptance: after an authorised service reload (`berlioz-reload=services`, theme 17) with a malformed `services!x.xml`, `load-status="reload-failed"`, the last valid services are still listed, and `load-failure` names the file and the parse error.
3. **Which source declared each service, and overrides**
   - Record the declaring source when a service is registered and output it as `service/@source` (the source's display name).
   - When a later source replaces the same method and URL pattern, output the replaced declaration (`<overridden source="…" pattern="…"/>` inside the service that won). This is already detected today, but only reported as a warning naming both sources.
   - Acceptance: a pattern declared in a JAR and replaced by `services.xml` shows the filesystem source on the service and the JAR declaration as overridden.
4. **Provenance of effective values in `Service.writeTo`**
   - `cache-control-origin="service|group|configuration|default"` alongside `cache-control`.
   - `response-code/@origin` with the same values.
   - Only output an origin where Berlioz actually knows it; never infer it.
5. **Option catalog generator (`ListBerliozOptions`)**
   - For each `BerliozOption`: `property`, `type` (`boolean|string`), `default`, `deprecated` (version) and `removal` (version), plus a one-line `description` maintained with the option.
   - Optional: the value in use, redacted like `GetGlobalConfig`, and where it came from (`file`, `system-property`, `alias`, `default`).
   - Supports XML and JSON. Cacheable: the catalog is constant for a given Berlioz version.
   - Replaces bzadmin's `GetBerliozOptions` and the option descriptions it maintains.
6. **`GetBerliozConfig` completeness**
   - List Berlioz servlets that are registered but not yet initialised, with `initialised="false"` and the fields known from the registration (name, mappings). Today `BerliozXML` and `BerliozJSON` only appear after their first request.
   - Report application-relative stylesheet paths such as `/xslt/html/{GROUP}.xsl`, which are currently withheld by the safe-path rule. Keep withholding paths that resolve outside the application or contain traversal.
7. **Public secure XSLT resolver**
   - Expose the resolver policy (classpath references mapped to their `jar:`/`file:` URL, local-only URLs, no network) via a public factory, e.g. `XsltResolvers.secure(ClassLoader)`, so that tools reading stylesheets apply the same rules as the renderer. bzadmin's XSLT documentation currently re-implements them.

1.0:

8. **Stable `ListRuntimeServices`**
   - Details to be confirmed.

Target release: items 1–7 in 0.14.5; item 8 in 1.0.

### 17. Scoped `berlioz-reload`

Planned (decided 2026-10-27): keep the control parameters to a minimum. `berlioz-reload` takes scopes, and `reload-services`, `clear-xsl-cache` and `reset-etags` are retired in its favour, leaving `berlioz-reload` and `berlioz-profile`.

Today `berlioz-reload=true` does everything at once: it reloads the global configuration and the services, clears the XSLT cache and resets the ETag seed. `RedirectFilter` and `RelocationFilter` also reload their rules. Three separate parameters do part of the work (`reload-services`, `clear-xsl-cache`, `reset-etags`), but nothing reloads only the configuration. Adding a parameter for each case would add more global parameters.

`berlioz-reload` also accepts a comma-separated list of scopes, and `true` keeps meaning everything:

- `berlioz-reload=true`: everything, unchanged.
- `berlioz-reload=services`: the services only.
- `berlioz-reload=config,services`: the global configuration, then the services.
- Possible scopes: `config`, `services`, `xslt`, `etags`, and possibly `redirects` for the filter rules.

The administrator chooses what to reload, and one parameter covers every case, including a configuration-only reload.

Retiring the other parameters:

- 0.14.x: the scopes replace them: `reload-services=true` is `berlioz-reload=services`, `clear-xsl-cache=true` is `berlioz-reload=xslt`, `reset-etags=true` is `berlioz-reload=etags`. The three parameters keep working as deprecated aliases of their scope, and log a deprecation warning once when applied.
- 1.0: the three parameters are removed. Only `berlioz-reload` and `berlioz-profile` remain.
- Documentation, `BerliozServlet` Javadoc and `ControlAuthorization` list only the two parameters, with the aliases in the migration notes.

Points still to decide:

- **Order**: apply scopes in a fixed order (`config` first, since other scopes may depend on it, then `services`, `xslt`, `etags`), whatever their order in the parameter.
- **Unknown scopes**: ignore them with a warning, or reject the whole request without applying any scope.
- **Partial reloads can be inconsistent**: reloading the services without resetting the ETags can return `304 Not Modified` for a service whose definition changed, because the ETag combines the seed with generator ETags only. Document which scopes usually go together, or decide whether some scopes imply others (for example `services` implying `etags`).
- **Configuration failures**: `GlobalSettings.load()` clears every property when the file cannot be parsed, which turns off control authorisation and reverts every option to its default. A `config` scope makes this easier to trigger on its own. Consider keeping the last valid configuration, as the services do (see theme 5).
- **Filters**: `RedirectFilter` and `RelocationFilter` compare the parameter with `"true"`, so they would need to understand the scope list.
- **Reporting**: control parameters are ignored silently when the request is not authorised. Reporting the scopes that were applied, for example in a response header, would let tools such as Berlioz Admin confirm the result without their own generator (bzadmin's `GetBerliozControl`).

Target release: scopes and deprecated aliases in 0.14.5; removal of `reload-services`, `clear-xsl-cache` and `reset-etags` in 1.0.

### 18. Admin Overlay Upstreaming (Requested By Berlioz Admin)

Berlioz Admin (bzadmin) replaces or works around several Berlioz generators and helpers. The items below move that work into Berlioz, so that any application gets the fixes and bzadmin can delete its copies. They complement theme 16, which covers the diagnostic output itself (option catalog, servlet completeness, public XSLT resolver), and theme 17, which covers the scoped `berlioz-reload` that replaces the other reload parameters and reporting what was applied. The bzadmin side is planned in `docs/implementation/11-berlioz-upstream.md` of the bzadmin repository.

0.14.x (fixes and additive):

1. **Redacted `GetSystemProperties`**
   - Today it is an `XmlGenerator` that writes every system property unsorted and unredacted, unlike `GetGlobalConfig`.
   - Redact values with `Redaction.redact(name, value)`, sort by name, and implement `Generator` (JSON: `properties` array of `{name, value}`), keeping the XML unchanged apart from the order and the redacted values.
   - Security fix: suitable for a patch release.
   - Acceptance: a system property `fixture.password` is `[REDACTED]` in XML and JSON; properties are sorted.
   - Replaces bzadmin's `GetSystemProperties`.
2. **Non-blocking CPU sample (`GetCpuSample`)**
   - `GetCPUTime` sleeps on the request thread (100 ms by default, up to 5 s with `interval`), measures only that window, truncates to whole percents and sums the CPU time of live threads only, so it mostly reports 0.
   - Add a generator that returns cumulative counters without sleeping: `<cpu-sample processors="…" uptime-ms="…" process-time-ns="…" gc-time-ms="…" system-load="…" load-average="…"/>`. Clients compute the usage between two samples. JDK-specific metrics (`ProcessCpuTime`, `CpuLoad`/`SystemCpuLoad`) are read by MBean attribute name, never through `com.sun.management`; an unavailable value is absent, never zero.
   - Deprecate `GetCPUTime` for removal in 1.0.
   - Acceptance: no sleep; values absent when the MBean does not provide them; XML and JSON.
   - Replaces bzadmin's `GetCpuSample` and `OptionalMetrics`.
3. **Richer, bounded `ListThreads` and `GetThreadInfo`**
   - Add per thread: `blocked-count`, `waited-count`, `in-native`, `deadlocked`, `lock` with its owner, and allocated bytes (`getThreadAllocatedBytes`, invoked by operation name on the threading MBean).
   - Bound the work: `ListThreads` reads stacks with `Integer.MAX_VALUE` depth; limit the number of threads (e.g. 2000) and the stack depth (e.g. 256 frames), and report the monitors and synchronizers each thread holds in `GetThreadInfo`.
   - Keep how a thread is classified (component, role) out of core: it is a presentation heuristic and stays in bzadmin (`ThreadOrigin`).
   - Acceptance: existing attributes keep their meaning; limits are stated in the output when reached.
   - Replaces bzadmin's `ListThreads`, `GetThreadInfo` and `ThreadDetails`.
4. **`GetGeneratorStatistics` as a `Generator`**
   - Implement `Generator` so the statistics have JSON output; keep the XML unchanged.
5. **Public list of control parameters and reload scopes**
   - The control parameters are string literals in `BerliozServlet.applyBerliozControl`; bzadmin's `AdminFilter` copies the list to reject them outside its own control services.
   - With theme 17, expose the parameters (`berlioz-reload`, `berlioz-profile`, plus the deprecated aliases until 1.0) and the reload scopes, e.g. `ControlParameter` and `ReloadScope` enums with `isControlParameter(String)` and a public parser of `berlioz-reload` values, and use them in `BerliozServlet`, `RedirectFilter` and `RelocationFilter`. No new parameter.
   - Acceptance: a test fails if a control parameter or scope is applied that the public lists do not contain.
6. **`BerliozOption` metadata accessors**
   - `isDeprecated()`, `deprecatedSince()`/`removal()`, `type()` and `description()` on `BerliozOption`, as the data source of the option catalog generator (theme 16, item 5). bzadmin reads `@Deprecated` by reflection today.
7. **Network check covering all forwarding headers, made public**
   - `ControlNetwork.LOOPBACK` and `LAN` check the remote address and every hop of `X-Forwarded-For` only. bzadmin's fallback login (`AdminFilter.isLoopback`) is stricter: it refuses any `Forwarded`, `X-Forwarded-Host`, `X-Forwarded-Proto` or `X-Real-IP` header, and requires the local address and the server name (`localhost` or a loopback literal) to be loopback.
   - Treat `Forwarded` (every `for=` parameter, RFC 7239) and `X-Real-IP` like `X-Forwarded-For`, for both `LOOPBACK` and `LAN`: each address must match the network, and an unparseable or obfuscated value (`unknown`, `_hidden`) fails. Literals are parsed, never resolved. This closes the gap for proxies that set these headers instead of `X-Forwarded-For`, and keeps local dev proxies working, since loopback hops still pass.
   - Do not adopt bzadmin's other rules. Refusing on header presence breaks local dev proxies. The `Host` check breaks custom dev hostnames (`myapp.local`, `*.localhost`) and does not catch a bare `proxy_pass`, which sends the upstream's `Host`. The local-address check adds nothing for a loopback caller. The channel remains a development convenience (`off` in production); the reverse-proxy caveat on `CONTROL_NETWORK` still stands.
   - Expose the check publicly, e.g. `ControlNetwork.matches(HttpServletRequest)`, so bzadmin can build on it and keep its stricter login rules on top.
   - Acceptance: a loopback request with `Forwarded: for=203.0.113.1` or `X-Real-IP: 203.0.113.1` is not authorised by the loopback channel; one with `Forwarded: for=127.0.0.1`, `X-Real-IP: ::1` or `Host: myapp.local` still is.
8. **Identity of the XSLT processor and the servlet container**
   - Report the `TransformerFactory` class Berlioz actually uses (and, for Saxon, its product title and version) in `GetBerliozConfig`. bzadmin assumes `TransformerFactory.newInstance()` and detects Saxon by reflection.
   - Give generators the container identity (`ServletContext.getServerInfo()`, Servlet API major.minor), e.g. through `Environment` or `GlobalSettings`, recorded by `InitServlet`. bzadmin records it from its filter's `init`.
9. **Stylesheet listing and selected source**
   - Expose the stylesheets Berlioz can select: the classpath stylesheets under `META-INF/berlioz/xslt/` (with the number of copies on the classpath) merged with the XSLT folder, mapped the same way as the 0.14.4 fallback, and a public way to locate one path (`StylesheetLocation` for a logical path).
   - Report in `GetBerliozConfig` the source selected for each group stylesheet (`XsltTransformer.location()`: `file` or `classpath`).
   - Replaces bzadmin's JAR scanning (`XsltFiles`) and its override detection.

1.0:

10. **Service-level authorisation hook**
    - bzadmin wraps every Berlioz generator it uses in a one-line delegating class that checks the admin verdict first, because Berlioz cannot guard a service. A service or generator guard (theme 8) would let applications use Berlioz generators directly, and ensure a new service cannot forget the check.
    - Acceptance for bzadmin: its `org.pageseeder.bzadmin.guard` package can be removed, with every admin service still denied without a verdict.

Target release: items 1–9 in 0.14.5 (items 1 and 7 as security improvements); item 10 with theme 8 in 1.0.

## Open Questions

- Should Jakarta support be a breaking major release, a parallel artifact, or a later 1.x line once the application migration window is clear?
- Should the `javax.servlet` line become maintenance-only immediately, or remain active until a specific application migration threshold is reached?
- ~~How should 0.14.0 secure request-level administration/control parameters by default: require an explicit control key, disable controls unless configured, or use a separate enable flag?~~ Released outcome: controls are closed by default; `berlioz.control.key` (explicit key) and `berlioz.control.network` (default `off`) are independent channels, plus a fixed delegated-authorization request attribute for host applications.
- ~~Should service-level metadata be part of normal output, diagnostic output, or both?~~ Decided for 0.14.1: runtime-derived metadata belongs in `GetLiveServices` and `GetMatchingService`; normal responses and the raw `GetServices` source view remain unchanged.
- ~~Should configuration requirements live in a separate `config-requirements.xml` file, inside `config.xml`, or be supported in both forms?~~ Decided for configuration validation: use a separate `WEB-INF/config/config-requirements.xml` file.
- ~~For wildcard requirements, should zero matches be a violation by default, or should that depend on the presence rule?~~ Decided for configuration validation: zero matches violate only a presence rule that requires at least one match.
- ~~How should Berlioz distinguish an application-specified property from a framework default when evaluating `specified` requirements?~~ Decided for configuration validation: preserve explicit/default provenance in the resolved configuration snapshot.
- Should direct services support only one handler forever, or is there a future aggregation model?
- How should raw output interact with cache headers, ETags, and content negotiation?
- Should authorization requirements be declared primarily by generator interfaces, service configuration, or both?
- What minimum user/principal abstraction should Berlioz expose without taking ownership of authentication?
- Should authorization checks run at service level, generator level, or both?
- Should interceptor hooks be added to core, or should they wait for optional observability modules?
- Which integrations are valuable enough to maintain as official modules?
- ~~Should the custom error XSLT be a single global stylesheet, or should it support per-group error templates?~~ Decided: single global stylesheet. Error dispatch bypasses the normal XSLT resolution path so per-group templates would add complexity without clear benefit.
- ~~Should 0.14.0 remove the legacy Berlioz error XML format?~~ Released outcome: the format is deprecated, Problem Details is the default, and `berlioz.errors.problem=false` remains as a migration opt-out.
- ~~Should classpath service discovery be enabled by default or require an explicit opt-in configuration flag?~~ Decided for 0.14.3: enable discovery by default for the narrow, exactly named overlay resource.
- ~~Should a JAR be allowed to declare a service group that conflicts with a filesystem-declared group, and if so which takes precedence?~~ Decided for 0.14.3: conflicts are determined by HTTP method and URI pattern; filesystem declarations load last and take precedence.
- ~~Should classpath XSLT templates be reloaded on request (development mode) or treated as immutable (production mode), and should this follow the existing `berlioz.xslt.cache` setting?~~ Decided for 0.14.3: classpath templates are immutable in `manual` and `auto`; `no` recompiles them.
- ~~Should `META-INF/berlioz/services/` be the canonical convention, or should Berlioz read a manifest entry or properties file that declares which resources to load?~~ Decided for 0.14.3: use one exact `META-INF/berlioz/services.xml` resource per JAR, with an explicit index reserved for a future multiple-file use case.
- Should non-fatal XSLT warnings be surfaced to the client (e.g. via a response header or in the output XML), or only logged server-side?
- ~~Should `berlioz.errors.detail=minimal` suppress the `detail` member from `ProblemDetails` responses?~~ Decided: `minimal` suppresses only framework-internal diagnostics (stack traces, exception class, HTTP headers and parameters). The `detail` member in RFC 9457 responses always reflects the error message passed to `Problems.forHttpError()`, which is the HTTP status phrase — safe for production.
- Should `QUERY` responses be cacheable, and if so should the ETag seed incorporate a hash of the request body?
- ~~Should the `application/x-www-form-urlencoded` `QUERY` body-parameter stopgap be always-on, or an explicit opt-in given that feature-detecting container-native `QUERY` parsing is inherently fragile?~~ Decided for 0.14.1: always-on, no `BerliozOption`. There is no backward-compatibility risk to gate against — `QUERY` dispatch is new in this same release, so no existing generator can depend on the body being left untouched — and the container-detection check (comparing `getParameterMap()` against a plain parse of the URL query string) is a reliable data comparison, not a fragile stream-consumption probe, so it is safe to rely on unconditionally.
