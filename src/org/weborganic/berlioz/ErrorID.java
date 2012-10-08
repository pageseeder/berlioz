/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

/**
 * An ID for errors to help with error handling and diagnostic.
 * 
 * <p>Error IDs starting with "bz" are reserved by Berlioz.
 * 
 * <p>Note: these are different and complementary to HTTP response codes.
 * 
 * @author Christophe Lauret
 * @version 30 June 2011
 * 
 * @since Berlioz 0.8.3
 */
@Beta public interface ErrorID {

  /**
   * Returns the error identifier as a string.
   * 
   * <p>Implementations should also ensure that the <code>toString()</code> returns the same value.
   * 
   * @return the error ID as a string.
   */
  String id();

}
