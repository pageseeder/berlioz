# Changelog

All notable changes to Berlioz are documented in this file. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

Berlioz is pre-1.0: minor versions (0.13, 0.14) may contain breaking changes, which are listed with upgrade instructions in the [upgrade guide](docs/upgrading.md). Planned work is in the [roadmap](ROADMAP.md).

## [Unreleased]

## [0.14.6] — 2026-11-12

Richer administration diagnostics, explicit native logging configuration,
and reliability fixes for error handling and redirect reloads.

### Added

- `ErrorHandlerServlet.BERLIOZ_ERROR_EXCEPTION` (`org.pageseeder.berlioz.error_exception`): the request attribute holding the exception of an error routed by Berlioz.
- `GetRedirectConfig` generator and `RedirectFilter.diagnosticSnapshot()`: every `RedirectFilter` and `RelocationFilter` instance with its `config` parameter, whether the file exists, the status and time of the last load, its failure, the rules loaded, with redacted targets, and the rules ignored because a pattern is missing or invalid, with their position and reason (XML and JSON). `RedirectConfig.patterns()` and `RelocationConfig.patterns()` return the loaded rules, and `ignored()` the ignored ones (`IgnoredRule`).
- `GetLoggingConfig` generator and `AppInitializer.loggingSnapshot()`: how Berlioz configured logging at startup, with each Logback and Log4J file in the order considered, its folder and relative path, its outcome (`absent`, `ignored`, `loaded`, `loaded-with-errors`, `failed`), the file in use, and whether Berlioz fell back to the console (XML and JSON). The problems reported while loading a file and the reason of a failure are only included when `berlioz.errors.detail` is not `minimal`.
- `GetLoggingConfig` reports the SLF4J `backend` (`logback`, `log4j2`, `jul`, `simple`, `nop` or `unknown`), how logging ended up as `configuration` (`file`, `console-default`, `native` or `none`), and, when set, the documented system property locating the backend's native configuration as `native-setting`, with its value sanitized at every detail level.
- `berlioz.logging` option (`auto` or `native`), read like the mode from the servlet init parameter `logging`, the context parameter `berlioz.logging`, the system property `berlioz.logging` or the environment variable `BERLIOZ_LOGGING`. With `native`, Berlioz looks at none of its logging files and leaves logging to the backend's native configuration. In `auto`, Berlioz prints a notice when it loads its own Logback file while `logback.configurationFile` is set.
- `GetGlobalConfig` reports the outcome of the last attempts to load the global configuration: `load-status` (`pending`, `ready`, `startup-failed` or `reload-failed`), `loaded` and `failed` times, and, after a failure, `load-failure` with the name of the file. A failed reload kept the last valid configuration but was only logged. The line and the parser's description of the error are only included when `berlioz.errors.detail` is not `minimal`. `GlobalSettings.getLoadDiagnostics()` returns the same information (`ConfigLoadDiagnostics`).
- `status` attribute on redirect rules (`301`, `302`, `307` or `308`), so that `RedirectFilter` can send method-preserving redirects (307 and 308). It takes precedence over `permanent`; without either, a redirect is temporary (302), as before. The schema and the DTD declare it. `RedirectPattern.status()` and `RedirectLocation.status()` return the status, a new `RedirectPattern(URIPattern, URIPattern, ContentStatus)` constructor accepts it, and `isPermanent()` is `true` for 301 and 308. `GetRedirectConfig` reports the `status` of each rule.
- `ConfigurationSnapshot.entries()` and `fields()` return the content of a diagnostic snapshot, such as `RedirectFilter.diagnosticSnapshot()` or `AppInitializer.loggingSnapshot()`, as unmodifiable maps keyed by the names of the XML/JSON schema, so code can read it without parsing the output of `writeTo`. Values are a `String`, `Boolean`, `Long`, `List<String>`, nested map or list of maps.
- `Redaction.redactValue(String)` redacts credentials whatever the name of the value: the password of a URL (or its user information without password) and the values of parameters with a sensitive name. `Redaction.redactUriPattern(String)` redacts a URI pattern such as a redirect target.

### Changed

- The `redirects` reload scope reaches every `RedirectFilter` and `RelocationFilter`, whatever its mapping. It applied only to the filters that the reload request passed through, so a filter mapped to other paths kept its old rules until a restart. Each filter now loads its rules again on its next request after a reload. `X-Berlioz-Reload` lists `redirects` for a reload made through `BerliozServlet` when a filter with a `config` file is initialized.
- A `RelocationFilter` configuration that fails to load is logged as an error, as for `RedirectFilter`, rather than a warning.
- `Redaction.redact(name, value)` and `Redaction.redactHeader(name, value)` also redact the content of values whose name is not sensitive, so `GetGlobalConfig`, `GetSystemProperties` and error details no longer show a credential in a URL, such as `jdbc:postgresql://app:s3cret@db/app`, or in a parameter, such as `?password=s3cret`. `GetSystemProperties` also redacts the program arguments of `sun.java.command`.

### Deprecated

- The `permanent` attribute of redirect rules and the `RedirectPattern(URIPattern, URIPattern, boolean)` constructor; they will be removed in 1.0. Use `status="301"` instead of `permanent="yes"`, and remove `permanent="no"`. Without `status`, `permanent="yes"` still gives 301. Berlioz logs a warning when it loads a file that uses `permanent`.
- Loading Log4j 1 configuration files (`log4j-<mode>.properties`, `log4j.properties`) at startup; it will be removed in 1.0. Berlioz prints a deprecation notice when it loads one, and a notice when SLF4J uses another backend, since the file may then not configure Berlioz's logs.

