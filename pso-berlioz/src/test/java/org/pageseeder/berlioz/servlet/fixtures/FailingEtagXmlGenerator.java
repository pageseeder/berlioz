package org.pageseeder.berlioz.servlet.fixtures;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.output.OutputWriter;

/** Throws from the cache callback but successfully generates XML or JSON content. */
public final class FailingEtagXmlGenerator implements Generator, Cacheable {

  @Override
  public String getETag(Request req) {
    throw new HttpException(null, 503) {}.header("Retry-After", "30");
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    out.startObject("result");
    out.field("message", "generated");
    out.endObject();
    return Response.ok();
  }
}
