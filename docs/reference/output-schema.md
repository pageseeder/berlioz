# Output Schema

This page documents the XML document that Berlioz builds for a service and passes to its stylesheet. Use the `*.src` mapping to see it for any URL during development ([XSLT](../guide/xslt.md#default-xml-and-source-output)).

```xml
<root service="user" group="default" flags="…">
  <header>
    <location scheme="https" host="example.org" port="443" path="/users/jsmith.html" query="tab=posts"
              base="https://example.org">https://example.org/users/jsmith.html?tab=posts</location>
    <path info="/users/jsmith" extension=".html"/>
    <http-parameters>
      <parameter name="tab">posts</parameter>
    </http-parameters>
    <uri-parameters>
      <parameter name="user">jsmith</parameter>
    </uri-parameters>
    <berlioz version="0.14.5" mode="production"/>
    <app version="1.2.0"/>
    <security nonce="…" source="berlioz"/>
  </header>
  <content generator="com.example.GetUser" name="user" target="main" etag="…" status="ok">
    <!-- generator output -->
  </content>
</root>
```

The kickstart `*.xml` fallback removes `<header>` and internal attributes before publishing this document; see [XSLT](../guide/xslt.md#default-xml-and-source-output).

## `root`

The document element. Stylesheets usually select templates on `@service`.

| Attribute | Description |
|---|---|
| `service` | The ID of the service |
| `group` | The group of the service |
| `flags` | The service's flags, when declared |

Content: `header`, then one `content` element per generator.

## `header`

Information about the request and the application.

| Element | Description |
|---|---|
| `location` | The URL of the request |
| `path` | How the path was split to select the service and the stylesheet |
| `http-parameters` | HTTP parameters |
| `uri-parameters` | Variables extracted from the URI pattern |
| `berlioz` | Berlioz version and mode |
| `app` | Application settings (`berlioz.app.*`), when configured |
| `security` | CSP nonce, when `berlioz.nonce.enable` is set |

With the deprecated `berlioz.xml.header.version=0.9`, the header also starts with the compatibility elements `group`, `service`, `path-info`, `context-path`, `scheme`, `host`, `port`, `url` and `query-string`. They are removed in 1.0.

## `location`

The URL of the request, split into its components so that stylesheets can easily use them. The text is the full URL.

| Attribute | Description |
|---|---|
| `scheme` | `http` or `https` |
| `host` | The host |
| `port` | The port, even when it is the default for the scheme |
| `path` | The path |
| `query` | The query, without `?` |
| `base` | Scheme, host and port (omitted when default), to rebuild URLs |

## `path`

The path split into the part matched against the URI pattern (`info`) and the part used to select the stylesheet (`prefix` or `extension`, depending on the servlet mapping; the standard mapping uses the extension).

```xml
<path info="/search" extension=".html"/>
```

| Attribute | Description |
|---|---|
| `context` | The servlet context path, when not empty |
| `prefix` | The servlet prefix, when the mapping uses one |
| `info` | The part matched against the URI pattern |
| `extension` | The extension, when the mapping uses one |

## `http-parameters`

The HTTP parameters, from the query string or a form body. Parameters with the same name are listed in the order they were sent. Very long names are skipped, and very long values are truncated, with `truncated="true"`.

```xml
<http-parameters>
  <parameter name="q">test</parameter>
</http-parameters>
```

## `uri-parameters`

The variables extracted from the URI pattern. If a service matches `/accounts/{account}` and the URL is `/accounts/jsmith.html`:

```xml
<uri-parameters>
  <parameter name="account">jsmith</parameter>
</uri-parameters>
```

## `parameter`

| Attribute | Description |
|---|---|
| `name` | The name of the parameter |
| `truncated` | `true` when the value was truncated (HTTP parameters only) |

The text is the value, escaped as XML text.

## `berlioz`

| Attribute | Description |
|---|---|
| `version` | The version of Berlioz |
| `mode` | The mode of the application |

## `app`

One attribute per `berlioz.app.*` setting with a valid XML name, for example `<app version="1.2.0" date="2026-10-29"/>` for `<app version="1.2.0" date="2026-10-29"/>` in the configuration.

## `security`

| Attribute | Description |
|---|---|
| `nonce` | A nonce for the Content Security Policy |
| `source` | `berlioz` when Berlioz generated the nonce, `header` when it came from the request attribute named by `berlioz.nonce.attribute` |

## `content`

One per generator in the service, in the order they are declared, containing the generator's output.

| Attribute | Description |
|---|---|
| `generator` | The Java class of the generator |
| `name` | The generator's name |
| `target` | The generator's target, when declared |
| `etag` | The generator's ETag, when it is `Cacheable` |
| `deprecated` | `true` when the generator class is deprecated |
| `status` | The generator's status after it ran |
| `profile-etag`, `profile-process`, `profile` | Time in milliseconds to compute the ETag, generate the content, and both; only with `berlioz-profile=true` |

When a generator fails and Berlioz catches the error, the `content` element contains a `<problem>` instead of the generator output ([error handling](../guide/error-handling.md)).
