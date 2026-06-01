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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.xmlwriter.XMLWriter;

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
 * <p>On success, writes a single {@code <sample>} element:</p>
 * <pre>{@code
 * <sample interval="100" cpu="12" user="9" system="3"/>
 * }</pre>
 * <dl>
 *   <dt>{@code interval}</dt><dd>The actual sampling interval used, in milliseconds.</dd>
 *   <dt>{@code cpu}</dt><dd>Total CPU usage as a percentage (user + system time).</dd>
 *   <dt>{@code user}</dt><dd>User-mode CPU usage as a percentage.</dd>
 *   <dt>{@code system}</dt><dd>Kernel-mode CPU usage as a percentage (cpu − user).</dd>
 * </dl>
 *
 * <p>On error, the response status is set to {@code 400 Bad Request} (invalid parameters) or
 * {@code 503 Service Unavailable} (sampling interrupted), with an XML comment describing the
 * cause.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
public final class GetCPUTime implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    int interval = req.getIntParameter("interval", 100);

    // Check that the interval is positive
    if (interval <= 0) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      xml.writeComment("Interval must be strictly positive");
      return;
    }

    long threadId = -1L;
    try {
      threadId = req.getLongParameter("thread", -1);
    } catch (NumberFormatException ex) {
      req.setStatus(ContentStatus.BAD_REQUEST);
      xml.writeComment("Invalid thread ID");
      return;
    }

    try {
      ThreadMXBean bean = ManagementFactory.getThreadMXBean();
      // measure
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

      // Calculate
      long time = end.time() - start.time();
      long user = end.user() - start.user();
      long cpu = end.cpu() - start.cpu();

      // Write XML
      xml.openElement("sample");
      xml.attribute("interval", Long.toString(interval));
      xml.attribute("cpu", Long.toString(cpu*100 / time));
      xml.attribute("user", Long.toString(user*100 / time));
      xml.attribute("system", Long.toString((cpu - user)*100 / time));
      xml.closeElement();

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      req.setStatus(ContentStatus.SERVICE_UNAVAILABLE);
      xml.writeComment("CPU time sampling was interrupted");
    }
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
