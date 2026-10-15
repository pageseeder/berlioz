[![Maven Central](https://img.shields.io/maven-central/v/org.pageseeder.berlioz/pso-berlioz.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.pageseeder.berlioz%22%20AND%20a:%22pso-berlioz%22)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pageseeder_berlioz&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pageseeder_berlioz)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=pageseeder_berlioz&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=pageseeder_berlioz)
[![javadoc](https://javadoc.io/badge2/org.pageseeder.berlioz/pso-berlioz/javadoc.svg)](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz)


# Berlioz
A light Web framework built on URI templates and XSLT

A simple, Java-based web framework that uses XLST for rendering and a URI template notation to allocate services. Implemented as a set of servlets, Berlioz is both powerful and simple. It is also easy to deploy and extend.

## What is Berlioz?

The founding principle of Berlioz is [separation of concerns](http://en.wikipedia.org/wiki/Separation_of_concerns). The design objectives are:
  * implement the business logic in Java
  * generate the XML necessary for the site
  * use XSLT to generate specific views of the data

By using the URI template notation as URI patterns, Berlioz provides more sophisticated URL matching than what is supported by Java servlets.

For example, Berlioz fire a service for any URL that match patterns like `/{user}/message/{id}`, `/wiki/{term}` or `/documents{;type,size}/title`; pass each URI parameter to the underlying Berlioz service.

A Berlioz service is simply a list of Java classes that generate XML. Think of it as a service model, not a formal **Web Service**. Once the XNL data has been created by the servlet, it is processed by XSLT into the destination format. While this is typically HTML, it can be any format that can be supported by XSLT (RSS, iCAl, .docx etc etc etc).

## Why Berlioz?

With so many web frameworks around, this is a very reasonable question. The answer is that there are surprisingly few options based on Java and XSLT and even fewer that are lightweight and robust. We have used Berlioz for a lot of implementations over the past decade and feel that it is a really good toolset for people with Java and XSLT skills. We posted it on Google Code as motivation to create the best quality of code and documentation possible. Right or wrong, our experience is that strictly internal projects don't get the same level of attention to detail as public projects.

If you want a sense of what Berlioz is all about, think modest features, highly extensible, quality code, simplicity and robustness. Also, Berlioz is production ready. It was designed for professional publishing applications and that is the environment it has evolved in.

Let us know if there is anything that we can do to help you use Berlioz. We want to create something that developers find useful, so feedback is welcome.

## API Documentation

The Java API documentation can be found [here](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz)

## Error Responses

Berlioz uses RFC 9457 Problem Details support for generator and framework errors.

## Classpath overlays

A Berlioz overlay is an ordinary JAR dependency that contributes services, generators, XSLT,
static resources, and servlet components without copying anything into the host application's
`WEB-INF` directory. Adding the dependency is the only application action required; discovery
is on by default.

An overlay JAR may contain:

- `META-INF/berlioz/services.xml` — exactly one per JAR, using the same `<service-config>`
  format as a filesystem `services.xml`.
- Generator classes referenced from that document.
- Classpath XSLT templates (see [Classpath and filesystem XSLT](#classpath-and-filesystem-xslt)
  below), including their relative `xsl:import`/`xsl:include` targets.
- Static resources under `META-INF/resources/`, served by the servlet container's standard
  resource-serving mechanism for packaged web fragments.
- Servlet, filter, and listener declarations in `META-INF/web-fragment.xml`, using a `<name>`
  so the host application can control fragment ordering if needed.

These are plain Servlet 3.0+ container mechanisms; Berlioz itself serves no part of this beyond
reading `META-INF/berlioz/services.xml`. `META-INF/resources` and `web-fragment.xml` only work
when the fragment JAR sits in the web application's own `WEB-INF/lib` (the scope these features
target); a container-wide shared classloader is not supported.

### Discovery and precedence

At each successful load, Berlioz calls `getResources("META-INF/berlioz/services.xml")` on the
web application's context classloader (falling back to the classloader that loaded Berlioz if
no context loader is available), so every contributing JAR on the classpath is found. Exploded
classpath directories resolve the same way, which is convenient during local development.

- Classpath sources are parsed before filesystem sources, so filesystem declarations always
  take precedence.
- A conflict is the same HTTP method and URI pattern declared more than once. The later
  declaration — in classpath-then-filesystem order — replaces the earlier one.
- Two classpath JARs that conflict with each other resolve deterministically (sorted by a safe
  display name, not classloader-dependent enumeration order), with a warning naming both the
  replaced and replacing declaration and their origins (e.g. `my-overlay.jar!META-INF/berlioz/services.xml`
  vs. `other-overlay.jar!META-INF/berlioz/services.xml`).
- A filesystem declaration always overrides a classpath declaration for the same method and
  pattern, regardless of load order.
- Declarations using the same URI pattern but different HTTP methods never conflict.

An application with no filesystem `services.xml` is officially supported: it loads from
classpath sources alone, and `AppInitializer` reports configuration as classpath-provided rather
than treating the missing file as an error.

### Transactional loading and reload failure

Every source is parsed in isolation and merged into a single candidate registry; the candidate
is only published if every source parses successfully. This matters for classpath overlays in
particular, since each overlay JAR's `services.xml` has its own `<service-config>` root, which
would otherwise clear everything parsed before it.

A malformed or unreachable source — classpath or filesystem — rejects the whole candidate:

- On startup, a failed load leaves the empty initial registry; requests receive the existing
  service-configuration `503` response until a load succeeds.
- On reload, a failed load keeps serving the last successfully published registry, including
  its warnings, source metadata, version, and content ETags, so existing requests and caches
  are unaffected by a bad deployment.

See [Service configuration loading and reloads](#service-configuration-loading-and-reloads)
below for the retry and diagnostic-visibility behavior shared with filesystem-only deployments.

## Classpath and filesystem XSLT

Primary and fallback stylesheets accept three kinds of value:

- `IDENTITY` — no transformation.
- A `classpath:` or `resource:` reference — resolved through the application classloader.
  `resource:` is a compatibility alias for `classpath:` with identical behavior. Both accept an
  optional leading slash: `classpath:xslt/main.xsl` and `classpath:/xslt/main.xsl` resolve to
  the same resource.
- Any other value — resolved as a filesystem path contained beneath `WEB-INF`, as before.

`{GROUP}` and `{SERVICE}` substitution is applied before resolution, for both primary and
fallback stylesheets.

```xml
<init-param>
  <param-name>stylesheet</param-name>
  <param-value>classpath:xslt/{GROUP}.xsl</param-value>
</init-param>
```

A classpath stylesheet may use relative `xsl:import`/`xsl:include` to reach other templates
packaged in the *same* JAR — the importing stylesheet's own resource URL is used as the
transform's system ID, so relative resolution stays within that artifact:

```xml
<xsl:import href="parts/imported.xsl"/>
<xsl:include href="parts/included.xsl"/>
```

Reaching a template in a *different* JAR (or in the filesystem) requires an explicit
`classpath:`/`resource:` reference as the `href`. Every import/include — relative or explicit —
is restricted to local `file:`/`jar:file:` resources; HTTP(S) and other network schemes are
always rejected, independently of the underlying XSLT processor's own `accessExternalStylesheet`
enforcement.

### Cache-mode behavior

`BerliozOption.XSLT_CACHE` (`no`, `auto`, `manual`) applies differently to filesystem and
classpath/JAR-backed templates, because a classpath resource cannot change without a
redeployment:

| Mode     | Filesystem                          | Classpath / `resource:`        |
| -------- | ------------------------------------ | ------------------------------- |
| `no`     | Recompile every use                  | Recompile every use             |
| `auto`   | Recompile when the file changes      | Treated as immutable, reused    |
| `manual` | Reused until explicitly cleared      | Treated as immutable, reused    |

Classpath cache keys distinguish identically named resources contributed by different JARs (and
by different application classloaders in the same JVM), so `clearCache()`/`clearAllCache()` can
still invalidate them individually, and an overlay's XSLT ETag changes when its own artifact —
including same-JAR imported/included templates — changes.

## Service configuration loading and reloads

Service discovery and parsing are transactional: an I/O or fatal parsing error rejects the
whole candidate configuration. An absent optional configuration directory is valid for
classpath-only applications; failure to enumerate an existing directory or the classpath is not.

Before the first successful load, requests receive a service-configuration `503` on failure.
Berlioz loads services lazily, so this does not itself prevent the servlet container from
starting; deployment readiness checks should exercise a known Berlioz route.

After a successful load, a failed reload keeps serving the complete last valid registry,
including its original warnings and source metadata. No partial contributions are published.
Failed loads are retried on the next request after a 30-second delay. An authorized
`reload-services` control request (or `ServiceLoader.requestReload()`) allows an immediate
retry. Explicit `ServiceLoader.load()` calls still throw on failure; `clear()` deliberately
removes the retained registry.

Each attempted failure is logged with its cause, and recovery is logged when publication
succeeds. The opt-in administrative `ListRuntimeServices` diagnostic (`GetLiveServices` remains
available as a deprecated forwarding alias until 1.0) reports `load-status` in XML and
`loadStatus` in JSON: `pending`, `ready`, `startup-failed`, or `reload-failed`. Its ETag
includes that status so a rejected reload becomes visible even though the registry is unchanged.
Exception details and source paths are not included in this status field. Protect diagnostic
mappings with application or container security.

## Runtime configuration diagnostics

`org.pageseeder.berlioz.generator.GetBerliozConfig` and
`org.pageseeder.berlioz.generator.GetErrorHandlerConfig` report the configuration of
initialized servlets. Add them explicitly to administrative services and protect those
services through your application's filters or container security. The generators do not
perform authorization themselves.

Both implement `Generator` and `Cacheable` and work with XML and JSON output. XML roots are
`berlioz-config` and `error-handler-config`, containing `servlet` elements; standalone JSON
uses a top-level `servlets` array, without a named root wrapper. Scalar fields become XML
attributes and JSON properties (hyphenated XML names become camelCase in JSON). Booleans
remain JSON booleans. Mappings and extensions are sorted string arrays in JSON and
space-separated XML attributes; optional fields are omitted. An empty registry produces
`<berlioz-config/>` or `<error-handler-config/>` and `{"servlets":[]}`.

`GetBerliozConfig` reports servlet defaults, which individual services or responses can
override: `name`, `mappings`, `media-type`, `charset`, `cache-control`, `compression`, and
`xslt-cache`. Compression means compression is permitted; `xslt-cache` describes the current
runtime template cache mode, separately from HTTP caching. `stylesheet` contains
`allocation` (`identity`, `global`, `group`, or `service`), with `source` (`filesystem` or
`classpath`) and a safe logical `path` when applicable. Identity contains only `allocation`.
An optional `fallback` contains its source and safe path. Group/service placeholders are
reported as configured, without creating a transformer or resolving a specific service.

For example:

```xml
<berlioz-config>
  <servlet name="html" mappings="*.html" media-type="text/html" charset="UTF-8"
           cache-control="no-cache" compression="true" xslt-cache="manual">
    <stylesheet allocation="group" source="classpath" path="classpath:xslt/{GROUP}.xsl"/>
  </servlet>
</berlioz-config>
```

`GetErrorHandlerConfig` reports `name`, `mappings`, `problem-xml`, and `detail`, followed by:

- `forward`: `source` (`explicit`, `discovered`, or `legacy-default`), `extensions`, and
  `default-extension`. Diagnostic extension names omit the leading dot.
- `ignore`: sorted `extensions`.
- `stylesheet`: `configured` (`custom` or `built-in`), optional safe custom `path`, and
  `selected` (`custom`, `built-in`, or `none`). An unavailable custom stylesheet includes
  `fallback-reason="missing-or-unreadable"`. Selection reflects availability, not successful
  XSLT compilation; `none` means raw XML fallback.

Servlet mappings are deployed URL patterns, not the container's error-page registrations.
Initialization settings are captured when each servlet starts and registrations are removed
on destruction. Error rendering options and XSLT cache mode reflect current global settings.
The snapshot uses only allowlisted fields; arbitrary parameters, secrets, absolute paths,
raw JAR URLs, exception messages, and internal ETag seeds are never included. Unsafe paths
are omitted while preserving source/selection information. Filesystem paths are relative to
`WEB-INF`; `resource:` references are reported using canonical `classpath:` names.

Each generator captures one immutable snapshot per request and reuses it for its response
and unquoted SHA-256 ETag, whichever is requested first. Only emitted content affects the
fingerprint; unrelated settings and secrets do not. Diagnostics perform no JAR enumeration,
transformer creation, or stylesheet compilation. Error stylesheet selection performs only a
small availability check. Deployment assumes Berlioz is in each application's `WEB-INF/lib`.

## Packaged overlay verification

Run `./gradlew overlayIntegrationTest` to deploy the test overlay JAR in a real Jetty WAR
and verify discovery, precedence, XSLT, static resources, web fragments, diagnostics, and
reload retention over HTTP. It also runs as part of `check` and `build`.
See [the deployment test documentation](tests/overlay-deployment/README.md) for setup and coverage.

## Migrating to 0.14.3

0.14.3 adds classpath overlay discovery, classpath-backed XSLT, and two new runtime
diagnostics. None of it requires a configuration change for an existing filesystem-only
application:

- An application with a `WEB-INF/config/services.xml` and no classpath overlay dependencies
  behaves exactly as before: there is nothing on the classpath to discover, so filesystem
  services remain the only source, and existing mapping precedence is unchanged.
- Existing `resource:` stylesheet references continue to resolve exactly as before;
  `classpath:` is the new canonical spelling of the same resolution, not a replacement
  requiring edits.
- `services!*.xml` filesystem modules now load in deterministic lexical order. Previously this
  followed directory-listing order, which was not guaranteed across platforms; an application
  relying on a specific non-alphabetical load order (uncommon, since override behavior did not
  previously distinguish file origin) should rename its modules accordingly.
- A failed *reload* (not startup) now keeps serving the last successful registry, including its
  warnings and ETags, instead of leaving the application with a cleared registry while the retry
  is pending. This is a reliability fix, not a behavior an application should have depended on.
- Adding a classpath overlay dependency is new, additive behavior: it only takes effect once
  such a dependency is actually present on the classpath. Filesystem declarations deterministically
  override classpath declarations for the same method and URI pattern, so an existing
  application can add an overlay dependency without its own `services.xml` losing precedence.
- `org.pageseeder.berlioz.generator.GetLiveServices` is renamed to
  `org.pageseeder.berlioz.generator.ListRuntimeServices`. `GetLiveServices` remains available
  as a deprecated forwarding alias with identical behavior and output until removal at 1.0;
  existing `services.xml` generator references do not need to change, but new configuration
  should reference `ListRuntimeServices` directly.

## Roadmap

The current development roadmap is available in [ROADMAP.md](ROADMAP.md).
