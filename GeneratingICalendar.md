# Introduction #

As a framework for creating website, Berlioz is typically used for producing HTML. However, being based on XML and XSLT 2.0 means that Berlioz is equally comfortable processing other output formats, such as .docx, RSS, XML, plain text and more.

The following example demonstrates how to use Berlioz to create non-HTML outputs, in this case, the iCalendar (iCal) format used with many calendar systems.

# Configuration #

To use Berlioz to generate iCalendar data, create a new Berlioz instance in your Web descriptor (`/WEB-INF/web.xml`):
```
  <!-- iCalendar servlet -->
  <servlet>
    <servlet-name>iCalendar</servlet-name>
    <servlet-class>org.weborganic.berlioz.servlet.BerliozServlet</servlet-class>
    <init-param>
      <param-name>stylesheet</param-name>
      <param-value>/xslt/ical/global.xsl</param-value>
    </init-param>
    <init-param>
      <param-name>content-type</param-name>
      <param-value>text/calendar</param-value>
    </init-param>
  </servlet>
```

This creates a new Berlioz instance that will use the XSLT located in `/WEB-INF/xslt/ical/global.xsl` and declares the content type to be `text/calendar`.

Once this has been configured, the servlet must then be mapped to the appropriate Servlet URL pattern:
```
  <!-- iCalendar servlet mapping  -->
  <servlet-mapping>
    <servlet-name>iCalendar</servlet-name>
    <url-pattern>/ical/*</url-pattern>
  </servlet-mapping>
```

Configure the services - see Configuration\_Services.

In the XSLT (`/WEB-INF/xslt/ical/global.xsl`), the output can be specified using:
```
  <xsl:output method="text" encoding="US-ASCII" media-type="text/calendar"/>
```

Berlioz will then use the media type and encoding information from the `<xsl:output>` element.

# Example #

The following example will create valid iCalendar data:

Sample XML:
```
<event uid="12g9sngd84931834gf781f3"
             start="20100707T073000"
             end="20100707T103000"
             location="Sydney"
             summary="Test Event"
>
```

Corresponding XSLT:

```
<?xml version="1.0"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs">

  <xsl:output method="text" encoding="iso-8859-1" media-type="text/calendar"/>

  <!--  RFC 2445 specifies that new lines should be CR LF -->
  <xsl:variable name="crlf"><xsl:text>&#13;&#10;</xsl:text></xsl:variable>

  <xsl:template match="//event">
      <xsl:text>BEGIN:VCALENDAR</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>PRODID:-//Weborganic//Berlioz 0.7//EN</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>VERSION:2.0</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>METHOD:PUBLISH</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>BEGIN:VEVENT</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>CLASS:PUBLIC</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>UID:</xsl:text><xsl:value-of select="@uid" /><xsl:value-of select="$crlf" />
      <xsl:text>DTSTAMP:</xsl:text><xsl:value-of select="format-dateTime(adjust-dateTime-to-timezone(current-dateTime(), xs:dayTimeDuration('PT0H')), '[Y0001][M01][D01]T[H01][m01][s01]')" />Z<xsl:value-of select="$crlf" />
      <xsl:text>DTSTART:</xsl:text><xsl:value-of select="format-dateTime(adjust-dateTime-to-timezone(xs:dateTime(@start), xs:dayTimeDuration('PT0H')), '[Y0001][M01][D01]T[H01][m01][s01]')" />Z<xsl:value-of select="$crlf" />
      <xsl:text>DTEND:</xsl:text><xsl:value-of select="format-dateTime(adjust-dateTime-to-timezone(xs:dateTime(@end), xs:dayTimeDuration('PT0H')), '[Y0001][M01][D01]T[H01][m01][s01]')" />Z<xsl:value-of select="$crlf" />
      <xsl:text>LOCATION:</xsl:text><xsl:value-of select="@location"/><xsl:value-of select="$crlf" />
      <xsl:text>SUMMARY:</xsl:text><xsl:value-of select="@summary" /><xsl:value-of select="$crlf" />
      <xsl:text>BEGIN:VALARM</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>TRIGGER:-PT15M</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>ACTION:DISPLAY</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>DESCRIPTION:Reminder</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>END:VALARM</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>END:VEVENT</xsl:text><xsl:value-of select="$crlf" />
      <xsl:text>END:VCALENDAR</xsl:text>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
```