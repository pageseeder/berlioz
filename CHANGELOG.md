# Changelog

All notable changes to Berlioz are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Berlioz is pre-1.0: minor versions (0.13, 0.14) may contain breaking changes, which are listed with upgrade instructions in the [upgrade guide](docs/upgrading.md). Planned work is in the [roadmap](ROADMAP.md).

## [Unreleased] — 0.14.5

Small security improvements, a scoped `berlioz-reload`, and diagnostics requested by Berlioz Admin.

### Added

- `berlioz-reload` accepts a comma-separated list of scopes (`config`, `services`, `xslt`, `etags`, `redirects`) as well as `true`. Scopes are applied in a fixed order, an unknown scope rejects the whole request, and the applied scopes are reported in the `X-Berlioz-Reload` response header. `ReloadScope` and `ControlParameter` expose the scopes and control parameter names.
- `requires-control` servlet init parameter, restricting a Berlioz servlet to requests authorized for Berlioz control.
- `ListBerliozOptions` generator: the catalog of Berlioz options with type, default, description and deprecation (XML and JSON). Backed by new `BerliozOption` accessors `type()`, `description()`, `isDeprecated()`, `deprecatedSince()` and `removal()`.
- `GetCpuSample` generator returning cumulative CPU counters without blocking the request thread.
- `ListRuntimeServices` reports the configuration sources of the last successful load, load warnings, the cause of a failed reload (`load-failure`), the `loaded`, `failed` and `retry-after` timestamps, the declaring `source` of each service, the declarations it `overridden`, and `response-code/@origin`.
- `GetBerliozConfig` reports registered servlets that are not initialized yet, application-relative stylesheet paths, the `xslt-processor`, the servlet `container`, the stylesheets selected so far (`stylesheet/selection`) and `requires-control`.
- `ListThreads` and `GetThreadInfo` report blocked and waited counts, `in-native`, `deadlocked`, allocated bytes and lock owners; `GetThreadInfo` also lists locked monitors and synchronizers.
- JSON output for `GetGeneratorStatistics`.
- Public helpers for tools: `XsltResolvers.secure()`, `XsltResolvers.locate(…)`, `StylesheetLocation.select(webInf, path)`, `StylesheetLocation.countClasspathCopies(path)`, `ControlNetwork.matches(HttpServletRequest)`, and `Environment.getServerInfo()`/`getServletApiVersion()`.

### Changed

- `GetSystemProperties` is a `Generator` with JSON output; properties are sorted by name.
- `ListThreads` is bounded to 2000 threads and 256 stack frames, stating the limits in the output when reached.
- A configuration reload that fails to parse keeps the last valid configuration instead of reverting every option to its default.

### Deprecated

- `reload-services`, `clear-xsl-cache` and `reset-etags`: use `berlioz-reload=services`, `xslt` and `etags`. Removal planned for 1.0.
- `GetCPUTime`: use `GetCpuSample`. Removal planned for 1.0.

### Fixed

- Group stylesheet selection no longer repeats filesystem path validation on each request.
- `X-Berlioz-Reload` reports only scopes applied by the filters and servlet reached by the request, including when a redirect ends processing early.
- `ListRuntimeServices` ETags now change when a configuration-only reload changes whether diagnostic details are included.
- A `<response-code>` declared directly under `<service-config>` was ignored; it now applies to groups and services that declare none.

### Security

- The kickstart default `*.xml` output no longer exposes the request `<header>` or internal `content` attributes (`@generator`, `@etag`, `@profile*`).
- The kickstart `*.src` mapping returns `404` unless the request is authorized for Berlioz control.
- `GetSystemProperties` redacts sensitive values.
- The control network check treats `Forwarded` and `X-Real-IP` like `X-Forwarded-For`.

## [0.14.4] — 2026-10-20

### Added

- Group stylesheets fall back to `META-INF/berlioz/xslt/` on the classpath when the filesystem file is absent, so overlays no longer need a stub stylesheet in the host application. Filesystem files take precedence, including invalid ones. `XsltTransformer.location()` reports the selected source.

## [0.14.3] — 2026-10-16

Reusable overlays as ordinary JAR dependencies.

### Added

