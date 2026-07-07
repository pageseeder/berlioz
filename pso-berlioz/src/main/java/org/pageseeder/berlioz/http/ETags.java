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
package org.pageseeder.berlioz.http;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for computing entity tags for the GZip-variant representation of a resource.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
public final class ETags {

  /**
   * Suffix appended to the ETag of a resource to identify its GZip-compressed representation.
   */
  static final String GZIP_ETAG_SUFFIX = "-gzip\"";

  /**
   * Utility class.
   */
  private ETags() {
  }

  /**
   * Returns the entity tag for a compressed response.
   *
   * @param etag the entity tag of the response before compression.
   * @return the entity tag of the compressed response.
   */
  public static @Nullable String getETagForGZip(@Nullable String etag) {
    if (etag == null) return null;
    int q = etag.lastIndexOf("\"");
    return (q > 0)? etag.substring(0, q)+GZIP_ETAG_SUFFIX : etag;
  }

  /**
   * Returns the entity tag for an uncompressed response by stripping the GZip suffix.
   *
   * <p>For example, {@code "abc-gzip"} becomes {@code "abc"}.
   * If the ETag does not carry the GZip suffix, it is returned unchanged.
   *
   * @param etag the entity tag, possibly carrying the GZip suffix.
   * @return the base entity tag without the GZip suffix, or the original ETag unchanged.
   */
  public static @Nullable String getETagForUncompressed(@Nullable String etag) {
    if (etag == null) return null;
    int q = etag.lastIndexOf(GZIP_ETAG_SUFFIX);
    return (q > 0) ? etag.substring(0, q) + '"' : etag;
  }

}
