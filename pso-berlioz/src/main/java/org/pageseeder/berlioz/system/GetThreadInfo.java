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

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * Returns information about a single JVM thread, including its stack trace and the locks it
 * holds.
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code id}</dt>
 *   <dd>Optional. The numeric thread ID to inspect; must be zero or positive. Defaults to the
 *       current (request-handling) thread when omitted. A negative value or a value that is
 *       not a valid long throws {@link org.pageseeder.berlioz.error.InvalidParameterException},
 *       which the framework maps to a {@code 400 Bad Request} problem response.</dd>
 * </dl>
 *
 * <h3>Limits and optional values</h3>
 * <p>At most {@value Threads#MAX_STACK_DEPTH} frames are written; a deeper stack states the
 * {@code stack-limit}. {@code allocated-bytes} is absent when the JVM does not provide it.
 * {@code lock} is the lock the thread is waiting for, with its owner when another thread holds
 * it. The {@code locked-monitors} and {@code locked-synchronizers} arrays are absent when the JVM
 * does not support monitoring them; a monitor's {@code depth} is the index of the stack frame
 * that locked it, absent when locked by native code or beyond the written frames.
 *
 * <h3>Returned XML</h3>
 * <p>When the thread is found:
 * <pre>{@code
 * <thread id="[id]" name="[name]" priority="[n]" state="[state]" alive="true" daemon="[true|false]" group="[group]"
 *         blocked-count="[n]" waited-count="[n]" in-native="[true|false]" deadlocked="[true|false]"
 *         allocated-bytes="[n]" stack-limit="[n]">
 *   <lock class="[class]" identity-hash="[hex]" owner-id="[id]" owner-name="[name]"/>
 *   <stacktrace>
 *     <element class="[class]" filename="[file]" method="[method]" line="[n]"/>
 *     ...
 *   </stacktrace>
 *   <locked-monitors>
 *     <monitor class="[class]" identity-hash="[hex]" depth="[n]"/>
 *     ...
 *   </locked-monitors>
 *   <locked-synchronizers>
 *     <synchronizer class="[class]" identity-hash="[hex]"/>
 *     ...
 *   </locked-synchronizers>
 * </thread>
 * }</pre>
 * <p>When no thread with the given ID exists:
 * <pre>{@code <no-thread id="[id]"/>}</pre>
 *
 * <h3>Returned JSON</h3>
 * <p>When the thread is found:
 * <pre>{@code
 * {
 *   "id": [id], "name": "[name]", "priority": [n], "state": "[state]", "alive": true,
 *   "daemon": [true|false], "group": "[group]",
 *   "blockedCount": [n], "waitedCount": [n], "inNative": [true|false], "deadlocked": [true|false],
 *   "allocatedBytes": [n], "stackLimit": [n],
 *   "lock": {"class": "[class]", "identityHash": "[hex]", "ownerId": [id], "ownerName": "[name]"},
 *   "stacktrace": [
 *     {"class": "[class]", "filename": "[file]", "method": "[method]", "line": [n]},
 *     ...
 *   ],
 *   "lockedMonitors": [{"class": "[class]", "identityHash": "[hex]", "depth": [n]}, ...],
 *   "lockedSynchronizers": [{"class": "[class]", "identityHash": "[hex]"}, ...]
 * }
 * }</pre>
 * <p>When no thread with the given ID exists:
 * <pre>{@code {"id": [id]} }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetThreadInfo"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.9.32
 */
@Beta
public final class GetThreadInfo implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    long threadId = req.parameter("id").asLong().inRange(0L, Long.MAX_VALUE)
        .optional(Thread.currentThread().getId());

    ThreadMXBean bean = Threads.getThreadMXBean();
    ThreadInfo thread = Threads.getThreadInfo(bean, threadId);
    if (thread != null) {
      toOutput(thread, bean, out);
    } else {
      out.startObject("no-thread");
      out.field("id", threadId);
      out.endObject();
    }
    return Response.ok();
  }

  /**
   * Writes a single thread with its stack trace and locks.
   *
   * @param thread The thread information to serialize.
   * @param bean   The thread management bean.
   * @param out    The output writer.
   */
  private static void toOutput(ThreadInfo thread, ThreadMXBean bean, OutputWriter out) {
    long id = thread.getThreadId();
    long[] allocated = Threads.getAllocatedBytes(new long[]{id});
    out.startObject("thread");
    Threads.writeThreadAttributes(out, thread, false);
    Threads.writeThreadDetails(out, thread, Threads.getDeadlockedThreads(bean).contains(id),
        allocated != null ? allocated[0] : -1);
    Threads.writeLock(out, thread);
    Threads.writeStackTrace(out, thread.getStackTrace());
    if (bean.isObjectMonitorUsageSupported()) Threads.writeLockedMonitors(out, thread);
    if (bean.isSynchronizerUsageSupported()) Threads.writeLockedSynchronizers(out, thread);
    out.endObject();
  }

}
