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
succeeds. The opt-in administrative `GetLiveServices` diagnostic reports `load-status` in XML
and `loadStatus` in JSON: `pending`, `ready`, `startup-failed`, or `reload-failed`. Its ETag
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

## Roadmap

The current development roadmap is available in [ROADMAP.md](ROADMAP.md).
