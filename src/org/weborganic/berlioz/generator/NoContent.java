/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Generates no content.
 * 
 * <p>This content generator is useful for when the XML header already contains
 * enough information for the purpose of application.
 * 
 * <p>This generator can also be useful to be picked up by the XSLT even if no content is required.
 * 
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 * 
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 * 
 * <h3>Returned XML</h3>
 * <p>This generator does not have any content, so the XML content is always empty.
 * <p>Since Berlioz always wraps generators' content, the final XML is always: 
 * <pre>{@code
 * <content generator="org.weborganic.berlioz.generator.NoContent"
 *               name="[name]" target="[target]" status="ok"/>}</pre>
 * 
 * <p><i>Note: since this generator does produce any data, the return status is always 
 * <code>ok</code>.</i></p>
 * 
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.weborganic.berlioz.generator.NoContent" 
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This Etag for this generator is always <code>"nocontent"</code>.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 1 June 2010
 */
public final class NoContent extends ContentGeneratorBase implements ContentGenerator, Cacheable {

  /**
   * Always returns the <code>"nocontent"</code>.
   * 
   * {@inheritDoc}
   */
  public String getETag(ContentRequest req) {
    return "nocontent";
  }

  /**
   * Do nothing.
   * 
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) {
  }

}
