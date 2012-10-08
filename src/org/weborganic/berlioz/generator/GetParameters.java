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

import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.util.MD5;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the HTTP Parameters as XML.
 *
 * <p>This content generator is only useful for when the XSLT needs to use the parameters to change
 * the content or for debugging.
 *
 * <pre>{@code
 *   <parameters>
 *     <parameter name="[name-A]">[value-A]</parameter>
 *     <parameter name="[name-B]">[value-B1]</parameter>
 *     <parameter name="[name-B]">[value-B2]</parameter>
 *     <parameter name="[name-C]">[value-C]</parameter>
 *     <parameter name="[name-D]">[value-D]</parameter>
 *     <code class="comment"><!-- ... --></code>
 *   </parameters>
 * }</pre>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.0 - 13 October 2011
 * @since Berlioz 0.7
 */
public final class GetParameters implements ContentGenerator, Cacheable {

  /**
   * Returns an MD5 Value of the query string.
   *
   * {@inheritDoc}
   */
  @Override
  public String getETag(ContentRequest req) {
    StringBuilder hash = new StringBuilder("?");
    Enumeration<String> names = req.getParameterNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      String[] values = req.getParameterValues(name);
      for (String value : values) {
        hash.append(name).append('=').append(value).append('&');
      }
    }
    // Returns a hash of the query string
    return MD5.hash(hash.toString());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    // write the http parameters
    xml.openElement("parameters", true);
    Enumeration<String> names = req.getParameterNames();
    while (names.hasMoreElements()) {
      String paramName = names.nextElement();
      String[] values = req.getParameterValues(paramName);
      for (String value : values) {
        xml.openElement("parameter", false);
        xml.attribute("name", paramName);
        xml.writeText(value);
        xml.closeElement();
      }
    }
    xml.closeElement(); // close parameters
  }

}
