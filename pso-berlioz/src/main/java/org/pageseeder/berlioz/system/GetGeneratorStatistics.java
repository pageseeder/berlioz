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

import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.servlet.BerliozConfig;
import org.pageseeder.xmlwriter.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public class GetGeneratorStatistics implements ContentGenerator {

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
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    StatisticsCollector collector = StatisticsCollector.getInstance();

    if ("true".equals(req.getParameter("reset", "false"))) {
      collector.clear();
    }

    collector.toXML(xml);
  }

}
