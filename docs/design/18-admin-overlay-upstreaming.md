# 18. Admin Overlay Upstreaming (Requested By Berlioz Admin)

**Status:** 0.14.5 part done · **Target:** items 1–8 and part of item 9 in 0.14.5; item 10 in 1.0

## Motivation

Berlioz Admin (bzadmin) replaces or works around several Berlioz generators and helpers. The items below move that work into Berlioz, so that any application gets the fixes and bzadmin can delete its copies. They complement [theme 16](16-admin-diagnostics.md), which covers the diagnostic output itself, and [theme 17](17-scoped-reload.md), which covers the scoped `berlioz-reload`. The bzadmin side is planned in `docs/implementation/11-berlioz-upstream.md` of the bzadmin repository.

## 0.14.x (fixes and additive)

1. ✓ **Redacted `GetSystemProperties`** (security fix)
   - It was an `XmlGenerator` writing every system property unsorted and unredacted, unlike `GetGlobalConfig`.
   - Values are redacted with `Redaction.redact(name, value)` and sorted by name; it implements `Generator` (JSON: `properties` array of `{name, value}`), with the XML unchanged apart from the order and the redacted values.
   - Acceptance: a system property `fixture.password` is `[REDACTED]` in XML and JSON; properties are sorted.
   - Replaces bzadmin's `GetSystemProperties`.
2. ✓ **Non-blocking CPU sample (`GetCpuSample`)**
   - `GetCPUTime` sleeps on the request thread (100 ms by default, up to 5 s with `interval`), measures only that window, truncates to whole percents and sums the CPU time of live threads only, so it mostly reports 0.
   - `GetCpuSample` returns cumulative counters without sleeping: `<cpu-sample processors="…" uptime-ms="…" process-time-ns="…" gc-time-ms="…" system-load="…" load-average="…"/>`. Clients compute the usage between two samples. JDK-specific metrics are read by MBean attribute name, never through `com.sun.management`; an unavailable value is absent, never zero.
   - `GetCPUTime` is deprecated for removal in 1.0.
   - Replaces bzadmin's `GetCpuSample` and `OptionalMetrics`.
3. ✓ **Richer, bounded `ListThreads` and `GetThreadInfo`**
   - Per thread: `blocked-count`, `waited-count`, `in-native`, `deadlocked`, `allocated-bytes` (invoked by operation name on the threading MBean, absent when unavailable), and a `lock` element with its owner. `GetThreadInfo` also lists `locked-monitors` and `locked-synchronizers`.
   - Bounded: at most 2000 threads (`total` and `limit` on the root when reached) and 256 frames (`stack-limit` on the thread when reached).
   - Thread classification (component, role) stays out of core: it is a presentation heuristic and stays in bzadmin (`ThreadOrigin`).
   - Replaces bzadmin's `ListThreads`, `GetThreadInfo` and `ThreadDetails`.
4. ✓ **`GetGeneratorStatistics` as a `Generator`**
   - JSON output (`since`, `statistics` array, status counts in camel case); XML unchanged.
5. ✓ **Public list of control parameters and reload scopes**
   - The control parameters were string literals in `BerliozServlet.applyBerliozControl`; bzadmin's `AdminFilter` copied the list to reject them outside its own control services.
   - Delivered as the `ControlParameter` and `ReloadScope` enums with `isControlParameter(String)` and a public parser of `berlioz-reload` values, used by `BerliozServlet`, `RedirectFilter` and `RelocationFilter`. No new parameter.
   - Acceptance: a test fails if a control parameter or scope is applied that the public lists do not contain.
6. ✓ **`BerliozOption` metadata accessors**
   - `type()` (`BerliozOption.Type`), `description()`, `isDeprecated()`, `deprecatedSince()` and `removal()`, as the data source of the option catalog ([theme 16](16-admin-diagnostics.md), item 5). A test keeps them consistent with `@Deprecated`. bzadmin read `@Deprecated` by reflection.
