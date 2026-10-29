# 0013. Diagnostics report only what Berlioz knows

- **Date:** 2026-10-29 (0.14.5)
- **Status:** Accepted

## Context

Tools such as Berlioz Admin asked for more diagnostics: where each effective value comes from, which stylesheets are available, which source wins. Some of these Berlioz knows exactly; others it could only infer, or compute in a way that depends on the container.

## Decision

Diagnostic generators report a fact only where Berlioz itself determines it; they never infer one.

- **Reported**: the declaring source of each service and the declarations it overrides (known from the merge); `response-code/@origin` (`service`, `group`, `service-config` or `default`), because only the services parser knows which level declared the rule once overlays are merged; the stylesheet source selected for a path.
- **Deferred**: the `cache-control` origin. `ListRuntimeServices` reports the `cache-control` of the servlet serving the diagnostic, not necessarily the one serving the service, so an origin would label a value that can be wrong. It waits for per-mapping reporting in 1.0.
- **Not in Berlioz**: listing the stylesheets available on the classpath. Berlioz only ever resolves one name at a time, and listing classpath folders depends on how each container packages JARs (`vfs:`, `wsjar:`, nested JARs). Berlioz exposes its selection rule instead (`StylesheetLocation.select`, `countClasspathCopies`).

Where a value is unavailable, it is absent rather than zero or a guess.

## Consequences

- Tools can trust what diagnostics report, and keep presentation heuristics (such as classifying threads) on their side.
- Values that are left out are explicitly documented, so their absence is not mistaken for a bug.
