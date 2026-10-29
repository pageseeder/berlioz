# 7. Content Negotiation

**Status:** Proposed · **Target:** 1.0

## Motivation

Berlioz determines the output format from the URL extension (`.xml`, `.json`, `.html`) via servlet mappings. Supporting the `Accept` header would allow a single endpoint to serve multiple formats based on client preference.

## Proposal

- Support `Accept` header content negotiation as an alternative to extension-based format selection.
- Use `406 Not Acceptable` when the requested format is not supported by the service.
- Decide how `Accept`-based negotiation interacts with extension-based format selection (precedence, override, fallback).
- Decide whether content negotiation should be opt-in per service or enabled globally.

## Decisions

Related decision records: [ADR-0007](../decisions/0007-404-for-unsupported-output-format.md), [ADR-0008](../decisions/0008-keep-xslt-fallback-for-json-requests.md).

- **Unsupported output format today**: when a matched service cannot produce the requested output format (JSON unsupported and no XML+XSLT fallback available), `BerliozServlet.process()` returns `404`, not `406`. `406` is conventionally tied to `Accept`-header negotiation, and extension-based selection does not clearly fit that model. Revisit with this theme.
- **XML/XSLT fallback for JSON-configured requests**: preserved throughout 0.14.x; revisit with this theme in 1.0.
