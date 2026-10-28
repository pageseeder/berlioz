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

import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.servlet.BerliozConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns generator execution statistics as XML or JSON.
 *
 * <p>On first instantiation, this generator registers a {@link StatisticsCollector} as the
 * global {@link org.pageseeder.berlioz.content.GeneratorListener} so that per-generator timing
 * data is collected across requests. If a listener is already registered (and is not the
 * collector), a warning is logged and statistics are not collected.
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code reset}</dt>
 *   <dd>Optional. When {@code "true"}, clears all accumulated statistics before returning the
 *       (now empty) output. Defaults to {@code "false"}.</dd>
 * </dl>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <statistics since="[iso8601-datetime]">
 *   <statistic generator="[class]" count="[n]"
 *              min-etag="[µs]"  max-etag="[µs]"  total-etag="[µs]"  avg-etag="[µs]"  avg-last-etag="[µs]"
 *              min-process="[µs]" max-process="[µs]" total-process="[µs]" avg-process="[µs]" avg-last-process="[µs]">
 *     <status ok="[n]" no_content="[n]" .../>
 *   </statistic>
 *   ...
 * </statistics>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "since": "[iso8601-datetime]",
 *   "statistics": [
 *     {"generator": "[class]", "count": [n],
 *      "minEtag": [µs], "minProcess": [µs], "maxEtag": [µs], "maxProcess": [µs],
 *      "totalEtag": [µs], "totalProcess": [µs], "avgEtag": [µs], "avgProcess": [µs],
 *      "avgLastEtag": [µs], "avgLastProcess": [µs],
 *      "status": {"ok": [n], "noContent": [n], ...}}
 *   ]
 * }
 * }</pre>
 *
 * <p>All time values are in microseconds. {@code avg-last-*} values are the average of the
 * most recent 10 invocations. The status counts are keyed by {@link
 * org.pageseeder.berlioz.content.ContentStatus} name in lower case ({@code no_content}) in XML,
 * and in camel case ({@code noContent}) in JSON.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetGeneratorStatistics"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.9.32
 */
public class GetGeneratorStatistics implements Generator {

  /**
   * A logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(GetGeneratorStatistics.class);

  /**
   * Will also create and bind a statistics collector to Berlioz.
   */
  public GetGeneratorStatistics() {
    GeneratorListener listener = BerliozConfig.getListener();
    StatisticsCollector collector = StatisticsCollector.getInstance();
    if (listener == null) {
      BerliozConfig.setListener(collector);
    } else if (collector != listener) {
      LOGGER.warn("Unable to initialise the Berlioz statistics for generators");
    }
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    StatisticsCollector collector = StatisticsCollector.getInstance();

    if ("true".equals(req.getParameter("reset", "false"))) {
      collector.clear();
    }

    collector.writeTo(out);
    return Response.ok();
  }

}
