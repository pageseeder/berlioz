# 18. Admin Overlay Upstreaming (Requested By Berlioz Admin)

**Status:** 0.14.5 part done; 0.14.6 items 11–13 done · **Target:** items 1–8 and part of item 9 in 0.14.5; items 11–14 in 0.14.6; item 10 in 1.0

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

## 0.14.6

Requested in the bzadmin proposals for Berlioz (`docs/design/berlioz-proposals.md` of the bzadmin repository).

11. ✓ **Redirect and relocation filter snapshot**
    - bzadmin read and parsed each mapping file again, repeated `ConfigLoader`'s DOCTYPE handling and size limit, and validated with the schemas of the Berlioz JAR, which are not an API. Its view showed the file, which can differ from the rules in use until the next reload.
    - `RedirectFilter.diagnosticSnapshot()` and the `GetRedirectConfig` generator report every filter instance: name, type, `config` parameter (relative to `WEB-INF`), whether the file exists, the status (`no-config`, `not-loaded`, `loaded`, `failed`) and time of the last load, `reload-pending`, the failure (its message only when `berlioz.errors.detail` is not `minimal`), the rules loaded (`from`, `to`, `permanent`), and the rules ignored because a pattern is missing or invalid (`position` in the file, `reason`, `from`, `to`), at most 2000 of each. `RedirectConfig` and `RelocationConfig` record the ignored rules (`ignored()`, `IgnoredRule`).
    - Targets are redacted with `Redaction.redactUriPattern` (item 13): the password of a URL, or its user information without password, and every query parameter value that is not a URI template expression are `[REDACTED]`.
    - Acceptance: bzadmin's `MappingFile`, `MappingParser` and copied DOCTYPE handling can be removed, keeping its `ignored-rule` warning.
12. ✓ **Reloads that reach every filter**
    - The `redirects` scope applied only to the filters that the reload request passed through, so a filter mapped to other paths kept its rules after "Reload everything"; bzadmin warned about it (`reload-not-covered`) from an analysis of the filter mappings.
    - A reload counter is incremented once per request with the authorized `redirects` scope, by `BerliozServlet` or a filter; each filter reloads its rules on its next request once the counter has changed. The servlet reports `redirects` only when a filter with a `config` file is initialized.
    - Acceptance: after a reload through the servlet, a filter that the request did not pass through applies the new rules on its next request; bzadmin's `reload-not-covered` warning can be removed.
13. ✓ **Value-based redaction** (security fix)
    - `Redaction` redacted by name only, so a credential in a value with an ordinary name was shown: a URL with user information (`jdbc:postgresql://app:s3cret@db/app`) or a sensitive parameter (`?password=s3cret`) in a global setting, a system property, or a request header or parameter of error details. bzadmin redacted system properties and redirect targets itself (`SystemProperties.redact`, `Targets.redact`).
    - `Redaction.redactValue(value)` redacts the password of a URL anywhere in the value (the whole user information when it has no `:`) and the values of parameters with a sensitive name, after `?`, `&`, `;`, whitespace or at the start. `redact(name, value)` and `redactHeader(name, value)` apply it when the name is not sensitive, so every caller is covered.
    - `Redaction.redactUriPattern(pattern)` replaces `MappingFilters.redactTarget`, with the same user information rule. Query values are all redacted there, since the parameter names of a target do not reveal a credential.
    - `GetSystemProperties` replaces the program arguments of `sun.java.command` after the main class or JAR: a rule for one property, kept in the generator.
    - Acceptance: bzadmin's `GetSystemProperties`, `SystemProperties` and `Targets.redact` can be removed.
14. **Logging initialisation snapshot**
    - `AppInitializer` chooses the logging configuration for the mode: `logback-<mode>.xml` in the application data, then in `WEB-INF`, then `logback.xml`, and the same for log4j. It reports what it did on the console and in the log only. bzadmin cannot tell which file is in use or which failed before it: logback does not keep its configuration file (since 1.5.27, only when `scan` is on, and a reset clears it), no status marks Berlioz's reset, and logback's configuration events are intended for internal testing. bzadmin copies the order of the candidates, which can change with Berlioz.
    - Berlioz keeps a read-only snapshot of the startup configuration, built from what `LogbackSetup` already records (`Result`, `Attempt`, `Outcome`) and from the log4j attempts: the framework (`logback`, `log4j` or `none`), the mode, each candidate in the order tried with its path relative to `WEB-INF` or the application data and its outcome (`absent`, `loaded`, `loaded-with-errors`, `failed`), the file in use, whether Berlioz fell back to its console default, and when. A `Generator` reports it in XML and JSON.
    - The problems reported by logback and the failure of a file (class and message) are shown only when `berlioz.errors.detail` is not `minimal`, as for the filter snapshot. Absolute paths are never shown.
    - The snapshot reports what Berlioz did at startup ([ADR-0013](../decisions/0013-diagnostics-report-only-known-facts.md)), not the current state of logback, which an application may reconfigure afterwards.
    - Acceptance: bzadmin's copy of the candidate order can be removed; its `not-writing` and `problems` verdicts can refer to the snapshot.

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

Decided 2026-11-11, implemented in 0.14.6:

- **Reload counter, not a registry**: a registry of reloadable components would reload on the control request itself and report each outcome, but adds registration, a lifecycle and an API that invites growth. A counter checked by each filter on its next request is a few lines, reaches every instance, and loads the rules where they are used, as at startup. The filter snapshot shows the outcome of each load. The filters are still tracked, for the snapshot and to know whether `redirects` applies, but they are not called by the reload.
- **Lazy reload**: an invalid file appears on the filter's next request, not in the reload response.
- **One snapshot for both filter types**: `RedirectFilter.diagnosticSnapshot()` and `RelocationFilter.diagnosticSnapshot()` return the same snapshot, so that one generator reports both.
- **Rejected for Berlioz** (bzadmin proposals): control parameters only on services that declare them, the reload scopes for generators, stable schemas for every `@Beta` generator, and public `Threads`/`OptionalMetrics` helpers. The reasons are in the bzadmin proposals document.