7. ✓ **Network check covering all forwarding headers, made public** (security fix)
   - `ControlNetwork.LOOPBACK` and `LAN` checked the remote address and every hop of `X-Forwarded-For` only. bzadmin's fallback login (`AdminFilter.isLoopback`) is stricter: it refuses any `Forwarded`, `X-Forwarded-Host`, `X-Forwarded-Proto` or `X-Real-IP` header, and requires the local address and the server name to be loopback.
   - `Forwarded` (every `for=` parameter, RFC 7239) and `X-Real-IP` are now treated like `X-Forwarded-For`, for both `LOOPBACK` and `LAN`: each address must match the network, and an unparseable or obfuscated value (`unknown`, `_hidden`) fails. Literals are parsed, never resolved. This closes the gap for proxies that set these headers instead of `X-Forwarded-For`, and keeps local dev proxies working, since loopback hops still pass.
   - Exposed publicly as `ControlNetwork.matches(HttpServletRequest)`, used by `ControlAuthorization.hasControl`, so bzadmin can build on it and keep its stricter login rules on top.
   - Acceptance: a loopback request with `Forwarded: for=203.0.113.1` or `X-Real-IP: 203.0.113.1` is not authorized by the loopback channel; one with `Forwarded: for=127.0.0.1`, `X-Real-IP: ::1` or `Host: myapp.local` still is.
8. ✓ **Identity of the XSLT processor and the servlet container**
   - `GetBerliozConfig` reports `xslt-processor` (factory class, and vendor, XSLT version and product from the standard `xsl:` system properties, so no Saxon reflection) and `container` (`server-info`, `servlet-api`).
   - Generators read the container from `Environment.getServerInfo()`/`getServletApiVersion()`, recorded by each Berlioz servlet from its `ServletContext` rather than by `InitServlet`, and the processor from `XsltProcessorInfo.current()`.
9. **Stylesheet listing and selected source**
   - ✓ (0.14.5) A public way to locate one path, `StylesheetLocation.select(webInf, path)`, also used by `BerliozConfig`, and the number of copies on the classpath, `StylesheetLocation.countClasspathCopies(path)`.
   - ✓ (0.14.5) `GetBerliozConfig` lists the stylesheets selected so far under `stylesheet/selection` (`name`, `source`, safe `path`).
   - Not in Berlioz: listing the classpath stylesheets under `META-INF/berlioz/xslt/` merged with the XSLT folder.
   - Replaces bzadmin's override detection; its JAR scanning (`XsltFiles`) remains for the listing.

## 1.0

10. **Service-level authorization hook**
    - bzadmin wraps every Berlioz generator it uses in a one-line delegating class that checks the admin verdict first, because Berlioz cannot guard a service. A service or generator guard ([theme 8](08-authorization-guards.md)) would let applications use Berlioz generators directly, and ensure a new service cannot forget the check.
    - Acceptance for bzadmin: its `org.pageseeder.bzadmin.guard` package can be removed, with every admin service still denied without a verdict.

## Decisions

Related decision records: [ADR-0013](../decisions/0013-diagnostics-report-only-known-facts.md).

- **Network check scope**: bzadmin's other rules are not adopted. Refusing on header presence breaks local dev proxies. The `Host` check breaks custom dev hostnames (`myapp.local`, `*.localhost`) and does not catch a bare `proxy_pass`, which sends the upstream's `Host`. The local-address check adds nothing for a loopback caller. The channel remains a development convenience (`off` in production); the reverse-proxy caveat on `CONTROL_NETWORK` still stands.

Decided 2026-10-29, implemented in 0.14.5:

- **Stylesheet lookup in Berlioz**: Berlioz owns the selection rule (an existing file wins, then the first `META-INF/berlioz/` copy for paths under `xslt/`), so it exposes it as `StylesheetLocation.select(webInf, path)` and `StylesheetLocation.countClasspathCopies(path)`. Tools no longer re-implement override detection.
- **No stylesheet listing in Berlioz**: Berlioz only ever resolves one name at a time. Listing a classpath folder means scanning JARs and directories, which depends on how each container packages them (`vfs:`, `wsjar:`, nested JARs). The listing stays in bzadmin, which asks Berlioz which source wins for each path.
