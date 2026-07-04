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

import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.servlet.BerliozConfig;
import org.pageseeder.berlioz.xml.XmlWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Returns generator execution statistics as XML.
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
 *     <status ok="[n]" not-modified="[n]" .../>
 *   </statistic>
 *   ...
 * </statistics>
 * }</pre>
 * <p>All time values are in microseconds. {@code avg-last-*} values are the average of the
 * most recent 10 invocations.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetGeneratorStatistics"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
public class GetGeneratorStatistics implements XmlGenerator {

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
  public Response generate(Request req, XmlWriter xml) {
    StatisticsCollector collector = StatisticsCollector.getInstance();

    if ("true".equals(req.getParameter("reset", "false"))) {
      collector.clear();
    }

    collector.toXml(xml);
    return Response.ok();
  }

}
