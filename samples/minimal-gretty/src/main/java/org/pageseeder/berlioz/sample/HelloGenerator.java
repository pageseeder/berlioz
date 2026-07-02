package org.pageseeder.berlioz.sample;

import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * Minimal generator using Berlioz's modern output API.
 *
 * <p>This generator is intentionally small: it shows the request/response shape that new
 * Berlioz code should prefer without introducing application-specific infrastructure. Unlike
 * {@link LegacyGenerator}, it writes through {@link OutputWriter}, so Berlioz can adapt the same
 * logical output to the configured XML/JSON response path.</p>
 */
public final class HelloGenerator implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    String name = req.getParameter("name", "Berlioz developer");

    // FieldOption.XML_ELEMENT keeps the message readable in the XML-backed HTML transform.
    out.startObject("hello")
      .field("path", req.getBerliozPath())
      .field("name", name)
      .field("message", "Hello " + name, OutputWriter.FieldOption.XML_ELEMENT)
      .endObject();

    return Response.ok();
  }
}
