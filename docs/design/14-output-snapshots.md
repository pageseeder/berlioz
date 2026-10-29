# 14. Reusable Format-Neutral Output Snapshots

**Status:** Proposed · **Target:** Undecided

## Motivation

Generators that parse and aggregate complex sources should be able to capture their output once and cache an immutable snapshot for reuse as either XML or JSON. This avoids repeating source processing or building an application-specific object model solely for serialization.

## Proposal

- `OutputBuffer`: a mutable builder that captures structural output operations while sources are parsed and aggregated.
- `OutputFragment`: an immutable `OutputWritable` snapshot that replays those operations through the destination `OutputWriter`, using the existing XML and JSON adapters.

Next work:

- Preserve operation order, object/array boundaries, names, scalar types, nulls, multi-value fields, and `FieldOption`/`ContextOption` values. Apply format-specific options during replay.
- Copy mutable inputs and consume iterables during capture so completed fragments are safe to share across requests without retaining source parsers or mutable application objects.
- Validate balanced contexts, define finalization behavior, and document valid insertion contexts, including XML attribute ordering and whether a fragment represents one complete object or a sequence of children.
- Keep cache keys, expiry, and invalidation application-controlled; one fragment should serve both XML and JSON without separate serialized cache entries.
- Verify equivalent direct and replayed XML/JSON output, snapshot isolation, concurrent replay, validation failures, and that replay does not close or flush the destination writer.
- Measure capture time, retained memory, and replay throughput before optimizing storage. Start with typed operations; defer a navigable tree API and serialized per-format caches until demonstrated needs justify them.
- Keep the implementation compatible with Java 11.

## Open Questions

- How should capture-time format detection work? The destination writer knows its format during replay, but a neutral buffer cannot choose both branches of Java code using `getType()`, `isXml()`, or `isJson()`. Evaluate a shared structural writing interface without format queries versus an `OutputWriter`-compatible buffer that rejects those queries explicitly.
