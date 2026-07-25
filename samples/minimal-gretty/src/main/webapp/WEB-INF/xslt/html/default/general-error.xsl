<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="#all">

<xsl:template match="root[@service = 'general-error']" mode="title">Minimal Berlioz Sample &#8212; Error test</xsl:template>

<xsl:template match="root[@service = 'general-error']" mode="body">
  <h1>Error test</h1>
  <xsl:choose>
    <xsl:when test="content/problem">
      <p>
        <code>ErrorGenerator</code> returned an RFC&#160;9457 problem, carried through the normal
        XML/XSLT pipeline instead of short-circuiting to the container error page:
      </p>
      <ul>
        <li>type: <code><xsl:value-of select="content/problem/type"/></code></li>
        <li>status: <code><xsl:value-of select="content/problem/status"/></code></li>
        <li>title: <code><xsl:value-of select="content/problem/title"/></code></li>
      </ul>
    </xsl:when>
    <xsl:otherwise>
      <p>
        <code>ErrorGenerator</code> ran without any query parameter, so it returned ordinary content
        instead of failing:
      </p>
      <p><xsl:value-of select="content/error/@message"/></p>
    </xsl:otherwise>
  </xsl:choose>
  <p>Ask it to fail instead, with a query parameter:</p>
  <ul>
    <li><a href="/test/error.html?problem=true">?problem=true</a> &#8212; RFC&#160;9457 problem, rendered by this same page</li>
    <li><a href="/test/error.html?throw=true">?throw=true</a> &#8212; uncaught exception, handled by the container error page instead</li>
    <li><a href="/test/error.html?http=451">?http=451</a> &#8212; a chosen HTTP status, also handled by the container error page</li>
  </ul>
  <p>
    &#8592; Back to the <a href="/">welcome page</a>.
  </p>
</xsl:template>

</xsl:stylesheet>
