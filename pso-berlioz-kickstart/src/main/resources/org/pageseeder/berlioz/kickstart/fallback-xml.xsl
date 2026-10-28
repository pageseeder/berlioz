<?xml version="1.0" encoding="UTF-8"?>
<!--
  Fallback template invoked by Berlioz for XML output when the group has no
  '/xslt/xml/{GROUP}.xsl' template.

  Secure by default: the Berlioz envelope is filtered through an allowlist to reduce
  potential information disclosure. This is not a foolproof safeguard; applications
  should generally provide their own '/xslt/xml/{GROUP}.xsl' to control what is exposed.
  The raw envelope remains available through the '*.src' mapping.

    - the <header> element is replaced by a comment noting its removal;
    - <root> keeps only '@service', '@group' and '@flags';
    - <content> keeps only '@name', '@target' and '@status';
    - the generator payload inside <content> is copied unchanged.

  Anything added to the envelope in future remains private by default.
-->
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<!-- XML properties -->
<xsl:output method="xml" media-type="application/xml" indent="no" encoding="utf-8" />

<!-- Only the envelope root is processed -->
<xsl:template match="/">
  <xsl:apply-templates select="root"/>
</xsl:template>

<!-- Envelope root: service identity only -->
<xsl:template match="root">
  <root>
    <xsl:copy-of select="@service | @group | @flags"/>
    <xsl:comment select="' header removed, attributes pruned '"/>
    <xsl:apply-templates select="content"/>
  </root>
</xsl:template>

<!-- Generator content: public attributes and unchanged payload -->
<xsl:template match="content">
  <content>
    <xsl:copy-of select="@name | @target | @status"/>
    <xsl:copy-of select="node()"/>
  </content>
</xsl:template>

</xsl:stylesheet>
