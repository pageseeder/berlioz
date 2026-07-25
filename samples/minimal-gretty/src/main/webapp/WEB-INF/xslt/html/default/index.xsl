<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="#all">

<xsl:template match="root[@service = 'index']" mode="title">Minimal Berlioz Sample</xsl:template>

<xsl:template match="root[@service = 'index']" mode="body">
  <header>
    <img src="/favicon.svg" alt=""/>
    <h1>Minimal Berlioz Sample</h1>
  </header>
  <p>
    A small WAR application demonstrating the minimum pieces of a Berlioz site: a
    <code>services.xml</code> mapping URLs to generators, one XSLT stylesheet per output format, and
    a couple of generator styles (modern <code>Generator</code>/<code>OutputWriter</code> and the
    legacy <code>ContentGenerator</code>/<code>XMLWriter</code> API).
  </p>

  <h2>Hello service</h2>
  <p>The same <code>HelloGenerator</code> output, rendered through each format servlet:</p>
  <ul>
    <li><a href="/hello.html">/hello.html</a> &#8212; wrapped in XML, transformed to HTML</li>
    <li><a href="/hello.xml">/hello.xml</a> &#8212; wrapped in XML, transformed by <code>xslt/xml/default.xsl</code></li>
    <li><a href="/hello.src">/hello.src</a> &#8212; the raw, untransformed source XML</li>
    <li><a href="/hello.json">/hello.json</a> &#8212; transformed by <code>xslt/json/default.xsl</code></li>
    <li><a href="/hello.html?name=World">/hello.html?name=World</a> &#8212; generator parameter</li>
  </ul>

  <h2>Direct JSON API</h2>
  <p>
    <code>GetNote</code> and <code>UpdateNote</code> are registered with <code>&lt;handler&gt;</code>,
    so they write the response directly instead of going through the XML/XSLT pipeline:
  </p>
  <ul>
    <li><a href="/api/note.json">/api/note.json</a> &#8212; read the current note</li>
  </ul>
  <pre>curl -X POST 'http://localhost:8999/api/note.json?text=Hello%20Berlioz'
curl http://localhost:8999/api/note.json</pre>

  <h2>Error handling</h2>
  <p><code>ErrorGenerator</code> triggers failures on demand, to exercise the Problem Details pipeline:</p>
  <ul>
    <li><a href="/test/error.html">/test/error.html</a> &#8212; no error requested</li>
    <li><a href="/test/error.html?problem=true">/test/error.html?problem=true</a> &#8212; RFC&#160;9457 problem response</li>
    <li><a href="/test/error.html?throw=true">/test/error.html?throw=true</a> &#8212; uncaught exception</li>
    <li><a href="/test/error.html?http=451">/test/error.html?http=451</a> &#8212; a chosen HTTP status</li>
    <li><a href="/api/error.json?problem=true">/api/error.json?problem=true</a> &#8212; same problem, as a direct JSON handler</li>
  </ul>

  <p>Run over HTTPS on <a href="https://localhost:8444/hello.html">https://localhost:8444/hello.html</a>.</p>
</xsl:template>

</xsl:stylesheet>
