/*
 * Copyright 2026 Allette Systems (Australia)
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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;

/**
 * Returns a sample of the cumulative CPU counters of the JVM as XML or JSON, without waiting.
 *
 * <p>Clients compute the usage between two successive samples, so it covers all the time between
 * them. Unlike {@link GetCPUTime}, this generator never sleeps on the request thread.
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <cpu-sample processors="8" uptime-ms="123456" process-time-ns="5230000000"
 *             gc-time-ms="120" system-load="0.15" load-average="1.25"/>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {"processors": 8, "uptimeMs": 123456, "processTimeNs": 5230000000,
 *  "gcTimeMs": 120, "systemLoad": 0.15, "loadAverage": 1.25}
 * }</pre>
 *
 * <dl>
 *   <dt>{@code processors}</dt><dd>Number of processors available to the JVM.</dd>
 *   <dt>{@code uptime-ms}</dt><dd>The time of the sample from the JVM's start, so that the
 *     elapsed time between two samples does not depend on the client's clock.</dd>
 *   <dt>{@code process-time-ns}</dt><dd>CPU time used by the whole process since it started,
 *     including threads that have ended (JDK-specific {@code ProcessCpuTime}). Its increase over
 *     the elapsed time of all processors is the process's share of the CPU.</dd>
 *   <dt>{@code gc-time-ms}</dt><dd>Accumulated collection time of all garbage collectors, an
 *     elapsed time rather than CPU time.</dd>
 *   <dt>{@code system-load}</dt><dd>Recent CPU usage of the whole machine from 0 to 1
 *     ({@code CpuLoad}, or {@code SystemCpuLoad} before Java 14). Not cumulative.</dd>
 *   <dt>{@code load-average}</dt><dd>System load average for the last minute; not available on
 *     Windows.</dd>
 * </dl>
 *
 * <p>JDK-specific metrics are read by MBean attribute name, never through
 * {@code com.sun.management}. A value that the JVM does not provide is absent, never zero.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetCpuSample"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
@Beta
public final class GetCpuSample implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
    sample(os.getAvailableProcessors(), ManagementFactory.getRuntimeMXBean().getUptime(),
        OptionalMetrics.platform(), ManagementFactory.getGarbageCollectorMXBeans(),
        os.getSystemLoadAverage(), out);
    return Response.ok();
  }

  /**
   * Writes the sample from the specified values.
   *
   * @param processors  The number of available processors
   * @param uptime      The uptime of the JVM in milliseconds
   * @param metrics     The optional metrics of the operating system
   * @param collectors  The garbage collectors
   * @param loadAverage The system load average, negative when not available
   * @param out         The output writer
   */
  static void sample(int processors, long uptime, OptionalMetrics metrics,
      List<GarbageCollectorMXBean> collectors, double loadAverage, OutputWriter out) {
    Long processTime = metrics.count("ProcessCpuTime");
    Long gcTime = collectionTime(collectors);
    // CpuLoad from Java 14, SystemCpuLoad before (deprecated since)
    Double system = metrics.fraction("CpuLoad", "SystemCpuLoad");
    out.startObject("cpu-sample");
    out.field("processors", processors);
    out.field("uptime-ms", uptime);
    if (processTime != null) out.field("process-time-ns", processTime.longValue());
    if (gcTime != null) out.field("gc-time-ms", gcTime.longValue());
    if (system != null) out.field("system-load", round(system));
    // Negative when not available
    if (loadAverage >= 0) out.field("load-average", round(loadAverage));
    out.endObject();
  }

  /**
   * @param collectors The garbage collectors
   * @return the collection time of the collectors that report one, or {@code null} if none does.
   */
  static @Nullable Long collectionTime(List<GarbageCollectorMXBean> collectors) {
    Long total = null;
    for (GarbageCollectorMXBean collector : collectors) {
      if (!collector.isValid()) continue;
      // -1 when undefined
      long time = collector.getCollectionTime();
      if (time >= 0) total = (total == null ? 0 : total) + time;
    }
    return total;
  }

  /**
   * @return the value rounded to three decimals.
   */
  private static double round(double value) {
    return Math.round(value * 1000) / 1000.0;
  }

}
