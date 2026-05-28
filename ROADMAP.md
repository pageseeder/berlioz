# Berlioz Roadmap

Berlioz is a lightweight Java web framework built around URI templates, XML content generation, and XSLT rendering. 
The next development cycle should strengthen those core ideas rather than turn Berlioz into a general-purpose 
application framework.

This roadmap is intentionally public and directional. It describes the capabilities we want to explore, the constraints
we want to keep, and the integration points that may make Berlioz easier to use in modern Java applications.

## Guiding Principles

- Keep Berlioz simple, explicit, and easy to reason about.
- Keep the core annotation-free. Annotation-based programming can be useful, but it should live in optional modules.
- Preserve URI templates as the primary request matching model.
- Preserve XML and XSLT as first-class mechanisms for generating XML and HTML responses.
- Make JSON and other output formats easier to support without weakening the XML/XSLT pipeline.
- Prefer small extension points over large framework dependencies.
- Keep optional integrations optional.

## Candidate Development Themes

### 1. Output-Aware Generators

Allow generators to write through a higher-level output abstraction so the same service can produce XML or JSON depending on the request format.

The current XML-first model should remain the default. The goal is to make output negotiation clearer and more capable, not to remove the XML writer model.

Areas to explore:

- A generator interface based on an `OutputWriter` or similar abstraction.
- Backward-compatible support for existing `ContentGenerator` implementations.
- Request format detection from URI extensions such as `.html`, `.xml`, `.json`, and `.src`.
- Rules for when output should be transformed by XSLT and when it should be written directly.
- A simple model for services that support only specific output formats.

Possible shape:

```java
void process(ContentRequest request, OutputWriter output);
```

The `OutputWriter` should keep XML efficient and natural while making direct JSON output possible where appropriate.

### 2. Typed and Validating Request Parameters

Add a smarter request API for reading parameters with constraints and predictable `400 Bad Request` handling.

The API should support common cases without annotations or reflection.

Examples of the kind of usage to consider:

```java
int page = request.parameter("page").asInt().defaultValue(1);
LocalDate from = request.parameter("from").asDate().required();
String sort = request.parameter("sort").oneOf("name", "date", "title").defaultValue("name");
```

Capabilities to consider:

- Required parameters.
- Default values.
- Type conversion for strings, numbers, booleans, dates, times, and enums.
- Nullable scalar access using JSpecify `@Nullable` where absence is expected.
- Range checks for numbers and dates.
- Allowed-value checks.
- Multi-value parameters.
- Structured error reporting.
- Automatic `400 Bad Request` responses when constraints fail.

This could borrow from Spring's conversion and validation concepts, but the Berlioz API should stay explicit and local to the request object.

The API should prefer simple nullable scalar values over `Optional<T>` for ordinary request access. When used with clear nullness annotations, `@Nullable` keeps the API direct and avoids unnecessary temporary objects.

### 3. Service and Page Metadata

Expose service and page metadata consistently.

Potential metadata:

- Service name or identifier.
- Matched URI template.
- HTTP method.
- Supported output formats.
- Cache policy.
- Page title or description.
- Generator list.
- Diagnostic information for development mode.

Potential output channels:

- HTTP headers.
- XML metadata nodes.
- JSON metadata properties.
- XSLT-accessible values or simple functions.
- Source or diagnostic views.

This should make services easier to inspect, document, test, and debug without adding heavy machinery to the runtime.

### 4. Error Handling and Problem Responses

Define clearer conventions for framework-generated errors.

Areas to consider:

- `400 Bad Request` for invalid parameters.
- `404 Not Found` for unmatched services.
- `405 Method Not Allowed` when a URI matches but the method does not.
- `406 Not Acceptable` when the requested output format is unsupported.
- `500 Internal Server Error` for unexpected generator failures.
- Explore RFC 9457 Problem Details as the standard shape for machine-readable error responses.

The same error model should be representable as XML, JSON, or transformed HTML.

RFC 9457 defines the Problem Details model for HTTP APIs, replacing RFC 7807. It provides a common structure for error responses using fields such as `type`, `title`, `status`, `detail`, and `instance`, while allowing extension members for framework-specific or application-specific details.

Berlioz could use this model for its own generated errors and expose them through the existing output pipeline:

- `application/problem+json` for JSON requests.
- `application/problem+xml` for XML requests.
- Problem XML transformed with XSLT for HTML requests.
- Source or diagnostic views when development diagnostics are enabled.

For request validation, Berlioz could add an extension member such as `errors` to report individual parameter failures, while keeping the top-level HTTP status code authoritative.

Spring and JAX-RS both have useful ideas here, especially exception mapping and structured problem responses. Berlioz can adopt the small useful parts without adopting their programming models.

### 5. Authentication and Authorization Guards

Make it easy for applications to guard services and generators without making Berlioz responsible for authentication itself.

Authentication should remain a separate concern. Applications, servlet containers, filters, reverse proxies, session systems, or identity providers can establish the current user or principal before Berlioz executes a service. Berlioz can then provide simple authorization hooks to decide whether a service or generator is allowed to run.

Expected behavior:

