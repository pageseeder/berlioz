# 3. Service Metadata And Diagnostics

**Status:** Done · **Target:** 0.14.1

## Motivation

Service inspection matters more now that services can expose different output formats and response modes. Services should be easy to inspect, document, test, and debug without adding metadata to every normal response or introducing heavy runtime machinery.

## Delivered

- `Service.writeTo(OutputWriter, ...)` exposes service identifier, group, method, URI templates, direct mode, cacheability, cache policy, response-code rule, and both the service's supported-output intersection and each generator's own explicit supported-output set (including custom `BerliozGenerator` implementations, whose capabilities cannot be inferred from the type label alone).
- Generator entries expose class, name, target, type, cacheability, status participation, and configured parameters.
- `GetLiveServices` (renamed `ListRuntimeServices` in 0.14.3) and `GetMatchingService` are format-agnostic `Generator` implementations that serialize the effective in-memory registry with equivalent, stable XML and JSON shapes (plural JSON array keys: `generators`, `parameters`, `urls`).
- `GetServices` deliberately continues to copy the source configuration and carries no runtime-derived attributes.
- `GetMatchingService` reports a single stable root shape (`matched` boolean) rather than two different root elements for the found and not-found cases, keeping its JSON output unambiguous.
- Diagnostic warnings for disjoint output sets and invalid direct services are computed by testable static methods (`Service.disjointOutputWarning()`, `Service.invalidDirectWarning()`); the invalid-direct check also rejects a direct service whose single generator supports no output format. The registry override warning (`ServiceRegistry.overrideWarning()`) names both the replaced and replacing service.
- Stable-schema XML/JSON tests exist for `Service.writeTo()`, `ListRuntimeServices`, and `GetMatchingService`, plus registration-behavior tests for duplicate patterns within a file and mapping overrides across registrations.

Later diagnostic work requested by Berlioz Admin continues in [theme 16](16-admin-diagnostics.md).

## Decisions

Related decision records: [ADR-0014](../decisions/0014-format-agnostic-built-in-generators.md).

- **Where service metadata belongs**: runtime-derived metadata belongs in the live diagnostic generators (`ListRuntimeServices`, `GetMatchingService`); normal responses and the raw `GetServices` source view remain unchanged.
