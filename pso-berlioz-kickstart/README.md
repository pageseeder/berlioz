# berlioz-kickstart
Simple jar for Servlet 3.0 to make it easier to configure a Berlioz site

This jar simply defines a Web fragment and some fallback templates to make it easier
to start your Berlioz project.

## Berlioz mapping

The Web fragment maps the extensions below to some Berlioz XSLT

    *.html -> /xslt/html/[group].xsl
    *.xml  -> /xslt/xml/[group].xsl   (fallback XSLT if undefined)
    *.json -> /xslt/json/[group].xsl  (fallback XSLT if undefined)
    *.src  -> Raw Berlioz XML (requires Berlioz control authorization)

The XML fallback reduces information disclosure by default (since 0.14.5): it only
keeps `root/@service`, `root/@group`, `root/@flags`, `content/@name`, `content/@target`,
`content/@status` and the generator payload. The `<header>` is replaced by a comment
noting its removal. Applications should generally provide their own
`/xslt/xml/[group].xsl` template; use `*.src` to inspect the full envelope.

The JSON fallback returns an empty object.

The `*.src` mapping shows developers the raw Berlioz XML before XSLT. Since 0.14.5, it only
serves requests authorized for Berlioz control (`berlioz.control.network`,
`berlioz.control.key`, or the delegated request attribute); other requests receive a 404.
For local development, set `berlioz.control.network=loopback`.

## Usage

Simply include the berlioz kickstart jar in your classpath and your webapp will be Berlioz-enabled with the extensions defined above.
