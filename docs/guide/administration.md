# Administration

This guide covers what operators and administration tools can do with a running Berlioz application: control parameters such as reloading, and the diagnostic generators.

## Control parameters

Berlioz has two control parameters, applied only to requests authorized for Berlioz control (`berlioz.control.key`, `berlioz.control.network`, or the delegated request attribute) and silently ignored otherwise:

- `berlioz-profile=true` enables profiling for the request.
- `berlioz-reload` takes `true` for everything, or a comma-separated list of scopes:

| Scope       | Effect                                                                  |
|-------------|-------------------------------------------------------------------------|
| `config`    | Reloads the global configuration; a failed reload keeps the last valid configuration. |
| `services`  | Reloads the services; a failed reload keeps the last valid registry.    |
| `xslt`      | Clears the XSLT cache.                                                  |
| `etags`     | Resets the ETag seed so that clients must revalidate.                   |
| `redirects` | Reloads the `RedirectFilter` and `RelocationFilter` rules.              |

For example, `?berlioz-reload=config,services`. Scopes are applied in the order of the table, whatever their order in the parameter. If the list contains an unknown scope, nothing is reloaded and a warning is logged.

No scope implies another. In particular, the response ETag combines the ETag seed, the generator ETags and the XSLT ETag, but not the service definitions: after changing services, use `berlioz-reload=services,etags` so that clients do not receive `304 Not Modified` for a service whose definition changed.

Authorized requests that reload at least one scope receive an `X-Berlioz-Reload` response header listing the accepted scopes, e.g. `X-Berlioz-Reload: config, services`. `redirects` is only applied where the filters are installed. `ReloadScope` and `ControlParameter` expose the scopes, the parser and the parameter names to tools such as Berlioz Admin.

`reload-services=true`, `clear-xsl-cache=true` and `reset-etags=true` are deprecated aliases of the `services`, `xslt` and `etags` scopes. They log a warning the first time they are used and will be removed in 1.0.

### Authorizing control

Control parameters are closed by default ([ADR-0006](../decisions/0006-close-control-parameters-by-default.md)). A request is authorized by any one of:

- `berlioz.control.key`: a shared secret, sent as `Authorization: Berlioz <key>`. Use a strong secret in production.
- `berlioz.control.network`: `loopback` or `lan`, for development only. Behind a reverse proxy, every request appears to come from the proxy, so never enable it there. Every address in `X-Forwarded-For`, `Forwarded` (`for=`) and `X-Real-IP` must also match the network, so a request relayed on behalf of an outside client is rejected. Forwarded addresses must be dotted-quad IPv4 or IPv6 literals; hostnames and abbreviated IPv4 forms such as `127.1` are rejected without DNS resolution.
- The request attribute `ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE`, set by the host application after its own authentication.

