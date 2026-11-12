# Logging Configuration And Diagnostics

**Status:** Proposed · **Target:** diagnostics and the `berlioz.logging` option in 0.14.6; Log4j 2 loading in a later 0.14.x; legacy removal in 1.0

## Motivation

SLF4J remains Berlioz's logging API. Applications choose its provider through their dependencies. Berlioz need not configure every backend: its useful contribution is selecting a configuration for the Berlioz mode and reporting what happened at startup.

The current startup snapshot reports files considered by Berlioz, but does not clearly distinguish the active backend from configuration ownership. The legacy Log4j loader can attempt configuration without establishing whether that backend receives Berlioz's logs.

## Proposed 0.14.6 Scope

- Identify the active backend from the actual SLF4J logger factory, rather than scanning installed libraries or configuration files. Recognize Logback, Log4j 2, JUL, Simple and NOP; retain the factory class for unknown or compatibility providers.
- Extend `GetLoggingConfig` and `AppInitializer.loggingSnapshot()` to report the active backend separately from whether Berlioz applied configuration. Preserve existing fields where practical and document any additions.
- Report how logging ended up as `configuration`, with one cause per value: `file` (Berlioz loaded one of its files), `console-default` (Berlioz's files failed, so it logs to the console), `native` (the application asked Berlioz to look at no files) or `none` (Berlioz looked and applied nothing). For `native` and `none`, say only that Berlioz did not configure logging; it has not verified the backend's effective configuration.
- Add `berlioz.logging=auto|native` to leave configuration entirely to the backend's native configuration, skipping Berlioz's file searches. `auto` is the default. The option is read like the mode, before logging initialization: servlet init-param `logging`, context-param `berlioz.logging`, system property `berlioz.logging`, then environment variable `BERLIOZ_LOGGING`. It is not a `config.xml` option, because that file depends on the mode and is loaded after logging.
- Retain mode-based Logback configuration. Load Logback files only when it is the active backend; preserve ignored-file diagnostics and legacy loading for 0.14.x compatibility. If no Berlioz candidate exists, leave native configuration intact.
- Do not defer automatically to an explicit native configuration location, such as `logback.configurationFile`. These properties are JVM-wide, so Berlioz cannot tell whether one was set for the container, another application or this one, and deferring would silently skip the files the application ships. In `auto`, Berlioz files take precedence, as before, and Berlioz prints a notice when the property is also set; deferring requires `berlioz.logging=native`, which can be set per application.
- Report one observed `native-setting` hint for the active backend: `logback.configurationFile`, `log4j2.configurationFile` or `java.util.logging.config.file`. Include its name and sanitized value regardless of `berlioz.errors.detail`: replace known application data and web application directory prefixes with `[APP_DATA]` and `[WEB-INF]`, and retain existing credential redaction and message bounds. Other paths remain visible. Do not resolve or open the value, infer whether it was used, or mark it as overridden. An absent hint does not mean the backend has no configuration. Aliases, environment variables and configuration classes are outside this initial scope.
- Document JUL and Simple configuration, including JUL's potential JVM-wide effects and the fact that mode-based configuration occurs during Berlioz initialization, after some startup messages may already have been emitted.

Changes to existing loading behavior must follow the 0.14.x compatibility policy: deprecate legacy behavior and retain an explicit compatibility path until 1.0 where needed. Do not add support for deprecated frameworks. Existing diagnostics must remain a startup record, not a promise of current effective configuration.

Validation should cover known and unknown factories, `berlioz.logging=native` without file searches, Berlioz file precedence over a native location in `auto`, preservation of native defaults, diagnostic redaction and compatibility behavior.

## Later 0.14.x

Add optional Log4j 2 mode-based XML loading: `log4j2-<mode>.xml` in the application data configuration folder, then the web application configuration folder, followed by `log4j2.xml` in the web application configuration folder. Use only the active backend, apply the same `auto` and `native` rules as Logback, and keep dependencies optional. Other formats remain available through native configuration.

Test provider selection, configuration loading, failure reporting and application context isolation before including this integration in a release.

## 1.0

Remove the deprecated Log4j 1 configuration path and transitional compatibility behavior, with migration guidance to native configuration or supported mode-based loading.

## Boundaries

- No backend selection setting: the application's SLF4J provider already makes that choice.
- No automatic JUL reconfiguration or bridge installation.
- No generic configuration loader, exhaustive format discovery, or cross-backend reconstruction of appenders and effective levels.
- No new runtime dependencies for applications that use another provider.
