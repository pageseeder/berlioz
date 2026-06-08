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

import org.jspecify.annotations.Nullable;

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
 * @version 0.13.2
 * @since 0.6
 */
public interface Cacheable {

  /**
   * Returns the ETag for the specified request context.
   *
   * <p>All generator types may override this method. The default dispatches to
   * {@link #getETag(ContentRequest)} only when this cacheable object is a legacy
   * {@link ContentGenerator} and the request is a {@link ContentRequest}, allowing old
   * content generators to keep overriding only the narrower method.
   *
   * <p>Non-{@link ContentGenerator} generators must override this method directly.
   *
   * @param req the request
   * @return the corresponding ETag, or {@code null}
   */
  @SuppressWarnings({"deprecation", "java:S1874"}) // intentional bridge to the deprecated narrower overload
  default @Nullable String getETag(Request req) {
    if (this instanceof ContentGenerator && req instanceof ContentRequest) {
      return getETag((ContentRequest) req);
    }
    return null;
  }

  /**
   * Returns the ETag for the specified content request.
   *
   * <p>Legacy {@link ContentGenerator} implementations may override this method.
   * Other generator types must override {@link #getETag(Request)} instead.
   * The default returns {@code null}.
   *
   * @param req the content request
   * @return the corresponding ETag, or {@code null}
   *
   * @deprecated Override {@link #getETag(Request)} instead. This overload remains as a
   * legacy bridge for {@link ContentGenerator} implementations only.
   */
  @Deprecated(since = "0.13.2")
  default @Nullable String getETag(ContentRequest req) {
    return null;
  }

}
