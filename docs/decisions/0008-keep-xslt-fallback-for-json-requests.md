# 0008. Keep the XML/XSLT fallback for JSON-configured requests in 0.14

- **Date:** 2026-07-25 (0.14.1)
- **Status:** Accepted

## Context

A servlet mapping can be configured for JSON while a service's generators only produce XML. Before direct JSON generators existed, applications produced JSON this way by transforming the XML with an XSLT stylesheet. Rejecting such requests would make JSON output strict, but break existing applications.

## Decision

Throughout 0.14.x, a JSON-configured request for a service that cannot write JSON directly still goes through the XML and XSLT pipeline. The question is revisited with [content negotiation](../design/07-content-negotiation.md) in 1.0.

## Consequences

- Existing XSLT-to-JSON applications keep working.
- The output format of a JSON-configured mapping depends on the service's generators, which diagnostics report as supported outputs (`ListRuntimeServices`).
