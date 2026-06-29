<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fail-safe stylesheet to display transform errors

  @author Christophe Lauret
  @version 0.13.5
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" encoding="utf-8" indent="yes" media-type="text/html" />

<!-- The ID of the error -->
<xsl:variable name="id" select="/*/@id"/>

<!-- Main template called in all cases. -->
<xsl:template match="/">
<!-- Display the HTML Doctype -->
<xsl:text disable-output-escaping="yes"><![CDATA[<!doctype html>
]]></xsl:text>
<html>
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title><xsl:value-of select="*/title"/></title>
  <style>
    /* ── Custom properties ───────────────────────────────────────── */
    :root {
      --accent:  #1a6fa8;
      --c-5xx:   #c01030;
      --c-4xx:   #c85000;
      --c-3xx:   #1a5f8e;
      --c-2xx:   #1a7a40;
      --c-1xx:   #555555;
      --bg:      #f8fafc;
      --surface: #ffffff;
      --text:    #1e293b;
      --muted:   #64748b;
      --border:  #e2e8f0;
      --shadow:  rgba(0,0,0,.08);
      --sans:    system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
      --mono:    ui-monospace, 'Cascadia Code', 'Source Code Pro', Menlo, Consolas, monospace;
    }

    @media (prefers-color-scheme: dark) {
      :root {
        --accent:  #60a5d4;
        --bg:      #0f172a;
        --surface: #1e293b;
        --text:    #e2e8f0;
        --muted:   #94a3b8;
        --border:  #334155;
        --shadow:  rgba(0,0,0,.4);
      }
    }

    /* ── Base ────────────────────────────────────────────────────── */
    *, *::before, *::after { box-sizing: border-box; }

    body {
      font-family: var(--sans);
      background: var(--bg);
      color: var(--text);
      border-top: 6px solid var(--accent);
      padding: 2rem 1rem;
      margin: 0;
    }

    /* Status colour on the top bar, driven by the body class */
    .server-error { border-top-color: var(--c-5xx); }
    .client-error { border-top-color: var(--c-4xx); }
    .redirection  { border-top-color: var(--c-3xx); }
    .successful   { border-top-color: var(--c-2xx); }
    .continue     { border-top-color: var(--c-1xx); }

    h1 { margin: 0 0 1rem; font-weight: 300; font-size: 2rem; }
    h2 { border-bottom: 2px solid var(--accent); color: var(--accent); font-size: 1.25rem; margin: 1.5rem 0 .5rem; }
    h3 { border-bottom: 1px solid var(--border); color: var(--accent); font-size: 1.1rem; margin: 1rem 0 .4rem; }
    h4 { font-size: 1rem; margin: 1rem 0 .25rem; }

    code, pre { font-family: var(--mono); font-size: .82em; }
    pre        { line-height: 1.5; color: var(--muted); overflow-x: auto; }

    /* ── Container ───────────────────────────────────────────────── */
    .container {
      max-width: 960px;
      margin: 0 auto;
      background: var(--surface);
      padding: 1.5rem 2rem;
      box-shadow: 0 2px 12px var(--shadow);
      border: 1px solid var(--border);
      border-radius: 6px;
    }

    .message { font-weight: 600; margin: .5rem 0; }

    /* ── Footer ──────────────────────────────────────────────────── */
    .footer {
      display: flex;
      justify-content: space-between;
      border-top: 1px solid var(--border);
      margin-top: 1.5rem;
      padding-top: .5rem;
      font-size: .8rem;
      color: var(--muted);
    }

    /* ── Location ────────────────────────────────────────────────── */
    .location { font-family: var(--mono); font-size: .8em; color: var(--muted); }

    /* ── Help block ──────────────────────────────────────────────── */
    .help {
      border: 1px solid #fef08a;
      background: #fefce8;
      border-radius: 4px;
      padding: .5rem .75rem;
      font-style: italic;
      margin: .75rem 0;
    }
    @media (prefers-color-scheme: dark) {
      .help { border-color: #713f12; background: #1c1108; }
    }
    .help p { margin: .25rem 0; }

    /* ── Collected-errors list ───────────────────────────────────── */
    ul.collected { list-style: none; padding: 0; margin: .5rem 0; }

    ul.collected li {
      display: flex;
      align-items: baseline;
      gap: .5rem;
      font-family: var(--mono);
      font-size: .8rem;
      margin-bottom: .25rem;
    }

    .line  { color: var(--muted); flex: 0 0 5rem; }
    .col   { color: var(--muted); flex: 0 0 6rem; }
    .level { color: var(--muted); flex: 0 0 5.5rem; text-align: center; font-weight: bold; border-radius: 4px; padding: 1px 4px; }

    .warning > .level { color: #d97706; }
    .error   > .level { color: #dc2626; }
    .fatal   > .level { color: #fff; background: var(--c-5xx); }

    /* ── Problem Details (RFC 9457) ──────────────────────────────── */
    .problem-type     { font-size: .8rem; color: var(--muted); margin: 0 0 .5rem; }
    .problem-instance { font-size: .8rem; color: var(--muted); margin: .25rem 0; }

    details.extensions {
      margin-top: 1rem;
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: .25rem .5rem;
    }
    details.extensions summary { cursor: pointer; color: var(--accent); font-size: .9rem; }
    details.extensions dl      { margin: .5rem 0 .25rem; font-size: .85rem; }
    details.extensions dt      { font-weight: bold; color: var(--muted); margin-top: .4rem; }
    details.extensions dd      { margin-left: 1rem; }
  </style>
</head>
<body>
  <xsl:attribute name="class">
    <xsl:apply-templates select="*" mode="class" />
  </xsl:attribute>
  <xsl:apply-templates select="*"/>
</body>
</html>
</xsl:template>

<xsl:template match="*" mode="class">
  <xsl:value-of select="name(.)"/>
</xsl:template>

<!-- Default template for errors -->
<xsl:template match="continue|successful|redirection|client-error|server-error">
  <div class="container">
    <h1><xsl:value-of select="@http-code"/> - <xsl:value-of select="title"/></h1>
    <xsl:if test="not(message = exception/message)">
      <p class="message"><xsl:value-of select="message"/></p>
    </xsl:if>
    <xsl:apply-templates select="." mode="help"/>
    <xsl:apply-templates select="exception|error"/>
    <xsl:apply-templates select="collected-errors"/>
    <div class="footer">
      <span id="datetime"><xsl:value-of select="format-dateTime(@datetime, '[MNn] [D], [Y] at [H01]:[m01]:[s01] [z]')"/></span>
      <span id="berlioz-version">Berlioz <xsl:value-of select="berlioz/@version"/></span>
    </div>
    <div hidden="">
      <xsl:copy-of select="."/>
    </div>
  </div>
</xsl:template>

<!-- Other errors -->
<xsl:template match="error[@http-code=404]">
  <div class="container client-error">
    <h1><xsl:value-of select="message"/></h1>
    <p class="message">Sorry but I could not find anything at <code><xsl:value-of select="@request-uri"/></code></p>
    <xsl:copy-of select="."/>
  </div>
</xsl:template>

<!-- Common templates ======================================================================== -->

<!-- Exception -->
<xsl:template match="exception">
  <div class="exception">
    <h2><xsl:value-of select="message"/></h2>
    <xsl:if test="not(following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace"/>
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Java Error -->
<xsl:template match="error[@class]">
  <div class="error">
    <h2><xsl:value-of select="message"/></h2>
    <xsl:if test="not(following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace"/>
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Cause of an exception -->
<xsl:template match="cause">
  <div class="cause">
    <h4><em>Caused by: </em> <xsl:value-of select="message"/></h4>
    <xsl:if test="not(parent::exception/following-sibling::collected-errors)">
      <xsl:apply-templates select="location"/>
    </xsl:if>
    <xsl:apply-templates select="stack-trace|cause"/>
  </div>
</xsl:template>

<!-- Stack Trace -->
<xsl:template match="stack-trace">
  <pre class="stacktrace">
  <!-- No need to display the stack trace if we know the error -->
  <xsl:if test="not($id = 'berlioz-unexpected' or starts-with($id, 'berlioz-generator'))">
    <xsl:attribute name="hidden">hidden</xsl:attribute>
  </xsl:if>
  <xsl:value-of select="text()"/></pre>
</xsl:template>

<!-- Location -->
<xsl:template match="location">
  <p class="location">File: <xsl:value-of select="@system-id"/>, Line: <xsl:value-of select="@line"/>, Column: <xsl:value-of select="@column"/></p>
</xsl:template>

<!-- Collected errors -->
<xsl:template match="collected-errors">
<xsl:for-each-group select="collected" group-by="location/@system-id">
  <h4><xsl:value-of select="location/@system-id"/></h4>
  <ul class="collected">
    <xsl:for-each select="current-group()">
      <li class="{@level}">
        <span class="level">[<xsl:value-of select="@level"/>]</span>
        <span class="line">Line: <xsl:value-of select="location/@line"/></span>
        <span class="col">Column: <xsl:value-of select="location/@column"/></span>
        <span class="info">
          <xsl:value-of select="message"/>
          <xsl:if test="cause and not(message = cause/message)">: <xsl:value-of select="cause/message"/></xsl:if>
        </span>
      </li>
    </xsl:for-each>
  </ul>
</xsl:for-each-group>
<!-- If there are error without a location -->
<xsl:apply-templates select="collected[not(location)]"/>
</xsl:template>

<xsl:template match="collected[not(location)]">
  <div class="exception">
    <h3><xsl:value-of select="message"/></h3>
    <xsl:apply-templates select="stack-trace" />
    <xsl:apply-templates select="cause[not(message = current()/message)]"/>
  </div>
</xsl:template>

<!-- Problem Details (RFC 9457) =============================================================== -->

<xsl:template match="problem" mode="class">
  <xsl:variable name="s" select="number(status)"/>
  <xsl:choose>
    <xsl:when test="$s >= 500">server-error</xsl:when>
    <xsl:when test="$s >= 400">client-error</xsl:when>
    <xsl:when test="$s >= 300">redirection</xsl:when>
    <xsl:when test="$s >= 200">successful</xsl:when>
    <xsl:when test="$s >= 100">continue</xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="problem">
  <div class="container">
    <h1><xsl:value-of select="status"/> - <xsl:value-of select="if (title != '') then title else 'Error'"/></h1>
    <xsl:if test="detail != ''">
      <p class="message"><xsl:value-of select="detail"/></p>
    </xsl:if>
    <xsl:if test="type != ''">
      <p class="problem-type">Type: <code><xsl:value-of select="type"/></code></p>
    </xsl:if>
    <xsl:if test="instance != ''">
      <p class="problem-instance">Instance: <code><xsl:value-of select="instance"/></code></p>
    </xsl:if>
    <xsl:variable name="extensions" select="*[not(self::type|self::status|self::title|self::detail|self::instance)]"/>
    <xsl:if test="$extensions">
      <details class="extensions">
        <summary>Additional details</summary>
        <dl>
          <xsl:for-each select="$extensions">
            <dt><xsl:value-of select="local-name()"/></dt>
            <dd><code><xsl:value-of select="."/></code></dd>
          </xsl:for-each>
        </dl>
      </details>
    </xsl:if>
    <div class="footer"/>
  </div>
</xsl:template>


<!-- Help for Specified Error IDs ============================================================== -->

<!-- No help: ignore -->
<xsl:template match="*" mode="help" />

<!-- Help: Services configuration could not be found  -->
<xsl:template match="server-error[@id='berlioz-services-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>service configuration</b>.</p>
  <p>To fix this problem, creates a file called '<b>services.xml</b>' and put it in your <code>/WEB-INF/config/</code> folder.</p>
</div>
</xsl:template>

<!-- Help: Services configuration is not well formed  -->
<xsl:template match="server-error[@id='berlioz-services-malformed']" mode="help">
<div class="help">
  <p>Berlioz was unable to parse the <b>service configuration</b>.</p>
  <p>To fix this problem, you need to fix the XML errors in the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<!-- Help: Services configuration is invalid  -->
<xsl:template match="server-error[@id='berlioz-services-invalid']" mode="help">
<div class="help">
  <p>Berlioz was unable to load the service configuration because of the errors listed below.</p>
  <p>To fix this problem, you need to modify the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<!-- Help: Transform file could not be found -->
<xsl:template match="server-error[@id='berlioz-transform-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>XSLT style sheet</b>.</p>
  <p>To fix this problem, simply create the style file describe below in your <code>/WEB-INF/</code> folder.</p>
</div>
</xsl:template>

<!-- Help: Transform file could not be found -->
<xsl:template match="server-error[@id='berlioz-transform-malformed-source-xml']" mode="help">
<div class="help">
  <p>Berlioz could not transform the <b>source XML</b> because it is not well-formed.</p>
  <p>To fix this problem, simply ensure that the XML returned by your generator is well formed.</p>
</div>
</xsl:template>

</xsl:stylesheet>
