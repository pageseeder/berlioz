# 10. Optional Integration Modules

**Status:** Exploring · **Target:** 1.1+

## Motivation

Keep Berlioz core small, but provide integration points for applications that already use other Java frameworks. These integrations should support Berlioz rather than replace its URI template, generator, and XSLT model.

## Proposal

Possible modules:

- Spring integration for resolving generators from an `ApplicationContext`.
- Spring integration for using `ConversionService` or `Validator`.
- Spring Security integration for evaluating [authorization requirements](08-authorization-guards.md) where applications already use it.
- Jakarta CDI integration for resolving generators as CDI beans.
- Optional annotation-based routing, metadata, or authorization module (for example `pso-berlioz-annotated`); the core stays annotation-free.
- Optional adapters for metrics and tracing.
- A [binary serialization module](11-binary-serialization.md).

All integration modules stay separate from the core runtime, whose only production dependency is `pso-xmlwriter`.

## Open Questions

- Which integrations are valuable enough to maintain as official modules?
