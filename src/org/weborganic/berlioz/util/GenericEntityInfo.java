/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.util.Date;

/**
 * A basic implementation of the entity info interface.
 *
 * <p>This class can be used as a base class for other entity info implementations.
 *
 * @author Christophe Lauret
 * @author 28 January 2010
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
  private final String _etag;

  /**
   * Creates a new entity info.
   *
   * @param modified    The last modified date of the entity.
   * @param contentType The content type of the entity.
   * @param etag        The etag for the entity.
   */
  public GenericEntityInfo(long modified, String contentType, String etag) {
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
  public GenericEntityInfo(Date modified, String contentType, String etag) {
    this(modified.getTime(), contentType, etag);
  }

  @Override
  public final long getLastModified() {
    return this._modified;
  }

  @Override
  public final String getMimeType() {
    return this._mime;
  }

  @Override
  public final String getETag() {
    return this._etag;
  }
}
