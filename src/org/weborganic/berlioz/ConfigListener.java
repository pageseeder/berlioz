/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz;

/**
 * An interface that can be used to provide methods for when the berlioz configuration is loaded.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.30 - 8 January 2015
 * @since Berlioz 0.9.30
 */
public interface ConfigListener {

  /**
   * This method is invoked when the global settings have been loaded or reloaded.
   */
  void load();

}
