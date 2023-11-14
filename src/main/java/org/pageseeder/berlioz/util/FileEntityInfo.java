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

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A basic implementation of the Entity info pointing to an existing file and producing
 * entity tags based on length and last modified date.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.6.0 - 31 May 2010
 * @since Berlioz 0.6
 */
public class FileEntityInfo implements EntityInfo {

  /**
   * The file representing the bundle.
   */
  private final File file;

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
    this.file = file;
    this.modified = ok? file.lastModified() : -1L;
    this.length = ok? file.length() : -1L;
    this.mime = mimeType;
  }

  @Override
  public final long getLastModified() {
    return this.modified;
  }

  /**
   * @return The content length.
   */
  public final long getContentLength() {
    return this.length;
  }

  @Override
  public final @NonNull String getMimeType() {
    return this.mime;
  }

  @Override
  public final @Nullable String getETag() {
    if ((this.length >= 0) || (this.modified >= 0)) return "\"" + this.length + "-" + this.modified + "\"";
    return null;
  }

  /**
   * Returns the file.
   *
   * @return the file used for this entity.
   */
  public final File getFile() {
    return this.file;
  }

}
