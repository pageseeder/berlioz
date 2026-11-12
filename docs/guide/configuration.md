# Configuration

A Berlioz application relies on a few configuration files besides the web descriptor:

| File | Location | Usage |
|---|---|---|
| `web.xml` | `WEB-INF/` | Web descriptor; optional with `pso-berlioz-kickstart` |
| `services.xml` | `WEB-INF/config/` | Maps URLs to generators ([services](services.md)) |
| `services!<group>.xml` | `WEB-INF/config/` | Additional service files, loaded after `services.xml` |
| `config.xml` | `WEB-INF/config/` | Global settings |
| `config-<mode>.xml` | `WEB-INF/config/` | Global settings for one mode, overriding `config.xml` |
| `logback-<mode>.xml` | `WEB-INF/config/` | Logging configuration (optional, see [logging](logging.md)) |
| `redirect.xml` | `WEB-INF/config/` | Redirect rules for `RedirectFilter` (optional, see [redirects](#redirects)) |

Global settings can also be written as `.properties` files instead of XML.

## Setting up Berlioz

1. Add `pso-berlioz-kickstart` to the application. Its web fragment registers the Berlioz servlets and maps `*.html`, `*.xml`, `*.json` and `*.src`. Without it, declare the Berlioz servlets in `web.xml`.
2. Choose a mode for each environment.
3. Add the global configuration in `WEB-INF/config/`.
4. Add the services configuration in `WEB-INF/config/`.

The [getting started](../../README.md#getting-started) section walks through a complete example.

## Modes

The mode identifies the environment, such as `local`, `dev` or `production`. It is set with the `berlioz.mode` system property, for example `-Dberlioz.mode=local`, and is reported in the `header/berlioz/@mode` of the output.

Berlioz loads `config.xml` first, then `config-<mode>.xml`, whose values override those of `config.xml`. Logging configuration is also selected by mode.

A separate application data directory can be set with `-Dberlioz.appdata=<path>`. Mode-specific configuration files in its `config` folder are used before those in `WEB-INF/config/`, which keeps environment-specific settings out of the WAR.

## Global settings

Settings are grouped under `<berlioz>`; attributes and nested elements combine into dot-separated property names. For example, the following sets `berlioz.errors.detail` and `berlioz.control.network`:

```xml
<global>
  <berlioz>
    <errors detail="full"/>
    <control network="loopback"/>
  </berlioz>
</global>
```

The defaults are meant for production. A typical development configuration (`config-local.xml`) relaxes a few of them:

```xml
<global>
  <berlioz>
    <errors detail="full"/>
    <xslt cache="auto"/>
    <control network="loopback"/>
  </berlioz>
</global>
```

Every Berlioz option is documented in the [`BerliozOption` Javadoc](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz), with its default and recommended development and production values. At runtime, the `ListBerliozOptions` generator lists them (see [administration](administration.md)).

### Application settings

Settings under `<app>` are for the application itself. They are available to generators through `GlobalSettings`, and appear as attributes of `header/app` in the output:

```xml
<global>
  <berlioz>
    <app version="1.2.0" date="2026-10-29"/>
  </berlioz>
</global>
```

### Variables

Values may contain `${name}` tokens, resolved against system properties and environment variables, with an optional default: `${name:default}`.

```xml
<global>
  <berlioz>
    <control key="${BERLIOZ_CONTROL_KEY}"/>
  </berlioz>
</global>
```

### Reloading

The configuration can be reloaded at runtime with `berlioz-reload=config` (see [administration](administration.md)). A reload that fails keeps the last valid configuration.

## Redirects

`RedirectFilter` redirects requests whose path matches a rule, using the file named by its `config` init parameter, relative to `WEB-INF`, such as `config/redirect.xml`. Rules are URI patterns: variables matched by `from` are expanded in `to`. The first matching rule applies.

```xml
<redirect-mapping>
  <redirect from="/"             to="/html/home"/>
  <redirect from="/{+path}.psml" to="/html/{+path}" status="301"/>
  <redirect from="/api/v1/{+p}"  to="/api/v2/{+p}"  status="308"/>
  <redirect from="/maintenance"  to="/html/status"  status="307"/>
</redirect-mapping>
```

`status` sets the HTTP status of the redirect:

| Status | Meaning | Method of the next request |
|---|---|---|
| `301` | Moved permanently | Clients may change `POST` to `GET` |
| `302` | Found (temporary) | Clients may change `POST` to `GET` |
| `307` | Temporary redirect | Unchanged, with its body |
| `308` | Permanent redirect | Unchanged, with its body |

Without `status`, the redirect is temporary (`302`). Any other value fails validation, and the filter has no rules until the file is fixed. Prefer `307` or `308` when a redirected path receives requests other than `GET` or `HEAD`. Browsers cache permanent redirects (`301` and `308`), so test a rule as temporary first. Rules are reloaded with `berlioz-reload=redirects` (see [administration](administration.md)).

The `permanent` attribute is deprecated since 0.14.6 and will be removed in 1.0. Berlioz still reads it when `status` is absent (`permanent="yes"` gives `301`), ignores it when `status` is present, and logs a warning when a file uses it. Replace `permanent="yes"` with `status="301"` and remove `permanent="no"`.
