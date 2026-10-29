# 17. Scoped `berlioz-reload`

**Status:** 0.14.5 part done · **Target:** scopes and deprecated aliases in 0.14.5; removal of the aliases in 1.0

## Motivation

`berlioz-reload=true` does everything at once: it reloads the global configuration and the services, clears the XSLT cache and resets the ETag seed. `RedirectFilter` and `RelocationFilter` also reload their rules. Three separate parameters did part of the work (`reload-services`, `clear-xsl-cache`, `reset-etags`), but nothing reloaded only the configuration. Adding a parameter for each case would add more global parameters.

Background: since 0.14.0, control parameters are closed by default. `berlioz.control.key` (explicit key) and `berlioz.control.network` (default `off`) are independent channels, plus a fixed delegated-authorization request attribute for host applications.

## Proposal

Keep the control parameters to a minimum (decided 2026-10-27). `berlioz-reload` takes scopes, and `reload-services`, `clear-xsl-cache` and `reset-etags` are retired in its favour, leaving `berlioz-reload` and `berlioz-profile`.

`berlioz-reload` accepts a comma-separated list of scopes, and `true` keeps meaning everything:

- `berlioz-reload=true`: everything, unchanged.
- `berlioz-reload=services`: the services only.
- `berlioz-reload=config,services`: the global configuration, then the services.
- Scopes: `config`, `services`, `xslt`, `etags`, and `redirects` for the filter rules.

The administrator chooses what to reload, and one parameter covers every case, including a configuration-only reload.

Retiring the other parameters:

- 0.14.x: `reload-services=true` is `berlioz-reload=services`, `clear-xsl-cache=true` is `berlioz-reload=xslt`, `reset-etags=true` is `berlioz-reload=etags`. The three parameters keep working as deprecated aliases of their scope, and log a deprecation warning once when applied.
- 1.0: the three parameters are removed. Only `berlioz-reload` and `berlioz-profile` remain.
- Documentation, `BerliozServlet` Javadoc and `ControlAuthorization` list only the two parameters, with the aliases in the migration notes.

## Decisions

Related decision records: [ADR-0006](../decisions/0006-close-control-parameters-by-default.md), [ADR-0011](../decisions/0011-keep-last-valid-state-on-failed-reload.md).

Decided 2026-10-28, implemented in 0.14.5:

- **Order**: scopes are applied in a fixed order (`config`, `services`, `xslt`, `etags`, `redirects`), whatever their order in the parameter.
- **Unknown scopes**: the whole request is rejected; no scope is applied and a warning is logged.
- **Partial reloads**: no scope implies another. The documentation recommends `services,etags` after changing services.
- **Configuration failures**: `GlobalSettings.load()` keeps the last valid configuration when a reload fails; startup is unchanged.
- **Filters**: `RedirectFilter` and `RelocationFilter` use the same parser and apply the `redirects` scope.
- **Reporting**: authorized requests that apply at least one scope receive `X-Berlioz-Reload` listing the accepted scopes.
- **Public API**: `ReloadScope` (scopes, parser, header) and `ControlParameter` (parameter names, `isControlParameter`), also covering [theme 18](18-admin-overlay-upstreaming.md), item 5.
