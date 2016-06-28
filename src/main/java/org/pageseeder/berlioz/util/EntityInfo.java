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
package org.pageseeder.berlioz.util;

/**
 * Interface to define basic information that can be held about a resource to be served via HTTP.
 *
 * <p>Objects implementing this interface can be used to determine the caching behaviour in the
 * HTTP response.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.7
 * @since Berlioz 0.6
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
   * @return a strong ETag if available
   */
  String getETag();

}
