package org.pageseeder.berlioz.sample.support;

import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.error.ProblemDetails;

/**
 * Reusable problem details for the sample application.
 *
 * <p>Keeping problem definitions in one place gives API responses stable problem types and titles.
 * Generators can then add occurrence-specific details, such as an exception extension, without
 * duplicating the canonical error metadata.</p>
 */
public final class ProblemRegistry {

  private ProblemRegistry() {}

  /**
   * Problem returned when the note file cannot be read.
   */
  public static final ProblemDetails NOTE_READ_ERROR = ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
      .type("urn:berlioz-sample:problem:note-read-failed")
      .title("Unable to read the note.");

  /**
   * Problem returned when the note file cannot be written.
   */
  public static final ProblemDetails NOTE_WRITE_ERROR = ProblemDetails.of(ContentStatus.INTERNAL_SERVER_ERROR)
      .type("urn:berlioz-sample:problem:note-write-failed")
      .title("Unable to write the note.");

}
