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

Already done:

- `400 Bad Request` for invalid typed parameters (via `InvalidParameterException`).
- `404 Not Found` for unmatched services.
- `405 Method Not Allowed` when a URI matches but the method does not (with `Allow` header).
- `500 Internal Server Error` for unexpected generator failures (forwarded to error handler).

Next work:

- Render problem responses consistently for JSON, XML, and transformed HTML.
- Define Berlioz problem `type` URIs and decide whether they resolve to public documentation.
- Decide how parameter validation failures should expose field-level details.
- Decide whether request validation should fail immediately or collect all parameter errors first.

The goal is not a large exception-mapping framework. The goal is a small, predictable error model that works across the existing output pipeline.

### 2. Error Handling Pipeline

Berlioz's error handling currently works but lacks user control over error presentation, leaks internal details by default, and handles static and dynamic XSLT errors with less distinction than it could.

Already done:

- `BerliozErrorID` classifies errors semantically (transform not found, invalid, dynamic error, malformed source XML, etc.).
- `XsltErrorCollector` collects warnings, errors, and fatals during XSLT processing.
- `ErrorHandlerServlet` generates error XML and applies the failsafe template.
- `XsltTransformer` catches both `TransformerConfigurationException` (static) and `TransformerException` (dynamic) and falls back to the failsafe template.
- The built-in failsafe XSLT renders error details with contextual help for known error IDs.

Next work:

- Allow applications to provide a custom error XSLT via a configuration option, with automatic fallback to the built-in failsafe when the custom template itself fails.
- Introduce an error detail level option to control how much information is included in error responses (stack traces, exception messages, source locations, HTTP headers and parameters).
- Improve the distinction between static XSLT errors (compilation failures) and dynamic XSLT errors (runtime failures), particularly around recoverability and the context included in the error output.
- Modernize the built-in failsafe template with current CSS practices, responsive layout, collapsible stack traces, and a structure aligned with Problem Details.

The goal is predictable, configurable error presentation that helps developers during development while protecting production environments from information leakage.

### 3. Finish Direct Output And Raw Output Support

Direct JSON and direct XML services are now part of the pipeline. `RawGenerator` exists as an API shape, but servlet dispatch support is still future work.

Next work:

- Add a dedicated servlet path for `RawGenerator`.
- Define content type and charset behavior for raw output.
- Decide how cache headers and ETags apply to raw responses.
- Keep the direct service rule simple: one handler, one complete response body.
- Document how `<handler>` differs from `<generator>`.

### 4. Content Negotiation

Berlioz currently determines the output format from the URL extension (`.xml`, `.json`, `.html`) via servlet mappings. Adding support for the `Accept` header would allow a single endpoint to serve multiple formats based on client preference.

Next work:

- Support `Accept` header content negotiation as an alternative to extension-based format selection.
- Use `406 Not Acceptable` when the requested format is not supported by the service.
- Decide how `Accept`-based negotiation interacts with extension-based format selection (precedence, override, fallback).
- Decide whether content negotiation should be opt-in per service or enabled globally.

### 5. Service Metadata And Diagnostics

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

### 6. Authentication And Authorization Guards

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

### 7. Interceptors And Observability

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

### 8. Classpath Overlay Discovery

Berlioz currently requires all service configurations and XSLT templates to be present on the filesystem under `WEB-INF/`. This means reusable admin or utility overlays must be distributed as WAR overlays — ZIP archives that are merged into the host application at build time. A classpath-based discovery model would allow an overlay to be packaged as a plain JAR dependency, with no file-system merging step required.

Already done:

- `BerliozConfig.toURL()` supports a `resource:` prefix for loading fallback and kickstart XSLT templates from the classpath.
- The failsafe error stylesheet is already loaded from the classpath via `ClassLoader.getResource()`.
- `web-fragment.xml` in `pso-berlioz-kickstart` already registers servlet mappings without requiring changes to the host application's `web.xml`.
- `java.util.ServiceLoader` is already used for `RedirectPolicy` discovery via `META-INF/services/`, establishing a workable SPI pattern.

Next work:

- Define a `META-INF/berlioz/services/` convention for service configuration files contributed by JARs on the classpath.
- Extend `ServiceLoader` to enumerate classpath resources under that path using `ClassLoader.getResources()` and merge them with filesystem-loaded configurations.
- Define load ordering and override behavior when the same service group is declared both on the classpath and on the filesystem.
- Extend XSLT template resolution to support a `classpath:` prefix on primary templates, generalizing the existing `resource:` fallback mechanism in `BerliozConfig`.
- Decide how the XSLT cache handles staleness for classpath resources, where `File.lastModified()` is not available.
- Keep filesystem-first as the default: classpath discovery should supplement, not replace, the existing `WEB-INF/config/` resolution path.

The goal is to allow a self-contained Berlioz overlay — XSLT templates, service configuration, static assets via `META-INF/resources/`, and a servlet filter via `web-fragment.xml` — to be distributed and consumed as a single JAR dependency.

### 9. Optional Integration Modules

Keep Berlioz core small, but provide integration points for applications that already use other Java frameworks.

Possible modules:

- Spring integration for resolving generators from an `ApplicationContext`.
- Spring integration for using `ConversionService` or `Validator`.
- Spring Security integration for evaluating authorization requirements where applications already use it.
- Jakarta CDI integration for resolving generators as CDI beans.
- Optional annotation-based routing, metadata, or authorization module.
- Optional adapters for metrics and tracing.
- Binary serialization module for high-performance output formats (see §10).

