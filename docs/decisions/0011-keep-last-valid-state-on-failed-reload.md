# 0011. Keep the last valid state when a reload fails

- **Date:** 2026-10-16 (services, 0.14.3); 2026-10-28 (configuration, 0.14.5)
- **Status:** Accepted

## Context

Configuration and services can be reloaded at runtime. A reload that fails halfway, for example on a malformed `services!x.xml`, used to leave the application with a cleared service registry, or with every option reverted to its default, which also turned off control authorization.

## Decision

Reloads are transactional: the new state is built and validated as a candidate, and published atomically only if it is complete.

- **Services**: every source is parsed into an isolated candidate, the candidates are merged in source order, and the registry is published in one step. If any source fails, the previous registry keeps serving, with its warnings, sources and ETags, and Berlioz retries after 30 seconds or on demand.
- **Configuration**: `GlobalSettings.load()` keeps the last valid configuration when a reload fails.
- **Startup** is unchanged: there is no previous state to keep.

Failures are reported, not hidden: `ListRuntimeServices` shows the failing source and the time of the next retry.

## Consequences

- A typo in a configuration file cannot take a running application down or open its control surface.
- Planned [configuration validation](../design/05-configuration-validation.md) follows the same rule: a candidate that fails validation is not published.
