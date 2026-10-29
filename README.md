[![Maven Central](https://img.shields.io/maven-central/v/org.pageseeder.berlioz/pso-berlioz.svg?label=Maven%20Central&color=blue)](https://search.maven.org/search?q=g:%22org.pageseeder.berlioz%22%20AND%20a:%22pso-berlioz%22)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=pageseeder_berlioz&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=pageseeder_berlioz)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=pageseeder_berlioz&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=pageseeder_berlioz)
[![javadoc](https://javadoc.io/badge2/org.pageseeder.berlioz/pso-berlioz/javadoc.svg)](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz)


# Berlioz
A light Web framework built on URI templates and XSLT

A simple, Java-based web framework that uses XSLT for rendering and a URI template notation to allocate services. Implemented as a set of servlets, Berlioz is both powerful and simple. It is also easy to deploy and extend.

## What is Berlioz?

The founding principle of Berlioz is [separation of concerns](http://en.wikipedia.org/wiki/Separation_of_concerns). The design objectives are:
  * implement the business logic in Java
  * generate the XML necessary for the site
  * use XSLT to generate specific views of the data

By using the URI template notation as URI patterns, Berlioz provides more sophisticated URL matching than what is supported by Java servlets.

For example, Berlioz fire a service for any URL that match patterns like `/{user}/message/{id}`, `/wiki/{term}` or `/documents{;type,size}/title`; pass each URI parameter to the underlying Berlioz service.

A Berlioz service is simply a list of Java classes that generate XML. Think of it as a service model, not a formal **Web Service**. Once the XML data has been created by the servlet, it is processed by XSLT into the destination format. While this is typically HTML, it can be any format that can be supported by XSLT (RSS, iCAl, .docx etc etc etc).

## Why Berlioz?

With so many web frameworks around, this is a very reasonable question. The answer is that there are surprisingly few options based on Java and XSLT and even fewer that are lightweight and robust. We have used Berlioz for a lot of implementations over the past decade and feel that it is a really good toolset for people with Java and XSLT skills. We posted it on Google Code as motivation to create the best quality of code and documentation possible. Right or wrong, our experience is that strictly internal projects don't get the same level of attention to detail as public projects.

If you want a sense of what Berlioz is all about, think modest features, highly extensible, quality code, simplicity and robustness. Also, Berlioz is production ready. It was designed for professional publishing applications and that is the environment it has evolved in.

Let us know if there is anything that we can do to help you use Berlioz. We want to create something that developers find useful, so feedback is welcome.

## Getting started

This section builds a one-page Berlioz application. The complete, runnable version is in
[`samples/minimal-gretty`](samples/minimal-gretty): run `./gradlew :samples:minimal-gretty:appRun`
and open http://localhost:8999/.

### Requirements

- Java 11 or later.
- A `javax.servlet` container (Servlet 4.0 API), such as Tomcat 9 or Jetty 10. Jakarta Servlet
  containers are not supported yet (see the [roadmap](ROADMAP.md)).
- An XSLT 2.0 processor, such as Saxon-HE.
- An SLF4J binding for logging.

JSON output works without extra dependencies; Berlioz uses Jackson, Gson or JSON-P instead when
one is on the classpath.

### 1. Add the dependencies

`pso-berlioz-kickstart` brings in the core library and a web fragment that maps `*.html`,
`*.xml`, `*.json` and `*.src` to Berlioz, so no `web.xml` configuration is needed.

```kotlin
// build.gradle.kts
dependencies {
  implementation("org.pageseeder.berlioz:pso-berlioz-kickstart:0.14.4")
  runtimeOnly("net.sf.saxon:Saxon-HE:12.10")
  runtimeOnly("org.slf4j:slf4j-simple:2.0.20")
}
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.pageseeder.berlioz</groupId>
  <artifactId>pso-berlioz-kickstart</artifactId>
  <version>0.14.4</version>
</dependency>
```

### 2. Map a URL to a generator

Services are declared in `WEB-INF/config/services.xml`. Each service matches a URI template and
lists the generators that produce its content:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE service-config PUBLIC "-//Berlioz//DTD::Services 1.0//EN"
    "https://pageseeder.org/schema/berlioz/services-1.0.dtd">
<service-config version="1.0">
  <services group="default">
    <namespace package="com.example"/>
    <service id="hello" method="get">
      <url pattern="/hello"/>
      <generator class="HelloGenerator"/>
    </service>
  </services>
</service-config>
```

The pattern can contain variables, such as `/users/{user}/messages/{id}`, which generators read
like request parameters.

### 3. Write the generator

A generator reads the request and writes data. Writing through `OutputWriter` lets the same
generator produce XML or JSON:

```java
package com.example;

import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

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

To report an error, throw an `HttpException` such as `InvalidParameterException`, or return
`Response.problem(…)`: Berlioz turns it into an RFC 9457 Problem Details response.

### 4. Write the stylesheet

Berlioz wraps the output of the service's generators in a `<root>` document and transforms it
with the stylesheet of the service's group: `WEB-INF/xslt/html/default.xsl` for `*.html` requests
to the `default` group.

```xml
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html" encoding="UTF-8"/>

  <xsl:template match="/root[@service = 'hello']">
    <html>
      <body>
        <h1><xsl:value-of select="content/hello/message"/></h1>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
```

### 5. Configure local development

Global settings live in `WEB-INF/config/config.xml`. Starting the container with
`-Dberlioz.mode=local` also loads `config-local.xml`, whose values override `config.xml`. It is
the usual place for development settings:

```xml
<!-- WEB-INF/config/config-local.xml -->
<global>
  <berlioz>
    <!-- Show full error details instead of the production default ('minimal') -->
    <errors detail="full"/>
    <!-- Recompile stylesheets when they change -->
    <xslt cache="auto"/>
    <!-- Allow control parameters and *.src from this machine only -->
    <control network="loopback"/>
  </berlioz>
</global>
```

The defaults are meant for production; see the
[`BerliozOption` Javadoc](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz) for every
option.

### 6. Run it

The application now has this layout:

```
src/main/java/com/example/HelloGenerator.java
src/main/webapp/WEB-INF/config/services.xml
src/main/webapp/WEB-INF/config/config.xml
src/main/webapp/WEB-INF/config/config-local.xml
src/main/webapp/WEB-INF/xslt/html/default.xsl
```

Deploy it and open `/hello.html?name=Berlioz`. Open `/hello.src` to see the XML that Berlioz
passes to the stylesheet, before any transformation.

### Next steps

- [`samples/minimal-gretty`](samples/minimal-gretty) also shows JSON API services, which use
  `<handler>` to return a generator's output directly without XSLT, and error handling.
- The [documentation](docs/README.md) covers each part of Berlioz in more detail.

## Documentation

- [Guides and reference](docs/README.md): configuration, services, generators, XSLT, error
  handling, caching, overlays, administration and logging.
- [Javadoc](https://javadoc.io/doc/org.pageseeder.berlioz/pso-berlioz): the Java API, including
  every configuration option in `BerliozOption`.

## Upgrading

See the [upgrade guide](docs/upgrading.md) for the changes an application may need when upgrading,
and the [changelog](CHANGELOG.md) for everything that changed in each release.

## Roadmap

The current development roadmap is available in [ROADMAP.md](ROADMAP.md).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for building and testing Berlioz.
