# Berlioz Documentation

New to Berlioz? Start with [Getting started](../README.md#getting-started) in the README, then run the [minimal sample](../samples/minimal-gretty).

## Guides

| Guide | What it covers |
|---|---|
| [Configuration](guide/configuration.md) | Configuration files, modes, global settings and variables |
| [Services](guide/services.md) | Mapping URLs to generators in `services.xml`, groups, handlers and response codes |
| [Generators](guide/generators.md) | Writing generators, reading parameters, XML and JSON output |
| [XSLT](guide/xslt.md) | Stylesheet selection, the kickstart mappings, classpath stylesheets and caching |
| [Error handling](guide/error-handling.md) | Problem Details, error options, custom error pages |
| [HTTP caching](guide/caching.md) | ETags and `Cache-Control` |
| [Overlays](guide/overlays.md) | Packaging services, generators and stylesheets as a JAR |
| [Administration](guide/administration.md) | Control parameters, reloading, and runtime diagnostics |
| [Logging](guide/logging.md) | SLF4J, Logback configuration by mode |

## Reference

- [Services schema](reference/services-schema.md): the `services.xml` format.
- [Output schema](reference/output-schema.md): the XML document Berlioz passes to stylesheets.
- [Javadoc](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz): the Java API, including every option in `BerliozOption`.

## Project

- [Upgrade guide](upgrading.md) and [changelog](../CHANGELOG.md).
- [Roadmap](../ROADMAP.md), [design documents](design/) and [decision records](decisions/).
- [Contributing](../CONTRIBUTING.md): building and testing Berlioz itself.
