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

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.management.JMException;
import javax.management.ObjectName;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * A utility class for retrieving information about threads.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.0
 */
final class Threads {

  /**
   * The maximum number of threads listed.
   */
  static final int MAX_THREADS = 2000;

  /**
   * The maximum number of stack frames written for a thread.
   */
  static final int MAX_STACK_DEPTH = 256;

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
   * Returns the IDs of live threads, at most {@link #MAX_THREADS}.
   *
   * @param ids the IDs of all live threads
   *
   * @return The first {@link #MAX_THREADS} IDs.
   */
  static long[] limit(long[] ids) {
    return ids.length > MAX_THREADS ? Arrays.copyOf(ids, MAX_THREADS) : ids;
  }

  /**
   * Returns the information of the specified threads.
   *
   * <p>Stack traces are read one frame beyond {@link #MAX_STACK_DEPTH} so that a truncated
   * stack can be detected with {@link #isStackTruncated(ThreadInfo)}.
   *
   * @param bean        the thread management bean
   * @param ids         the IDs of the threads
   * @param stackTraces whether to include stack traces
   *
   * @return Information for each thread, {@code null} for threads that are no longer alive.
   */
  static @Nullable ThreadInfo[] getThreadInfo(ThreadMXBean bean, long[] ids, boolean stackTraces) {
    return bean.getThreadInfo(ids, stackTraces ? MAX_STACK_DEPTH + 1 : 0);
  }

  /**
   * Returns thread information by ID, including its stack trace and, when supported by the JVM,
   * the monitors and synchronizers it holds.
   *
   * @param bean the thread management bean
   * @param id   the ID of the thread
   *
   * @return The corresponding thread information or <code>null</code>.
   */
  static @Nullable ThreadInfo getThreadInfo(ThreadMXBean bean, long id) {
    return bean.getThreadInfo(new long[]{id}, bean.isObjectMonitorUsageSupported(),
        bean.isSynchronizerUsageSupported(), MAX_STACK_DEPTH + 1)[0];
  }

  /**
   * Returns the IDs of the threads that are deadlocked waiting for monitors and, when supported
   * by the JVM, ownable synchronizers.
   *
   * @param bean the thread management bean
   *
   * @return The IDs of deadlocked threads (may be empty).
   */
  static Set<Long> getDeadlockedThreads(ThreadMXBean bean) {
    long[] ids = bean.isSynchronizerUsageSupported()
        ? bean.findDeadlockedThreads()
        : bean.findMonitorDeadlockedThreads();
    Set<Long> deadlocked = new HashSet<>();
    if (ids != null) {
      for (long id : ids) deadlocked.add(id);
    }
    return deadlocked;
  }

  /**
   * Returns the bytes allocated by the specified threads.
   *
   * <p>The operation {@code getThreadAllocatedBytes} is invoked by name on the threading MBean
   * so that no class depends on {@code com.sun.management}.
   *
   * @param ids the IDs of the threads
   *
   * @return The allocated bytes for each thread (negative when not available),
   *         or {@code null} if the JVM does not provide them.
   */
  static long @Nullable[] getAllocatedBytes(long[] ids) {
    try {
      ObjectName name = new ObjectName(ManagementFactory.THREAD_MXBEAN_NAME);
      Object bytes = ManagementFactory.getPlatformMBeanServer()
          .invoke(name, "getThreadAllocatedBytes", new Object[]{ids}, new String[]{long[].class.getName()});
      return bytes instanceof long[] && ((long[]) bytes).length == ids.length ? (long[]) bytes : null;
    } catch (JMException | RuntimeException ex) {
      return null;
    }
  }

  /**
   * Indicates whether the stack trace of the thread exceeds {@link #MAX_STACK_DEPTH}.
   *
   * @param thread the thread information
   *
   * @return {@code true} if only part of the stack trace is written.
   */
  static boolean isStackTruncated(ThreadInfo thread) {
    return thread.getStackTrace().length > MAX_STACK_DEPTH;
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
   * Writes the thread contention, native and allocation fields, and the stack depth limit when
   * the stack trace is truncated.
   *
   * @param out            The output writer.
   * @param thread         The thread information to write.
   * @param deadlocked     Whether the thread is deadlocked.
   * @param allocatedBytes The bytes allocated by the thread, negative when not available.
   */
  static void writeThreadDetails(OutputWriter out, ThreadInfo thread, boolean deadlocked, long allocatedBytes) {
    out.field("blocked-count", thread.getBlockedCount());
    out.field("waited-count", thread.getWaitedCount());
    out.field("in-native", thread.isInNative());
    out.field("deadlocked", deadlocked);
    if (allocatedBytes >= 0) out.field("allocated-bytes", allocatedBytes);
    if (isStackTruncated(thread)) out.field("stack-limit", MAX_STACK_DEPTH);
  }

  /**
   * Writes the lock the thread is waiting for, with its owner if any.
   *
   * @param out    The output writer.
   * @param thread The thread information.
   */
  static void writeLock(OutputWriter out, ThreadInfo thread) {
    LockInfo lock = thread.getLockInfo();
    if (lock == null) return;
    out.startObject("lock");
    writeLockIdentity(out, lock);
    long owner = thread.getLockOwnerId();
    if (owner >= 0) {
      out.field("owner-id", owner);
      out.optionalField("owner-name", thread.getLockOwnerName());
    }
    out.endObject();
  }

  /**
   * Writes the monitors held by the thread with the stack depth at which each was locked.
   *
   * @param out    The output writer.
   * @param thread The thread information, read with locked monitors.
   */
  static void writeLockedMonitors(OutputWriter out, ThreadInfo thread) {
    out.startArray("locked-monitors");
    for (MonitorInfo monitor : thread.getLockedMonitors()) {
      out.startObject("monitor");
      writeLockIdentity(out, monitor);
      // Negative when locked by native code or beyond the stack read
      int depth = monitor.getLockedStackDepth();
      if (depth >= 0 && depth < MAX_STACK_DEPTH) out.field("depth", depth);
      out.endObject();
    }
    out.endArray();
  }

  /**
   * Writes the ownable synchronizers held by the thread.
   *
   * @param out    The output writer.
   * @param thread The thread information, read with locked synchronizers.
   */
  static void writeLockedSynchronizers(OutputWriter out, ThreadInfo thread) {
    out.startArray("locked-synchronizers");
    for (LockInfo synchronizer : thread.getLockedSynchronizers()) {
      out.startObject("synchronizer");
      writeLockIdentity(out, synchronizer);
      out.endObject();
    }
    out.endArray();
  }

  private static void writeLockIdentity(OutputWriter out, LockInfo lock) {
    out.field("class", lock.getClassName());
    out.field("identity-hash", Integer.toHexString(lock.getIdentityHashCode()));
  }

  /**
   * Writes a stack trace as a {@code stacktrace} array of {@code element} objects, in both
   * XML and JSON, up to {@link #MAX_STACK_DEPTH} frames.
   *
   * @param out        The output writer.
   * @param stacktrace The stack trace to write, or {@code null} to write nothing.
   */
  static void writeStackTrace(OutputWriter out, StackTraceElement @Nullable[] stacktrace) {
    if (stacktrace == null) return;
    out.startArray("stacktrace");
    int depth = Math.min(stacktrace.length, MAX_STACK_DEPTH);
    for (int i = 0; i < depth; i++) {
      StackTraceElement element = stacktrace[i];
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
