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
