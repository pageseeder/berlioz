# XSLT

Berlioz combines the output of a service's generators into one XML document (described in the [output schema](../reference/output-schema.md)) and transforms it with XSLT. Stylesheets need an XSLT 2.0 processor, such as Saxon-HE.

## Kickstart mappings

`pso-berlioz-kickstart` maps four extensions, each to a Berlioz servlet with its own stylesheet:

| Mapping | Stylesheet | Without a group stylesheet |
|---|---|---|
| `*.html` | `WEB-INF/xslt/html/<group>.xsl` | Error |
| `*.xml` | `WEB-INF/xslt/xml/<group>.xsl` | Fallback: the generator output, without internal details |
| `*.json` | `WEB-INF/xslt/json/<group>.xsl` | Fallback: `{}` |
| `*.src` | None: the raw XML before transformation | Requires Berlioz control authorization |

A JSON request for a service whose generators can write JSON is answered directly, without XSLT.

## Stylesheet configuration

Each Berlioz servlet takes a `stylesheet` and an optional `fallback-stylesheet` init parameter. Their value can be:

- `IDENTITY`: no transformation.
- A `classpath:` reference, resolved through the application class loader. An optional leading slash is accepted: `classpath:xslt/main.xsl` and `classpath:/xslt/main.xsl` resolve to the same resource. `resource:` is a **deprecated** alias with identical behavior, until 1.0.
- Any other value: a filesystem path beneath `WEB-INF`.

`{GROUP}` and `{SERVICE}` are replaced by the service's group and ID before resolution, for both primary and fallback stylesheets.

```xml
<init-param>
  <param-name>stylesheet</param-name>
  <param-value>classpath:xslt/{GROUP}.xsl</param-value>
</init-param>
```

## Imports and includes

A classpath stylesheet may use relative `xsl:import`/`xsl:include` to reach other templates in the *same* JAR; the stylesheet's own resource URL is the base for relative references:

```xml
<xsl:import href="parts/imported.xsl"/>
<xsl:include href="parts/included.xsl"/>
```

Reaching a template in a *different* JAR or in the filesystem requires an explicit `classpath:` (or deprecated `resource:`) reference as the `href`. Every import and include is restricted to local `file:` and `jar:file:` resources; HTTP(S) and other network schemes are always rejected, independently of the XSLT processor's own `accessExternalStylesheet` setting.

Tools that read or compile stylesheets outside the renderer can apply the same rules through `org.pageseeder.berlioz.xslt.XsltResolvers` (since 0.14.5). `XsltResolvers.secure()` (or `secure(ClassLoader)`) returns the renderer's `URIResolver`; like any `URIResolver`, it returns `null` for approved relative and `file:`/`jar:file:` references so the processor loads them. `XsltResolvers.locate(href, base, loader)` applies the same checks and returns the URL the renderer would load for every approved reference, so a tool can read the stylesheet as plain XML; denied, malformed or missing references throw a `TransformerException`. Nothing is read or fetched while checking a reference.

## Group stylesheets from overlays

Since 0.14.4, filesystem group stylesheets fall back to [overlays](overlays.md) on the classpath. With `stylesheet=xslt/html/{GROUP}.xsl`, an absent `WEB-INF/xslt/html/admin.xsl` selects `META-INF/berlioz/xslt/html/admin.xsl` from the classpath, so the host application no longer needs an import stub.

This is enabled by default and has no separate switch. It applies only to filesystem stylesheet patterns containing `{GROUP}` whose resolved path is under `WEB-INF/xslt/`. Explicit `classpath:` patterns, fixed filesystem stylesheets, and files outside that directory are unaffected. A present filesystem file always wins, including an unreadable, invalid or uncompilable one: those failures remain errors. When there is no overlay either, the `fallback-stylesheet` applies, or the request fails if none is configured.

Overlay lookup uses the application class loader (thread context class loader, with the framework loader as fallback). For duplicate resources, its first `getResource` match wins: parent delegation and then the loader's classpath order determine precedence. Berlioz does not sort or merge stylesheet resources, so keep the deployment classpath order stable; this differs from how service declarations are merged. Relative imports and includes keep the selected resource URL as their base, including nested directories inside JARs.

