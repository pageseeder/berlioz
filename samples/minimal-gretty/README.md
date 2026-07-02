# Minimal Berlioz Gretty Sample

This module is a small WAR application that demonstrates the minimum pieces of a Berlioz site:

- `pso-berlioz-kickstart` on the classpath for servlet mappings and fallback transforms.
- `WEB-INF/config/services.xml` for URI-to-generator mapping.
- `WEB-INF/config/config.xml` plus `config-local.xml` for base and local-mode settings.
- `WEB-INF/xslt/html/default.xsl` for HTML rendering.
- `WEB-INF/xslt/xml/default.xsl` and `WEB-INF/xslt/json/default.xsl` for simple format checks.

Run it locally:

```bash
./gradlew :samples:minimal-gretty:appRun
```

Then try:

- http://localhost:8999/hello.html
- http://localhost:8999/hello.xml
- http://localhost:8999/hello.src
- http://localhost:8999/hello.json
- http://localhost:8999/api/note.json
- https://localhost:8444/hello.html

Gretty runs with `-Dberlioz.mode=local` and `-Dberlioz.appdata=local/appdata`, so `config-local.xml`
overrides the base configuration during local development.

The API sample uses direct JSON generators:

```bash
curl http://localhost:8999/api/note.json
curl -X POST 'http://localhost:8999/api/note.json?text=Hello%20Berlioz'
curl http://localhost:8999/api/note.json
```
