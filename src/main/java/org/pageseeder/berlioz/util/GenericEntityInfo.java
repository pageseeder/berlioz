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

import java.util.Date;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A basic implementation of the entity info interface.
 *
 * <p>This class can be used as a base class for other entity info implementations.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.6.0 - 31 May 2010
 * @since Berlioz 0.6
 */
public class GenericEntityInfo implements EntityInfo {

  /**
   * The last modified.
   */
  private final long _modified;

  /**
   * The content type
   */
  private final String _mime;

  /**
   * The entity tag.
   */
  private final @Nullable String _etag;

  /**
   * Creates a new entity info.
   *
   * @param modified    The last modified date of the entity.
   * @param contentType The content type of the entity.
   * @param etag        The etag for the entity.
   */
  public GenericEntityInfo(long modified, String contentType, @Nullable String etag) {
    this._modified = modified;
    this._mime = contentType;
    this._etag = etag;
  }

  /**
   * Creates a new entity info.
   *
   * @param modified    The last modified date of the entity.
   * @param contentType The content type of the entity.
   * @param etag        The etag for the entity.
   */
  public GenericEntityInfo(Date modified, String contentType, @Nullable String etag) {
    this(modified.getTime(), contentType, etag);
  }

  @Override
  public final long getLastModified() {
    return this._modified;
  }

  @Override
  public final @NonNull String getMimeType() {
    return this._mime;
  }

  @Override
  public final @Nullable String getETag() {
    return this._etag;
  }
}
