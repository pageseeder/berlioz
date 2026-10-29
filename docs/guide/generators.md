# Generators

A generator reads the request and writes data. Berlioz combines the output of a service's generators and transforms it with XSLT, or sends a handler's output directly ([services](services.md)).

## Generator types

| Interface | Writes with | Output |
|---|---|---|
| `Generator` | `OutputWriter` | XML or JSON, depending on the request |
| `XmlGenerator` | `XmlWriter` | XML only |
| `JsonGenerator` | `JsonWriter` | JSON only |
| `ContentGenerator` | `XMLWriter` (`pso-xmlwriter`) | XML only; the original API, still supported |

All are in `org.pageseeder.berlioz.content`. Prefer `Generator` for new code: the same class can serve an HTML page through XSLT and a JSON API.

```java
public final class HelloGenerator implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    String name = req.getParameter("name", "world");
    out.startObject("hello")
      .field("name", name)
      .field("message", "Hello " + name, OutputWriter.FieldOption.XML_ELEMENT)
      .endObject();
    return Response.ok();
  }
}
```

In XML, fields become attributes unless they use `FieldOption.XML_ELEMENT`; the output above is:

```xml
<hello name="world"><message>Hello world</message></hello>
```

A generator returns a `Response`: `Response.ok()`, a status, a redirect, headers, or a problem.

A service whose generators cannot produce the requested format (for example a JSON request to a service with an `XmlGenerator`) is transformed with the group's XSLT instead.

## Reading parameters

`Request` gives access to HTTP parameters and to the variables of the matched URI pattern, by name. Typed parameters validate and convert values, and throw an `InvalidParameterException` (`400 Bad Request`) when a value is missing or invalid:

```java
int page       = req.parameter("page").asInt().clamp(1, 10000).defaultValue(1);
LocalDate from = req.parameter("from").asLocalDate().optional();
String sort    = req.parameter("sort").oneOf("name", "date", "title").required();
```

`ParameterSpec<T>` defines a parameter once so that several generators can reuse it.

## Errors

Throw an `HttpException`, such as `InvalidParameterException` or `UpstreamException`, or return `Response.problem(…)`. Berlioz turns it into an RFC 9457 Problem Details response; see [error handling](error-handling.md).

## Caching

A generator that implements `Cacheable` returns an ETag, which lets Berlioz answer `304 Not Modified`; see [HTTP caching](caching.md).

## Logging

Generators log through SLF4J; see [logging](logging.md).

## Legacy `ContentGenerator`

Existing applications implement `ContentGenerator`:

```java
public final class HelloWorld implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    String hello = req.getParameter("message", "Hello World");
    xml.openElement("message");
    xml.writeText(hello);
    xml.closeElement();
  }
}
```

`ContentGenerator` remains supported. Converting to `Generator` adds JSON output and the `Response` model.
