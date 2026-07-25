# Berlioz Roadmap

Berlioz is a lightweight Java web framework built around URI templates, generator-based content, XML, JSON, and XSLT rendering.

The 0.13.x releases delivered the complete roadmap for that cycle: typed request parameters, safer XML parsing and XSLT caching, output-aware generators, direct JSON responses, explicit response values, RFC 9457 Problem Details, stronger redirect/filesystem protections, and the full configurable error handling pipeline. **0.13.5 was released on 2026-07-03 as the final 0.13.x release.**

**0.14.0 was released on 2026-07-21.** It moved the framework to secure defaults, closed administration controls by default, strengthened the XSLT error pipeline, and modernized the built-in diagnostic generators.

**0.14.1 was released on 2026-07-25.** It delivered the runtime service-diagnostics model, first-class HTTP `QUERY` support, and hardened Content-Type/header handling across the servlet layer. Development is now on **0.14.2-SNAPSHOT** and focuses on classpath overlay discovery before configuration validation and a 1.0 release.

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

0.14.1 delivered the runtime diagnostics model, core HTTP `QUERY` dispatch, and the smaller output/error contracts. The overlay and configuration-lifecycle work stays deferred to 0.14.2/0.14.3.

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

### 0.14.2: Classpath Overlay Discovery

0.14.2 should deliver reusable overlays as ordinary JAR dependencies:

- Use one exactly addressable `META-INF/berlioz/services.xml` resource per contributing JAR; an explicit index can be added later if multiple documents per JAR prove necessary.
- Load classpath contributions first and filesystem configuration second, so application-owned filesystem mappings take precedence.
- Define mapping conflicts by HTTP method and URI pattern, and report the origin of both the replaced and replacing declarations.
- Make service loading transactional so an invalid contribution does not publish a partially populated registry.
- Generalize service inputs from files to source descriptors carrying a URL and origin metadata.
- Add format-agnostic `GetBerliozConfig` and `GetErrorHandlerConfig` diagnostic generators so admin overlays can inspect how the deployed Berlioz and error-handler servlets are actually configured and mapped.
- Have the Berlioz diagnostic expose each servlet name, URL mappings, effective media type and charset, stylesheet/fallback allocation, cache policy, and compression setting; have the error-handler diagnostic expose its mappings, explicit versus discovered forward extensions, ignored extensions, default extension, and effective problem/detail/stylesheet options.
- Give both diagnostics stable equivalent XML/JSON schemas, correlate entries by servlet name and mapping, omit sensitive values, and document them as opt-in/admin-only services.
- Support `classpath:` primary XSLT templates while retaining `resource:` as a compatibility alias.
- Treat classpath templates as immutable in `manual` and `auto` cache modes; `no` may recompile them on each use.
- Verify discovery, conflict handling, XSLT imports/includes, static resources, and `web-fragment.xml` using a real packaged overlay JAR.

### 0.14.3: Configuration Requirements And Validation

0.14.3 should add application-facing configuration validation with explicit property provenance and safe diagnostics:

- Read requirements from a separate `WEB-INF/config/config-requirements.xml` file after variable resolution.
- Preserve whether each resolved property was explicitly specified or inherited from a framework default.
- Support exact and simple wildcard property matching and presence checks for `defined`, `specified`, and `has-value`.
- Treat zero wildcard matches as a violation only when the requirement's presence rule requires a match.
- Implement built-in constraints for boolean, integer, number, regex, enum, URL, URI, port, hostname, and path values.
- Add a validation report model with severity, requirement identity, matched property name, and safe messages that omit raw values by default.
- Default to report-and-continue behavior, with an option to fail startup or reload on error-level violations.
- Validate reload candidates before publication so a failed reload retains the last valid configuration.
- Add an opt-in XML/JSON diagnostic generator and tests for matching, presence, constraints, provenance, reload, fail-fast behavior, and redaction.

After these releases are complete, Berlioz may be ready for a 1.0.0 release.

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

### 4. Classpath Overlay Discovery

Berlioz currently requires all service configurations and XSLT templates to be present on the filesystem under `WEB-INF/`. This means reusable admin or utility overlays must be distributed as WAR overlays — ZIP archives that are merged into the host application at build time. A classpath-based discovery model would allow an overlay to be packaged as a plain JAR dependency, with no file-system merging step required.

