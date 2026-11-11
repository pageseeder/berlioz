# Logging Configuration And Diagnostics

**Status:** Proposed · **Target:** diagnostics and external configuration in 0.14.6; Log4j 2 loading in a later 0.14.x; legacy removal in 1.0

## Motivation

SLF4J remains Berlioz's logging API. Applications choose its provider through their dependencies. Berlioz need not configure every backend: its useful contribution is selecting a configuration for the Berlioz mode and reporting what happened at startup.

The current startup snapshot reports files considered by Berlioz, but does not clearly distinguish the active backend from configuration ownership. The legacy Log4j loader can attempt configuration without establishing whether that backend receives Berlioz's logs.

## Proposed 0.14.6 Scope

- Identify the active backend from the actual SLF4J logger factory, rather than scanning installed libraries or configuration files. Recognize Logback, Log4j 2, JUL, Simple and NOP; retain the factory class for unknown or compatibility providers.
- Extend `GetLoggingConfig` and `AppInitializer.loggingSnapshot()` to report the active backend separately from whether Berlioz applied configuration. Preserve existing fields where practical and document any additions.
- Describe external configuration precisely: Berlioz did not configure logging; it has not verified the backend's effective configuration.
- Add an option to leave configuration entirely to the application, backend or container, skipping Berlioz's file searches. The option name and startup configuration source remain to be settled; it must be available before logging initialization.
- Retain mode-based Logback configuration, inspect only the active backend's candidates, and respect an explicit native configuration location. If no Berlioz candidate exists, leave native configuration intact.
- Report an explicit native configuration location when safely available, using existing detail controls and path/credential redaction. Do not claim that a property proves a file was successfully loaded.
- Document JUL and Simple configuration, including JUL's potential JVM-wide effects and the fact that mode-based configuration occurs during Berlioz initialization, after some startup messages may already have been emitted.

Changes to existing loading behavior must follow the 0.14.x compatibility policy: deprecate legacy behavior and retain an explicit compatibility path until 1.0 where needed. Do not add support for deprecated frameworks. Existing diagnostics must remain a startup record, not a promise of current effective configuration.

Validation should cover known and unknown factories, externally managed logging without file searches, native configuration precedence, preservation of native defaults, diagnostic redaction and compatibility behavior.

## Later 0.14.x

Add optional Log4j 2 mode-based XML loading: `log4j2-<mode>.xml` in the application data configuration folder, then the web application configuration folder, followed by `log4j2.xml` in the web application configuration folder. Use only the active backend, respect its explicit native configuration location, and keep dependencies optional. Other formats remain available through native configuration.

Test provider selection, configuration loading, failure reporting and application context isolation before including this integration in a release.

## 1.0

Remove the deprecated Log4j 1 configuration path and transitional compatibility behavior, with migration guidance to native configuration or supported mode-based loading.

## Boundaries

- No backend selection setting: the application's SLF4J provider already makes that choice.
- No automatic JUL reconfiguration or bridge installation.
- No generic configuration loader, exhaustive format discovery, or cross-backend reconstruction of appenders and effective levels.
- No new runtime dependencies for applications that use another provider.
