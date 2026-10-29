# 13. HTTP QUERY Method Support

**Status:** Done; body-aware caching open · **Target:** 0.14.1

## Motivation

Several servlet containers are adding support for the HTTP `QUERY` method: a safe, idempotent method with a request body, intended to replace GET with oversized query strings and ad-hoc POST-as-GET tunneling. Berlioz supports `QUERY` as a first-class mappable method.

## Delivered

- `QUERY` is a mappable `HttpMethod`, alongside `GET`, `POST`, `PUT`, `PATCH`, `DELETE`. `ServiceRegistry` and `ServicesHandler10` were already keyed off `HttpMethod` generically, so registering and matching `QUERY` services required no structural change beyond the enum constant and allowing `query` in the `services-1.0.dtd` `method` attribute.
- `QUERY` is treated as safe/idempotent for `405`-vs-`404` handling (`handleNoMatch`): an unmatched `QUERY` request always gets a plain `404` rather than probing other registered methods for a `405`, as for `GET`/`HEAD`. The Servlet 4.0 API has no `HttpServlet.doQuery()`; Berlioz's generic `service()` dispatch routes `QUERY` through the same `process()` path as the other methods.
- `QUERY` is excluded from ETag/conditional-request caching: the ETag is derived from the URL alone, so two different `QUERY` bodies against the same URL would incorrectly share a cached response.
- `application/x-www-form-urlencoded` body-to-parameter emulation. `QueryBodyParameters` checks whether the servlet engine already exposes body parameters through `getParameterMap()`, by comparing it against a plain parse of the URL query string, and only reads and parses the body itself when the engine clearly has not. Parsed values reach generators through the existing `Request`/`ContentRequest` parameter API, with the lowest precedence (below the URL query string and URI template variables).
- RFC 10008 media-type requirements are enforced after routing and before generator invocation: a matched `QUERY` request without `Content-Type` receives `400`, malformed encoded form input receives `400`, and form bodies beyond the one-megabyte emulation limit or 1,000 field occurrences receive `413`. The decoder avoids attacker-sized `split()` arrays and intermediate repeated-value lists, and body buffering rejects oversized declared lengths before reading and never preallocates from an untrusted `Content-Length`. A present but unsupported media type remains the generator's responsibility and should produce `415`.

## Decisions

Related decision records: [ADR-0012](../decisions/0012-always-emulate-query-form-bodies.md).

- **Body-parameter emulation is always on, with no `BerliozOption`**: `QUERY` dispatch was new in the same release, so no existing generator can depend on the body being left untouched. The container-detection check is a reliable data comparison, not a fragile stream-consumption probe, and it makes the class a no-op as soon as a container parses `QUERY` bodies natively, with no configuration to revisit.

## Open Questions

- Should `QUERY` responses be cacheable, and if so should the ETag seed incorporate a hash of the request body?
