<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fail-safe stylesheet to display transform errors

  @author Christophe Lauret
  @version 0.13.5
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:f="urn:berlioz:function"
                exclude-result-prefixes="#all">

<xsl:output method="html" encoding="utf-8" media-type="text/html" version="5.0"/>

<!-- Berlioz version injected by XsltTransformer at runtime; empty when called directly (e.g. in tests). -->
<xsl:param name="berlioz-version" as="xs:string" select="''"/>

<!-- The ID of the error -->
<xsl:variable name="id" select="/*/@id"/>

<!-- Main template called in all cases. -->
<xsl:template match="/">
<html lang="en">
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
    pre { line-height: 1.5; color: var(--muted); overflow-x: auto; margin: 0; }

    summary { cursor: pointer; user-select: none; }

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
      border-left: 4px solid #f59e0b;
      background: rgba(245, 158, 11, 0.07);
      border-radius: 0 4px 4px 0;
      padding: .6rem .75rem;
      margin: .75rem 0;
    }
    .help p { margin: .25rem 0; }

    /* ── Collapsible stack trace ─────────────────────────────────── */
    details.stack-trace { margin-top: .75rem; }
    details.stack-trace > summary { color: var(--muted); font-size: .85rem; }
    details.stack-trace > summary:hover { color: var(--accent); }
    details.stack-trace pre { margin-top: .5rem; }

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
    details.extensions > summary { color: var(--accent); font-size: .9rem; }
    details.extensions dl      { margin: .5rem 0 .25rem; font-size: .85rem; }
    details.extensions dt      { font-weight: bold; color: var(--muted); margin-top: .4rem; }
    details.extensions dd      { margin-left: 1rem; }
  </style>
</head>
<body class="{f:body-class(*)}">
  <xsl:apply-templates select="*"/>
</body>
</html>
</xsl:template>

<xsl:function name="f:body-class">
  <xsl:param name="element" />
  <xsl:choose>
    <xsl:when test="name($element) = 'problem' and $element/status castable as xs:integer">
      <xsl:variable name="s" select="xs:integer($element/status)"/>
      <xsl:sequence select="if ($s >= 500) then 'server-error'
                   else if ($s >= 400) then 'client-error'
                   else if ($s >= 300) then 'redirection'
                   else if ($s >= 200) then 'successful'
                   else                     'continue'"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:sequence select="name($element)"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:function>

<!-- Default template for errors -->
<xsl:template match="continue|successful|redirection|client-error|server-error">
  <div class="container">
    <h1><xsl:value-of select="@http-code"/> – <xsl:value-of select="title"/></h1>
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

<!-- Stack Trace — collapsible; open by default only for unexpected/generator errors -->
<xsl:template match="stack-trace">
  <details class="stack-trace">
    <xsl:if test="$id = 'berlioz-unexpected' or starts-with($id, 'berlioz-generator')">
      <xsl:attribute name="open">open</xsl:attribute>
    </xsl:if>
    <summary>Stack trace</summary>
    <pre><xsl:value-of select="text()"/></pre>
  </details>
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
    <xsl:apply-templates select="." mode="help"/>
    <!-- Structured exception extension (message, stack trace, cause chain) -->
    <xsl:apply-templates select="exception"/>
    <!-- Other non-standard extension members -->
    <xsl:variable name="extensions" select="*[not(self::type|self::status|self::title|self::detail|self::instance|self::exception)]"/>
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
    <div class="footer">
      <span id="datetime"><xsl:value-of select="format-dateTime(current-dateTime(), '[MNn] [D], [Y] at [H01]:[m01]:[s01] [z]')"/></span>
      <xsl:if test="$berlioz-version != ''">
        <span id="berlioz-version">Berlioz <xsl:value-of select="$berlioz-version"/></span>
      </xsl:if>
    </div>
  </div>
</xsl:template>


<!-- Help for Specified Error IDs ============================================================== -->

<!-- No help: ignore -->
<xsl:template match="*" mode="help" />

<!-- ── Unexpected ── -->
<xsl:template match="*[@id='berlioz-unexpected']" mode="help">
<div class="help">
  <p>An unexpected error occurred that Berlioz was not able to classify.</p>
  <p>Check the stack trace below and the server logs for context.</p>
</div>
</xsl:template>

<!-- ── Lifecycle ── -->
<xsl:template match="*[@id='berlioz-lifecycle-error']" mode="help">
<div class="help">
  <p>An error occurred during Berlioz <b>initialization or shutdown</b>.</p>
  <p>Check the application server startup logs. The application may not have initialised correctly.</p>
</div>
</xsl:template>

<!-- ── Services configuration ── -->
<xsl:template match="server-error[@id='berlioz-services-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>service configuration</b>.</p>
  <p>Create a file called '<b>services.xml</b>' and put it in your <code>/WEB-INF/config/</code> folder.</p>
</div>
</xsl:template>

<xsl:template match="server-error[@id='berlioz-services-malformed']" mode="help">
<div class="help">
  <p>Berlioz was unable to parse the <b>service configuration</b> — the file is not well-formed XML.</p>
  <p>Fix the XML errors in '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>'.</p>
</div>
</xsl:template>

