/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import java.io.IOException;
import java.util.Enumeration;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the Global properties as XML.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 21 May 2010
 */
public final class GetConfig extends ContentGeneratorBase implements ContentGenerator {

  /**
   * {@inheritDoc}
   */
  public void manage(ContentRequest req, XMLWriter xml) {
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    Enumeration<?> names = GlobalSettings.propertyNames();
    xml.openElement("properties", true);
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      xml.openElement("property", false);
      xml.attribute("name", name);
      xml.attribute("value", GlobalSettings.get(name));
      xml.closeElement();
    }
    xml.closeElement(); // close http-parameters
  }

}
