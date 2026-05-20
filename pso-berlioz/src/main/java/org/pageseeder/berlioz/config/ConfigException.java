package org.pageseeder.berlioz.config;

import org.pageseeder.berlioz.BerliozException;

public class ConfigException extends BerliozException {

  /**
   * Creates a new compound exception.
   *
   * @param message An explanatory message.
   * @param cause   The error collector.
   */
  public ConfigException(String message, Exception cause) {
    super(message, cause);
  }

}
