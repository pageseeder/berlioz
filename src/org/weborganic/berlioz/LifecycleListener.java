package org.weborganic.berlioz;

/**
 * An interface that can be used to provide methods for when Berlioz starts and stop.
 * 
 * <p>Implementations can be used to initialise database connections, indexes and other I/O
 * resources which are common to many generators. 
 * 
 * <p>Implementations must provide an <code>public</code> empty constructor for the init servlet
 * to invoke through reflection.
 * 
 * @author Christophe Lauret
 * @version 31 October 2011
 */
public interface LifecycleListener {

  /**
   * This method is called when Berlioz starts by the BerliozInit servlet's initialisation method.
   * 
   * <p>It is called after the system properties and global settings have been loaded.
   * 
   * <p>This method should not throw any error. Errors must be handled internally and the 
   * method {@link #isAlive()} should return <code>false</code>.
   * 
   * <p>Feedback on this method may be reported on <code>System.out</code> and prefixed by 
   * <code>[BERLIOZ_INIT] </code>. For example:
   * <pre>
   *   [BERLIOZ_INIT] Initialising index in /WEB-INF/index OK
   *   [BERLIOZ_INIT] Initialising database in /WEB-INF/db OK
   * </pre>
   * 
   * After this method has been called, the method {@link #isAlive()} should return <code>true</code>
   * unless some fatal errors occurred.
   * 
   * @return <code>true</code> if the start was successful.
   */
  boolean start();

  /**
   * This method is called when Berlioz stops by the BerliozInit servlet's destroy method.
   * 
   * <p>This method should not throw any error. Errors must be handled internally.
   * 
   * <p>The method {@link #isAlive()} should return <code>false</code> after this method was invoked.
   * 
   * <p>Feedback on this method may be reported on <code>System.out</code> and prefixed by 
   * <code>[BERLIOZ_STOP] </code>. For example:
   * <pre>
   *   [BERLIOZ_STOP] Releasing all index resources OK
   *   [BERLIOZ_STOP] Closing database connections OK
   * </pre>
   * 
   * @return <code>true</code> if the stop was successful.
   */
  boolean stop();

  /**
   * This method should be used by implementations to indicate whether they started normally. 
   * 
   * <p>This method can be used to determine whether the resources set to be initialised by the 
   * <code>start</code> methods are actually available or not.
   * 
   * @return <code>true</code> if the {@link #start()} was invoked successfully;
   *         <code>false</code> if the {@link #start()} failed or after the {@link #stop()} was invoked.
   */
  boolean isAlive();

}
