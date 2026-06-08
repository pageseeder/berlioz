package org.pageseeder.berlioz.servlet.fixtures;

import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.json.JsonWriter;

public final class DirectJsonGenerator implements JsonGenerator {

  @Override
  public Response generate(Request req, JsonWriter json) {
    json.startObject();
    json.field("message", "hello");
    json.field("path", req.getBerliozPath());
    json.endObject();
    return Response.ok();
  }
}
