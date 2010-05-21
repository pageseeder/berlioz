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

import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the HTTP Parameters as XML.
 * 
 * This content generator is only useful for when the XSLT needs to use the HTTP parameters to change the content.
 * 
 * <pre class="xml">{@code
 *   <http-parameters>
 *     <parameter name="[name-A]">[value-A]</parameter>
 *     <parameter name="[name-B]">[value-B1]</parameter>
 *     <parameter name="[name-B]">[value-B2]</parameter>
 *     <parameter name="[name-C]">[value-C]</parameter>
 *     <parameter name="[name-D]">[value-D]</parameter>
 *     <code class="comment"><!-- ... --></code>
 *   </http-parameters>
 * }</pre>
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 20 May 2010
 */
public final class GetHttpParameters extends ContentGeneratorBase implements ContentGenerator {

  /**
   * {@inheritDoc}
   */
  public void manage(ContentRequest req, XMLWriter xml) {
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    // write the http parameters
    xml.openElement("http-parameters", true);
    Enumeration<?> names = req.getParameterNames();
    while (names.hasMoreElements()) {
      String paramName = (String)names.nextElement();
      String[] values = req.getParameterValues(paramName);
      for (int i = 0; i < values.length; i++) {
        xml.openElement("parameter", false);
        xml.attribute("name", paramName);
        xml.writeText(values[i]);
        xml.closeElement();
      }
    }
    xml.closeElement(); // close http-parameters
  }

}
