/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.regex.Pattern;

/**
 * A class implementing this interface can be matched.
 *
 * This interface can be used to indicate whether a class can be used for pattern matching.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface Matchable {

  /**
   * Indicates whether this token matches the specified part of a URL.
   *
   * @param part The part of URL to test for matching.
   *
   * @return <code>true</code> if it matches; <code>false</code> otherwise.
   */
  boolean match(String part);

  /**
   * Returns a regular expression pattern corresponding to this object.
   *
   * @return The regular expression pattern corresponding to this object.
   */
  Pattern pattern();

}
