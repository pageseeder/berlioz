package org.pageseeder.berlioz.sample.api;

import java.io.IOException;

import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.sample.support.ProblemRegistry;

/**
 * Returns the current sample note as a direct API response.
 *
 * <p>The service is configured with a {@code <handler>} entry, so Berlioz sends this generator's
 * output directly instead of wrapping it in the normal Berlioz XML envelope and applying XSLT.
 * It implements {@link Generator} rather than {@code JsonGenerator} to demonstrate the generic
 * output writer in a direct service.</p>
 *
 * <p>The note store deliberately returns an empty string when no note has been saved yet. That
 * gives the API a stable, easy-to-test first response while keeping the sample storage layer tiny.</p>
 */
public final class GetNote implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    try {
      String text = NoteStore.read(req);
      // Direct handlers own the whole payload, so there is no Berlioz response wrapper here.
      out.startObject("note")
          .field("text", text)
          .field("empty", text.isEmpty())
          .field("length", text.length())
          .endObject();
      return Response.ok();
    } catch (IOException ex) {
      // Keep the reusable problem definition separate from the occurrence-specific exception diagnostic
      return Response.problem(ProblemRegistry.NOTE_READ_ERROR.diagnostic(ex));
    }
  }

}
