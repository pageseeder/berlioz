package org.pageseeder.berlioz.util;

import java.io.IOException;

/**
 * Wraps I/O exceptions for classes where the occurrence is unlikely because the
 * underlying data stream never actually throw any IOException.
 *
 * @author Christophe Lauret
 */
public class WriteFailureException extends RuntimeException {

  /** As per requirement for Serializable */
  private static final long serialVersionUID = 5755916484156063531L;

  public WriteFailureException(String message, IOException cause) {
    super(message, cause);
  }

  public WriteFailureException(String message, IOException cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
