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
package org.pageseeder.berlioz.content;

import java.io.IOException;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.xmlwriter.XMLWriter;

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
