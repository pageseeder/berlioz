# 2. Error Handling Pipeline

**Status:** Done · **Target:** 0.13.5, defaults changed in 0.14.0

## Motivation

Errors should be presented predictably and configurably: helpful to developers during development, without leaking information in production.

## Delivered

Applications can control diagnostic verbosity, use RFC 9457 Problem Details, supply a custom error stylesheet, and the built-in failsafe template renders cleanly across all error types.

- `BerliozErrorID` classifies errors semantically (transform not found, invalid, dynamic error, malformed source XML, etc.).
- `XsltErrorCollector` collects warnings, errors, and fatals during XSLT processing.
- `XsltTransformer` catches both `TransformerConfigurationException` (static) and `TransformerException` (dynamic) and falls back to the failsafe template, with distinct `BerliozErrorID` values for each.
- `ERROR_DETAIL` (`berlioz.errors.detail`) controls diagnostic verbosity in both legacy XML and RFC 9457 problem responses.
- `ERROR_PROBLEM_FORMAT` (`berlioz.errors.problem`) makes `ErrorHandlerServlet` emit RFC 9457 `<problem>` XML, the default since 0.14.0. The deprecated legacy format remains available as a temporary migration option.
- `ERROR_STYLESHEET` (`berlioz.errors.stylesheet`) lets applications supply a custom error XSLT, with automatic fallback to the built-in failsafe, then to raw XML.
- Modernized failsafe template: CSS variables, dark-mode support, responsive layout, collapsible stack traces via `<details>`/`<summary>`, structured `<exception>` rendering inside Problem Details responses, and `http-headers`/`http-parameters` diagnostic blocks in full-detail mode.
- Sensitive values in diagnostic request headers and parameters are redacted before serialization.
- Runtime XSLT 2.0 probing reports a clear static diagnostic when no XSLT 2.0 processor is available.
- XSLT failures go through the standard HTTP 500 error pipeline (0.14.0).

Problem Details became the default in 0.14.0 and the legacy non-Problem-Details format is deprecated. It should be removed only after a clear migration window; until then it uses the unified `<error http-class="…">` element rather than the older `<server-error>`/`<client-error>` names.

## Decisions

Related decision records: [ADR-0005](../decisions/0005-problem-details-error-format.md).

- **Custom error stylesheet scope**: a single global stylesheet, not per-group templates. Error dispatch bypasses the normal XSLT resolution path, so per-group templates would add complexity without clear benefit.
- **Legacy error XML in 0.14.0**: deprecated rather than removed. Problem Details is the default, and `berlioz.errors.problem=false` remains as a migration opt-out.
- **`detail` under `berlioz.errors.detail=minimal`**: `minimal` suppresses only framework-internal diagnostics (stack traces, exception class, HTTP headers and parameters). The `detail` member in RFC 9457 responses always reflects the error message passed to `Problems.forHttpError()`, which is the HTTP status phrase and safe for production.
- **Problem type URIs**: `urn:berlioz:problem:*` is reserved for Berlioz-internal types; applications own their own scheme.

## Open Questions

- Should non-fatal XSLT warnings be surfaced to the client (for example via a response header or in the output XML), or only logged server-side?
- When should the deprecated legacy error format be removed? The roadmap currently plans 1.0.
