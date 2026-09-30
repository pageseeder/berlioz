package org.pageseeder.berlioz.fixture;

import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

public final class OverlayGenerator implements Generator {
  @Override public Response generate(Request request, OutputWriter out) {
    out.startObject("overlay");
    out.field("message", "overlay-from-jar");
    out.field("packaged", getClass().getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar"));
    out.endObject();
    return Response.ok();
  }
}