- Return `401 Unauthorized` when authentication is required but no authenticated user is available.
- Return `403 Forbidden` when the user is authenticated but lacks the required permission, role, or capability.
- Use RFC 9457 Problem Details for machine-readable authorization failures.
- Include `WWW-Authenticate` headers for authentication schemes that require them.

Possible guard models:

- Programmatic generator guards, for example `Guard.authenticated().and(canWrite(document))`.
- Generator interfaces such as `GuardedGenerator`, `RequiresAuthorization`, or `PermissionCheck`.
- Service-level requirements in `services.xml`, with application-defined meanings for roles and permissions.
- An application-provided `AuthorizationManager` that evaluates declared requirements.
- Lightweight interceptors that can run before services or generators.
- Optional annotation support in a separate module, for example `@RequiresRole` or `@RequiresPermission`.

The core model should not require Berlioz to choose between role-based access control, permission-based access control, attribute-based checks, or application-specific predicates. Instead, Berlioz should provide a small result model such as:

- Allowed.
- Authentication required.
- Forbidden.
- Custom problem response.

This keeps policy decisions in application code while giving Berlioz consistent early-return behavior, response status handling, and XML/JSON/HTML problem output.

### 6. Interceptors and Observability

Introduce lightweight hooks around request processing.

Possible hooks:

- Before service execution.
- After successful service execution.
- Around individual generator execution.
- On errors.
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

### 7. Optional Integration Modules

Keep Berlioz core small, but provide integration points for applications that already use other Java frameworks.

Possible modules:

- Spring integration for resolving generators from an `ApplicationContext`.
- Spring integration for using `ConversionService` or `Validator`.
- Spring Security integration for evaluating authorization requirements where applications already use it.
- Jakarta CDI integration for resolving generators as CDI beans.
- Optional annotation-based routing, metadata, or authorization module.
- Optional JSON integration modules for Jackson, Gson, or Jakarta JSON-P.

These integrations should support Berlioz rather than replace its URI template, generator, and XSLT model.

### 8. Jakarta Servlet Support

Move Berlioz to the Jakarta Servlet namespace for modern servlet containers.

**Note**: This migration is intentionally deferred while many existing applications remain on `javax.servlet`. The `javax` line will continue as the active release until a migration window is identified.

Topics to resolve:

- Whether Jakarta support should be a breaking major release.
- Whether to maintain a legacy `javax.servlet` line for existing applications.
- Whether servlet-specific code should be isolated enough to support parallel `javax` and `jakarta` artifacts.
- How the mock and kickstart modules should track the migration.

Likely outcome:

- A Jakarta-based Berlioz line for current containers.
- Clear migration notes for applications moving from `javax.servlet` to `jakarta.servlet`.

## Possible Milestones

### Milestone 1: Foundations

- Defer Jakarta migration; document compatibility policy for existing `javax.servlet` applications.
- Define compatibility policy for existing applications.
- Document the desired request/output lifecycle.
- Identify which APIs must remain source-compatible.

### Milestone 2: Request and Error Model

- Add typed parameter access.
- Add constraint failures and structured `400` responses.
- Define RFC 9457 Problem Details support for framework-generated errors.
- Define framework error response structure.
- Add focused tests for request parsing and failed constraints.

### Milestone 3: Output Model

- Introduce an output-aware generator API.
- Preserve compatibility for existing XML generators.
- Define output format negotiation rules.
- Support direct JSON output for services that opt in.

### Milestone 4: Metadata and Diagnostics

- Add service/page metadata APIs.
- Expose metadata through headers and generated output.
- Improve source or diagnostic output for development mode.

### Milestone 5: Authorization and Guarding

- Define a small authorization result model for allowed, authentication required, forbidden, and custom problem responses.
- Add programmatic guard support for services or generators.
- Consider service-level authorization requirements in `services.xml`.
- Define how authorization failures are represented using RFC 9457 Problem Details.
- Add tests for `401` and `403` early-return behavior.

### Milestone 6: Integration and Instrumentation

- Add lightweight interceptor hooks.
- Add optional Spring or CDI generator resolution.
- Add optional Spring Security or application authorization adapters.
- Add optional metrics/tracing integration.
- Keep integration modules separate from the core runtime.

### Milestone 7: Jakarta Migration

- Decide final Jakarta migration strategy.
- Release Jakarta-based line with migration notes.
- Determine whether `javax.servlet` line continues as maintenance-only.

## Open Questions

- Should the Jakarta migration be released as Berlioz 1.0?
- Should the `javax.servlet` line become maintenance-only immediately, or remain active until a specific application migration threshold is reached?
- What is the smallest useful `OutputWriter` API?
- Should output negotiation prefer URI extension over the `Accept` header?
- How should XML aggregation work when some generators can write JSON directly?
- Should request validation fail immediately or collect all parameter errors first?
- Which Problem Details `type` URIs should Berlioz define, and should they resolve to public documentation?
- Should authorization requirements be declared primarily by generator interfaces, service configuration, or both?
- What minimum user/principal abstraction should Berlioz expose without taking ownership of authentication?
- Should authorization checks run at service level, generator level, or both?
- What metadata belongs in core, and what belongs in diagnostics only?
- Which integrations are valuable enough to maintain as official modules?
