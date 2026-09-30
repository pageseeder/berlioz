<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:import href="parts/imported.xsl"/>
  <xsl:include href="parts/included.xsl"/>
  <xsl:output method="html" encoding="UTF-8"/>
  <xsl:template match="/">
    <html><body><p id="message"><xsl:value-of select="//overlay/@message"/></p>
      <xsl:call-template name="imported"/><xsl:call-template name="included"/>
    </body></html>
  </xsl:template>
</xsl:stylesheet>
