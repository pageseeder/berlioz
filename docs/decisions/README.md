# Architecture Decision Records

This folder records the decisions that shape Berlioz across releases and themes: what was decided, why, and what follows from it. Decisions specific to one roadmap theme are recorded in its [design document](../design/).

Records use the format described by Michael Nygard in [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions). A record is not edited once accepted, except to change its status; a decision that changes gets a new record that supersedes the old one.

| # | Decision | Status |
|---|---|---|
| [0001](0001-record-architecture-decisions.md) | Record architecture decisions | Accepted |
| [0002](0002-keep-the-core-annotation-free.md) | Keep the core annotation-free | Accepted |
| [0003](0003-keep-integrations-optional.md) | Keep third-party integrations optional | Accepted |
| [0004](0004-secure-defaults-with-deprecated-opt-outs.md) | Secure defaults with deprecated opt-outs until 1.0 | Accepted |
| [0005](0005-problem-details-error-format.md) | Use RFC 9457 Problem Details for errors | Accepted |
| [0006](0006-close-control-parameters-by-default.md) | Close control parameters by default | Accepted |
| [0007](0007-404-for-unsupported-output-format.md) | Return 404, not 406, for an unsupported output format | Accepted |
| [0008](0008-keep-xslt-fallback-for-json-requests.md) | Keep the XML/XSLT fallback for JSON-configured requests in 0.14 | Accepted |
| [0009](0009-error-media-type-from-servlet-mapping.md) | Resolve the error media type from the servlet mapping | Accepted |
| [0010](0010-classpath-overlays-with-filesystem-precedence.md) | Discover classpath overlays by default, with filesystem precedence | Accepted |
| [0011](0011-keep-last-valid-state-on-failed-reload.md) | Keep the last valid state when a reload fails | Accepted |
| [0012](0012-always-emulate-query-form-bodies.md) | Always emulate `QUERY` form bodies | Accepted |
| [0013](0013-diagnostics-report-only-known-facts.md) | Diagnostics report only what Berlioz knows | Accepted |
| [0014](0014-format-agnostic-built-in-generators.md) | Make built-in generators format-agnostic where the data allows | Accepted |

## Adding a record

Copy the structure of an existing record, take the next number, and add it to the table above:

```markdown
# NNNN. Decision in a few words

- **Date:** YYYY-MM-DD
- **Status:** Proposed | Accepted | Superseded by [NNNN](NNNN-….md)

## Context
## Decision
## Consequences
```
