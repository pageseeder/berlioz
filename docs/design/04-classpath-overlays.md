# 4. Classpath Overlay Discovery

**Status:** Done · **Target:** 0.14.3, stub-free group stylesheets in 0.14.4

## Motivation

Up to 0.14.2, service configuration and primary XSLT templates had to be on the filesystem under `WEB-INF/`, so reusable admin or utility overlays had to be distributed as WAR overlays: ZIP archives merged into the host application at build time. A self-contained overlay (XSLT templates, service configuration, static assets via `META-INF/resources/`, and servlet components via `web-fragment.xml`) should be distributable and consumable as a single ordinary JAR dependency, with no filesystem merge step.

Existing foundations this built on:

- `BerliozConfig.toURL()` already supported a `resource:` prefix for loading fallback and kickstart XSLT templates from the classpath.
- The failsafe error stylesheet was already loaded from the classpath via `ClassLoader.getResource()`.
- `web-fragment.xml` in `pso-berlioz-kickstart` already registered servlet mappings without requiring changes to the host application's `web.xml`.
- `java.util.ServiceLoader` was already used for `RedirectPolicy` discovery via `META-INF/services/`, establishing a workable SPI pattern.

## Delivered

- Classpath `META-INF/berlioz/services.xml` discovery, enabled by default, with filesystem precedence and origin-aware conflict reporting.
- Transactional candidate parsing and atomic registry publication; a failed reload keeps serving the last successful registry.
- `classpath:` XSLT resolution (with `resource:` kept as a deprecated alias until 1.0), immutable classpath caching, and a secure import/include resolver.
- The `GetBerliozConfig` and `GetErrorHandlerConfig` diagnostic generators.
- The `ListRuntimeServices` rename (`GetLiveServices` kept as a deprecated alias until 1.0).
- Verification against a real packaged overlay JAR in a Jetty container (`./gradlew overlayIntegrationTest`).
- 0.14.4: group stylesheets fall back to `META-INF/berlioz/xslt/` on the classpath when the filesystem file is absent, so overlays no longer need a stub stylesheet in the host application. Filesystem entries take precedence, including invalid ones.

The full list of changes is in the [changelog](../../CHANGELOG.md) under 0.14.3 and 0.14.4.

## Decisions

Related decision records: [ADR-0010](../decisions/0010-classpath-overlays-with-filesystem-precedence.md), [ADR-0011](../decisions/0011-keep-last-valid-state-on-failed-reload.md).

- **Discovery default**: enabled by default for the narrow, exactly named overlay resource. Adding the overlay dependency is the only application action required.
- **Resource convention**: exactly one `META-INF/berlioz/services.xml` resource per JAR. An explicit index (manifest entry or properties file) is reserved for a future need for several documents per JAR.
- **Conflicts and precedence**: conflicts are determined by HTTP method and normalized URI pattern; same-pattern declarations with different methods never conflict. Classpath sources load first and filesystem sources last, so filesystem declarations take precedence.
- **Classpath XSLT caching**: classpath templates are immutable under the `manual` and `auto` cache modes; `no` recompiles them. Cache keys and ETags distinguish identically named resources contributed by different JARs or class loaders.
- **Resolver policy**: relative `xsl:import`/`xsl:include` within the same JAR and explicit `classpath:`/`resource:` references are allowed; every network protocol is denied.
