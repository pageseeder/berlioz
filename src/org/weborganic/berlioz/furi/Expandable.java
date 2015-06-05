/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

/**
 * A class implementing this interface can be expanded.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface Expandable {

  /**
   * Expands this object to produce a URI fragment as defined by the URI Template specifications.
   *
   * @param parameters The list of parameters and their values for substitution.
   *
   * @return The expanded URI fragment
   */
  String expand(Parameters parameters);

}