### Fixed

- With `berlioz.errors.handle=false`, Berlioz no longer sets `javax.servlet.error.exception` before `sendError`, so Tomcat answers with the status of the error instead of handling it as an uncaught exception: with an error page for `java.lang.Exception`, as Kickstart declares, the error of a generator that throws, such as an `InvalidParameterException` (400), was answered 500. The exception is passed in `org.pageseeder.berlioz.error_exception` instead, which `ErrorHandlerServlet` and `GetErrorDetails` read.
- `RedirectFilter` and `RelocationFilter` without a `config` init parameter no longer fail every request with a `NullPointerException` (500); as logged at startup, they have no effect and pass requests through. A configuration file without rules is now logged as a warning rather than at info level, since the filter then does no useful work. Log messages of both filters name the filter instance.
- The error page of a `POST`, `PUT`, `DELETE`, `PATCH` or `QUERY` request is found: when rendering an error, `BerliozServlet` looks the service up as `GET` instead of answering `405 Method Not Allowed` and replacing the error message.
- `ErrorHandlerServlet` handles errors whatever the HTTP method; the error of a `PUT` or `DELETE` request was answered `405`, and that of a `PATCH` request `501`.
- Resetting an error response keeps the `WWW-Authenticate`, `Proxy-Authenticate`, `Allow` and `Retry-After` headers, so that the challenge of container authentication survives a `401` error page and a `405` keeps its `Allow` header.
- Logback configuration at startup no longer fails silently. When the last Logback file found could not be loaded, such as a malformed `logback.xml`, Berlioz left Logback reset with no appender, so nothing was logged; it now logs to the console at `INFO` level and says so. Errors that Logback reports without failing, such as an appender class that cannot be found, are printed instead of "logback config file OK"; with Logback 1.5, such an error can leave the whole file unapplied. When a file fails and a later one loads, for example `logback.xml` after a malformed `logback-<mode>.xml`, a warning naming the failed file is logged. Logback files are ignored, with a console message, when SLF4J is not bound to Logback. Logback 1.3 or later is supported.
- `berlioz-reload` is no longer applied a second time when Berlioz renders the error of an authorized control request. An error dispatch carries the parameters of the request that failed, so a reload that caused the error, such as `berlioz-reload=services` with an invalid services file, was requested again while the error page rendered, by `BerliozServlet` and by `RedirectFilter` and `RelocationFilter` mapped to error dispatches.
- Rules reloaded by a `RedirectFilter` or `RelocationFilter` are visible to every request thread, and loaded once per reload, even when requests arrive together, such as on first use after startup; the rules were held in a field that was not safely published, and each concurrent request parsed the file.
- `redirect-mapping-1.0.dtd` declares `permanent` as `yes` or `no`, as the schema and the parser do; it declared `true` or `false`, so a file written to the DTD failed validation and the redirect filter had no rules. Existing files that load need no change: the values accepted are the same as before.

## [0.14.5] — 2026-10-29

Small security improvements, a scoped `berlioz-reload`, and diagnostics requested by Berlioz Admin.

### Added

- Main JAR manifests declare stable `Automatic-Module-Name` values (`org.pageseeder.berlioz`, `org.pageseeder.berlioz.kickstart`, `org.pageseeder.berlioz.mock`), `Bundle-License: Apache-2.0`, `Implementation-Vendor-Id`, `Implementation-URL` and `Build-Jdk-Spec: 11`.
- The licence is included as `META-INF/LICENCE.md` in the main, sources and Javadoc JARs.
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

- Updated the optional Gson integration dependency to 2.15.0 and Jackson to 2.22.4.
- `GetSystemProperties` is a `Generator` with JSON output; properties are sorted by name.
- `ListThreads` is bounded to 2000 threads and 256 stack frames, stating the limits in the output when reached.
- A configuration reload that fails to parse keeps the last valid configuration instead of reverting every option to its default.

### Deprecated

- `reload-services`, `clear-xsl-cache` and `reset-etags`: use `berlioz-reload=services`, `xslt` and `etags`. Removal planned for 1.0.
- `GetCPUTime`: use `GetCpuSample`. Removal planned for 1.0.

### Fixed

- The published POMs of `pso-berlioz-kickstart` and `pso-berlioz-mock` now use their own names and descriptions instead of those of `pso-berlioz`.
- Group stylesheet selection no longer repeats filesystem path validation on each request.
- `X-Berlioz-Reload` reports only scopes applied by the filters and servlet reached by the request, including when a redirect ends processing early.
- `ListRuntimeServices` ETags now change when a configuration-only reload changes whether diagnostic details are included.
- A `<response-code>` declared directly under `<service-config>` was ignored; it now applies to groups and services that declare none.

### Security

- The kickstart default `*.xml` output no longer exposes the request `<header>` or internal `content` attributes (`@generator`, `@etag`, `@profile*`).
- The kickstart `*.src` mapping returns `404` unless the request is authorized for Berlioz control.
- `GetSystemProperties` redacts sensitive values.
- The control network check treats `Forwarded` and `X-Real-IP` like `X-Forwarded-For`.
- Forwarding addresses must be dotted-quad IPv4 or IPv6 literals; hostname-shaped values can no longer trigger DNS resolution during control authorization.

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

[Unreleased]: https://github.com/pageseeder/berlioz/compare/v0.14.6...HEAD
[0.14.6]: https://github.com/pageseeder/berlioz/compare/v0.14.5...v0.14.6
[0.14.5]: https://github.com/pageseeder/berlioz/compare/v0.14.4...v0.14.5
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
