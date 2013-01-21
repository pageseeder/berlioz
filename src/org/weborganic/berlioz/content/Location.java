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
 * This class provides information about the location of resource associated with the content request.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.13 - 21 January 2013
 */
public interface Location extends XMLWritable {

  /**
   * @return the scheme of the URI
   */
  String scheme();

  /**
   * @return the host of the URI
   */
  String host();

  /**
   * @return the port used for the URI
   */
  int port();

  /**
   * @return the complete path path of the URI
   */
  String path();

  /**
   * @return the query part of the URI
   */
  String query();

  /**
   * @return the path information.
   */
  PathInfo info();

}
