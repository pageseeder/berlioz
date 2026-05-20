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

import org.eclipse.jdt.annotation.Nullable;

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
  @Nullable String getETag(ContentRequest req);

}
