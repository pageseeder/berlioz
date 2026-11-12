# 0015. Leaving logging to the backend is opt-in

- **Date:** 2026-11-12 (0.14.6)
- **Status:** Accepted

## Context

Berlioz configures Logback at startup from mode-based files (`logback-<mode>.xml`, `logback.xml`), resetting Logback first. Some applications want their backend or container to configure logging instead, for example when Logback is shared in the container's `lib` folder and a reset would wipe the container's configuration.

Backends document system properties that point at a native configuration, such as `logback.configurationFile`, `log4j2.configurationFile` or `java.util.logging.config.file`. Berlioz could defer whenever one is set. But these properties apply to the whole JVM: Berlioz cannot tell whether one was set for the container, for another application or for this one. Deferring automatically would silently skip the files the application ships in `WEB-INF/config`.

The startup diagnostics (`GetLoggingConfig`) also need to say who configured logging, without claiming what the backend's effective configuration is (see [0013](0013-diagnostics-report-only-known-facts.md)).

## Decision

- Applications opt in with `berlioz.logging=native`. Berlioz then looks at none of its files and does not reset the backend. The default, `auto`, keeps the existing behavior. The option is read like the mode, before logging is configured (servlet init parameter, context parameter, system property, environment variable), because `config.xml` depends on the mode and is loaded after logging. The context parameter, which applies to one application, is the recommended source.
- A native location property never takes precedence automatically. In `auto`, Berlioz files win, and Berlioz prints a notice when it loads its own file while the property is set. When no Berlioz file exists, the backend's configuration is left intact.
- Diagnostics report the backend from the SLF4J logger factory, and report how logging ended up as `configuration`, with one cause per value: `file`, `console-default`, `native` or `none`. `native` and `none` say only that Berlioz applied nothing.
- The documented location property of the active backend is reported as a hint (`native-setting`), with its value sanitized. Berlioz does not resolve or open it, does not infer whether the backend used it, and does not flag it as overridden.

## Consequences

- No application changes its logging behavior on upgrade: no deprecation path is needed.
- An application configured by its container must set `berlioz.logging=native`; a stray JVM-wide property cannot disable the application's own files.
- Tools can tell whether Berlioz applied a configuration, but not whether the backend's effective configuration is the intended one: an absent `native-setting` does not mean that the backend has no configuration.
- Log4j 2 loading, planned for a later 0.14.x, follows the same `auto` and `native` rules.
