# Error Handling

Berlioz reports errors as RFC 9457 Problem Details, in XML (`application/problem+xml`) or JSON (`application/problem+json`). This guide covers the error options, how errors flow through Berlioz, and how generators report errors. The rationale is in [ADR-0005](../decisions/0005-problem-details-error-format.md).

## 1. Overview

Berlioz has two distinct error layers. The **generator layer** handles exceptions thrown during content generation (within the XML pipeline). The **framework layer** handles everything the generator layer cannot: no matching service, XSLT failures, configuration errors, or uncaught generator exceptions when catch mode is off.

Since 0.14.0, both layers emit **RFC 9457 Problem Details** by default. The pre-0.14 non-Problem-Details XML format still exists as a deprecated escape hatch (`berlioz.errors.problem=false`) for applications migrating their XSLT templates, but it — and the option that selects it — will be removed in 1.0.

---

## 2. Configuration Options

All options are set in the global configuration (`WEB-INF/config/config.xml`, or `config-<mode>.xml` per environment); see [configuration](configuration.md).

### `berlioz.errors.handle` — `ERROR_HANDLER`

**Type:** boolean | **Default:** `true`

Controls where errors go when the framework needs to report one.

- `true` — Berlioz handles it internally, dispatching to `ErrorHandlerServlet` (HTML/XML) or writing `application/problem+json` directly (JSON-typed servlets)
- `false` — delegates to the servlet container via `res.sendError()`, letting `web.xml` `<error-page>` entries take over

| Development | Production |
|---|---|
| `true` | `false` (prefer custom `web.xml` error pages) |

### `berlioz.errors.generator-catch` — `ERROR_GENERATOR_CATCH`

**Type:** boolean | **Default:** `true`

Controls whether generator exceptions are caught by Berlioz or re-thrown.

- `true` — exceptions are caught, converted to a `<problem>` element inline in the XML, and the XSLT pipeline continues
- `false` — exceptions propagate; the servlet layer calls `sendError()` and the XSLT pipeline is bypassed

| Development | Production |
|---|---|
| `false` (see all errors immediately) | `true` (degrade gracefully) |

> **Note:** `HttpException` subclasses (`InvalidParameterException`, `UpstreamException`, custom) are **always** caught and converted regardless of this setting. The flag only affects `Exception`/`BerliozException`.

### `berlioz.errors.detail` — `ERROR_DETAIL`

**Type:** string | **Default:** `"minimal"`

Controls how much diagnostic information is included in error responses. Applies to both the legacy format and the RFC 9457 problem format.

| Value | What is included |
|---|---|
| `full` | Stack trace, cause chain, HTTP headers, HTTP parameters |
| `standard` | Exception class, message, and source location (when available from a `SAXParseException`/`TransformerException`) — no stack trace |
| `minimal` | HTTP status, title, and message only — no exception info |

| Development | Production |
|---|---|
| `full` | `minimal` |

> **Security note:** `minimal` is the safe-by-default fallback and the actual default. Any unrecognised value also resolves to `MINIMAL`. Set `berlioz.errors.detail=full` explicitly in development configurations — do not rely on a permissive default.

### `berlioz.errors.problem` — `ERROR_PROBLEM_FORMAT`

**Type:** boolean | **Default:** `true`

When `true` (the default), `ErrorHandlerServlet` and the generator-catch path emit RFC 9457 `<problem>` XML instead of the deprecated non-Problem-Details `<error>` format. Both formats are handled by the built-in failsafe XSLT, so switching does not break error page rendering.

