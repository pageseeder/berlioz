# HTTP Caching

Whenever it can, Berlioz generates [entity tags](https://www.rfc-editor.org/rfc/rfc9110#name-etag) (ETags) so that clients and proxies can cache responses and Berlioz can answer `304 Not Modified` without running the generators.

## Is a service cacheable?

A service is cacheable when every generator in it implements `Cacheable` and returns an ETag:

```java
public final class GetArticle implements Generator, Cacheable {

  @Override
  public @Nullable String getETag(Request req) {
    // For example, the last modified date of the source document
    return Long.toString(lastModified(req.getParameter("id", "")));
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    // ...
  }
}
```

The ETag does not need to be specific: the last modified date of a file, database or index is fine, as long as it changes whenever the output would. It must be fast to compute, because it runs on every request; if it isn't comfortably under 10 ms, there may be no point in computing one. The `berlioz-profile` control parameter reports the time taken (see [administration](administration.md)).

Returning `null` makes the response non-cacheable for that request, which is useful when a generator can only sometimes produce a cacheable response. If `getETag` throws, Berlioz logs the failure and treats the response as non-cacheable. Checks that must run on every request, such as authorization, must not rely on content generation, since a matching ETag skips it.

Legacy `ContentGenerator` implementations override `getETag(ContentRequest)` instead, which is deprecated.

Only `GET` and `HEAD` requests use ETags. `QUERY` responses are never cached, since the ETag does not take the request body into account.

## How Berlioz computes the ETag

For a cacheable service, Berlioz combines:

- the ETag seed created at startup;
- the ETag of the XSLT templates, when the response is transformed;
- the ETag of each generator.

So the ETag changes when the application restarts, when a stylesheet changes, or when any generator returns a different ETag. It does not include the service definitions: after changing services, reload with `berlioz-reload=services,etags` so that clients revalidate ([administration](administration.md)).

## How Berlioz sets `Cache-Control`

For a cacheable response, Berlioz sends `ETag`, `Expires` and `Cache-Control`. The `Cache-Control` value is the first of:

1. The `cache-control` attribute of the service in `services.xml`.
2. The `cache-control` init parameter of the Berlioz servlet.
3. The global `berlioz.http.cache-control` setting.
4. `no-cache`.

A response that is not cacheable is sent with `Cache-Control: no-cache`, and error responses with `no-store`.

### Service

```xml
<service id="article" method="get" cache-control="public, max-age=300">
  ...
</service>
```

### Servlet

```xml
<servlet>
  <servlet-name>BerliozHTML</servlet-name>
  <servlet-class>org.pageseeder.berlioz.servlet.BerliozServlet</servlet-class>
  <init-param>
    <param-name>cache-control</param-name>
    <param-value>private, max-age=0</param-value>
  </init-param>
</servlet>
```

### Global configuration

The global configuration is usually the best place for the default value:

```xml
<global>
  <berlioz>
    <http cache-control="private, max-age=0"/>
  </berlioz>
</global>
```

In development, `max-age=0` or the default `no-cache` ensures that browsers always revalidate pages.
