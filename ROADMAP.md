# Berlioz Roadmap

Berlioz is a lightweight Java web framework built around URI templates, generator-based content, XML, JSON, and XSLT rendering.

This page describes where Berlioz is heading and the status of each release. What changed in past releases is in the [changelog](CHANGELOG.md), how to upgrade is in the [upgrade guide](docs/upgrading.md), and the details of each theme are in its [design document](docs/design/). Design decisions are recorded in [`docs/decisions/`](docs/decisions/).

## Guiding Principles

- Keep Berlioz simple, explicit, and easy to reason about.
- Keep the core annotation-free. Annotation-based programming can be useful, but it should live in optional modules.
- Preserve URI templates as the primary request-matching model.
- Preserve XML and XSLT as first-class mechanisms for generating XML and HTML responses.
- Keep JSON support direct and practical without weakening the XML/XSLT pipeline.
- Prefer small extension points over large framework dependencies.
- Keep optional integrations optional.
- Maintain source compatibility for existing `ContentGenerator` applications where feasible.
- Treat security hardening as part of the core framework contract, not as an optional add-on.

## Where We Are

The 0.13 line introduced output-aware generators, direct JSON responses, and RFC 9457 Problem Details. The 0.14 line moved Berlioz to secure defaults and made it easier to package and operate: reusable overlays as ordinary JARs, runtime diagnostics, and finer administration controls.

0.14.6 fixes the defects that Berlioz Admin found and exposes state that only Berlioz knows, so that admin tools no longer copy Berlioz internals. Configuration validation follows in a later 0.14.x release. After that, we will assess readiness for 1.0, which completes the core runtime (raw output, content negotiation, authorization guards) and removes the APIs deprecated during 0.14.

## Release Plan

| Release | Focus | Status |
|---|---|---|
| 0.13.5 | Problem Details and error handling pipeline (final 0.13) | Released 2026-07-03 |
| 0.14.0 | Secure defaults | Released 2026-07-21 |
| 0.14.1 | Runtime introspection and HTTP `QUERY` | Released 2026-07-25 |
| 0.14.2 | Session creation hotfix | Released 2026-09-04 |
| 0.14.3 | Classpath overlay discovery | Released 2026-10-16 |
| 0.14.4 | Stub-free group stylesheet overlays | Released 2026-10-20 |
| 0.14.5 | Security improvements, scoped reload, admin diagnostics | Released 2026-10-29 |
| **0.14.6** | **Fixes and diagnostics for admin tools** | **Next** |
| 0.14.x | Configuration requirements and validation | Planned |
| 1.0 | Core runtime completion; removal of 0.14 deprecations | Planned |
| 1.1+ | Optional integrations, additional formats, Jakarta | Exploratory |

### 0.14.6: Fixes And Diagnostics For Admin Tools

Fix the defects that Berlioz Admin found: error pages for every HTTP method, errors routed to the container with `berlioz.errors.handle=false`, Logback configuration that failed silently, a reload applied again while an error renders, and redirect and relocation filters without a `config` parameter.

Expose state that only Berlioz knows, so that admin tools no longer parse Berlioz's files or repeat its rules: the rules loaded by the redirect and relocation filters, a `redirects` reload that reaches every filter, redaction of credentials by value, which logging configuration Berlioz loaded at startup, and whether the last configuration reload failed ([theme 18](docs/design/18-admin-overlay-upstreaming.md), items 11–15).

Improve logging diagnostics to distinguish the active SLF4J backend from configuration applied by Berlioz, and let applications leave logging configuration entirely to their backend or container. Keep mode-based Logback loading as a convenience, with an explicit option to defer to native configuration ([theme 19](docs/design/19-logging-configuration.md)).

### 0.14.x: Configuration Requirements And Validation

Let applications declare the global properties they need in `WEB-INF/config/config-requirements.xml`, validate them on every load, and inspect the result through a diagnostic generator. Violations are reported by default; applications can choose to reject a configuration with errors, in which case a reload keeps the last valid configuration. See [theme 5](docs/design/05-configuration-validation.md).

Add optional Log4j 2 mode-based XML configuration using the same location precedence as Logback, after the logging diagnostics and configuration ownership changes in 0.14.6 ([theme 19](docs/design/19-logging-configuration.md)).

### 1.0

Complete the core runtime without turning Berlioz into a broad application framework:

- Raw output dispatch for `RawGenerator` ([theme 6](docs/design/06-raw-output.md)).
- `Accept`-header content negotiation and `406 Not Acceptable` ([theme 7](docs/design/07-content-negotiation.md)).
- Authorization guards for services and generators, with `401`/`403` Problem Details ([theme 8](docs/design/08-authorization-guards.md)).
- A decision on interceptor hooks in core ([theme 9](docs/design/09-interceptors-observability.md)).
- A stable `ListRuntimeServices` schema, including per-mapping `cache-control` and its origin ([theme 16](docs/design/16-admin-diagnostics.md)).
- Removal of APIs deprecated in 0.14: the legacy error XML format, `resource:` XSLT prefix, `GetLiveServices`, `GetCPUTime`, and the `reload-services`, `clear-xsl-cache` and `reset-etags` parameters. The [upgrade guide](docs/upgrading.md) lists replacements.

### 1.1 And Later

Optional integration modules (Spring, CDI, metrics, tracing), a binary serialization module, and Jakarta Servlet support. These stay outside the core and are driven by demand.

## Themes

Themes are long-running areas of work. Each has a design document with its motivation, planned shape, decisions and open questions. Numbers are stable identifiers, not priorities.

| # | Theme | Target | Status |
|---|---|---|---|
| 2 | [Error handling pipeline](docs/design/02-error-handling.md) | 0.14.0 | ✓ Done |
| 3 | [Service metadata and diagnostics](docs/design/03-service-diagnostics.md) | 0.14.1 | ✓ Done |
| 4 | [Classpath overlay discovery](docs/design/04-classpath-overlays.md) | 0.14.3 | ✓ Done |
| 5 | [Configuration requirements and validation](docs/design/05-configuration-validation.md) | 0.14.x | Designed |
| 6 | [Raw output](docs/design/06-raw-output.md) | 1.0 | Proposed |
| 7 | [Content negotiation](docs/design/07-content-negotiation.md) | 1.0 | Proposed |
| 8 | [Authorization guards](docs/design/08-authorization-guards.md) | 1.0 | Exploring |
| 9 | [Interceptors and observability](docs/design/09-interceptors-observability.md) | 1.0 | Exploring |
| 10 | [Optional integration modules](docs/design/10-integration-modules.md) | 1.1+ | Exploring |
| 11 | [Binary serialization module](docs/design/11-binary-serialization.md) | 1.1+ | Exploring |
| 12 | [Jakarta Servlet support](docs/design/12-jakarta.md) | Undecided | Deferred |
| 13 | [HTTP `QUERY` method](docs/design/13-http-query.md) | 0.14.1 | ✓ Done; body-aware caching open |
| 14 | [Format-neutral output snapshots](docs/design/14-output-snapshots.md) | Undecided | Proposed |
| 15 | [Minimal default XML exposure](docs/design/15-minimal-xml-exposure.md) | 0.14.5 | ✓ Done |
| 16 | [Admin diagnostics](docs/design/16-admin-diagnostics.md) | 0.14.5 / 1.0 | ✓ 0.14.5 part done |
| 17 | [Scoped `berlioz-reload`](docs/design/17-scoped-reload.md) | 0.14.5 / 1.0 | ✓ 0.14.5 part done |
| 18 | [Admin overlay upstreaming](docs/design/18-admin-overlay-upstreaming.md) | 0.14.5 / 0.14.6 / 1.0 | ✓ 0.14.5 and 0.14.6 parts done |
| 19 | [Logging configuration and diagnostics](docs/design/19-logging-configuration.md) | 0.14.6 / 0.14.x / 1.0 | ✓ 0.14.6 part done |

Status values: **Exploring** (problem understood, approach open), **Proposed** (approach sketched), **Designed** (ready to implement), **Done**.

## Open Questions

Questions that affect the direction of the project. Questions specific to a theme are in its design document. Feedback is welcome in [GitHub issues](https://github.com/pageseeder/berlioz/issues).

- Should Jakarta support be a breaking major release, a parallel artifact, or a later 1.x line?
- Should the `javax.servlet` line become maintenance-only once Jakarta ships, or stay active until applications have migrated?
- Which integrations are valuable enough to maintain as official modules?
- Should interceptor hooks be added to core, or wait for optional observability modules?