> **Deprecated since 0.14.0, scheduled for removal in 1.0.** Problem Details will always be used from 1.0 onward. Set `berlioz.errors.problem=false` only as a temporary migration aid if existing XSLT templates still consume the pre-0.14 legacy XML shape — see [§7](#7-error-format-reference). This flag has no effect on JSON: JSON-configured servlets always emit `application/problem+json`, since there was never a legacy JSON representation to fall back to.

| Development | Production |
|---|---|
| `true` (default — no action needed) | `true` (default — no action needed) |

### `berlioz.errors.stylesheet` — `ERROR_STYLESHEET`

**Type:** string | **Default:** `""` (empty — use built-in failsafe)

Path relative to `WEB-INF` of a custom XSLT stylesheet for rendering error pages. Only used by `ErrorHandlerServlet` (the HTML path).

**Fallback chain:**
1. `WEB-INF/<configured-value>` — if set and the file exists and is readable
2. Built-in classpath template (`org/pageseeder/berlioz/xslt/failsafe-error-html.xsl`)
3. Raw XML — if neither template loads (content type becomes `application/xml`)

| Development | Production |
|---|---|
| `""` (use built-in) | `xslt/error.xsl` (branded error page) or `""` |

---

## 3. Error Paths

### 3.1 Generator-Level Errors (Inside the Pipeline)

When a generator throws an exception, the servlet layer catches it and converts it to a `ProblemDetails` object (when `ERROR_PROBLEM_FORMAT=true`, the default) or a legacy `<berlioz-exception>` element (when explicitly set to `false`). The XSLT pipeline still runs.

**Exception → Problem mapping** (with `ERROR_PROBLEM_FORMAT=true`):

| Exception | HTTP Code | Problem type URN |
|---|---|---|
| `InvalidParameterException` | 400 | `urn:berlioz:problem:invalid-parameter` |
| `UpstreamException` | 502 | `urn:berlioz:problem:upstream-error` |
| Any other `HttpException` subclass, or one built via `HttpException.of(problem)` | Code from `getHttpCode()` / the carried `ProblemDetails` | `urn:berlioz:problem:http-signal` (default) or a custom type if `toProblem()` is overridden |
| Any other `Exception` | 500 | `urn:berlioz:problem:generator-error` |

For **non-direct services**, the problem is serialised as an inline `<problem>` element inside the `<content>` wrapper element — the full XML document including other generators is still passed to XSLT.

For **direct services** (single generator, no envelope), the problem becomes the entire response document. Content type is `application/problem+xml`.

If `ERROR_GENERATOR_CATCH=false` and the exception is not an `HttpException`, the exception is re-thrown and the framework calls `sendError()` instead.

Response headers set on an `HttpException` via `.header(name, value)` (e.g. `Retry-After`) are carried through onto the actual HTTP response — see [§4](#4-generator-level-exception-api).

### 3.2 Framework-Level Errors (Outside the Pipeline)

These include: service not found (404), method not allowed (405), XSLT transformation failure (500), service configuration errors (503), and lifecycle errors. All go through `BerliozServlet.sendError()`.

**Decision tree inside `sendError()`:**

```
sendError() called
├── Already in error dispatch? (ERROR_STATUS_CODE attribute set)
│   └── Always: set attributes → dispatch to ErrorHandlerServlet
├── ERROR_HANDLER = false?
│   └── res.sendError(code, message)  →  web.xml error-page handles it
├── Servlet media type is JSON?
│   └── Write application/problem+json directly  (no XSLT)
└── Default (HTML/XML servlets):
    ├── Set ERROR_* request attributes, including BERLIOZ_ERROR_MEDIA_TYPE
    │   (the matched service's configured content type)
    └── Dispatch to ErrorHandlerServlet
        ├── resolveFormat(): JSON / HTML / XML, preferring BERLIOZ_ERROR_MEDIA_TYPE
        │   over the URL extension (see below)
        ├── resolveErrorStylesheet() for the HTML path
        │   ├── berlioz.errors.stylesheet configured & file exists → custom XSL
        │   └── fallback → built-in failsafe-error-html.xsl
        └── If ERROR_PROBLEM_FORMAT=true → toProblemXml()   (<problem>)
            If ERROR_PROBLEM_FORMAT=false → toLegacyXml()  (<error>)
```

**Response format resolution** (`ErrorHandlerServlet.resolveFormat()`): the expected format is resolved from the `BERLIOZ_ERROR_MEDIA_TYPE` request attribute that `BerliozServlet` records from the matched servlet mapping's configured media type, before dispatching the error — this is more reliable than guessing from the URL extension, which breaks down for non-standard URL patterns. Only when that attribute is absent — errors that reach `ErrorHandlerServlet` directly via the container's `<error-page>` mechanism with no `BerliozServlet` involved, e.g. unmapped paths or static-resource errors — does it fall back to extension inference (`.json` → JSON, `.html` or empty → HTML, otherwise raw XML).

### 3.3 XSLT Failure Path

When XSLT transformation fails, `BerliozServlet` routes a 500 error with a `BerliozErrorID` such as `berlioz-transform-not-found` or `berlioz-transform-dynamic-error`. The `ErrorHandlerServlet` picks up that ID and includes it in the error document type URI as `urn:berlioz:problem:<id-without-prefix>`.

If the failure happens *while already rendering an error page* (a second XSLT failure during error rendering), Berlioz does not retry indefinitely: it gives the application's error handling one attempt, then falls back to `ErrorHandlerServlet.handleTerminal()`, a minimal renderer that uses only the built-in failsafe template and module-owned resources, bypassing any custom stylesheet.

---

## 4. Generator-Level Exception API

### Built-in exceptions (`@Beta`, `org.pageseeder.berlioz.error`)

**`InvalidParameterException`** — 400 Bad Request. Use the factory methods:

```java
// Required parameter absent
throw InvalidParameterException.required("q");

// Cannot parse value
throw InvalidParameterException.invalidFormat("page", raw, "integer");

// Cannot parse value, preserving the original parse exception as the cause
throw InvalidParameterException.invalidFormat("page", raw, "integer", parseEx);

// Value outside allowed range
throw InvalidParameterException.outOfRange("count", raw, "must be >= 1");

// Value not in allowed set
throw InvalidParameterException.notAllowed("format", raw, "json", "xml");

// Custom constraint
throw InvalidParameterException.constraintFailed("id", raw, "must be even");
```

**`UpstreamException`** — 502 Bad Gateway:

```java
throw new UpstreamException("Search index unavailable", "search-api");
throw new UpstreamException("Timed out connecting to CMS", cause);
```

### `HttpException` — two ways to define custom signals

`HttpException` is the base class for generator-level HTTP short-circuiting. It suppresses stack trace capture (it's a flow-control signal, not a bug) and always produces an RFC 9457 problem via `toProblem()`.

**Registry pattern (recommended)** — define `ProblemDetails` constants once, throw them via `HttpException.of(...)`. No subclassing needed:

```java
// One-time definition, e.g. in a registry class
static final ProblemDetails QUOTA_EXCEEDED = ProblemDetails.of(429)
    .type("urn:myapp:problem:quota-exceeded")
    .title("Rate Limit Exceeded");

// In a generator
throw HttpException.of(QUOTA_EXCEEDED.detail("Daily limit reached for user " + userId));

// With a cause
throw HttpException.of(QUOTA_EXCEEDED, cause);
```

**Subclassing (escape hatch)** — when the problem type and title are fixed for a given exception class, override `toProblem()`:

```java
public class LegalHoldException extends HttpException {
  public LegalHoldException(String reason) { super(reason, 451); }

  @Override
  public ProblemDetails toProblem() {
    return ProblemDetails.of(451)
        .type("urn:myapp:problem:legal-hold")
        .title("Unavailable For Legal Reasons")
        .detail(getMessage());
  }
}
```

Do not add an `exception` extension inside a custom `toProblem()` — the framework appends diagnostic detail (class, message, stack trace) automatically according to `berlioz.errors.detail`.

### Setting response headers on an error

`HttpException.header(name, value)` (and the equivalent `Response.header(name, value)` for non-exception paths) attaches transport-level headers that the framework carries onto the actual HTTP response — useful for things like `Retry-After` on a 429/503:

```java
throw HttpException.of(QUOTA_EXCEEDED.detail("Try again later"))
    .header("Retry-After", "60");
```

`ProblemDetails` itself stays body-only per RFC 9457; headers are always set through `.header(...)`, never as problem fields. Only set headers a generator genuinely owns — framework-managed headers (`Content-Type`, `Content-Length`, `ETag`, `Cache-Control`, `Date`, `Set-Cookie`) are rejected or overwritten by the servlet layer.

### Using `ProblemDetails` directly (generator return value)

Generators using the new `Generator` / `XmlGenerator` API can return `Response.problem(...)` instead of throwing:

```java
ProblemDetails problem = ProblemDetails.of(ContentStatus.NOT_FOUND)
    .type("urn:myapp:problem:document-not-found")
    .title("Document Not Found")
    .detail("No document with id '" + id + "' exists")
    .instance("/documents/" + id);
return Response.problem(problem);
```

Use `Problems` factory methods to get framework-standard problem types:

```java
// Inside a catch block
} catch (InvalidParameterException ex) {
  return Response.problem(Problems.forInvalidParameter(ex));
}
```

### Extension vs. diagnostic members

`ProblemDetails` distinguishes two kinds of extra data:

- **Extension members** (`.extension(name, value)` or `.extension(ProblemExtension)`) — always serialized, part of the problem's public contract (e.g. `parameter`/`reason` on `InvalidParameterException`).
- **Diagnostic members** (`.diagnostic(name, value)`, `.diagnostic(Throwable)`) — invisible by default. They are only folded into the response (via `forDetailLevel(level)`, applied automatically by the framework render layer) when `berlioz.errors.detail` is `standard` or `full`. This is the same mechanism the framework uses internally to attach exception detail, and it's the safe way for a generator to attach developer-only context (internal IDs, raw upstream payloads) without leaking it in production:

```java
return Response.problem(
    ProblemDetails.of(502)
        .type("urn:myapp:problem:upstream-timeout")
        .title("Upstream Timeout")
        .diagnostic("upstream-url", url)
        .diagnostic(cause));
```

---

## 5. `BerliozErrorID` Reference

The framework attaches one of these IDs to every error it generates. They appear as the `id` attribute on legacy error elements and as the problem type URI suffix in problem format.

| ID | Situation |
|---|---|
| `berlioz-unexpected` | Catch-all for unidentified errors |
| `berlioz-services-not-found` | `services.xml` file missing |
| `berlioz-services-malformed` | `services.xml` not well-formed |
| `berlioz-services-invalid` | `services.xml` fails validation |
| `berlioz-transform-not-found` | XSLT file does not exist |
| `berlioz-transform-invalid` | XSLT file has compile errors |
| `berlioz-transform-malformed-source-xml` | Generator produced malformed XML |
| `berlioz-transform-dynamic-error` | XSLT runtime error |
| `berlioz-generator-error-unchecked` | Generator threw unchecked exception |
| `berlioz-generator-error-unforced` | Generator threw `BerliozException` |
| `berlioz-generator-error-multiple` | More than one generator in the same service threw an error (`CompoundBerliozException`) |
| `berlioz-invalid-parameter` | `InvalidParameterException` |
| `berlioz-upstream-error` | `UpstreamException` |
| `berlioz-http-signal` | Custom `HttpException` subclass or registry-based `HttpException.of(...)` |

---

## 6. Recommended Settings

### Development

```xml
<!-- config-local.xml -->
<global>
  <berlioz>
    <errors handle="true"
            generator-catch="false"
            detail="full" />
  </berlioz>
</global>
```

- `generator-catch=false` — let exceptions surface immediately with full detail rather than being swallowed into a problem response
- `detail=full` — stack traces visible in the browser
- `problem` is left unset — `true` (RFC 9457) is now the default

### Production

```xml
<global>
  <berlioz>
    <errors handle="false"
            generator-catch="true"
            detail="minimal"
            stylesheet="xslt/error.xsl"/>
  </berlioz>
</global>
```

- `handle=false` — let the container dispatch to branded `web.xml` error pages
- `generator-catch=true` — degrade gracefully; a single generator failure does not kill the page
- `detail=minimal` — this is also the built-in default; set explicitly for clarity — no internal info leaked to clients
- `stylesheet` — custom branded error page when Berlioz does handle an error itself

> If you do not have custom `web.xml` error pages yet, temporarily set `handle=true` in production until they are in place.

> **Migrating from pre-0.14 XSLT templates?** Only set `problem="false"` temporarily if your error stylesheet still expects the old `<server-error>`/`<client-error>` shape (now unified `<error>` — see [§7](#7-error-format-reference)). Plan to update the stylesheet and drop the flag before 1.0, when it is removed.

---

## 7. Error Format Reference

### Legacy format (`ERROR_PROBLEM_FORMAT=false`) — deprecated, removed in 1.0

Since 0.14.0 there is a single `<error>` root element (the old `<server-error>`/`<client-error>` split is gone); the HTTP class is carried in the `http-class` attribute instead of the element name:

```xml
<?xml version="1.0"?>
<error http-class="server-error" http-code="500" datetime="2026-06-29T..." id="berlioz-transform-dynamic-error">
  <berlioz version="0.14.1"/>
  <title>Internal Server Error</title>
  <message>XSLT dynamic error ...</message>
  <request-uri>/home.html</request-uri>
  <servlet>BerliozServlet</servlet>
  <!-- detail=standard adds a summary: -->
  <exception class="javax.xml.transform.TransformerException">
    <message>...</message>
  </exception>
  <!-- detail=full instead adds the full chain, plus http-headers and http-parameters: -->
  <exception class="javax.xml.transform.TransformerException">
    <message>...</message>
    <stack-trace>...</stack-trace>
    <cause>...</cause>
  </exception>
  <http-headers>
    <header name="..." value="..."/>
  </http-headers>
  <http-parameters>
    <parameter name="..." value="..."/>
  </http-parameters>
</error>
```

### Problem format (`ERROR_PROBLEM_FORMAT=true`, default)

```xml
<?xml version="1.0"?>
<problem>
  <type>urn:berlioz:problem:transform-dynamic-error</type>
  <status>500</status>
  <title>Internal Server Error</title>
  <detail>XSLT dynamic error ...</detail>
  <!-- detail=standard or full adds: -->
  <exception class="javax.xml.transform.TransformerException" type="TransformerException">
    <message>...</message>
    <location line="42" column="7" system-id="/xslt/html/main.xsl"/>
    <!-- detail=full additionally adds: stack-trace, cause -->
  </exception>
</problem>
```

### Response headers on error responses

All framework-generated error responses are sent with `Cache-Control: no-store` (errors are never cached), plus any headers carried by the triggering `HttpException` (e.g. `Retry-After`) applied first, so framework-owned headers win on a name collision.

---

## 8. `web.xml` Integration

To use `ERROR_HANDLER=false` with container error pages, map `ErrorHandlerServlet` and add error-page entries:

```xml
<!-- Register the error handler (no URL mapping needed) -->
<servlet>
  <servlet-name>ErrorHandlerServlet</servlet-name>
  <servlet-class>org.pageseeder.berlioz.servlet.ErrorHandlerServlet</servlet-class>
  <load-on-startup>2</load-on-startup>
  <init-param>
    <!-- Extensions that keep their original extension when forwarded -->
    <param-name>forward-extensions</param-name>
    <param-value>.html,.xml</param-value>
  </init-param>
  <init-param>
    <!-- Extensions that get a 0-byte response (no error page needed) -->
    <param-name>ignore-extensions</param-name>
    <param-value>.jpg,.png,.css,.js,.ico</param-value>
  </init-param>
</servlet>

<!-- Route errors to the handler (optional — only when ERROR_HANDLER=true) -->
<error-page>
  <error-code>404</error-code>
  <location>/error.auto</location>
</error-page>
<error-page>
  <error-code>500</error-code>
  <location>/error.auto</location>
</error-page>
```

The `.auto` extension is a special Berlioz convention: `ErrorHandlerServlet` replaces it with the extension of the original request (`.html` → `/error.html`, `.xml` → `/error.xml`), so one error service can serve all formats.

> When an error reaches `ErrorHandlerServlet` this way — directly via the container, with no `BerliozServlet` dispatch involved — there is no `BERLIOZ_ERROR_MEDIA_TYPE` attribute to resolve, so the response format falls back to inferring from the URL extension (see [§3.2](#32-framework-level-errors-outside-the-pipeline)).
