/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

/**
 * Interface to define basic information that can be held about a resource to be served via HTTP.
 *
 * <p>Objects implementing this interface can be used to determine the caching behaviour in the
 * HTTP response.
 *
 * @author Christophe Lauret
 * @version 25 January 2010
 */
public interface EntityInfo {

  /**
   * Get last modified time.
   *
   * @return lastModified time value; -1 if unknown.
   */
  long getLastModified();

  /**
   * Returns the MIME Type as defined for the server.
   *
   * @return Returns the MIME Type.
   */
  String getMimeType();

  /**
   * Get ETag.
   *
   * @return strong ETag if available, otherwise weak ETag.
   */
  String getETag();

}
