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
 * @version Berlioz 0.11.5
 * @since Berlioz 0.11.5
 */
public final class ServerTimingHeader {

  private final List<PerformanceServerTiming> _timings = new ArrayList<>();

  public void add(PerformanceServerTiming timing) {
    this._timings.add(timing);
  }

  public void addMetric(String name) {
    this._timings.add(new PerformanceServerTiming(name, -1));
  }

  public void addMetric(String name, double durationMillis) {
    this._timings.add(new PerformanceServerTiming(name, durationMillis));
  }

  public void addMetric(String name, String description) {
    this._timings.add(new PerformanceServerTiming(name, description,-1));
  }

  public void addMetric(String name, String description, double durationMillis) {
    this._timings.add(new PerformanceServerTiming(name, description,durationMillis));
  }

  public void addMetricNano(String name, double durationNano) {
    this._timings.add(new PerformanceServerTiming(name, durationNano*.000001));
  }

  public void addMetricNano(String name, String description, long durationNano) {
    this._timings.add(new PerformanceServerTiming(name, description, durationNano*.000001));
  }

  public String toValue() {
    return this._timings.stream().map(PerformanceServerTiming::toHeaderString).collect(Collectors.joining(", "));
  }

  public static void addMetricNano(HttpServletResponse response, String name, String description, long durationNano) {
    PerformanceServerTiming metric = new PerformanceServerTiming(name, description,durationNano*.000001);
    response.addHeader(HttpHeaders.SERVER_TIMING, metric.toHeaderString());
  }

  public void addHeaderTo(HttpServletResponse response) {
    response.addHeader(HttpHeaders.SERVER_TIMING, this.toValue());
  }

}
