# 0009. Resolve the error media type from the servlet mapping

- **Date:** 2026-07-25 (0.14.1)
- **Status:** Accepted

## Context

`ErrorHandlerServlet` inferred the format of an error response from the URL extension (`.html`, `.json`, otherwise XML). This is wrong whenever an application maps a Berlioz servlet to a different extension or media type, and it required a hard-coded list of extensions.

## Decision

`BerliozServlet` records the media type configured for the matched servlet mapping (`BerliozConfig.getMediaType()`) on the request, as `ErrorHandlerServlet.BERLIOZ_ERROR_MEDIA_TYPE`, before dispatching an error. `ErrorHandlerServlet` prefers that attribute. If an exception escapes before it is set, the handler looks up the originating Berlioz servlet registration by name.

Extension inference remains only for errors that reach the error handler without any Berlioz servlet involved: unmapped paths and static resources.

## Consequences

- Errors are rendered in the format the client asked for, including for application-defined mappings.
- Default `.auto` error forwarding is derived from the extensions of the deployed Berlioz servlets, with no hard-coded list.
- A resolved JSON expectation always produces `application/problem+json` ([0005](0005-problem-details-error-format.md)).
