package org.pageseeder.berlioz.sample;

import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.HttpException;
import org.pageseeder.berlioz.error.ProblemDetails;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * A sample generator that generates errors on purpose for testing error handling.
 */
public final class ErrorGenerator implements Generator {

  /**
   * Problem returned when the note file cannot be written.
   */
  private static final ProblemDetails SAMPLE_PROBLEM = ProblemDetails.of(ContentStatus.CONFLICT)
      .type("urn:berlioz-sample:problem:test")
      .title("This is a sample problem for testing only");

  @Override
  public Response generate(Request req, OutputWriter out) {

    boolean hasProblem = req.parameter("problem").asBoolean().defaultValue(false);
    boolean throwError = req.parameter("throw").asBoolean().defaultValue(false);
    int httpcode = req.parameter("http").asInt().defaultValue(-1);

    // We return a problem if requested
    if (hasProblem)
      return Response.problem(SAMPLE_PROBLEM);

    // We throw an error if requested
    if (throwError)
      throwError();

    // We return an HTTP error if requested
    if (httpcode > 0)
      throw new RequestedHttpError(httpcode);

    // FieldOption.XML_ELEMENT keeps the message readable in the XML-backed HTML transform.
    out.startObject("error")
      .field("message", "No error specified!")
      .endObject();

    return Response.ok();
  }

  /**
   * Deliberately throws an unchecked exception for testing purposes.
   */
  private void throwError() {
    try {
      throw new IllegalArgumentException("This is the root cause!");
    } catch (Exception ex) {
      throw new RuntimeException("Error thrown by the test generator", ex);
    }
  }

  /**
   * A custom HTTP error.
   *
   * <p>This example lets you choose the HTTP error code to return for testing, but
   * typically, the HTTP status code should be predefined.
   */
  private static final class RequestedHttpError extends HttpException {
    public RequestedHttpError(int code) {
      super("Requested HTTP error", code);
    }
  }
}
