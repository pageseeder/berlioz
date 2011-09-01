/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.util;

import java.io.File;

/**
 * A basic implementation of the Entity info pointing to an existing file and producing weak
 * entity tags.
 * 
 * @author Christophe Lauret
 * @version 28 January 2010
 */
public class FileEntityInfo implements EntityInfo {

  /** 
   * The file representing the bundle.
   */
  private final File _file;

  /**
   * The last modified.
   */
  private final long modified;

  /**
   * The length of the file.
   */
  private final long length;

  /**
   * The length of the file.
   */
  private final String mime;

  /**
   * Creates a new entity info for the specified file.
   * 
   * @param file     The file representing the bundle.
   * @param mimeType The content type of the file.
   */
  public FileEntityInfo(File file, String mimeType) {
    boolean ok = file.exists();
    this._file = file;
    this.modified = ok? file.lastModified() : -1L;
    this.length = ok? file.length() : -1L;
    this.mime = mimeType;
  }

  /**
   * {@inheritDoc}
   */
  public final long getLastModified() {
    return this.modified;
  }

  /**
   * @return The content length.
   */
  public final long getContentLength() {
    return this.length;
  }

  /**
   * {@inheritDoc}
   */
  public final String getMimeType() {
    return this.mime;
  }

  /**
   * {@inheritDoc}
   * 
   * Always a weak ETag.
   */
  public final String getETag() {
    if ((this.length >= 0) || (this.modified >= 0)) {
      return "W/\"" + this.length + "-" + this.modified + "\"";
    }
    return null;
  }

  /**
   * Returns the file.
   * 
   * @return the file used for this entity.
   */
  public final File getFile() {
    return this._file;
  }

}
