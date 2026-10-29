# 6. Raw Output

**Status:** Proposed · **Target:** 1.0

## Motivation

Direct JSON and direct XML services are part of the pipeline. `RawGenerator` exists as an API shape, but servlet dispatch support is still missing.

## Proposal

- Add a dedicated servlet path for `RawGenerator`.
- Define content type and charset behavior for raw output.
- Decide how cache headers and ETags apply to raw responses.
- Keep the direct service rule simple: one handler, one complete response body.
- Document how `<handler>` differs from `<generator>`.
- Add tests for raw response status, headers, ETags, and body writing.

## Open Questions

- Should direct services support only one handler forever, or is there a future aggregation model?
- How should raw output interact with cache headers, ETags, and [content negotiation](07-content-negotiation.md)?
- When there is no transform, `executeTransform()` returns `XmlContent`, whose media type is always `application/xml`. Should `XmlContent` carry the configured media type and charset, or should `executeTransform()` return an output object based on `config.getMediaType()`?
- Should services whose generators have disjoint output types stay invalid, or should Berlioz offer an explicit wrapper or adaptation for advanced cases?
