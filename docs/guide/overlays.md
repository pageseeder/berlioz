# Overlays

A Berlioz overlay is an ordinary JAR dependency that contributes services, generators, XSLT, static resources, and servlet components without copying anything into the host application's `WEB-INF` directory. Adding the dependency is the only step needed; discovery is on by default. The rationale is in [ADR-0010](../decisions/0010-classpath-overlays-with-filesystem-precedence.md).

## What an overlay contains

- `META-INF/berlioz/services.xml`: exactly one per JAR, using the same `<service-config>` format as a filesystem `services.xml`.
- Generator classes referenced from that document.
- Stylesheets, either under `META-INF/berlioz/xslt/` to provide group stylesheets, or referenced with `classpath:`, including their relative `xsl:import`/`xsl:include` targets ([XSLT](xslt.md)).
- Static resources under `META-INF/resources/`, served by the servlet container's standard mechanism for packaged web fragments.
- Servlet, filter, and listener declarations in `META-INF/web-fragment.xml`, with a `<name>` so the host application can control fragment ordering if needed.

These are plain Servlet 3.0+ container mechanisms; Berlioz itself only reads `META-INF/berlioz/services.xml` and the stylesheets. `META-INF/resources` and `web-fragment.xml` only work when the JAR is in the web application's own `WEB-INF/lib`; a container-wide shared class loader is not supported.

## Discovery and precedence

At each successful load, Berlioz calls `getResources("META-INF/berlioz/services.xml")` on the web application's context class loader (falling back to the class loader that loaded Berlioz), so every contributing JAR on the classpath is found. Exploded classpath directories resolve the same way, which is convenient during local development.

- Classpath sources are parsed before filesystem sources, so filesystem declarations always take precedence.
- A conflict is the same HTTP method and URI pattern declared more than once. The later declaration, in classpath-then-filesystem order, replaces the earlier one.
- Two classpath JARs that conflict with each other resolve deterministically (sorted by a safe display name, not class loader enumeration order), with a warning naming both declarations and their origins, such as `my-overlay.jar!META-INF/berlioz/services.xml` and `other-overlay.jar!META-INF/berlioz/services.xml`.
- A filesystem declaration always overrides a classpath declaration for the same method and pattern.
- Declarations using the same URI pattern but different HTTP methods never conflict.

An application with no filesystem `services.xml` is supported: it loads services from classpath sources alone, and `AppInitializer` reports the configuration as classpath-provided rather than treating the missing file as an error.

`ListRuntimeServices` shows the source of each service and the declarations it overrides ([administration](administration.md)).

## Transactional loading

Every source is parsed in isolation and merged into a single candidate registry, which is only published if every source parses successfully. This matters for overlays in particular, since each overlay's `services.xml` has its own `<service-config>` root, which would otherwise clear everything parsed before it.

A malformed or unreachable source, on the classpath or the filesystem, rejects the whole candidate:

- On startup, the registry stays empty, and requests receive a service-configuration `503` until a load succeeds.
- On reload, the last successfully published registry keeps serving, including its warnings, source metadata, version, and content ETags, so existing requests and caches are unaffected by a bad deployment.

See [services](services.md#loading-and-reloading) for retries.

## Group stylesheets

An overlay can provide the stylesheet for its own group under `META-INF/berlioz/xslt/`, for example `META-INF/berlioz/xslt/html/admin.xsl` for the `admin` group. Since 0.14.4, the host application does not need a stub stylesheet; a file in `WEB-INF/xslt/` still takes precedence. See [XSLT](xslt.md#group-stylesheets-from-overlays).

## Securing overlay services

Overlay services, such as administration tools, are reachable like any other service. Protect them with the application's filters or container security; diagnostic generators do not perform authorization themselves.
