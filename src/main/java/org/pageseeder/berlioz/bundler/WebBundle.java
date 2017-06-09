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
package org.pageseeder.berlioz.bundler;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.berlioz.util.MD5;

/**
 * A bundle of files to serve.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class WebBundle {

  /**
   * The name of the bundle.
   */
  private final String _name;

  /**
   * The id of this bundle.
   */
  private final String _id;

  /**
   * The list of  of the bundle.
   */
  private final List<File> _files;

  /**
   * The name of the bundle.
   */
  private final List<File> _imported;

  /**
   * Whether the bundle is minimized.
   */
  private boolean _minimized;

  /**
   * The hash values for this bundle.
   */
  private volatile @Nullable String _etag;

  /**
   * Creates a new bundles of files.
   *
   * @param name      The name of the bundle.
   * @param files     The files to bundle.
   * @param minimized Whether the files are minimized.
   */
  public WebBundle(String name, List<File> files, boolean minimized) {
    this._name = name;
    this._files = Collections.unmodifiableList(files);
    this._id = id(files);
    this._imported = new ArrayList<>();
    this._minimized = minimized;
  }

  /**
   * Returns the name of this bundle.
   * @return the name of this bundle.
   */
  public String name() {
    return this._name;
  }

  /**
   * Returns the list of files to bundle.
   * @return the list of files to bundle.
   */
  public List<File> files() {
    return this._files;
  }

  /**
   * Returns an ID for that bundle based on the names list of files.
   *
   * <p>The ID of the bundle remain constant.
   *
   * @return an ID for that bundle based on the list of files.
   */
  public String id() {
    return this._id;
  }

  /**
   * Clears the list of imported files.
   */
  public void clearImport() {
    this._imported.clear();
  }

  /**
   * Adds a file to consider as an import.
   * @param f the file to import.
   */
  public void addImport(File f) {
    this._imported.add(f);
  }

  /**
   * Returns the etag for that bundle.
   *
   * <p>The etag changes when any of the file changes.
   *
   * @param refresh <code>true</code> to calculate the etag; <code>false</code> otherwise.
   * @return the etag for that bundle.
   */
  public String getETag(boolean refresh) {
    String etag = this._etag;
    if (etag == null || refresh) {
      etag = calculateEtag(this._files, this._imported);
      this._etag = etag;
    }
    return etag;
  }

  /**
   * Calculates whether the bundles is still fresh by comparing the etag.
   *
   * @return <code>true</code> if still fresh;
   *         <code>false</code> otherwise.
   */
  public boolean isFresh() {
    String etag = calculateEtag(this._files, this._imported);
    return etag.equals(this._etag);
  }

  /**
   * Indicates whether the web bundle is minimized.
   *
   * @return <code>true</code> if minimized; <code>false</code> otherwise.
   */
  public boolean isMinimized() {
    return this._minimized;
  }

  /**
   * @return <code>true</code> if it can be safely minimized; <code>false</code> otherwise.
   */
  public boolean isCSSMinimizable() {
    for (File f : this._files) {
      if (f.getName().endsWith(".min.css")) return false;
    }
    for (File f : this._imported) {
      if (f.getName().endsWith(".min.css")) return false;
    }
    return true;
  }

  /**
   * Returns the filename of this bundle.
   *
   * <p>The filename is: <code>[name]-[isodate]-[etag-suffix].[extension]</code>.
   * <p>Or <code>[name]-[isodate]-[etag-suffix].min.[extension]</code> if minimized.
   *
   * @return the filename of this bundle.
   */
  public String getFileName() {
    StringBuilder filename = new StringBuilder(this._name);
    filename.append('-');
    filename.append(ISO8601.CALENDAR_DATE.format(getMostRecent(this._files)));
    String etag = getETag(false);
    filename.append('-').append(etag.substring(etag.length()-4));
    String ext = getExtension(this._files.get(0));
    if (this._minimized) {
      filename.append(".min");
    }
    if (ext != null) {
      filename.append(ext);
    }
    return filename.toString();
  }

  // Static methods -------------------------------------------------------------------------------

  /**
   * Returns an ID for this bundle.
   *
   * @param files the files included in the bundle.
   * @return An ID based on the hash value of the concatenation.
   */
  public static String id(List<File> files) {
    StringBuilder id = new StringBuilder();
    for (File f : files) {
      id.append(f.getAbsolutePath());
    }
    return MD5.hash(id.toString());
  }

  /**
   * Calculate the etag for the specified lists of files based on the absolute path, length
   * and last modified date.
   *
   * @param files    the list of files.
   * @param imported the list of imported files (CSS only).
   *
   * @return an MD5 value.
   */
  private static String calculateEtag(List<File> files, List<File> imported) {
    StringBuilder key = new StringBuilder();
    for (File f : files) {
      appendKey(f, key);
    }
    // Also include files that are imported (@import rules in CSS)
    for (File f : imported) {
      appendKey(f, key);
    }
    return MD5.hash(key.toString());
  }

  /**
   * Returns the extension of the specified file including the dot.
   *
   * @param file the file which extension is needed.
   * @return the extension or <code>null</code> if none available.
   */
  private static @Nullable String getExtension(File file) {
    int dot = file.getName().lastIndexOf('.');
    return dot >= 0? file.getName().substring(dot) : null;
  }

  /**
   * Appends the key based on absolute path, length and last modified date for for one file.
   *
   * @param f   The file which
   * @param key Key to append.
   */
  private static void appendKey(File f, StringBuilder key) {
    key.append(f.getAbsolutePath());
    key.append(f.length()).append('>');
    key.append(f.lastModified()).append('|');
  }

  /**
   * Returns the date of the most recent file.
   * @param files the list of files.
   * @return the date of the most recent.
   */
  private static long getMostRecent(List<File> files) {
    long mostRecent = 0;
    for (File f : files) {
      if (f.lastModified() > mostRecent) {
        mostRecent = f.lastModified();
      }
    }
    return mostRecent;
  }
}
