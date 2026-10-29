# 16. Admin Diagnostics (Requested By Berlioz Admin)

**Status:** 0.14.5 part done · **Target:** items 1–7 in 0.14.5; item 8 and the `cache-control` origin in 1.0

## Motivation

Berlioz Admin (bzadmin) displays Berlioz's own diagnostic generators directly. It only ships an overlay generator where Berlioz has no equivalent. The items below let it drop those workarounds and show information that was marked "not available". Unless stated otherwise, each item only adds new elements or attributes to existing output; existing fields keep their meaning.

Related: [theme 17](17-scoped-reload.md) (scoped reload) and [theme 18](18-admin-overlay-upstreaming.md) (generators and helpers moved from bzadmin into Berlioz).

## 0.14.x (additive)

1. ✓ **Configuration sources in `ListRuntimeServices`**
   - List the service configuration sources in load order, after the services: `<source kind="classpath|filesystem" name="bzadmin-0.14.0-beta.jar!META-INF/berlioz/services.xml"/>` (JSON: `sources` array).
   - Delivered: sources are those of the last successful load (from `ServiceLoader.getLoadDiagnostics()`, not rediscovered per request), so they always match the listed services and the ETag.
   - Acceptance: a host with an overlay JAR, `services.xml` and `services!x.xml` lists all three, classpath sources first.
2. ✓ **Load diagnostics in `ListRuntimeServices`**
   - The warnings of the last successful load: `<warning line="…" source="…">message</warning>`. Include `line` only when `berlioz.errors.detail` is not `minimal`, as `GetServices` does.
   - When a reload fails, its cause (`<load-failure source="…" line="…">message</load-failure>`), kept until the next successful load. The message is generic; the line and parser message are included only when `berlioz.errors.detail` is not `minimal`.
   - Timestamps: `loaded` (last successful load), `failed` (last failed attempt) and `retry-after` (when Berlioz will try again), as ISO 8601 attributes on the root element.
   - Acceptance: after an authorized service reload (`berlioz-reload=services`) with a malformed `services!x.xml`, `load-status="reload-failed"`, the last valid services are still listed, and `load-failure` names the file and the parse error.
3. ✓ **Which source declared each service, and overrides**
   - `service/@source`: the declaring source's display name, recorded when the service is registered.
   - When a later source replaces the same method and URL pattern, the replaced declaration is written after the generators as `<overridden service="…" source="…" pattern="…"/>` (JSON `overridden` array), in load order when several sources declare the same key. Previously this was only reported as a warning.
   - Acceptance: a pattern declared in a JAR and replaced by `services.xml` shows the filesystem source on the service and the JAR declaration as overridden.
4. **Provenance of effective values in `Service.writeTo`**
   - ✓ (0.14.5) `response-code/@origin`: `service`, `group`, `service-config` or `default`.
   - (1.0, with item 8) `cache-control-origin="service|servlet|configuration|default"` alongside `cache-control`; there is no group level for `cache-control`.
   - Only output an origin where Berlioz actually knows it; never infer it.
5. ✓ **Option catalog generator (`ListBerliozOptions`)**
   - For each `BerliozOption`: `property`, `type` (`boolean|string`), `default`, `deprecated` (version) and `removal` (version), plus a one-line `description` maintained with the option.
   - Supports XML and JSON. Cacheable: the catalog is constant for a given Berlioz version. Declaration order.
   - The value in use and its origin are left out: `GetGlobalConfig` returns the values, and provenance belongs with [configuration validation](05-configuration-validation.md).
   - Replaces bzadmin's `GetBerliozOptions` and the option descriptions it maintains.
6. ✓ **`GetBerliozConfig` completeness**
   - Registered Berlioz servlets that are not yet initialized are listed from `ServletContext.getServletRegistrations()` with `name`, `mappings` and `initialised="false"`. Previously `BerliozXML` and `BerliozJSON` only appeared after their first request.
   - Filesystem stylesheet paths are reported relative to `WEB-INF` as Berlioz resolves them (`/xslt/html/{GROUP}.xsl` as `xslt/html/{GROUP}.xsl`), still withholding traversal and anything outside `WEB-INF`.
7. ✓ **Public secure XSLT resolver**
   - `XsltResolvers.secure()`/`secure(ClassLoader)` expose the resolver policy (classpath references mapped to their `jar:`/`file:` URL, local-only URLs, no network), and the renderer uses it too. `XsltResolvers.locate(href, base, loader)` returns the validated URL of any approved reference, for tools reading stylesheets without a processor. bzadmin's XSLT documentation previously re-implemented the rules.

## 1.0

8. **Stable `ListRuntimeServices`**
   - Details to be confirmed. Includes reporting `cache-control` per servlet mapping, which the `cache-control` origin (item 4) depends on.

## Decisions

Related decision records: [ADR-0013](../decisions/0013-diagnostics-report-only-known-facts.md).

Decided 2026-10-29, implemented in 0.14.5:

- **Response code origin**: Berlioz reports it because only the services parser knows which level declared the rule once overlays are merged. Values are `service`, `group`, `service-config` (the root element, named after it to avoid confusion with `config.xml`) and `default`; services built without an origin report none.
- **Root-level rule fix**: a `<response-code>` under `<service-config>`, allowed by the DTD and schema, was ignored in favour of the default rule. It now applies to groups and services that declare none. This can change status codes, so it is in the 0.14.5 migration notes.
- **`cache-control` origin deferred to 1.0**: there is no group level, so the values would be `service`, `servlet`, `configuration` and `default`. `ListRuntimeServices` reports the `cache-control` of the servlet serving the diagnostic, not necessarily the one serving the service, so an origin would label a value that can be wrong. It waits for per-mapping reporting in a stable `ListRuntimeServices` (item 8).
