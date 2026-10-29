# Services

A service maps one or more URL patterns and an HTTP method to the generators that produce the response. Services are declared in `WEB-INF/config/services.xml`; the complete format is in the [services schema](../reference/services-schema.md).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE service-config PUBLIC "-//Berlioz//DTD::Services 1.0//EN"
    "https://pageseeder.org/schema/berlioz/services-1.0.dtd">
<service-config version="1.0">
  <services group="default">
    <namespace package="com.example.generators"/>
    <service id="user" method="get" cache-control="private, max-age=60">
      <url pattern="/users/{user}"/>
      <generator class="GetUser" name="user" target="main"/>
      <generator class="GetRecentPosts" target="aside"/>
    </service>
  </services>
</service-config>
```

## URL patterns

Patterns are [URI templates](https://www.rfc-editor.org/rfc/rfc6570) matched against the path, without the extension that selects the output format: `/users/{user}` matches `/users/jsmith.html`, `/users/jsmith.json`, and so on. Patterns such as `/{user}/message/{id}`, `/wiki/{term}` or `/documents{;type,size}/title` are supported. Variables are passed to generators as URI parameters.

A service can declare several `<url>` patterns. When several services match a URL, the most specific pattern wins.

## Methods

`method` is one of `get`, `post`, `put`, `patch`, `delete` or `query`. A `get` service also answers `HEAD`. A `POST` request never falls back to a `get` service; map it explicitly.

## Groups

Every service belongs to a group (`<services group="…">`). The group selects the stylesheet used to render the service, such as `WEB-INF/xslt/html/<group>.xsl` ([XSLT](xslt.md)). Services are usually grouped by page layout, by area of the site, or by usage (pages, API). If services share styles, scripts or templates, they probably belong in the same group.

## Generators and handlers

Generators run in the order they are declared, and their outputs are combined into one XML document passed to the stylesheet. `name` and `target` are copied to the output so that stylesheets can decide where to place each generator's content. `<parameter>` elements pass or rename parameters for a generator; their values can contain `{name}` placeholders replaced by request parameters.

Instead of generators, a service can declare a single `<handler>`. Its output is the whole response, written directly as JSON or XML without the Berlioz envelope or XSLT. Handlers suit API endpoints:

```xml
<service id="get-note" method="get">
  <url pattern="/api/note"/>
  <handler class="com.example.api.GetNote"/>
</service>
```

Writing generators is covered in [generators](generators.md).

### Namespaces

`<namespace package="…"/>` lets generator classes be written without their package. A namespace with a `prefix` lets classes be written as `prefix:ClassName`. Namespaces declared under `<service-config>` apply to every group; those under `<services>` apply to that group only.

## Response codes

When a service has several generators, the HTTP status is computed from their individual statuses. By default it is the highest status of all generators, so one generator returning `404` makes the whole response `404`.

`<response-code>` changes that rule. It can be declared on a service, a group, or the whole `<service-config>`; the most specific declaration applies.

```xml
<!-- Only the 'user' generator decides the status -->
<response-code use="name:user" rule="first"/>
```

- `use` selects the generators that count: `name:` followed by generator names, or `target:` followed by targets. `name:*` selects every generator.
- `rule` is `highest` (default), `lowest` or `first`.

This allows partial failure: a page can still be returned with `200` when a secondary generator fails.

## Several service files

Large applications can split services into `services!<name>.xml` files next to `services.xml`. They load after `services.xml`, in alphabetical order. Their root element is `<services group="…">`, not `<service-config>`.

Services can also come from JAR dependencies ([overlays](overlays.md)). Filesystem declarations always take precedence over classpath declarations for the same method and pattern.

## Loading and reloading

Service discovery and parsing are transactional: an I/O or fatal parsing error rejects the whole candidate configuration. An absent configuration directory is valid for applications whose services all come from overlays; failure to read an existing directory or the classpath is not.

Before the first successful load, requests receive a service-configuration `503`. Berlioz loads services lazily, so this does not prevent the servlet container from starting; deployment readiness checks should request a known Berlioz route.

After a successful load, a failed reload keeps serving the complete last valid registry, including its warnings and source metadata. Nothing is published partially. Failed loads are retried on the next request after a 30-second delay; an authorized `berlioz-reload=services` request (or `ServiceLoader.requestReload()`) retries immediately. Explicit `ServiceLoader.load()` calls still throw on failure, and `clear()` deliberately removes the retained registry.

Each failure is logged with its cause, and recovery is logged when a load succeeds. The `ListRuntimeServices` diagnostic shows the load status, sources and failures ([administration](administration.md)).
