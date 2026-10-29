# 0004. Secure defaults with deprecated opt-outs until 1.0

- **Date:** 2026-07-21 (0.14.0)
- **Status:** Accepted

## Context

Up to 0.13, several defaults favored convenience or compatibility over safety: full error detail, legacy error XML, GET-via-POST fallback, the 0.9 XML header with extra request details, and open control parameters. 0.13.5 shipped the safer behaviors as opt-ins, but applications that never change their configuration stayed exposed.

Changing defaults breaks applications that rely on the old behavior, and many existing applications cannot migrate in one step.

## Decision

From 0.14.0, the out-of-the-box behavior is the production-oriented one. Each former default remains available through an explicit option, which is deprecated when introduced and removed in 1.0:

| Default from 0.14.0 | Opt-out (deprecated) |
|---|---|
| RFC 9457 Problem Details | `berlioz.errors.problem=false` |
| `berlioz.errors.detail=minimal` | Set `standard` or `full` (not deprecated) |
| No GET-via-POST fallback | `berlioz.http.get-via-post=true` |
| XML 1.0 header | `berlioz.xml.header.version=0.9` |
| Control parameters closed ([0006](0006-close-control-parameters-by-default.md)) | Configure a control channel |

Deprecated values are detected once when the configuration loads and logged as a warning, never per request.

## Consequences

- New applications are secure without configuration; relaxing a protection is always an explicit choice.
- Upgrading applications can restore old behavior for one release and migrate at their own pace, guided by the warnings.
- The same pattern is used for later changes within 0.14: new safe behavior by default, deprecated aliases until 1.0 (for example `resource:` XSLT references and the old reload parameters).
- The [upgrade guide](../upgrading.md) lists every removal planned for 1.0.