Selection is checked whenever the servlet obtains a group transformer, in every cache mode. Adding or removing a filesystem override replaces the transformer, invalidates compiled entries for both selections, and recomputes its ETag. Classpath content changes require a redeployment or an explicit cache clear. `config.getTransformer(service).location()` reports the selection: `kind()` is `FILESYSTEM` or `CLASSPATH`, and `logicalPath()` the safe `WEB-INF/...` or `classpath:META-INF/berlioz/...` reference. Selection is also logged at debug level.

Tools can apply the same selection to any path without a servlet (since 0.14.5). `StylesheetLocation.select(webInf, "xslt/html/admin.xsl")` returns the file when it exists, otherwise the first `META-INF/berlioz/xslt/html/admin.xsl` on the classpath (or the missing file when there is neither). `StylesheetLocation.countClasspathCopies("xslt/html/admin.xsl")` counts the copies on the classpath, of which only the first can be selected. Berlioz does not list available stylesheets: enumerating classpath folders depends on how each container packages JARs, so tools find candidate paths themselves and ask Berlioz which one wins.

## Caching

`berlioz.xslt.cache` (`no`, `auto`, `manual`) controls when compiled stylesheets are reused. It applies differently to filesystem and classpath templates, because a classpath resource cannot change without a redeployment:

| Mode | Filesystem | Classpath |
|---|---|---|
| `no` | Recompiled on every use | Recompiled on every use |
| `auto` | Recompiled when the file changes | Treated as immutable, reused |
| `manual` (default) | Reused until explicitly cleared | Treated as immutable, reused |

Use `auto` during development and `manual` in production; `berlioz-reload=xslt` clears the cache ([administration](administration.md)).

Classpath cache keys distinguish identically named resources contributed by different JARs (and by different application class loaders in the same JVM), so `clearCache()`/`clearAllCache()` can still invalidate them individually. An overlay's XSLT ETag changes when its own artifact changes, including imported and included templates in the same JAR.

## Default XML and source output

When a group has no `xml/<group>.xsl`, the kickstart `*.xml` mapping applies its fallback stylesheet. Since 0.14.5, the fallback filters the document through an allowlist to reduce information disclosure:

- `<header>` (Berlioz version and mode, `berlioz.app.*` settings, CSP nonce, location and host details, HTTP and URI parameters) is replaced by an XML comment noting its removal.
- `<root>` keeps only `@service`, `@group` and `@flags`.
- Each `<content>` keeps only `@name`, `@target` and `@status`; the generator payload is copied unchanged.

This is not a complete safeguard: the generator payload itself is still published as is. Applications should generally provide their own `xml/<group>.xsl` to control what is exposed. A template that copies the whole source document (`<xsl:sequence select="."/>`) exposes everything the fallback hides; copy only what clients need:

```xml
<xsl:template match="/root">
  <root service="{@service}">
    <xsl:copy-of select="content/node()"/>
  </root>
</xsl:template>
```

The `*.src` mapping (`BerliozSource`) returns the full document before XSLT so that developers can see the source. It sets the `requires-control` init parameter, so it only serves requests authorized for Berlioz control: the delegated request attribute, `berlioz.control.key`, or `berlioz.control.network`. Other requests, including `OPTIONS`, receive a `404`. For local development, `berlioz.control.network=loopback` is usually enough:

```xml
<global>
  <berlioz>
    <control network="loopback"/>
  </berlioz>
</global>
```

`requires-control` can be set on any `BerliozServlet`. To open the source mapping again, redeclare the servlet in `web.xml` with the same name; `web.xml` init parameters override those of the fragment:

```xml
<servlet>
  <servlet-name>BerliozSource</servlet-name>
  <servlet-class>org.pageseeder.berlioz.servlet.BerliozServlet</servlet-class>
  <init-param>
    <param-name>requires-control</param-name>
    <param-value>false</param-value>
  </init-param>
</servlet>
```

## JSON from XSLT

A stylesheet can produce JSON: declare an XML output with the JSON media type, and Berlioz converts the XML result to JSON.

```xml
<xsl:output method="xml" media-type="application/json"/>
```

Attributes in the `http://pageseeder.org/JSON` namespace give type hints (`json:boolean`, `json:number`, `json:null`); see the `org.pageseeder.berlioz.aeson` package in the Javadoc. For new JSON endpoints, a `Generator` or handler writing JSON directly is simpler ([generators](generators.md)).
