/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.bundler;

/**
 * The type of bundling required.
 *
 * <p>The bundler only supports two types.
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public enum BundleType {

  /** For JavaScript (simple concatenation). */
  JS(".js"),

  /** For CSS Styles (expands imports rules). */
  CSS(".css");

  /** extension corresponding to this type. */
  private final String _ext;

  /**
   * Creates a new type for the specified extension.
   *
   * @param ext The extension.
   */
  private BundleType(String ext) {
    this._ext = ext;
  }

  /**
   * @param ext The extension to match.
   * @return <code>true</code> if the argument matches the extension; <code>false</code> otherwise.
   */
  boolean matches(String ext) {
    return this._ext.equalsIgnoreCase(ext);
  }

}
