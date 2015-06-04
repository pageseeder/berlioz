/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.system;

/**
 * A utility class for retrieving information about threads.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
final class Threads {

  /** Utility class */
  private Threads() {
  }

  /**
   * Get Root group thread.
   *
   * @return The root group thread.
   */
  protected static ThreadGroup getRootThreadGroup() {
    ThreadGroup current = Thread.currentThread().getThreadGroup();
    ThreadGroup parent;
    while ((parent = current.getParent()) != null) {
      current = parent;
    }
    return current;
  }

  /**
   * Returns a thread by ID.
   *
   * @param id the ID of the thread
   * @return The corresponding thread instance or <code>null</code>.
   */
  protected static Thread getThread(long id) {
    ThreadGroup root = getRootThreadGroup();
    // load the threads in an array
    Thread[] threads = new Thread[root.activeCount()];
    while (root.enumerate(threads, true) == threads.length) {
      threads = new Thread[threads.length * 2];
    }
    // Look for the thread in the array
    for (Thread t : threads) {
      if (t.getId() == id) return t;
    }
    // not found
    return null;
  }
}
