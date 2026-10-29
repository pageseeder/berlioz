# Services Schema

This page documents the format of `services.xml`. The DTD is [`services-1.0.dtd`](../../pso-berlioz/src/main/resources/library/services-1.0.dtd); the [services guide](../guide/services.md) explains how to use it.

## Quick reference

```xml
<service-config version="1.0">
  <!-- Java packages for generator class names, for all groups -->
  <namespace package="[java_package]" prefix="[prefix]"?/> *
  <!-- Response code rule for all services -->
  <response-code use="[selector]" rule="[highest|lowest|first]"?/> ?

  <!-- Group of services -->
  <services group="[group_name]"> *
    <namespace package="[java_package]" prefix="[prefix]"?/> *
    <response-code use="[selector]" rule="[highest|lowest|first]"?/> ?

    <!-- Individual service -->
    <service id="[service_id]" method="[get|post|put|patch|delete|query]"
             flags="[flags]"? cache-control="[http_cache_control]"?> *
      <url pattern="[uri_pattern]"/> +
      <response-code use="[selector]" rule="[highest|lowest|first]"?/> ?
      <!-- Either generators... -->
      <generator class="[class_name]"? name="[name]"? target="[target]"?> *
        <parameter name="[name]" value="[value]"/> *
      </generator>
      <!-- ...or a single handler -->
      <handler class="[class_name]"?> ?
        <parameter name="[name]" value="[value]"/> *
      </handler>
    </service>
  </services>
</service-config>
```

A `services!<name>.xml` file has a `<services group="…">` root element instead of `<service-config>`.

## Data types

| Type | Values |
|---|---|
| URI pattern | A URI template ([RFC 6570](https://www.rfc-editor.org/rfc/rfc6570)), matched against the path without its extension |
| HTTP method | `get`, `put`, `delete`, `post`, `patch`, `query` |
| Code rule | `highest`, `lowest`, `first` |

## Elements

### `service-config`

The root element of `services.xml`.

| Attribute | Type | Description |
|---|---|---|
| `version` | `1.0` | Optional; if specified, must be `1.0` |

Content: `namespace*`, `response-code?`, `services*`.

### `services`

A group of services. It is the root element of `services!<name>.xml` files; there it must not be wrapped in `<service-config>`, which would clear the services loaded before it.

The group selects the principal stylesheet for its services, such as `xslt/html/<group>.xsl`. Services that share layout, styles, scripts or templates generally belong in the same group.

| Attribute | Type | Description |
|---|---|---|
| `group` | NMTOKEN | Required. The name of the group |

Content: `namespace*`, `response-code?`, `service*`.

### `namespace`

Maps a Java package so that generator class names can be written without it. Without a `prefix`, unqualified class names (no `.` or `:`) are resolved against the package. With a `prefix`, class names written `prefix:ClassName` resolve to `package.ClassName`.

Declarations under `<service-config>` apply to all groups. Declarations under `<services>` apply to that group only and override a declaration with the same prefix from `<service-config>`.

| Attribute | Type | Description |
|---|---|---|
| `package` | CDATA | Required. The Java package, such as `com.example.generators` |
| `prefix` | CDATA | The prefix used before `:` in class names; omit for the default namespace |

### `service`

A single service, matching at least one URL. Generators are invoked in the order they are declared. A service has either generators or one handler.

| Attribute | Type | Description |
|---|---|---|
| `id` | ID | Required. A unique identifier, available to stylesheets as `root/@service` |
| `method` | HTTP method | Required. The HTTP method the service accepts |
| `flags` | NMTOKENS | Values that qualify the service, available to stylesheets as `root/@flags` |
| `cache-control` | CDATA | The `Cache-Control` header for cacheable responses ([HTTP caching](../guide/caching.md)) |

Content: `url+`, `response-code?`, then `generator*` or `handler`.

### `url`

A URI pattern that the service matches.

| Attribute | Type | Description |
|---|---|---|
| `pattern` | URI pattern | Required |

### `generator`

A generator to invoke for the service. Its output is wrapped in a `<content>` element and passed to XSLT with the other generators' output.

| Attribute | Type | Description |
|---|---|---|
| `class` | CDATA | The generator class; defaults to `NoContent` when omitted |
| `name` | NMTOKEN | A name, copied to `content/@name` and usable in `response-code` selectors |
| `target` | NMTOKEN | A target, copied to `content/@target`, for stylesheets to decide where content goes |

Content: `parameter*`.

### `handler`

A single generator whose output is the whole response, written directly without the Berlioz envelope or XSLT.

| Attribute | Type | Description |
|---|---|---|
| `class` | CDATA | The generator class; defaults to `NoContent` when omitted |

Content: `parameter*`.

### `parameter`

A parameter passed to the generator, for example to rename a request parameter or combine values.

| Attribute | Type | Description |
|---|---|---|
| `name` | CDATA | Required. The parameter name |
| `value` | CDATA | Required. A static value, or a template with `{name}` placeholders replaced by request parameters |

### `response-code`

How the HTTP status of a service is computed from the statuses of its generators. Useful when a service has several generators and the failure of a secondary one (for example returning `404`) should not fail the whole service.

It can be declared on the whole configuration, a group, or a service; the most specific declaration applies. Without any declaration, the status is the highest of all generators (`use="name:*" rule="highest"`).

| Attribute | Type | Description |
|---|---|---|
| `use` | CDATA | Required. Which generators count: `name:` followed by generator names, or `target:` followed by targets; `name:*` for all |
| `rule` | Code rule | `highest` (default), `lowest`, or `first` |
