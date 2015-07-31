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
