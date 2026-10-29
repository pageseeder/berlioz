# 5. Configuration Requirements And Validation

**Status:** Designed · **Target:** 0.14.6 (deferred from 0.14.5)

## Motivation

Many Berlioz applications depend on application-specific global properties being present and well-formed. Berlioz should be able to validate those requirements when global configuration is loaded, report violations consistently, and expose the result through a diagnostic generator.

## Groundwork

- `OptionDeprecations` (package-private) holds the deprecated-value checks for the three 0.14.0 deprecated options: `berlioz.http.get-via-post=true`, `berlioz.errors.problem=false`, and `berlioz.xml.header.version=0.9`. `GlobalSettings.load()` calls it once after a successful load or reload; no warning is ever emitted per request.
- `OptionDeprecations` is the internal precursor to `ConfigurationValidator`: the check-and-warn pattern it establishes will generalize into the declarative, application-facing validation layer.
- Since 0.14.5, a configuration reload that fails to parse keeps the last valid configuration.

## Proposal

- Add a separate declarative `WEB-INF/config/config-requirements.xml` file, evaluated after global properties are fully resolved (including variable resolution).
- Retain property provenance so validation can distinguish an explicitly specified value from a framework default.
- Match requirements by property name, including simple wildcard patterns such as `test.*.url`.
- Distinguish presence checks:
  - `defined`: the resolved property key exists.
  - `specified`: the application supplied the property rather than only inheriting a framework default.
  - `has-value`: the resolved value is non-null and non-blank after trimming.
- Treat zero wildcard matches as a violation only when the applicable presence rule requires at least one match.
- Support built-in value constraints for booleans, integers, numbers, regular expressions, enumerated values, URLs, URIs, ports, hostnames, and paths.
- Record violations with severity (`error`, `warning`, `info`) and enough metadata to identify the requirement, matched property, and failing constraint.
- Omit raw values from diagnostic output by default, especially for secret-like keys.
- Use report-and-continue behavior by default, with an option to fail application startup or configuration reload when `error` violations are present.
- Validate a reload candidate before publishing it; failed validation retains the last valid configuration.
- Add an opt-in diagnostic generator that emits the current validation report as XML, with JSON available through the normal output pipeline. It should be opt-in or intended for admin-only services.

Potential core model:

- `ConfigurationRequirement`
- `ConfigurationConstraint`
- `ConfigurationViolation`
- `ConfigurationValidationReport`
- `ConfigurationValidator`

The first implementation should keep the constraint language small and declarative. A later extension point, such as `ServiceLoader<ConfigurationConstraintProvider>`, can allow applications or modules to contribute custom constraints without turning the core into a general validation framework.

Out of scope for the first release: custom constraint providers and discovery of requirements from the classpath.

## Decisions

Related decision records: [ADR-0011](../decisions/0011-keep-last-valid-state-on-failed-reload.md).

- **Deprecation metadata location**: originally kept off `BerliozOption`; since 0.14.5, `BerliozOption` exposes `isDeprecated()`, `deprecatedSince()` and `removal()` for the option catalog ([theme 18](18-admin-overlay-upstreaming.md)).
- **Requirements file**: a separate `WEB-INF/config/config-requirements.xml` file, rather than inside `config.xml` or both.
- **Wildcard matches**: zero matches violate only a presence rule that requires at least one match.
- **Specified vs default**: preserve explicit/default provenance in the resolved configuration snapshot.

## Follow-up

After configuration validation ships, assess readiness for a 1.0.0 release and confirm which remaining core capabilities are required for 1.0.0 versus later 1.0.x releases.
