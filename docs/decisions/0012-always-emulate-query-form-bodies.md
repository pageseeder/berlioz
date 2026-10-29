# 0012. Always emulate `QUERY` form bodies

- **Date:** 2026-07-25 (0.14.1)
- **Status:** Accepted

## Context

Servlet containers do not yet parse `application/x-www-form-urlencoded` bodies of HTTP `QUERY` requests into request parameters, as they do for `POST`. Berlioz can parse them itself, but must not interfere once a container does. The question was whether this should be always on or an explicit `BerliozOption`, given that detecting native container support might be fragile.

## Decision

The emulation is always on, with no option. `QueryBodyParameters` compares the container's parameter map with a plain parse of the URL query string, and only reads the body when the container clearly has not.

## Consequences

- There is no compatibility risk: `QUERY` dispatch was new in the same release, so no existing generator could depend on the body being left untouched.
- The detection is a data comparison, not a probe of the input stream, so it is reliable. The class becomes a no-op when a container adds native support, with no configuration to revisit.
- The emulation is bounded (1 MB, 1,000 fields) and responds with `400` or `413` beyond that; see [theme 13](../design/13-http-query.md).
