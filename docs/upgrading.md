# Upgrading Berlioz

This guide lists what an application may need to change when upgrading Berlioz. Releases not listed here need no application changes. The full list of changes is in the [changelog](../CHANGELOG.md).

Berlioz is pre-1.0: minor versions (0.13, 0.14) may contain breaking changes. Within the 0.14 line, deprecated behavior keeps working until 1.0, so upgrade one release at a time and clear deprecation warnings as you go.

## Planned removals in 1.0

These are deprecated in 0.14 and will be removed in 1.0. Each logs a warning when used.

| Deprecated | Since | Replacement |
|---|---|---|
| `berlioz.errors.problem=false` (legacy error XML) | 0.14.0 | RFC 9457 Problem Details (the default) |
| `berlioz.http.get-via-post=true` | 0.14.0 | Map services to `POST` explicitly |
| `berlioz.xml.header.version=0.9` | 0.14.0 | XML header version `1.0` (the default) |
| `resource:` XSLT prefix | 0.14.3 | `classpath:` |
| `GetLiveServices` generator | 0.14.3 | `ListRuntimeServices` |
| `reload-services=true` | 0.14.5 | `berlioz-reload=services` |
| `clear-xsl-cache=true` | 0.14.5 | `berlioz-reload=xslt` |
| `reset-etags=true` | 0.14.5 | `berlioz-reload=etags` |
| `GetCPUTime` generator | 0.14.5 | `GetCpuSample` |

## Upgrading to 0.14.5

0.14.5 reduces what the kickstart web fragment exposes by default and scopes the `berlioz-reload` control parameter.

