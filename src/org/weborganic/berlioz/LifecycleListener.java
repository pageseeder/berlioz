/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
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
 *
 * @version Berlioz 0.9.1 - 30 October 2011
 * @since Berlioz 0.9
 */
@Beta
public interface LifecycleListener {

  /**
   * This method is called when Berlioz starts by the BerliozInit servlet's initialisation method.
   *
   * <p>It is called after the system properties and global settings have been loaded.
   *
   * <p>This method should not throw any error. Errors must be handled internally and return <code>false</code>.
   *
   * <p>Feedback on this method may be reported on <code>System.out</code> and prefixed by
   * <code>[BERLIOZ_INIT] </code>. For example:
   * <pre>
   *   [BERLIOZ_INIT] Checking required properties - OK
   *   [BERLIOZ_INIT] Initialising index in /WEB-INF/index - OK
   *   [BERLIOZ_INIT] Initialising database in /WEB-INF/db - OK
   * </pre>
   *
   * @return <code>true</code> if the start was successful.
   */
  boolean start();

  /**
   * This method is called when Berlioz stops by the BerliozInit servlet's destroy method.
   *
   * <p>This method should not throw any error. Errors must be handled internally.
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

}
