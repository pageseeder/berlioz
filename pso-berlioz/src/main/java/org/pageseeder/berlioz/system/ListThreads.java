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

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * Returns information about the threads running in the system.
 *
 * <ul>
 *   <li><code>NEW</code>. The thread has been created but hasn't run yet.</li>
 *   <li><code>TERMINATED</code>. The thread has run to completion but hasn't been deleted yet by the JVM.</li>
 *   <li><code>RUNNABLE</code>. The thread is running.</li>
 *   <li><code>BLOCKED</code>. The thread is blocked waiting on a lock (such as in a synchronized block or method).</li>
 *   <li><code>WAITING</code>. The thread is waiting until another thread calls notify().</li>
 *   <li><code>TIMED_WAITING</code>. The thread is either waiting or in a sleep().</li>
 * </ul>
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code stacktraces}</dt>
 *   <dd>Optional. When {@code "true"}, includes each thread's stack trace. Defaults to
 *       {@code "false"}.</dd>
 *   <dt>{@code threadtime}</dt>
 *   <dd>Optional. When {@code "true"}, includes per-thread CPU/user time (if supported by the
 *       JVM). Defaults to {@code "false"}.</dd>
 * </dl>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <threads>
 *   <thread id="[id]" name="[name]" priority="[n]" state="[state]" alive="true" daemon="[true|false]" group="[group]" current="true">
 *     <times cpu="[n]" user="[n]" system="[n]"/>            <!-- only if threadtime=true and stacktraces=false -->
 *     <time cpu="[n]" user="[n]" system="[n]"/>             <!-- only if threadtime=true and stacktraces=true -->
 *     <stacktrace>                                          <!-- only if stacktraces=true -->
 *       <element class="[class]" filename="[file]" method="[method]" line="[n]"/>
 *       ...
 *     </stacktrace>
 *   </thread>
 *   ...
 * </threads>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "threads": [
 *     {
 *       "id": [id], "name": "[name]", "priority": [n], "state": "[state]", "alive": true,
 *       "daemon": [true|false], "group": "[group]", "current": "true",
 *       "times": {"cpu": [n], "user": [n], "system": [n]},   <!-- only if threadtime=true and stacktraces=false -->
 *       "time": {"cpu": [n], "user": [n], "system": [n]},    <!-- only if threadtime=true and stacktraces=true -->
 *       "stacktrace": [                                      <!-- only if stacktraces=true -->
 *         {"class": "[class]", "filename": "[file]", "method": "[method]", "line": [n]},
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public final class ListThreads implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    boolean stackTraces = "true".equals(req.getParameter("stacktraces"));
    boolean threadTime = "true".equals(req.getParameter("threadtime"));

    ThreadMXBean threadBean = Threads.getThreadMXBean();
    ThreadMXBean bean = threadTime ? threadBean : null;
    if (bean != null && !bean.isThreadCpuTimeSupported()) {
      bean = null;
    }

    out.startObject("threads");
    out.startArray("threads", OutputWriter.ContextOption.JSON_ONLY);
    ThreadInfo[] threads = Threads.getThreadInfo(threadBean, stackTraces ? Integer.MAX_VALUE : 0);
    for (ThreadInfo thread : threads) {
      if (thread != null) {
        if (stackTraces) {
          toOutput(thread, thread.getStackTrace(), bean, out);
        } else {
          toOutput(thread, bean, out);
        }
      }
    }
    out.endArray();
    out.endObject();
    return Response.ok();
  }

  private static void toOutput(ThreadInfo thread, @Nullable ThreadMXBean bean, OutputWriter out) {
    out.startObject("thread");
    Threads.writeThreadAttributes(out, thread, true);

    if (bean != null) {
      final long cpu = bean.getThreadCpuTime(thread.getThreadId());
      final long user = bean.getThreadUserTime(thread.getThreadId());
      out.startObject("times");
      out.field("cpu", cpu);
      out.field("user", user);
      out.field("system", cpu - user);
      out.endObject();
    }

    out.endObject();
  }

  private static void toOutput(ThreadInfo thread, StackTraceElement[] stacktrace, @Nullable ThreadMXBean bean, OutputWriter out) {
    out.startObject("thread");
    Threads.writeThreadAttributes(out, thread, true);

    if (bean != null) {
      final long cpu = bean.getThreadCpuTime(thread.getThreadId());
      final long user = bean.getThreadUserTime(thread.getThreadId());
      out.startObject("time");
      out.field("cpu", cpu);
      out.field("user", user);
      out.field("system", cpu - user);
      out.endObject();
    }

    Threads.writeStackTrace(out, stacktrace);

    out.endObject();
  }

}
