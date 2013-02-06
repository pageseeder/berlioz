/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import org.weborganic.berlioz.Beta;

/**
 * A listener for when requests have been processed for a generator.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.16 - 6 February 2013
 * @since Berlioz 0.9.16
 */
@Beta
public interface GeneratorListener {

  /**
   * Reports when a request has been processed for a generator.
   *
   * @param service   The Berlioz service
   * @param generator The content generator
   * @param status    The content status
   * @param etag      The time taken to generate the etag in nanoseconds
   * @param process   The time taken to process the request in nanoseconds
   */
  void generate(Service service, ContentGenerator generator, ContentStatus status, long etag, long process);

}
