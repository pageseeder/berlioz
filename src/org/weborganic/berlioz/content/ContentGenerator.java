/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.io.IOException;

import org.weborganic.berlioz.BerliozException;

import com.topologi.diffx.xml.XMLWriter;

/**
 * An interface to generate XML content.
 *
 * <p>Each content generator performs a particular function and should write the results onto
 * the given XML writer.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.3 - 8 July 2010
 * @since Berlioz 0.6
 */
public interface ContentGenerator {

  /**
   * Produces the actual content.
   *
   * <p>This is the main method of this interface, it should:
   * <ul>
   *   <li>perform one specific function</li>
   *   <li>write the result on the XML writer</li>
   * </ul>
   *
   * <p>Implementation should specify which attribute or parameters are used or required.
   *
   * @param req The content request.
   * @param xml The XML output.
   *
   * @throws BerliozException If an exception is thrown, it should be wrapped into a Berlioz
   *                         exception in order to provide additional details.
   *
   * @throws IOException    If an I/O error occurs while writing to the XML writer.
   */
  void process(ContentRequest req, XMLWriter xml)
    throws BerliozException, IOException;

}
