/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.system;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.GeneratorListener;
import org.weborganic.berlioz.servlet.BerliozConfig;

import com.topologi.diffx.xml.XMLWriter;

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
    if (BerliozConfig.getListener() == null) {
      BerliozConfig.setListener(collector);
    } else if (collector != listener) {
      LOGGER.warn("Unable to initialise the Berlioz statistics for generators");
    }
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws BerliozException, IOException {
    StatisticsCollector collector = StatisticsCollector.getInstance();

    if ("true".equals(req.getParameter("reset", "false"))) {
      collector.clear();
    }

    collector.toXML(xml);
  }

}
