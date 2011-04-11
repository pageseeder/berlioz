/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * Generators implementing this interface can be initialised and destroyed. 
 * 
 * <p>They must provide <code>init</code> and <code>destroy</code> methods.
 * 
 * <p>Use this interface when the initialisation of a generator may yield exception
 * or is processor intensive. 
 * 
 * @author Christophe Lauret
 * @version 8 July 2010
 */
public interface Initializable {

  /**
   * Initialises this generator.
   */
  void init();

  /**
   * Destroys this generator.
   */
  void destroy();

}
