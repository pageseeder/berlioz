/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.bundler;

import java.io.Serializable;

/**
 * Holds the definition of a single bundle configuration item.
 *
 * <p>A bundle definition has:
 * <ul>
 *   <li>A name (e.g. "global", "group", "service")</li>
 *   <li>A filename which may include tokens (e.g. "global", "{GROUP}", "{SERVICE}")</li>
 *   <li>A list of web paths included as part of this bundle which may also include dynamic tokens.</li>
 * </ul>
 *
 * <p>When a bundle definition is instantiated for a specific service, the tokens are resolved and fixed.
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
final class BundleDefinition implements Serializable {

  /** As per requirement for <code>Serializable</code> */
  private static final long serialVersionUID = -663743071617576797L;

  /**
   * The name of the bundle definition.
   */
  private final String _name;

  /**
   * The name to use of the filename of the bundle.
   */
  private final String _filename;

  /**
   * The list of paths to include in the bundle.
   */
  private final String[] _paths;

  /**
   * @param name     The name of the bundle.
   * @param filename The name to use of the filename of the bundle.
   * @param paths    The list of paths to include in the bundle.
   */
  public BundleDefinition(String name, String filename, String paths) {
    this._name = name;
    this._filename = filename;
    this._paths = paths.split(",");
  }

  /**
   * @return The name of the bundle.
   */
  public String name() {
    return this._name;
  }

  /**
   * The name to use for the file name of the bundle instance.
   *
   * <p>The name may contain tokens to be resolved when instantiated.
   *
   * @return The name to use of the filename of the bundle.
   */
  public String filename() {
    return this._filename;
  }

  /**
   * The list of paths to include in the bundle.
   *
   * <p>The name may contain tokens to be resolved when instantiated.
   *
   * @return The list of paths to include in the bundle.
   */
  public String[] paths() {
    return this._paths;
  }
}
