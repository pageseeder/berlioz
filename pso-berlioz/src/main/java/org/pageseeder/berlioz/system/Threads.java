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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * A utility class for retrieving information about threads.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.14.0
 */
final class Threads {

  private static final String NO_THREAD_GROUP = "(No thread group)";

  /** Utility class */
  private Threads() {
  }

  /**
   * Returns the thread management bean.
   *
   * @return The thread management bean.
   */
  static ThreadMXBean getThreadMXBean() {
    return ManagementFactory.getThreadMXBean();
  }

  /**
   * Returns all live thread information.
   *
   * @param bean      the thread management bean
   * @param maxDepth  the maximum stack trace depth
   *
   * @return Information for all live threads.
   */
  static ThreadInfo[] getThreadInfo(ThreadMXBean bean, int maxDepth) {
    return bean.getThreadInfo(bean.getAllThreadIds(), maxDepth);
  }

  /**
   * Returns thread information by ID.
   *
   * @param id the ID of the thread
   * @return The corresponding thread information or <code>null</code>.
   */
  static @Nullable ThreadInfo getThreadInfo(long id) {
    return getThreadMXBean().getThreadInfo(id, Integer.MAX_VALUE);
  }

  /**
   * Returns the compatibility value for the legacy group attribute.
   *
   * @return The thread group name placeholder.
   */
  static String threadGroupName() {
    return NO_THREAD_GROUP;
  }

  /**
   * Writes the common thread identity fields shared by {@code GetThreadInfo} and
   * {@code ListThreads}.
   *
   * @param out         The output writer.
   * @param thread      The thread information to write.
   * @param markCurrent Whether to flag the entry with {@code current="true"} when it
   *                    corresponds to the calling thread.
   */
  static void writeThreadAttributes(OutputWriter out, ThreadInfo thread, boolean markCurrent) {
    out.field("id", thread.getThreadId());
    out.field("name", thread.getThreadName());
    out.field("priority", thread.getPriority());
    out.field("state", thread.getThreadState().name());
    out.field("alive", true);
    out.field("daemon", thread.isDaemon());
    out.field("group", threadGroupName());
    if (markCurrent && thread.getThreadId() == Thread.currentThread().getId()) {
      out.field("current", "true");
    }
  }

  /**
   * Writes a stack trace as a {@code stacktrace} array of {@code element} objects, in both
   * XML and JSON.
   *
   * @param out        The output writer.
   * @param stacktrace The stack trace to write, or {@code null} to write nothing.
   */
  static void writeStackTrace(OutputWriter out, StackTraceElement @Nullable[] stacktrace) {
    if (stacktrace == null) return;
    out.startArray("stacktrace");
    for (StackTraceElement element : stacktrace) {
      out.startObject("element");
      out.field("class", element.getClassName());
      out.optionalField("filename", element.getFileName());
      out.optionalField("method", element.getMethodName());
      int line = element.getLineNumber();
      out.optionalField("line", line >= 0 ? line : null);
      out.endObject();
    }
    out.endArray();
  }

}
