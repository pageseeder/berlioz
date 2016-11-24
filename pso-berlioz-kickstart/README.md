# berlioz-kickstart
Simple jar for Servlet 3.0 to make it easier to configure a Berlioz site

This jar simply defines a Web fragment and some fallback templates to make it easier
to start your Berlioz project.

## Berlioz mapping

The Web fragment maps the extensions below to some Berlioz XSLT

    *.html -> /xslt/html/[group].xsl
    *.xml  -> /xslt/xml/[group].xsl   (fallback XSLT if undefined)
    *.json -> /xslt/json/[group].xsl  (fallback XSLT if undefined)
    *.src  -> Raw Berlioz XML

## Usage

Simply include the berlioz kickstart jar in your classpath and your webapp will be Berlioz-enabled with the extensions defined above.
