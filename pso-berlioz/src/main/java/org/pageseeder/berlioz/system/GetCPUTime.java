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
import java.lang.management.ThreadMXBean;

import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.error.Problems;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * A content generator that measures CPU usage over a short sampling interval.
 *
 * <p>It takes two snapshots of thread CPU and user time separated by a configurable sleep
 * interval, then expresses the delta as a percentage of elapsed wall-clock time. The result
 * covers either all JVM threads combined or a single identified thread.</p>
 *
 * <h2>Parameters</h2>
 * <dl>
 *   <dt>{@code interval}</dt>
 *   <dd>Sampling duration in milliseconds (default: {@code 100}). Must be strictly positive.</dd>
 *   <dt>{@code thread}</dt>
 *   <dd>Thread ID to measure (default: {@code -1}, meaning all threads). Omit or pass {@code -1}
 *       to aggregate across the whole JVM, excluding the current request thread.</dd>
 * </dl>
 *
 * <h2>Output</h2>
 * <p>On success, writes a single {@code <sample>} element (or the equivalent JSON object):</p>
 * <pre>{@code
 * <sample interval="100" cpu="12" user="9" system="3"/>
 * }</pre>
 * <pre>{@code
 * {"interval": 100, "cpu": 12, "user": 9, "system": 3}
 * }</pre>
 * <dl>
 *   <dt>{@code interval}</dt><dd>The actual sampling interval used, in milliseconds.</dd>
 *   <dt>{@code cpu}</dt><dd>Total CPU usage as a percentage (user + system time).</dd>
 *   <dt>{@code user}</dt><dd>User-mode CPU usage as a percentage.</dd>
 *   <dt>{@code system}</dt><dd>Kernel-mode CPU usage as a percentage (cpu − user).</dd>
 * </dl>
 *
 * <p>On error, an RFC 9457 problem response is returned: {@code 400 Bad Request} (invalid
 * {@code interval}) or {@code 503 Service Unavailable} (sampling interrupted).</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
public final class GetCPUTime implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    int interval = req.parameter("interval").asInt().inRange(1, 5000).optional(100);
    long threadId = req.parameter("thread").asLong().defaultValue(-1L);

    try {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      Sample start;
      Sample end;
      if (threadId == -1L) {
        long current = Thread.currentThread().getId();
        start = global(bean, current);
        Thread.sleep(interval);
        end = global(bean, current);
      } else {
        start = single(bean, threadId);
        Thread.sleep(interval);
        end = single(bean, threadId);
      }

      long time = end.time() - start.time();
      long user = end.user() - start.user();
      long cpu = end.cpu() - start.cpu();

      out.startObject("sample");
      out.field("interval", interval);
      out.field("cpu", cpu * 100 / time);
      out.field("user", user * 100 / time);
      out.field("system", (cpu - user) * 100 / time);
      out.endObject();

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return Response.problem(Problems.forHttpError(503, "CPU time sampling was interrupted"));
    }
    return Response.ok();
  }

  /**
   * Captures a CPU/user time snapshot across all live JVM threads, excluding the calling thread.
   *
   * <p>Threads that have died since {@link ThreadMXBean#getAllThreadIds()} was called return
   * {@code -1} for their times and are silently skipped.</p>
   *
   * @param bean    the thread management bean used to query per-thread CPU times
   * @param current the ID of the calling thread, excluded from the aggregate to avoid
   *                skewing results with the overhead of this measurement itself
   * @return a snapshot holding the summed CPU and user times (in nanoseconds) across all
   *         included threads, plus the wall-clock time at which the snapshot was taken
   */
  private Sample global(ThreadMXBean bean, long current) {
    long cpuTotal = 0L;
    long userTotal = 0L;
    final long[] threadIds = bean.getAllThreadIds();
    for (long id : threadIds) {
      if (id != current) {
        final long cpu = bean.getThreadCpuTime(id);
        final long user = bean.getThreadUserTime(id);
        if (cpu != -1 && user != -1) {
          cpuTotal += cpu;
          userTotal += user;
        }
      }
    }
    return new Sample(cpuTotal, userTotal);
  }

  /**
   * Captures a CPU/user time snapshot for a single thread.
   *
   * <p>If the thread has terminated between the call and the query, both times will be
   * {@code -1}; in that case a zeroed sample is returned so downstream delta calculations
   * remain valid.</p>
   *
   * @param bean the thread management bean used to query per-thread CPU times
   * @param id   the ID of the thread to measure
   * @return a snapshot holding the thread's CPU and user times (in nanoseconds), or a zeroed
   *         snapshot if the thread is no longer alive
   */
  private Sample single(ThreadMXBean bean, long id) {
    final long cpu = bean.getThreadCpuTime(id);
    final long user = bean.getThreadUserTime(id);
    // The thread has died!
    if (cpu == -1 || user == -1) return new Sample(0L, 0L);
    else return new Sample(cpu, user);
  }

  /**
   * An immutable point-in-time snapshot of CPU usage for one or more threads.
   *
   * <p>The wall-clock timestamp ({@link #time()}) is captured at construction so that two
   * snapshots taken before and after a sleep interval can be compared to compute CPU
   * percentages relative to elapsed real time.</p>
   */
  private static class Sample {
    private final long time = System.nanoTime();
    private final long cpu;
    private final long user;
    public Sample(long cpu, long user) {
      this.cpu = cpu;
      this.user = user;
    }
    public long cpu() {
      return this.cpu;
    }
    public long user() {
      return this.user;
    }
    public long time() {
      return this.time;
    }
  }

}