Already done:

- `BerliozConfig.toURL()` supports a `resource:` prefix for loading fallback and kickstart XSLT templates from the classpath.
- The failsafe error stylesheet is already loaded from the classpath via `ClassLoader.getResource()`.
- `web-fragment.xml` in `pso-berlioz-kickstart` already registers servlet mappings without requiring changes to the host application's `web.xml`.
- `java.util.ServiceLoader` is already used for `RedirectPolicy` discovery via `META-INF/services/`, establishing a workable SPI pattern.

Next work:

- Discover one exactly addressable `META-INF/berlioz/services.xml` resource per JAR. Class loaders cannot portably enumerate arbitrary children beneath a resource directory, so `META-INF/berlioz/services/` is not used as an implicit listing convention.
- Enable discovery for this narrow convention by default: adding the overlay dependency is the explicit application action.
- Load classpath configurations first and filesystem configurations second, giving application-owned filesystem mappings final precedence.
- Define conflicts by HTTP method and URI pattern rather than group name alone, and include source origins in override diagnostics.
- Generalize `ServiceLoader` from `File` inputs to source descriptors with a URL and origin, and publish a new registry only after all inputs parse successfully.
- Add format-agnostic `GetBerliozConfig` and `GetErrorHandlerConfig` generators for reusable admin overlays. `GetBerliozConfig` should enumerate deployed Berlioz servlet registrations and effective runtime settings (name, mappings, media type/charset, stylesheet and fallback allocation, cache policy, and compression). `GetErrorHandlerConfig` should expose the error-handler registration and effective routing/rendering policy (mappings, explicit or deployment-discovered forward extensions, ignored/default extensions, Problem Details mode, detail level, and custom/failsafe stylesheet selection).
- Use stable equivalent XML/JSON schemas, identify configured versus derived values where relevant, correlate the two views by servlet name and mapping, omit secrets and arbitrary raw global-property values, and keep the generators opt-in/admin-only.
- Add schema and deployment tests using the packaged overlay JAR so admin tools see the same servlet registrations and effective configuration that the runtime error-selection path uses.
- Extend XSLT template resolution with a `classpath:` prefix for primary templates while retaining `resource:` as a compatibility alias.
- Refactor XSLT caching to support URL-backed templates. Classpath templates are immutable in `manual` and `auto` modes; `no` recompiles them when requested.
- Verify the complete deployment model with a real JAR containing `META-INF/berlioz/services.xml`, `META-INF/resources/` static assets, `META-INF/web-fragment.xml`, and classpath XSLT, including imports/includes.

The goal is to allow a self-contained Berlioz overlay — XSLT templates, service configuration, static assets via `META-INF/resources/`, and a servlet filter via `web-fragment.xml` — to be distributed and consumed as a single JAR dependency.

### 5. Configuration Requirements And Validation

Many Berlioz applications depend on application-specific global properties being present and well-formed. Berlioz should be able to validate those requirements when global configuration is loaded, report violations consistently, and expose the result through a diagnostic generator.

Already done (groundwork):

- `OptionDeprecations` (package-private) holds the deprecated-value checks for the three 0.14.0 deprecated options: `berlioz.http.get-via-post=true`, `berlioz.errors.problem=false`, and `berlioz.xml.header.version=0.9`. `GlobalSettings.load()` calls it once after a successful load or reload; no warning is ever emitted per request.
- `BerliozOption` is unchanged — deprecation metadata does not live on the enum.
- `OptionDeprecations` is the internal precursor to Theme 5's `ConfigurationValidator`: the check-and-warn pattern it establishes will generalize into the declarative, application-facing validation layer.

Planned shape for 0.14.3:

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

## Open Questions