- Classpath overlay discovery: one `META-INF/berlioz/services.xml` per JAR (or exploded classpath directory) is loaded automatically. Classpath sources load before filesystem sources, so filesystem declarations take precedence for the same HTTP method and URI pattern. Override warnings name both declarations and their origins.
- `classpath:` prefix for primary and fallback XSLT, with a resolver that allows `xsl:import`/`xsl:include` within the same JAR and explicit `classpath:` references, and denies every network protocol.
- `GetBerliozConfig` and `GetErrorHandlerConfig` diagnostic generators reporting the deployed servlet and error handler configuration (XML and JSON).
- `ListRuntimeServices`, the new name of `GetLiveServices`.
- `ServiceSource`/`ServiceOrigin` descriptors in `ServiceLoader`; `load(File)` and `listServiceFiles()` remain source-compatible.
- `berlioz.redirect.trust-forwarded-headers` option.

### Changed

- Service loading is transactional: all sources are parsed and merged before the registry is published. A failed reload keeps serving the last successful registry and retries after 30 seconds.
- `services!*.xml` files load in lexical order instead of directory-listing order.
- Overlay generator classes are loaded through the web application's class loader.
- Classpath XSLT templates are treated as immutable under the `manual` and `auto` cache modes.

### Deprecated

- `resource:` XSLT prefix: use `classpath:`. Removal planned for 1.0.
- `GetLiveServices`: use `ListRuntimeServices`. Removal planned for 1.0.

### Security

- Redirect validation ignores `X-Forwarded-Host` and `X-Forwarded-Proto` by default.

## [0.14.2] — 2026-09-04

### Changed

- Updated Jackson to 2.22.2 and JSpecify to 1.0.1.

### Fixed

- Request wrappers no longer create an HTTP session unintentionally.

## [0.14.1] — 2026-07-25

Runtime introspection and HTTP `QUERY`.

### Added

- HTTP `QUERY` as a mappable method (`method="query"` in `services.xml`). `application/x-www-form-urlencoded` bodies are exposed as request parameters when the container does not parse them. A `QUERY` without `Content-Type` or with malformed form encoding receives `400`; a form body over 1 MB or 1,000 fields receives `413`. `QUERY` responses are not cached.
- Supported output formats for each service and generator in `Service.writeTo()`; JSON output for `GetLiveServices` and `GetMatchingService`.
- `HttpException.header(name, value)` for response headers such as `Retry-After`, and `HttpException.findIn(Throwable)`.
- `ContentType` class for parsing `Content-Type` values.
- Warnings for misconfigured service registrations in `GetServices`.
- Media types for modern web formats (WebP, AVIF, JPEG XL, WOFF2, WebAssembly, WebM, and others).

### Changed

- `ErrorHandlerServlet` renders errors in the media type of the Berlioz servlet that handled the request, falling back to the URL extension. JSON errors are always `application/problem+json`.
- Default `.auto` error forwarding is derived from the extension mappings of the deployed Berlioz servlets.
- Errors for unknown non-asset extensions use the HTML failsafe page instead of raw XML.
- `GetMatchingService` uses a single root element with a `matched` flag.
- A direct service whose generator supports no output format is rejected.

### Fixed

- The service `cache-control` diagnostic reported the length of the value instead of the value.
- XML responses for non-`GET`/`HEAD` requests to cacheable services were missing cache headers.
- Exception cause-chain traversal during error rendering is limited to 100 levels.

## [0.14.0] — 2026-07-21

Secure defaults.

### Added

- Authorization for control parameters through `berlioz.control.key`, `berlioz.control.network`, or a delegated request attribute.
- Configurable gzip compression thresholds.
- Sensitivity-aware XSLT diagnostics.
- JSON output for the built-in system and diagnostic generators where supported.
- A warning at load time for deprecated option values.

### Changed

- RFC 9457 Problem Details is the default error format, and `berlioz.errors.detail` defaults to `minimal`.
- Control parameters are closed by default.
- GET-via-POST fallback is disabled and XML 1.0 headers are the default.
- Legacy error XML uses a unified `<error http-class="…">` element.
- XSLT failures go through the standard HTTP 500 error pipeline.

### Deprecated

- `berlioz.errors.problem=false` (legacy error XML), `berlioz.http.get-via-post=true` and `berlioz.xml.header.version=0.9`.

### Fixed

- `Content-Length` for UTF-8 responses.
- Several thread-safety issues.

## [0.13.5] — 2026-07-03

Problem Details and the configurable error handling pipeline. Final 0.13 release.

### Added

