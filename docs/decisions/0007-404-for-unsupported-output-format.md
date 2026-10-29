# 0007. Return 404, not 406, for an unsupported output format

- **Date:** 2026-07-20
- **Status:** Accepted

## Context

When a request matches a service but the service cannot produce the requested output format (for example JSON is not supported and there is no XML+XSLT fallback), Berlioz has to choose a status code. `406 Not Acceptable` describes the situation, but it is conventionally tied to `Accept`-header content negotiation, whereas Berlioz selects the format from the URL extension through servlet mappings.

## Decision

`BerliozServlet` returns `404 Not Found` in this case.

## Consequences

- Callers cannot distinguish "the service exists but not in this format" from "no such service", which also avoids revealing which services exist.
- Revisit only together with real `Accept`-header [content negotiation](../design/07-content-negotiation.md), where `406` becomes the natural answer. Do not change it as a standalone fix.
