/*
 * Copyright 2015 Allette Systems (Australia)
 * http://www.allette.com.au
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.pageseeder.berlioz.system;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A utility class for retrieving information about threads.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
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
    if (current == null) throw new IllegalStateException("The current thread has died?");
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
  protected static @Nullable Thread getThread(long id) {
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

  protected static String toThreadGroupName(Thread thread) {
    ThreadGroup group = thread.getThreadGroup();
    if (group == null) return "(No thread group)";
    String groupName = group.getName();
    return groupName != null? groupName : "(No thread group Name)";
  }

}
