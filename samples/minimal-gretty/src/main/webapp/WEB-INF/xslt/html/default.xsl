<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                exclude-result-prefixes="#all">

<!-- One include per service in the 'default' group; each provides 'title' and 'body' mode
     templates matching 'root[@service = ...]'. -->
<xsl:include href="default/index.xsl"/>
<xsl:include href="default/hello.xsl"/>
<xsl:include href="default/general-error.xsl"/>

<xsl:output method="html" media-type="text/html" encoding="UTF-8" indent="yes"/>

<xsl:template match="/root">
  <html lang="en">
    <head>
      <meta charset="UTF-8"/>
      <meta name="viewport" content="width=device-width, initial-scale=1"/>
      <title><xsl:apply-templates select="." mode="title"/></title>
      <link rel="icon" href="/favicon.svg" type="image/svg+xml"/>
      <link rel="alternate icon" href="/favicon.ico"/>
      <style>
        body {
          margin: 2rem auto;
          max-width: 40rem;
          padding: 0 1.5rem;
          color: #1f2933;
          font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          line-height: 1.5;
        }
        header {
          display: flex;
          align-items: center;
          gap: 0.75rem;
          margin-bottom: 1.5rem;
        }
        header img {
          width: 2.5rem;
          height: 2.5rem;
        }
        h1 {
          font-size: 1.5rem;
          margin: 0;
        }
        h2 {
          font-size: 1.05rem;
          margin: 2rem 0 0.5rem;
          border-top: 1px solid #e4e7eb;
          padding-top: 1.25rem;
        }
        code, pre {
          background: #edf2f7;
          border-radius: 4px;
        }
        code {
          padding: 0.1rem 0.3rem;
        }
        pre {
          padding: 0.75rem 1rem;
          overflow-x: auto;
        }
        a {
          color: #2b6cb0;
        }
      </style>
    </head>
    <body>
      <xsl:apply-templates select="." mode="body"/>
    </body>
  </html>
</xsl:template>

<!-- Fallback for any service without a dedicated 'default/<service>.xsl' include. -->
<xsl:template match="root" mode="title">Minimal Berlioz Sample</xsl:template>

<xsl:template match="root" mode="body">
  <h1>Minimal Berlioz Sample</h1>
  <p>No template for service <code><xsl:value-of select="@service"/></code>.</p>
</xsl:template>

</xsl:stylesheet>
