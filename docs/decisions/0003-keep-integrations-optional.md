# 0003. Keep third-party integrations optional

- **Date:** 2026-10-29 (recorded; long-standing principle)
- **Status:** Accepted

## Context

Berlioz is deployed into applications that already have their own dependencies, servlet containers and JSON libraries. Every library Berlioz brings in can conflict with an application's version or force a choice on it.

## Decision

The core module keeps its runtime dependencies to `pso-xmlwriter` and the SLF4J API. Everything else is either:

- `compileOnly`, and used only when present: the servlet API (provided by the container), Jackson, Gson, and JSR-374 JSON (`javax.json` and `jakarta.json`); or
- in a separate module: integrations such as Spring, CDI, metrics, tracing, or binary serialization ([theme 10](../design/10-integration-modules.md)).

## Consequences

- Applications choose their JSON library; Berlioz adapts to it.
- Code that uses an optional library must not be loaded unless that library is on the classpath.
- Integration modules support the Berlioz model (URI templates, generators, XSLT); they do not replace it.
