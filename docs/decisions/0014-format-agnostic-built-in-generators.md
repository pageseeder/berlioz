# 0014. Make built-in generators format-agnostic where the data allows

- **Date:** 2026-07-21 (0.14.0; extended in 0.14.5)
- **Status:** Accepted

## Context

The built-in system and diagnostic generators wrote XML only. Monitoring, health checks and admin tools want JSON, and since 0.13.2 a `Generator` can write both through `OutputWriter`. Converting every generator would add churn where JSON has no clear value.

## Decision

A built-in generator implements `Generator` (XML and JSON through `OutputWriter`) when its output is data with an obvious JSON shape, and stays an `XmlGenerator` when its output is intrinsically markup or raw XML.

Converted: `GetParameters`, `GetGlobalConfig`, `GetRuntimeInfo`, `GetFileSystemInfo`, `GetCPUTime`, `ListLibraries`, `GetThreadInfo`, `ListThreads`, `ListRuntimeServices`, `GetMatchingService`, and in 0.14.5 `GetSystemProperties` and `GetGeneratorStatistics`. New diagnostic generators are written as `Generator` from the start.

Kept as `XmlGenerator`:

| Generator | Reason |
|---|---|
| `NoContent` | No output; not worth the churn. |
| `GetServices` | Streams the raw source configuration with `XmlCopier`; there is nothing to re-serialize. |
| `GetWebBundles` | Produces `<script>`/`<style>` elements for XSLT HTML templates: markup, not data. |
| `GetErrorDetails` | Writes exceptions and parser locators through XML-only helpers, and a JSON form of an error page is not compelling. |

## Consequences

- Each converted generator has the same structure in XML and JSON, with plural keys for JSON arrays. Where an XML shape could not map cleanly (for example dynamic manifest attribute names in `ListLibraries`), it was changed to a uniform `name`/`value` list in both formats.
- Error paths in converted generators return Problem Details (`Response.problem(…)`) instead of XML comments.
- A generator blocked only by an XML-only helper becomes convertible once that helper implements `OutputWritable`, as `StatisticsCollector` did in 0.14.5.
