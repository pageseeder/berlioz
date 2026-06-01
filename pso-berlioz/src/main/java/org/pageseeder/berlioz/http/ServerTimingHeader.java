/*
 * Copyright 2019 Allette Systems (Australia)
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
package org.pageseeder.berlioz.http;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Server timing information.
 *
 * @see <a href="https://www.w3.org/TR/server-timing/">W3: Server Timing</a>
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Server-Timing">MDN: Server Timing</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.11.5
 */
public final class ServerTimingHeader {

  private final List<PerformanceServerTiming> timings = new ArrayList<>();

  /**
   * Creates an empty {@code Server-Timing} header.
   */
  public ServerTimingHeader() {
    // timings field is initialized inline
  }

  /**
   * Adds an existing server-timing metric to this header.
   *
   * @param timing the metric to add.
   */
  public void add(PerformanceServerTiming timing) {
    this.timings.add(timing);
  }

  /**
   * Adds a metric without a description or duration.
   *
   * @param name the metric name.
   */
  public void addMetric(String name) {
    this.timings.add(new PerformanceServerTiming(name, -1));
  }

  /**
   * Adds a metric with a duration in milliseconds.
   *
   * @param name the metric name.
   * @param durationMillis the metric duration in milliseconds.
   */
  public void addMetric(String name, double durationMillis) {
    this.timings.add(new PerformanceServerTiming(name, durationMillis));
  }

  /**
   * Adds a metric with a description and no duration.
   *
   * @param name the metric name.
   * @param description the server-specified metric description.
   */
  public void addMetric(String name, String description) {
    this.timings.add(new PerformanceServerTiming(name, description,-1));
  }

  /**
   * Adds a metric with a description and duration in milliseconds.
   *
   * @param name the metric name.
   * @param description the server-specified metric description.
   * @param durationMillis the metric duration in milliseconds.
   */
  public void addMetric(String name, String description, double durationMillis) {
    this.timings.add(new PerformanceServerTiming(name, description,durationMillis));
  }

  /**
   * Adds a metric with a duration in nanoseconds.
   *
   * @param name the metric name.
   * @param durationNano the metric duration in nanoseconds.
   */
  public void addMetricNano(String name, double durationNano) {
    this.timings.add(new PerformanceServerTiming(name, durationNano*.000001));
  }

  /**
   * Adds a metric with a description and duration in nanoseconds.
   *
   * @param name the metric name.
   * @param description the server-specified metric description.
   * @param durationNano the metric duration in nanoseconds.
   */
  public void addMetricNano(String name, String description, long durationNano) {
    this.timings.add(new PerformanceServerTiming(name, description, durationNano*.000001));
  }

  /**
   * Returns the value for the {@code Server-Timing} header.
   *
   * @return a comma-separated list of metrics.
   */
  public String toValue() {
    return this.timings.stream().map(PerformanceServerTiming::toHeaderString).collect(Collectors.joining(", "));
  }

  /**
   * Adds a single server-timing metric with a nanosecond duration to the response.
   *
   * @param response the HTTP servlet response to update.
   * @param name the metric name.
   * @param description the server-specified metric description.
   * @param durationNano the metric duration in nanoseconds.
   */
  public static void addMetricNano(HttpServletResponse response, String name, String description, long durationNano) {
    PerformanceServerTiming metric = new PerformanceServerTiming(name, description,durationNano*.000001);
    response.addHeader(HttpHeaders.SERVER_TIMING, metric.toHeaderString());
  }

  /**
   * Adds this server-timing header value to the response.
   *
   * @param response the HTTP servlet response to update.
   */
  public void addHeaderTo(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SERVER_TIMING, this.toValue());
  }

}
