<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="xml" media-type="application/xml" indent="no" encoding="utf-8"/>

<xsl:template match="/">
  <xsl:apply-templates />
</xsl:template>

<xsl:template match="root">
  <!-- swapping `root` for `service` as an example -->
  <service id="{@service}" group="{@group}">
    <!-- inlining generator content to simplify output -->
    <xsl:sequence select="content/*"/>
  </service>
</xsl:template>

<xsl:template match="*">
  <xsl:sequence select="."/>
</xsl:template>

</xsl:stylesheet>
