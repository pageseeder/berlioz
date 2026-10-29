# 0010. Discover classpath overlays by default, with filesystem precedence

- **Date:** 2026-10-16 (0.14.3)
- **Status:** Accepted

## Context

Reusable admin or utility features had to be distributed as WAR overlays merged into the host application at build time. Loading services and XSLT from JARs raises questions of discovery (opt-in or automatic), naming (a directory, a manifest entry or a fixed resource), conflicts, and caching.

## Decision

- **Automatic discovery**: each JAR or classpath directory may contribute exactly one `META-INF/berlioz/services.xml`. Discovery is on by default, since the resource name is narrow and explicit. An index for several documents per JAR is reserved for a future need.
- **Precedence**: classpath sources load first, filesystem sources last. A conflict is the same HTTP method and normalized URI pattern; the later declaration wins and a warning names both.
- **Stylesheets**: an existing filesystem stylesheet wins, including an invalid one; otherwise the first `META-INF/berlioz/xslt/` copy on the classpath is used (0.14.4). `classpath:` is the canonical prefix.
- **Caching**: classpath templates are immutable under the `manual` and `auto` cache modes and recompiled only under `no`.
- **Resolution security**: stylesheets may import and include within the same JAR or through explicit `classpath:` references; network protocols are always denied.

## Consequences

- Adding an overlay dependency is the only step an application needs; its own `WEB-INF` configuration always has the last word.
- Diagnostics report the source of each service, the declarations it overrides, and which stylesheet source was selected.
- Berlioz resolves one stylesheet name at a time and does not list classpath folders, which depends on container packaging; tools that need a listing build it themselves ([0013](0013-diagnostics-report-only-known-facts.md)).
