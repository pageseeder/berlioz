package org.weborganic.berlioz.util;

import org.weborganic.berlioz.ErrorID;
import org.weborganic.berlioz.Beta;

/**
 * A enumeration of error known by Berlioz, so that it is easier to identify the type of error which occurred.
 * 
 * <p>Note: these are different and somewhat complementary to HTTP response code.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
@Beta public enum BerliozInternal implements ErrorID {

  /**
   * A completely unexpected error.
   */
  UNEXPECTED,

  /**
   * An error occurred because the XSLT file could not be found.
   */
  SERVICES_NOT_FOUND,

  /**
   * An error occurred because the XSLT file could not be found.
   */
  SERVICES_MALFORMED,

  /**
   * An error occurred because the service configuration file could not be found.
   */
  SERVICES_INVALID,

  /**
   * A transform error occurred because the XSLT file could not be found.
   */
  TRANSFORM_NOT_FOUND,

  /**
   * A transform error occurred because the XSLT is not valid.
   */
  TRANSFORM_INVALID,

  /**
   * A transform error occurred because the source XML was not well-formed.
   */
  TRANSFORM_MALFORMED_SOURCE_XML,

  /**
   * A dynamic transform error occurred (for example because the XSLT function expected a different data type).
   */
  TRANSFORM_DYNAMIC_ERROR,

  /**
   * An unchecked exception was thrown by a generator.
   */
  GENERATOR_UNCHECKED;

  /**
   * Returns a string representation of this error code.
   */
  public String toString() {
    return "bzi-"+name().toLowerCase().replace('_', '-');
  };
}