- The default `*.xml` output (no `xml/{GROUP}.xsl` for the group) no longer includes the `<header>` or internal `<content>` attributes such as `@generator`, `@etag` and `@profile*`. Clients reading `header/…` from the default `.xml` output need an application template. See [Default XML and source output](../README.md#default-xml-and-source-output-since-0145).
- The `*.src` mapping now returns `404` unless the request is authorized for Berlioz control. No code should rely on `.src`; developers who use it locally should set `berlioz.control.network=loopback` (as most already do for the control parameters), or `berlioz.control.key`.
- `GetBerliozConfig` reports a new `requires-control` servlet field, the stylesheets selected so far (`stylesheet/selection`), the XSLT processor (`xslt-processor`) and the servlet container (`container`). Clients that treated every child of `berlioz-config` as a servlet should select `servlet` elements.
- `ListRuntimeServices` reports the `source` of each service and the declarations it overrides (`overridden`), which were previously only logged as warnings.
- `GetBerliozConfig` lists Berlioz servlets that are not initialized yet, with only `name`, `mappings` and `initialised="false"`, and now reports application-relative stylesheet paths such as `/xslt/html/{GROUP}.xsl` (as `xslt/html/{GROUP}.xsl`) that were previously withheld.
- `reload-services`, `clear-xsl-cache` and `reset-etags` are deprecated: use `berlioz-reload=services`, `berlioz-reload=xslt` and `berlioz-reload=etags`. They keep working until 1.0. `berlioz-reload=true` is unchanged. See [Control parameters and scoped reload](../README.md#control-parameters-and-scoped-reload-since-0145).
- `GetCPUTime` is deprecated: use `GetCpuSample`, which returns cumulative counters; compute the usage between two samples.
- A configuration reload that fails to parse now keeps the last valid configuration instead of reverting every option to its default (which also turned off control authorization).
- A `<response-code>` declared directly under `<service-config>` now applies to every service whose group and service declare none. It was previously ignored, so those services used the default rule (`use="name:*" rule="highest"`) and may now compute a different status code. Remove the root-level element to keep the previous behaviour. `ListRuntimeServices` reports which rule applies in `response-code/@origin`.

## Upgrading to 0.14.3

0.14.3 adds classpath overlay discovery, classpath-backed XSLT, and two new runtime diagnostics. None of it requires a configuration change for an existing filesystem-only application:

- An application with a `WEB-INF/config/services.xml` and no classpath overlay dependencies behaves exactly as before: there is nothing on the classpath to discover, so filesystem services remain the only source, and existing mapping precedence is unchanged.
- Existing `resource:` stylesheet references continue to resolve exactly as before, but `resource:` is now deprecated and will be removed in 1.0 — `classpath:` is the canonical spelling and the only form that will remain. Migrate existing `resource:` references when convenient; there is no behavior change to work around before 1.0.
- `services!*.xml` filesystem modules now load in deterministic lexical order. Previously this followed directory-listing order, which was not guaranteed across platforms; an application relying on a specific non-alphabetical load order (uncommon, since override behavior did not previously distinguish file origin) should rename its modules accordingly.
- A failed *reload* (not startup) now keeps serving the last successful registry, including its warnings and ETags, instead of leaving the application with a cleared registry while the retry is pending. This is a reliability fix, not a behavior an application should have depended on.
- Adding a classpath overlay dependency is new, additive behavior: it only takes effect once such a dependency is actually present on the classpath. Filesystem declarations deterministically override classpath declarations for the same method and URI pattern, so an existing application can add an overlay dependency without its own `services.xml` losing precedence.
- `org.pageseeder.berlioz.generator.GetLiveServices` is renamed to `org.pageseeder.berlioz.generator.ListRuntimeServices`. `GetLiveServices` remains available as a deprecated forwarding alias with identical behavior and output until removal at 1.0; existing `services.xml` generator references do not need to change, but new configuration should reference `ListRuntimeServices` directly.
- Redirect validation now ignores `X-Forwarded-Host` and `X-Forwarded-Proto` by default. Behind a reverse proxy, configure trusted forwarded-header handling in the servlet container so its request host, scheme, and port reflect the public origin. If container handling is unavailable, `berlioz.redirect.trust-forwarded-headers=true` restores header-based origin detection only when every request passes through a trusted proxy that overwrites these headers and direct backend access is prevented.

## Upgrading to 0.14.1

- `GetMatchingService` has a single root element with a `matched` attribute (JSON: `matched` field) for both the found and not-found cases. Clients that distinguished the two cases by root element should read `matched` instead.
- `ErrorHandlerServlet` renders errors in the media type configured for the Berlioz servlet that handled the request, rather than inferring it from the URL extension. JSON errors are always `application/problem+json`, even with `berlioz.errors.problem=false`.
- Errors for unknown non-asset extensions now use the HTML failsafe page instead of raw XML; `.xml` and `.src` still return raw XML.
- A direct service whose single generator supports no output format is now rejected at load time instead of being registered silently.

## Upgrading to 0.14.0

0.14.0 moves the defaults to the production-oriented side. Each change keeps an opt-out until 1.0, but the opt-outs are deprecated.

- **Problem Details by default.** `ErrorHandlerServlet` emits RFC 9457 Problem Details. Error stylesheets that match the legacy `<server-error>`/`<client-error>` elements must be updated, or set `berlioz.errors.problem=false` temporarily; the legacy format now uses a unified `<error http-class="…">` element.
- **Minimal error detail by default.** `berlioz.errors.detail` defaults to `minimal` (previously `full`): no stack traces, exception classes, headers or parameters in error responses. Set it to `standard` or `full` in development.
- **Control parameters closed by default.** `berlioz-reload` and `berlioz-profile` are ignored unless the request is authorized through one of:
  - `berlioz.control.key`: a shared secret sent as `Authorization: Berlioz <key>`;
  - `berlioz.control.network`: `loopback` or `lan`, for development only;
  - the delegated request attribute `ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE`, set by the host application's own filter.
- **No GET-via-POST fallback.** A `POST` no longer falls back to a service mapped to `GET`. Map the service to `POST`, or set `berlioz.http.get-via-post=true` temporarily.
- **XML 1.0 headers by default.** The `0.9` header's compatibility elements (`<group>`, `<service>`, `<path-info>`, `<host>`, `<port>`, `<url>`, `<query-string>`) are no longer emitted. Update XSLT templates that use them, or set `berlioz.xml.header.version=0.9` temporarily.
- **XSLT failures** produce a standard HTTP 500 error response through the error handling pipeline, so custom error stylesheets apply to them.
