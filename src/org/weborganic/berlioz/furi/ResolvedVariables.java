/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.Set;

/**
 * Holds the values of a resolved variables.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface ResolvedVariables {

  /**
   * Returns the names of the variables which have been resolved.
   *
   * @return The names of the variables which have been resolved.
   */
  Set<String> names();

  /**
   * Returns the object corresponding to the specified variable name.
   *
   * @param name The name of the variable.
   *
   * @return The object corresponding to the specified variable; may be <code>null</code>.
   */
  Object get(String name);

}
