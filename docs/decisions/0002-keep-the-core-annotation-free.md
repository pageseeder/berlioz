# 0002. Keep the core annotation-free

- **Date:** 2026-10-29 (recorded; long-standing principle)
- **Status:** Accepted

## Context

Annotation-based programming (parameter validation, authorization, routing) is convenient and was explored in the separate berlioz-plus project, with annotations such as `@Parameter`, `@Authenticated` and a `Constraint` SPI. Annotations in the core would add reflection, hidden behavior and a second programming model next to URI templates, `services.xml` and generator classes.

The generator API is also still settling (`Request`, the `BerliozGenerator` hierarchy, `OutputWriter`), and an annotation module needs a fixed generator signature and a decision on whether constraint failures produce Problem Details.

## Decision

`pso-berlioz` does not define or process annotations for application behavior. Annotation-based programming belongs in optional modules, such as a future `pso-berlioz-annotated`.

That module waits until the generator API is settled, so that early adopters do not face breaking changes. Typed parameter access in the core is programmatic (`Request#parameter(…)`, `ParameterSpec<T>`).

## Consequences

- Applications that do not want annotations pay nothing for them.
- Features that could be annotation-driven, such as [authorization guards](../design/08-authorization-guards.md), are designed programmatically first, with annotations as an optional layer.
- Parts of berlioz-plus are superseded by core features (`ParameterSpec<T>` replaces `ValidatedRequest`; `XmlWriter` already throws unchecked exceptions).
