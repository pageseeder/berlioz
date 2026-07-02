package org.pageseeder.berlioz.sample.api;

import java.io.IOException;

import org.pageseeder.berlioz.content.JsonGenerator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.ExceptionDetail;
import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.sample.support.AppParameters;
import org.pageseeder.berlioz.sample.support.ProblemRegistry;

/**
 * Updates the sample note and returns the saved value as JSON.
 *
 * <p>This generator implements {@link JsonGenerator}, which is the simplest option for direct
 * JSON API endpoints. The request body is not parsed here on purpose: the current sample uses the
 * typed parameter API so developers can see validation, bad-request handling, and direct JSON
 * output without also needing a JSON-body parsing example.</p>
 *
 * <p>Try it with:
 * {@code curl -X POST 'http://localhost:8999/api/note.json?text=Hello'}.</p>
 */
public final class UpdateNote implements JsonGenerator {

  @Override
  public Response generate(Request req, JsonWriter json) {
    // Centralising parameter rules keeps generator code focused on behavior.
    String text = req.parameter(AppParameters.TEXT);

    try {
      NoteStore.write(req, text);
      json.startObject()
          .field("updated", true)
          .field("text", text)
          .field("length", text.length())
          .endObject();
      return Response.ok();
    } catch (IOException ex) {
      // The problem type is reusable; the exception detail describes this particular failure.
      // Pass false so stack traces are never sent to clients; use true only in dev tooling.
      return Response.problem(ProblemRegistry.NOTE_WRITE_ERROR
          .extension(ExceptionDetail.of(ex, false)));
    }
  }

}
