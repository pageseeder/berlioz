/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import java.io.File;
import java.io.IOException;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.xml.XMLExtractorHelper;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the current service configuration as XML.
 * 
 * <p>This content generator is mostly useful for developers to see how the services are configured.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 31 May 2010
 */
public final class GetServices extends ContentGeneratorBase implements ContentGenerator, Cacheable {

  /**
   * {@inheritDoc}
   */
  public String getETag(ContentRequest req) {
    File services = new File(GlobalSettings.getRepository(), "config/services.xml");
    return services.length()+"x"+services.lastModified();
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    File services = new File(GlobalSettings.getRepository(), "config/services.xml");

    // All good, print to the XML stream
    if (services.exists()) {
      XMLExtractorHelper.extract(services, xml, true);
    }
  }

}
