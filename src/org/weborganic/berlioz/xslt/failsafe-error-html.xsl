<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  Fail-safe stylesheet to display transform errors 

  @author Christophe Lauret
  @version 15 June 2011
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!--
  General Output properties.
  (Ensure that the doctype does not triggers Quirks Mode)
-->
<xsl:output method="html" encoding="utf-8" indent="yes" undeclare-prefixes="no" media-type="text/html" />

<!-- Main template called in all cases. -->
<xsl:template match="/">
<!-- Display the HTML Doctype -->
<xsl:text disable-output-escaping="yes"><![CDATA[<!doctype html>
]]></xsl:text>
<html>
<head>
  <title><xsl:value-of select="error/title"/></title>
  <style type="text/css">
body {font-family: Frutiger, "Frutiger Linotype", Univers, Calibri, "Gill Sans", "Gill Sans MT", "Myriad Pro", Myriad, "Liberation Sans",  Tahoma, Geneva, "Helvetica Neue", Helvetica, Arial, sans-serif; background: #ddd;}
h1   {margin-top: 0; border-bottom: 3px solid #fc0; color: #e60;}
h2   {border-bottom: 2px solid #09f; color: #09f; font-size: 3ex}
h3   {font-size: 2ex}

code {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 80%; line-height: 150%}
pre  {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 70%; line-height: 150%; color: #666; }

.container       {width: 1000px; margin: 30px auto; background: white; padding: 10px; border: 3px solid #999; -moz-border-radius: 12px; border-radius: 12px; -moz-box-shadow: 0 0 20px #aaa; box-shadow: 0 0 20px #aaa}
.message         {font-weight: bold}
.footer          {border-top: 2px solid #999; height: 20px; color: #666; font-size: 80%;}
.location        {font-family: Consolas, "Lucida Console", "Lucida Sans Typewriter", "Courier New", monospace; font-size: 80%; line-height: 150%}
#datetime        {float: left;}
#berlioz-version {float: right;}

.known .stacktrace {display: none}

li          {list-style-type: none; clear: both;}
.line, .col {float:left;margin-right: 10px; color: #999;}
.warning:before  {content: "[Warning] "; color: orange; display: inline-block; width: 70px}
.error:before    {content: "[Error] "; color: red; display: inline-block; width: 70px}
.fatal:before    {content: "[Fatal] "; color: red; display: inline-block; width: 70px}
  </style>
</head>
<body>
  <xsl:apply-templates select="*"/>
</body>
</html>
</xsl:template>


<!-- Default template for errors -->
<xsl:template match="error">
  <div class="container {if (not(@id = 'bzi-unexpected')) then 'known' else 'unknown'}">
    <h1><xsl:value-of select="@http-code"/> - <xsl:value-of select="title"/></h1>
    <xsl:if test="message != exception/message">
      <p class="message"><xsl:value-of select="message"/></p>
    </xsl:if>
    <xsl:apply-templates select="." mode="help"/>
    <xsl:apply-templates select="exception"/>
    <xsl:apply-templates select="collected-errors"/>
    <div class="footer">
      <div id="datetime"><xsl:value-of select="format-dateTime(@datetime, '[MNn] [D], [Y] at [H01]:[m01]:[s01] [z]')"/></div>
      <div id="berlioz-version">Berlioz <xsl:value-of select="berlioz/@version"/></div>
    </div>
    <div hidden="hidden" style="display:none">
      <xsl:copy-of select="."/>
    </div>
  </div>
</xsl:template>

<!-- Other errors -->
<xsl:template match="error[@code=404]">
  <div class="container">
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
    <xsl:apply-templates select="location|stack-trace|cause"/>
  </div>
</xsl:template>

<!-- Cause of an exception -->
<xsl:template match="cause">
  <div class="cause">
    <h3>Caused by: <xsl:value-of select="message"/></h3>
    <xsl:apply-templates select="location|stack-trace|cause"/>
  </div>
</xsl:template>

<!-- Stack Trace -->
<xsl:template match="stack-trace">
  <pre class="stacktrace"><xsl:value-of select="text()"/></pre>
</xsl:template>

<!-- Location -->
<xsl:template match="location">
  <p class="location">File: <xsl:value-of select="@system-id"/>, Line: <xsl:value-of select="@line"/>, Column: <xsl:value-of select="@column"/></p>
</xsl:template>

<!-- Exception -->
<xsl:template match="collected-errors">
<ul class="collected">
  <xsl:for-each select="collected">
  <li class="{@level}">
    <span class="line">Line: <xsl:value-of select="exception/location/@line"/></span>
    <span class="col">Column: <xsl:value-of select="exception/location/@column"/></span>
    <span class="info"><xsl:value-of select="exception/message"/></span>
  </li>
  </xsl:for-each>
</ul>
</xsl:template>

<!-- Help for Specifid Error IDs ============================================================== -->

<!-- No help ignore -->
<xsl:template match="error" mode="help" />

<xsl:template match="error[@id='bzi-services-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>service configuration</b>.</p>
  <p>To fix this problem, creates a file called '<b>services.xml</b>' and put it in your <code>/WEB-INF/config/</code> folder.</p>
</div>
</xsl:template>

<xsl:template match="error[@id='bzi-services-malformed']" mode="help">
<div class="help">
  <p>Berlioz was unable to parse the <b>service configuration</b>.</p>
  <p>To fix this problem, you need to fix the XML errors in the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<xsl:template match="error[@id='bzi-services-invalid']" mode="help">
<div class="help">
  <p>Berlioz was unable to load the service configuration because of the errors listed below.</p>
  <p>To fix this problem, you need to modify the '<b>/WEB-INF<xsl:value-of select="(//location)[1]/@system-id"/></b>' file.</p>
</div>
</xsl:template>

<xsl:template match="error[@id='bzi-transform-not-found']" mode="help">
<div class="help">
  <p>Berlioz was unable to find the <b>XSLT style sheet</b>.</p>
  <p>To fix this problem, simply create the style file describe below in your <code>/WEB-INF/</code> folder.</p>
</div>
</xsl:template>

</xsl:stylesheet>
