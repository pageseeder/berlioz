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
  <title>Berlioz: Server Error</title>
  <style type="text/css">
body      {font-family: Frutiger, "Frutiger Linotype", Univers, Calibri, "Gill Sans", "Gill Sans MT", "Myriad Pro", Myriad, "DejaVu Sans Condensed", "Liberation Sans", "Nimbus Sans L", Tahoma, Geneva, "Helvetica Neue", Helvetica, Arial, sans-serif; background: #f7f7f7;}
.error    {width: 1000px; margin: 30px auto; background: white; padding: 10px; border: 3px solid #999; -moz-border-radius: 12px; border-radius: 12px; -moz-box-shadow: 0 0 20px #aaa; box-shadow: 0 0 20px #aaa}
.error h1 { margin-top: 0; border-bottom: 3px solid #fc0; color: #e60;}
.message {font-weight: bold}
h4 {border-bottom: 2px solid #09f; color: #09f}
.line {float: left; margin-right: 10px; color: #999; font-family: Consolas, "Andale Mono WT", "Andale Mono", "Lucida Console", "Lucida Sans Typewriter", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Liberation Mono", "Nimbus Mono L", Monaco, "Courier New", Courier, monospace; font-size: 80%; line-height: 150%}
.col {float: left; margin-right: 10px; color: #999; font-family: Consolas, "Andale Mono WT", "Andale Mono", "Lucida Console", "Lucida Sans Typewriter", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Liberation Mono", "Nimbus Mono L", Monaco, "Courier New", Courier, monospace; font-size: 80%; line-height: 150%}
li {list-style-type: none; clear: both;}
code {font-family: Consolas, "Andale Mono WT", "Andale Mono", "Lucida Console", "Lucida Sans Typewriter", "DejaVu Sans Mono", "Bitstream Vera Sans Mono", "Liberation Mono", "Nimbus Mono L", Monaco, "Courier New", Courier, monospace; font-size: 80%; line-height: 150%}
  </style>
</head>
<body>
  <xsl:apply-templates select="*"/>
</body>
</html>
</xsl:template>


<!-- Other errors -->
<xsl:template match="error">
  <div class="error template">
    <h1>Berlioz: <xsl:value-of select="@code"/> - <xsl:value-of select="message"/></h1>
    <p class="message"><xsl:value-of select="exception/message"/></p>
    <xsl:copy-of select="."/>
  </div>
</xsl:template>

<!-- Other errors -->
<xsl:template match="error[@code=404]">
  <div class="error template">
    <h1>Berlioz: <xsl:value-of select="message"/></h1>
    <p class="message">Sorry but I could not find anything at <code><xsl:value-of select="@request-uri"/></code></p>
    <xsl:copy-of select="."/>
  </div>
</xsl:template>

</xsl:stylesheet>