The same authorization guards the kickstart `*.src` mapping ([XSLT](xslt.md#default-xml-and-source-output)).

## Diagnostic generators

Berlioz includes generators that report its runtime state. They are not mapped by default: add them to administrative services in `services.xml`, and protect those services through the application's filters or container security. The generators do not perform authorization themselves.

```xml
<services group="admin">
  <namespace package="org.pageseeder.berlioz.generator"/>
  <namespace prefix="system" package="org.pageseeder.berlioz.system"/>
  <service id="admin-services" method="get">
    <url pattern="/admin/services"/>
    <handler class="ListRuntimeServices"/>
  </service>
  <service id="admin-threads" method="get">
    <url pattern="/admin/threads"/>
    <handler class="system:ListThreads"/>
  </service>
</services>
```

| Generator | Reports |
|---|---|
| `generator.ListRuntimeServices` | Registered services, their sources, and the last load |
| `generator.GetMatchingService` | The service matching a URL |
| `generator.GetServices` | The service configuration as written (XML only) |
| `generator.GetBerliozConfig` | Berlioz servlets, stylesheets, XSLT processor and container |
| `generator.GetErrorHandlerConfig` | Error handler configuration |
| `generator.ListBerliozOptions` | Every Berlioz option, with defaults and deprecations |
| `generator.GetGlobalConfig` | Global settings in use, with sensitive values redacted |
| `system.GetSystemProperties` | System properties, with sensitive values redacted |
| `system.GetRuntimeInfo`, `system.GetFileSystemInfo` | Memory and disk |
| `system.GetCpuSample` | Cumulative CPU counters; compute usage between two samples |
| `system.ListThreads`, `system.GetThreadInfo` | Threads, locks and stack traces (bounded) |
| `system.GetGeneratorStatistics` | Generator invocation statistics |
| `system.ListLibraries` | Libraries and their manifest attributes |

Package prefixes are `org.pageseeder.berlioz.generator` and `org.pageseeder.berlioz.system`. The sections below detail the most structured outputs.

### Services: `ListRuntimeServices`

`ListRuntimeServices` lists the services currently registered (`GetLiveServices` remains available as a deprecated alias until 1.0). It reports `load-status` in XML and `loadStatus` in JSON: `pending`, `ready`, `startup-failed`, or `reload-failed`. Its ETag includes that status so a rejected reload becomes visible even though the registry is unchanged. Exception details and source paths are not included in this status field.

Since 0.14.5, `ListRuntimeServices` also lists, after the services, the configuration sources of the last successful load in load order (`<source kind="classpath|filesystem" name="…"/>`, JSON `sources`), the warnings of that load (`<warning source="…" line="…">`, JSON `warnings`) and, while the last attempt has failed, its cause (`<load-failure source="…" line="…">`, JSON `loadFailure`), kept until the next successful load. The root carries ISO 8601 `loaded`, `failed` and `retry-after` timestamps when known. Sources are display names, never full paths or URLs. `line` and the parser's description of the failure are only included when `berlioz.errors.detail` is `standard` or `full`; at `minimal` the failure has a generic message. The same snapshot is available programmatically from `ServiceLoader.getLoadDiagnostics()`.

Each service also names the configuration that declared it (`service/@source`, the same display name as the sources). When a later source declares the same HTTP method and URI pattern, the service that wins lists every declaration it replaced, in load order, after its generators:

```xml
<service id="home" group="default" method="get" ... source="services.xml">
  ...
  <overridden service="home" source="bzadmin-0.14.0.jar!META-INF/berlioz/services.xml" pattern="/home"/>
</service>
```

In JSON, `overridden` is an array of `{service, source, pattern}`. A service that still serves other patterns stays listed in its own source. Services written by `Service.writeTo` without a registry, or registered directly rather than by `ServiceLoader`, have no source.

Since 0.14.5, `response-code` also names where its rule was declared in `origin`: `service`, `group` (the enclosing `<services>` element), `service-config` (the root element) or `default` when no rule was declared. The origin is omitted for services built without it, such as those created directly with `Service.Builder`.

### Servlet configuration: `GetBerliozConfig` and `GetErrorHandlerConfig`

`org.pageseeder.berlioz.generator.GetBerliozConfig` and `org.pageseeder.berlioz.generator.GetErrorHandlerConfig` report the configuration of initialized servlets; `GetBerliozConfig` also lists Berlioz servlets that are registered but not initialized yet.

Both implement `Generator` and `Cacheable` and work with XML and JSON output. XML roots are `berlioz-config` and `error-handler-config`, containing `servlet` elements; standalone JSON uses a top-level `servlets` array, without a named root wrapper. Scalar fields become XML attributes and JSON properties (hyphenated XML names become camelCase in JSON). Booleans remain JSON booleans. Mappings and extensions are sorted string arrays in JSON and space-separated XML attributes; optional fields are omitted. An empty registry produces `<error-handler-config/>` and `{"servlets":[]}`; `GetBerliozConfig` still reports the XSLT processor.

`GetBerliozConfig` reports servlet defaults, which individual services or responses can override: `name`, `mappings`, `media-type`, `charset`, `cache-control`, `compression`, `requires-control`, and `xslt-cache`. Compression means compression is permitted; `requires-control` means only requests authorized for Berlioz control are served; `xslt-cache` describes the current runtime template cache mode, separately from HTTP caching. `stylesheet` contains `allocation` (`identity`, `global`, `group`, or `service`), with `source` (`filesystem` or `classpath`) and a safe logical `path` when applicable. A filesystem path is reported relative to `WEB-INF`, as Berlioz resolves it: `/xslt/html/{GROUP}.xsl` is reported as `xslt/html/{GROUP}.xsl`; a path with `..` segments is never reported. Identity contains only `allocation`. An optional `fallback` contains its source and safe path. Group/service placeholders are reported as configured, without creating a transformer or resolving a specific service.

A `BerliozServlet` registered with the container but not initialized yet (it has no `load-on-startup` and has not been requested) only reports `name`, `mappings` and `initialised="false"`; its other fields are only known once it starts. Initialized servlets have no `initialised` field.

Since 0.14.5, `stylesheet` also lists the stylesheets selected so far as `selection` elements (JSON: a `selections` array): `name` (`global`, the group, or the service ID), `source` (`filesystem` or `classpath`), and `path` (relative to `WEB-INF` for a file, the `classpath:` reference for a resource) when it is safe to report. A stylesheet is selected the first time a service uses it, so a group appears only once it has been requested; with `{GROUP}`, the entry shows whether the last request used the file or a classpath overlay.

After the servlets, `xslt-processor` identifies the XSLT processor Berlioz uses: the `factory` class, and the `vendor`, `xslt-version`, `product-name` and `product-version` read from the standard XSLT system properties (the product is absent for XSLT 1.0 processors). `container` reports `server-info` (`ServletContext.getServerInfo()`) and the supported `servlet-api` version; it is omitted when no servlet is registered or the container does not identify itself. Generators can read the same values from `Environment.getServerInfo()` and `Environment.getServletApiVersion()`, and the processor from `XsltProcessorInfo.current()`.

For example:

```xml
<berlioz-config>
  <servlet name="html" mappings="*.html" media-type="text/html" charset="UTF-8"
           cache-control="no-cache" compression="true" requires-control="false"
           xslt-cache="manual">
    <stylesheet allocation="group" source="filesystem" path="xslt/html/{GROUP}.xsl">
      <selection name="default" source="filesystem" path="xslt/html/default.xsl"/>
      <selection name="admin" source="classpath" path="classpath:META-INF/berlioz/xslt/html/admin.xsl"/>
    </stylesheet>
  </servlet>
  <servlet name="json" mappings="*.json" initialised="false"/>
  <xslt-processor factory="net.sf.saxon.TransformerFactoryImpl" vendor="Saxonica"
                  xslt-version="3.0" product-name="SAXON" product-version="HE 12.10"/>
  <container server-info="Apache Tomcat/9.0.80" servlet-api="4.0"/>
</berlioz-config>
```

`GetErrorHandlerConfig` reports `name`, `mappings`, `problem-xml`, and `detail`, followed by:

- `forward`: `source` (`explicit`, `discovered`, or `legacy-default`), `extensions`, and `default-extension`. Diagnostic extension names omit the leading dot.
- `ignore`: sorted `extensions`.
- `stylesheet`: `configured` (`custom` or `built-in`), optional safe custom `path`, and `selected` (`custom`, `built-in`, or `none`). An unavailable custom stylesheet includes `fallback-reason="missing-or-unreadable"`. Selection reflects availability, not successful XSLT compilation; `none` means raw XML fallback.

Servlet mappings are deployed URL patterns, not the container's error-page registrations. Initialization settings are captured when each servlet starts and registrations are removed on destruction. Error rendering options and XSLT cache mode reflect current global settings. The snapshot uses only allowlisted fields; arbitrary parameters, secrets, absolute paths, raw JAR URLs, exception messages, and internal ETag seeds are never included. Unsafe paths are omitted while preserving source/selection information. Filesystem paths are relative to `WEB-INF`; `resource:` references are reported using canonical `classpath:` names.

Each generator captures one immutable snapshot per request and reuses it for its response and unquoted SHA-256 ETag, whichever is requested first. Only emitted content affects the fingerprint; unrelated settings and secrets do not. Diagnostics perform no JAR enumeration, transformer creation, or stylesheet compilation. Error stylesheet selection performs only a small availability check. Deployment assumes Berlioz is in each application's `WEB-INF/lib`.

### Options: `ListBerliozOptions`

`org.pageseeder.berlioz.generator.ListBerliozOptions` lists every `BerliozOption` in declaration order: `property`, `type` (`boolean` or `string`), `default`, a one-line `description`, and for deprecated options `deprecated` and `removal` versions. The XML root is `berlioz-options` with `option` elements; JSON uses an `options` array, with boolean defaults as JSON booleans. Values in use are not included; `GetGlobalConfig` returns them, redacted. The catalog is constant for a given Berlioz version and its ETag is a hash of its content. The same metadata is available programmatically through `BerliozOption.type()`, `description()`, `isDeprecated()`, `deprecatedSince()` and `removal()`.
