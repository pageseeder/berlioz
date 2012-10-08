/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * Generators implementing this interface can be cached and must provide a consistent content
 * response given a content request.
 *
 * <p>They must provide an <b>unquoted</b> ETag for a given content request. The Etag is considered
 * strong by default.
 *
 * @see <a href="http://tools.ietf.org/html/rfc2616#section-14.19">Hypertext Transfer Protocol --
 * HTTP/1.1: 14.19 ETag</a>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.3 - 31 May 2010
 * @since  Berlioz 0.6
 */
public interface Cacheable {

  /**
   * Returns the ETag for the specified content request.
   *
   * @param req the content request.
   * @return The corresponding ETag.
   */
  String getETag(ContentRequest req);

}
