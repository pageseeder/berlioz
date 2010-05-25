/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import java.io.IOException;

import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns information about the specified file.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 20 May 2010
 */
public final class FileInfo extends ContentGeneratorBase implements ContentGenerator {

  /**
   * {@inheritDoc}
   */
  public void manage(ContentRequest req, XMLWriter xml) {
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
  }

}