<xsl:template match="server-error[@id='berlioz-services-invalid']" mode="help">
<div class="help">
  <p>Berlioz was unable to load the <b>service configuration</b> because of validation errors listed below.</p>
  <p>Correct the invalid entries in '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>'.</p>
</div>
</xsl:template>

<!-- ── XSLT transform ── -->
<xsl:template match="*[@id='berlioz-transform-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>XSLT stylesheet</b> for this URL.</p>
  <p>Create the stylesheet file in your <code>/WEB-INF/</code> folder at the path shown in the error details.</p>
</div>
</xsl:template>

<xsl:template match="*[@id='berlioz-transform-invalid']" mode="help">
<div class="help">
  <p>The <b>XSLT stylesheet</b> contains a static error and could not be compiled.</p>
  <p>Check the error location and message below, and fix the XPath or XSLT syntax error in the stylesheet.</p>
</div>
</xsl:template>

<xsl:template match="*[@id='berlioz-transform-dynamic-error']" mode="help">
<div class="help">
  <p>A <b>runtime error</b> occurred during XSLT transformation.</p>
  <p>Check the location and message below. Ensure the source XML matches what the stylesheet expects at that point.</p>
</div>
</xsl:template>

<xsl:template match="*[@id='berlioz-transform-malformed-source-xml']" mode="help">
<div class="help">
  <p>Berlioz could not transform the response because the <b>source XML is not well-formed</b>.</p>
  <p>Ensure that every content generator for this service writes valid XML.</p>
</div>
</xsl:template>

<!-- ── Generator errors ── -->
<xsl:template match="*[@id='berlioz-generator-error-unchecked']" mode="help">
<div class="help">
  <p>A content generator threw an <b>unexpected runtime exception</b>.</p>
  <p>Check the stack trace below and the server logs for context. This is likely a programming error in the generator.</p>
</div>
</xsl:template>

<xsl:template match="*[@id='berlioz-generator-error-unforced']" mode="help">
<div class="help">
  <p>A content generator reported an error using a <b>Berlioz exception</b>.</p>
  <p>Check the error message and the generator implementation to understand why the error was raised.</p>
</div>
</xsl:template>

<xsl:template match="*[@id='berlioz-generator-error-multiple']" mode="help">
<div class="help">
  <p>Multiple content generators reported errors during this request.</p>
  <p>Check the individual error details below to identify which generators failed and why.</p>
</div>
</xsl:template>

<!-- Problem Details help (matched on type URI — mirrors the legacy help above) ============== -->

<xsl:template match="problem[type='urn:berlioz:problem:unexpected']" mode="help">
<div class="help">
  <p>An unexpected error occurred that Berlioz was not able to classify.</p>
  <p>Check the stack trace (if included) and the server logs for context.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:lifecycle-error']" mode="help">
<div class="help">
  <p>An error occurred during Berlioz <b>initialization or shutdown</b>.</p>
  <p>Check the application server startup logs. The application may not have initialised correctly.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:services-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>service configuration</b>.</p>
  <p>Create a file called '<b>services.xml</b>' and put it in your <code>/WEB-INF/config/</code> folder.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:services-malformed']" mode="help">
<div class="help">
  <p>Berlioz was unable to parse the <b>service configuration</b> — the file is not well-formed XML.</p>
  <p>Fix the XML errors in your <code>/WEB-INF/config/services.xml</code> file.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:services-invalid']" mode="help">
<div class="help">
  <p>Berlioz was unable to load the <b>service configuration</b> because of validation errors.</p>
  <p>Correct the invalid entries in your <code>/WEB-INF/config/services.xml</code> file.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:transform-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>XSLT stylesheet</b> for this URL.</p>
  <p>Create the stylesheet file in your <code>/WEB-INF/</code> folder.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:transform-invalid']" mode="help">
<div class="help">
  <p>The <b>XSLT stylesheet</b> contains a static error and could not be compiled.</p>
  <p>Check the error details and fix the XPath or XSLT syntax error in the stylesheet.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:transform-dynamic-error']" mode="help">
<div class="help">
  <p>A <b>runtime error</b> occurred during XSLT transformation.</p>
  <p>Ensure the source XML matches what the stylesheet expects at the failing point.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:transform-malformed-source-xml']" mode="help">
<div class="help">
  <p>Berlioz could not transform the response because the <b>source XML is not well-formed</b>.</p>
  <p>Ensure that every content generator for this service writes valid XML.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:generator-error-unchecked']" mode="help">
<div class="help">
  <p>A content generator threw an <b>unexpected runtime exception</b>.</p>
  <p>Check the exception details (if included) and the server logs. This is likely a programming error in the generator.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:generator-error-unforced']" mode="help">
<div class="help">
  <p>A content generator reported an error using a <b>Berlioz exception</b>.</p>
  <p>Check the detail message and the generator implementation to understand why the error was raised.</p>
</div>
</xsl:template>

<xsl:template match="problem[type='urn:berlioz:problem:generator-error-multiple']" mode="help">
<div class="help">
  <p>Multiple content generators reported errors during this request.</p>
  <p>Check the individual error details to identify which generators failed and why.</p>
</div>
</xsl:template>

</xsl:stylesheet>
