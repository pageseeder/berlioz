/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import com.topologi.diffx.xml.XMLWritable;

/**
 * Returns information about the content path.
 *
 * <p>None of the methods in this class will return <code>null</code> but empty string.
 * The full path can be constructed by concatenating the <i>context</i>, <i>prefix</i>,
 * <i>path</i> and <i>extension</i> (in that order).
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public interface PathInfo extends XMLWritable {

  /**
   * Returns the context of the Web application.
   *
   * @return the context of the Web application.
   */
  String context();

  /**
   * Returns the prefix used for the request if mapped as "/xxx/*".
   *
   * @return the prefix or empty string.
   */
  String prefix();

  /**
   * Returns the berlioz path used for the request that is the part that corresponds to the wildcard.
   *
   * @return the berlioz path used for the request that is the part that corresponds to the wildcard.
   */
  String path();

  /**
   * Returns the suffix used for the request if mapped as "*.xxx" including the '.'.
   *
   * @return the suffix or empty string.
   */
  String extension();

}