These integrations should support Berlioz rather than replace its URI template, generator, and XSLT model.

### 10. Binary Serialization Module

Berlioz's generator hierarchy (`XmlGenerator`, `JsonGenerator`, `RawGenerator`) already supports multiple output formats dispatched by URL extension. A future optional module could extend this to high-performance binary serialization formats, following the same pattern as the existing `aeson` JSON adapters.

Recommended first candidate:

- **MessagePack** — schema-less and structurally similar to JSON, so the generator API can mirror `JsonGenerator` closely. A `pso-berlioz-msgpack` module would provide a `MessagePackGenerator` base class and wire the `*.msgpack` extension into servlet dispatch.

Other candidates to evaluate later:

- **Protocol Buffers** — requires pre-defined `.proto` schemas compiled to Java classes. Higher friction for users but strong ecosystem support and compact wire format.
- **Apache Avro** — schema-based with optional schema evolution. Natural fit for data interchange but heavier setup than MessagePack.
- **FlatBuffers** — zero-copy deserialization for performance-critical paths. Niche use case within a web framework.

Schema-based formats (Protobuf, Avro, FlatBuffers) would need a different generator contract where the user returns a pre-built typed message rather than writing fields dynamically, so they represent a larger API design decision.

No core changes are expected: the existing extension-based dispatch and generator hierarchy should accommodate binary formats without modification.

### 9. Jakarta Servlet Support

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
- Decide immediate versus collected parameter validation failures.
- Define problem type URI conventions.

### Milestone 3: Error Handling Pipeline

- Add `berlioz.errors.stylesheet` option for custom error XSLT with fallback chain (custom → failsafe → raw XML).
- Add `berlioz.errors.detail` option with `full`, `standard`, and `minimal` levels controlling what is serialized into error responses.
- Refine static vs. dynamic XSLT error handling: surface non-fatal warnings without breaking the page, include source context for dynamic errors in development mode.
- Modernize the failsafe XSLT template with responsive layout, `<details>`/`<summary>` for stack traces, CSS variables, and Problem Details-aligned structure.
- Ensure error detail levels apply consistently across `XsltTransformer`, `ErrorHandlerServlet`, and `ProblemDetails` rendering.
- Add test coverage for error detail filtering, custom error template resolution, and failsafe fallback behavior.

### Milestone 4: Content Negotiation

- Support `Accept` header content negotiation.
- Use `406 Not Acceptable` when the requested format is not supported.
- Define interaction with extension-based format selection.

### Milestone 5: Metadata And Diagnostics

- Add supported-output metadata to source or diagnostic output.
- Expose direct handler and generator capability information.
- Improve service registry diagnostic warnings.
- Add tests for metadata stability.

### Milestone 6: Raw Output

- Implement servlet dispatch for `RawGenerator`.
- Define raw content type and cache behavior.
- Add tests for raw response status, headers, ETags, and body writing.
- Document raw output constraints.

### Milestone 7: Authorization And Interceptors

- Define a small authorization result model.
- Add programmatic guard support for services or generators.
- Define how authorization failures map to Problem Details.
- Decide whether interceptor hooks belong in core or an optional module.
- Add tests for `401` and `403` early-return behavior.

### Milestone 8: Integration And Instrumentation

- Add optional Spring or CDI generator resolution.
- Add optional Spring Security or application authorization adapters.
- Add optional metrics/tracing integration.
- Keep integration modules separate from the core runtime.

### Milestone 8b: Classpath Overlay Discovery

- Define the `META-INF/berlioz/services/` classpath convention for service configuration files.
- Extend `ServiceLoader` to enumerate and merge classpath service configurations alongside filesystem ones.
- Document load ordering and override behavior between classpath and filesystem sources.
- Extend XSLT template resolution to support a `classpath:` prefix as a first-class option, not just a fallback mechanism.
- Decide and implement staleness/cache-invalidation behavior for classpath-resident templates.
- Add test coverage for classpath service discovery, XSLT classpath loading, and mixed classpath/filesystem configurations.
- Verify that an overlay packaged as a JAR — with `META-INF/berlioz/services/`, `META-INF/resources/` static assets, `META-INF/web-fragment.xml`, and classpath XSLT — deploys correctly without any host application changes beyond adding the dependency.

### Milestone 9: Jakarta Migration

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
- Should the custom error XSLT be a single global stylesheet, or should it support per-group error templates (like the normal XSLT resolution)?
- Should classpath service discovery be enabled by default or require an explicit opt-in configuration flag?
- Should a JAR be allowed to declare a service group that conflicts with a filesystem-declared group, and if so which takes precedence?
- Should classpath XSLT templates be reloaded on request (development mode) or treated as immutable (production mode), and should this follow the existing `berlioz.xslt.cache` setting?
- Should `META-INF/berlioz/services/` be the canonical convention, or should Berlioz read a manifest entry or properties file that declares which resources to load?
- Should non-fatal XSLT warnings be surfaced to the client (e.g. via a response header or in the output XML), or only logged server-side?
- Should `berlioz.errors.detail=minimal` suppress the `detail` member from `ProblemDetails` responses, or only suppress framework-internal information like stack traces?
