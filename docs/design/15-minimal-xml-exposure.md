# 15. Minimal Default XML Exposure

**Status:** Done · **Target:** 0.14.5

## Motivation

The kickstart `fallback-xml.xsl` copied the source document verbatim (`<xsl:sequence select="."/>`), so every `*.xml` request for a group without its own `/xslt/xml/{GROUP}.xsl` published the full internal envelope. JSON was unaffected because `fallback-json.xsl` returns `{}`. The envelope discloses:

- Framework fingerprint and environment: `header/berlioz/@version` and `@mode`.
- Arbitrary application settings: `header/app` (every `berlioz.app.*` property).
- The CSP nonce, which is meaningful only in HTML: `header/security/@nonce`.
- Internal topology behind a reverse proxy, and reflected `Host` values: `header/location`, `header/path`, and the deprecated `host`, `port`, `scheme`, `url`, and `context-path` elements.
- Reflected request input: `header/http-parameters` and `header/uri-parameters`.
- Implementation details: `content/@generator` (fully qualified class names), `content/@etag` (internal cache keys), `content/@profile*` (timings, when `berlioz.profile` is enabled), and `content/@deprecated`.

The kickstart `*.src` mapping (`BerliozSource`, `stylesheet=IDENTITY`) returned the raw envelope for every service with no mode, network, or key guard, and is hard for applications to remove from a web fragment. Changing the `.xml` fallback alone would leave the same data reachable through `.src`.

## Delivered

- `fallback-xml.xsl` is an allowlist rather than a denylist, so future envelope additions stay private unless explicitly exposed: `<header>` is replaced with a comment noting its removal; `root/@service`, `@group`, and `@flags` and `content/@name`, `@target`, and `@status` are kept; the generator payload is copied unchanged. Error content is already governed by `berlioz.errors.detail`.
- A `requires-control` servlet init parameter, enabled on the kickstart `BerliozSource`: unauthorized `.src` requests (including `OPTIONS`) receive `404`.
- `README.md` and the kickstart README warn that application `xml/{GROUP}.xsl` templates copying the whole source document expose the same data, and show a safe pattern.
- Tests assert the fallback output contains no `header`, `@generator`, `@etag`, or `@profile*`, and that `.src` is denied without control authorization (`FallbackXmlTest`).
- Migration notes: clients reading `header/…` from the default `.xml` output must use an application template instead.

## Decisions

Related decision records: [ADR-0006](../decisions/0006-close-control-parameters-by-default.md).

- **Guarding `.src`**: the options were serving it only in `dev` mode, guarding it with the existing control authorization (`berlioz.control.network`/`berlioz.control.key`), or removing it from kickstart and requiring an explicit mapping. Chosen: the control authorization, through the `requires-control` init parameter. Developers who use `.src` locally typically already set `berlioz.control.network=loopback`.
