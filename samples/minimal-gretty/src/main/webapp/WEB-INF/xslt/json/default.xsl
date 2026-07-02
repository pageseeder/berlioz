<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:json="http://weborganic.org/JSON"
                exclude-result-prefixes="#all">

<xsl:output method="xml" media-type="application/json" encoding="utf-8"/>

<xsl:template match="/">
  <json:object/>
</xsl:template>

</xsl:stylesheet>
