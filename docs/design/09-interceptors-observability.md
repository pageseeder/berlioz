# 9. Interceptors And Observability

**Status:** Exploring · **Target:** 1.0 (decision)

## Motivation

`GeneratorListener` already provides a focused generator timing hook. The next step is deciding whether Berlioz needs a broader interceptor model.

## Proposal

Possible hooks:

- Before service execution.
- After successful service execution.
- Around individual generator execution.
- On generator errors.
- Around XSLT transformation.
- Around authorization checks.

Potential uses:

- Logging.
- Timing, including XSLT transformation performance.
- Metrics.
- Tracing.
- Security checks.
- Development diagnostics.

Natural optional integrations:

- Micrometer.
- OpenTelemetry.
- Application-specific audit logging.

The core should expose a small lifecycle model, with hooks only where they support clear core use cases. Metrics and tracing dependencies stay in [optional modules](10-integration-modules.md).

## Open Questions

- Should interceptor hooks be added to core, or should they wait for optional observability modules?
