/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.List;


/**
 * Defines tokens which use an operator to handle one or more variables.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface TokenOperator extends Token {

  /**
   * Returns the list of variables used in this token.
   *
   * @return the list of variables.
   */
  List<Variable> variables();

}
