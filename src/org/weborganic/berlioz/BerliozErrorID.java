/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

/**
 * A enumeration of errors known by Berlioz, so that it is easier to identify the type of error which occurred.
 * 
 * <p>These are included in error responses whenever Berlioz is able to identify the error.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 */
@Beta public enum BerliozErrorID implements ErrorID {

  /**
   * A completely unexpected error.
   */
  UNEXPECTED,

  /**
   * An error occurred while trying to start or stop Berlioz.
   */
  LIFECYCLE_ERROR,

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
  GENERATOR_ERROR_UNCHECKED,

  /**
   * An exception was thrown deliberately by a generator.
   */
  GENERATOR_ERROR_UNFORCED,

  /**
   * Multiple errors were thrown by a generator.
   */
  GENERATOR_ERROR_MULTIPLE;

  /**
   * Returns a string representation of this error code.
   * 
   * <p>The ID is the same as the name, but
   * <ul>
   *   <li>In lower case;</li>
   *   <li>Using '-' instead of '_';</li>
   *   <li>Prefixed by "berlioz-"</li>
   * </ul>
   * 
   * <p>For example, the ID of <code>SERVICES_NOT_FOUND</code> is <code>bzi-services-not-found</code>.
   * 
   * @return The ID of this error code.
   */
  public final String id() {
    return "berlioz-"+name().toLowerCase().replace('_', '-');
  };

  /**
   * Returns the same as the <code>id()</code> method.
   *
   * {@inheritDoc}
   */
  @Override
  public final String toString() {
    return this.id();
  };
}