- Should Jakarta support be a breaking major release, a parallel artifact, or a later 1.x line once the application migration window is clear?
- Should the `javax.servlet` line become maintenance-only immediately, or remain active until a specific application migration threshold is reached?
- ~~How should 0.14.0 secure request-level administration/control parameters by default: require an explicit control key, disable controls unless configured, or use a separate enable flag?~~ Released outcome: controls are closed by default; `berlioz.control.key` (explicit key) and `berlioz.control.network` (default `off`) are independent channels, plus a fixed delegated-authorization request attribute for host applications.
- ~~Should service-level metadata be part of normal output, diagnostic output, or both?~~ Decided for 0.14.1: runtime-derived metadata belongs in `GetLiveServices` and `GetMatchingService`; normal responses and the raw `GetServices` source view remain unchanged.
- ~~Should configuration requirements live in a separate `config-requirements.xml` file, inside `config.xml`, or be supported in both forms?~~ Decided for 0.14.3: use a separate `WEB-INF/config/config-requirements.xml` file.
- ~~For wildcard requirements, should zero matches be a violation by default, or should that depend on the presence rule?~~ Decided for 0.14.3: zero matches violate only a presence rule that requires at least one match.
- ~~How should Berlioz distinguish an application-specified property from a framework default when evaluating `specified` requirements?~~ Decided for 0.14.3: preserve explicit/default provenance in the resolved configuration snapshot.
- Should direct services support only one handler forever, or is there a future aggregation model?
- How should raw output interact with cache headers, ETags, and content negotiation?
- Should authorization requirements be declared primarily by generator interfaces, service configuration, or both?
- What minimum user/principal abstraction should Berlioz expose without taking ownership of authentication?
- Should authorization checks run at service level, generator level, or both?
- Should interceptor hooks be added to core, or should they wait for optional observability modules?
- Which integrations are valuable enough to maintain as official modules?
- ~~Should the custom error XSLT be a single global stylesheet, or should it support per-group error templates?~~ Decided: single global stylesheet. Error dispatch bypasses the normal XSLT resolution path so per-group templates would add complexity without clear benefit.
- ~~Should 0.14.0 remove the legacy Berlioz error XML format?~~ Released outcome: the format is deprecated, Problem Details is the default, and `berlioz.errors.problem=false` remains as a migration opt-out.
- ~~Should classpath service discovery be enabled by default or require an explicit opt-in configuration flag?~~ Decided for 0.14.2: enable discovery by default for the narrow, exactly named overlay resource.
- ~~Should a JAR be allowed to declare a service group that conflicts with a filesystem-declared group, and if so which takes precedence?~~ Decided for 0.14.2: conflicts are determined by HTTP method and URI pattern; filesystem declarations load last and take precedence.
- ~~Should classpath XSLT templates be reloaded on request (development mode) or treated as immutable (production mode), and should this follow the existing `berlioz.xslt.cache` setting?~~ Decided for 0.14.2: classpath templates are immutable in `manual` and `auto`; `no` recompiles them.
- ~~Should `META-INF/berlioz/services/` be the canonical convention, or should Berlioz read a manifest entry or properties file that declares which resources to load?~~ Decided for 0.14.2: use one exact `META-INF/berlioz/services.xml` resource per JAR, with an explicit index reserved for a future multiple-file use case.
- Should non-fatal XSLT warnings be surfaced to the client (e.g. via a response header or in the output XML), or only logged server-side?
- ~~Should `berlioz.errors.detail=minimal` suppress the `detail` member from `ProblemDetails` responses?~~ Decided: `minimal` suppresses only framework-internal diagnostics (stack traces, exception class, HTTP headers and parameters). The `detail` member in RFC 9457 responses always reflects the error message passed to `Problems.forHttpError()`, which is the HTTP status phrase — safe for production.
- Should `QUERY` responses be cacheable, and if so should the ETag seed incorporate a hash of the request body?
- ~~Should the `application/x-www-form-urlencoded` `QUERY` body-parameter stopgap be always-on, or an explicit opt-in given that feature-detecting container-native `QUERY` parsing is inherently fragile?~~ Decided for 0.14.1: always-on, no `BerliozOption`. There is no backward-compatibility risk to gate against — `QUERY` dispatch is new in this same release, so no existing generator can depend on the body being left untouched — and the container-detection check (comparing `getParameterMap()` against a plain parse of the URL query string) is a reliable data comparison, not a fragile stream-consumption probe, so it is safe to rely on unconditionally.
