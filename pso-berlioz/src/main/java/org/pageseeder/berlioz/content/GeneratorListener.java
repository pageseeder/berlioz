/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.content;

import org.pageseeder.berlioz.Beta;

/**
 * A listener for when requests have been processed for a generator.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.16
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
