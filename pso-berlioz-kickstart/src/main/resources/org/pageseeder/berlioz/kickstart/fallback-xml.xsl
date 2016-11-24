<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fallback template invoked by Berlioz for XML output returning a copy of the source XML.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- XML properties -->
<xsl:output method="xml" media-type="application/xml" indent="no" encoding="utf-8" />

<!-- Default -->
<xsl:template match="/">
<xsl:sequence select="."/>
</xsl:template>

</xsl:stylesheet>