- `HttpException` base class for short-circuiting a request with an HTTP error (400–599) from a generator.
- `Problems` factory, `ProblemExtension` and `ExceptionDetail`; custom problem status codes through `ProblemDetails.of(int)` and `Response.problem(…)`.
- Direct services return problems as `application/problem+json` or `application/problem+xml`; envelope services embed them inline, with the final status governed by the service's response-code rule.
- `berlioz.errors.problem` to emit RFC 9457 `<problem>` XML from `ErrorHandlerServlet`.
- `berlioz.errors.detail` (`minimal`, `standard`, `full`) controlling diagnostic detail in error responses.
- `berlioz.errors.stylesheet` for a custom error XSLT, with fallback to the built-in failsafe.
- A diagnostic page when no XSLT 2.0 processor is available.
- `Server-Timing` metrics for direct JSON services.
- `ContentStatus.UNAUTHORIZED`.
- A minimal sample application (`samples/minimal-gretty`).

### Changed

- Problem APIs live in `org.pageseeder.berlioz.error`.
- The failsafe XSLT renders `<problem>` responses.

### Security

- Sensitive HTTP headers and parameters are redacted in full-detail error output.

## [0.13.4] — 2026-06-25

### Added

- `${variable}` tokens in configuration properties, with defaults and environment variables.

### Fixed

- URI template path variables separated by dots match again.

## [0.13.3] — 2026-06-13

### Added

- `UpstreamException` (502 Bad Gateway) with an optional `upstream-service` extension.
- Writing `Iterable` values with `OutputWriter`, `XmlWriter` and `JsonWriter`.
- `XmlWriterAdapter` bridging the `XmlWriter` and legacy `XMLWriter` APIs.

## [0.13.2] — 2026-06-09

Output-aware generators and the response model.

### Added

- `BerliozGenerator` hierarchy: `XmlGenerator`, `JsonGenerator`, `Generator` and `RawGenerator`, alongside the legacy `ContentGenerator`.
- `Response` for generator status, redirects, headers and problems, and `ProblemDetails` as an RFC 9457 value object.
- Direct JSON responses, with ETag and conditional request handling for cacheable generators, and JSON profiling output.
- `patch` method, `<namespace>` and `<handler>` in the services schema.
- Supported-output negotiation across a service's generators, with warnings for disjoint output formats.
- `ContentStatus.PERMANENT_REDIRECT` (308).
- `RedirectPolicy` and `berlioz.redirect.allowed-hosts`.

### Security

- Hardened redirects, relocations, stylesheet resolution, environment path resolution, web bundle CSS references and response header validation.

## [0.13.1] — 2026-06-04

### Added

- Typed request parameters: `Request#parameter(…)` and reusable `ParameterSpec<T>` definitions, with required values, defaults, enums, ranges, clamps, regular expressions and custom parsers.
- `XsltTemplateCache` and `XsltCacheMode`.

### Changed

- XML parsing moves to `Xml`.
- XSLT cache ETags are deterministic across restarts.
- The failsafe XSLT template is compiled once.

### Deprecated

- `XMLUtils`: use `Xml`.

### Security

- `GetGlobalConfig` redacts sensitive property values.
- `GetParameters` limits the number, name length and value length of parameters.

## Earlier Releases

For 0.13.0 and earlier, see the [release tags](https://github.com/pageseeder/berlioz/tags).

[Unreleased]: https://github.com/pageseeder/berlioz/compare/v0.14.4...HEAD
[0.14.4]: https://github.com/pageseeder/berlioz/compare/v0.14.3...v0.14.4
[0.14.3]: https://github.com/pageseeder/berlioz/compare/v0.14.2...v0.14.3
[0.14.2]: https://github.com/pageseeder/berlioz/compare/v0.14.1...v0.14.2
[0.14.1]: https://github.com/pageseeder/berlioz/compare/v0.14.0...v0.14.1
[0.14.0]: https://github.com/pageseeder/berlioz/compare/v0.13.5...v0.14.0
[0.13.5]: https://github.com/pageseeder/berlioz/compare/v0.13.4...v0.13.5
[0.13.4]: https://github.com/pageseeder/berlioz/compare/v0.13.3...v0.13.4
[0.13.3]: https://github.com/pageseeder/berlioz/compare/v0.13.2...v0.13.3
[0.13.2]: https://github.com/pageseeder/berlioz/compare/v0.13.1...v0.13.2
[0.13.1]: https://github.com/pageseeder/berlioz/compare/v0.13.0...v0.13.1
