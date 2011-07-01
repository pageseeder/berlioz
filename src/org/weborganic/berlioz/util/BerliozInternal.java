package org.weborganic.berlioz.util;

import org.weborganic.berlioz.ErrorID;
import org.weborganic.berlioz.Beta;

/**
 * A enumeration of errors known by Berlioz, so that it is easier to identify the type of error which occurred.
 * 
 * <p>These are included in error responses whenever Berlioz is able to identify the error.
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
   * 
   * <p>The ID is the same as the name, but
   * <ul>
   *   <li>In lower case;</li>
   *   <li>Using '-' instead of '_';</li>
   *   <li>Prefixed by "bzi"</li>
   * </ul>
   * 
   * <p>For example, the ID of <code>SERVICES_NOT_FOUND</code> is <code>bzi-services-not-found</code>.
   * 
   * @return The ID of this error code.
   */
  public final String id() {
    return "bzi-"+name().toLowerCase().replace('_', '-');
  };

  /**
   * Returns the same as the <code>id()</code> method.
   *
   * {@inheritDoc}
   */
  public final String toString() {
    return this.id();
  };
}
