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
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.util.MD5;
import org.weborganic.berlioz.xml.XMLCopy;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the current service configuration as XML.
 * 
 * <p>This content generator is mostly useful for developers to see how the services are configured.
 * 
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 * 
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 * 
 * <h3>Returned XML</h3>
 * <p>This generator contains the <code>/WEB-INF/config/services.xml</code> used by Berlioz to load
 * its services.</p>
 * <pre>{@code <services version="1.0"> ... </services>}</pre>
 * <p>The formatting of the XML may differ from the actual files as it is parsed before being 
 * returned; the XML declaration and comments are stripped.</p>
 *
 * <h3>Error Handling</h3>
 * <p>Should there be any problem parsing or reading the file, the XML returned will be:
 * <pre>{@code <no-data error="[error]" details="[error-details]"/>}</pre>
 * <p>The error details are only shown if available. 
 * 
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.weborganic.berlioz.generator.GetServices" 
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator uses a weak etag based on the name, length and last modified date of the file.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version Berlioz 0.9.0 - 13 October 2011
 * @since Berlioz 0.8
 */
public final class GetServices implements ContentGenerator, Cacheable {

  /**
   * Default location of the services.
   */
  private static final String SERVICES = "config/services.xml";

  /**
   * {@inheritDoc}
   */
  public String getETag(ContentRequest req) {
    File services = new File(GlobalSettings.getRepository(), SERVICES);
    return MD5.hash(services.length()+"x"+services.lastModified());
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    File services = new File(GlobalSettings.getRepository(), SERVICES);

    // All good, print to the XML stream
    if (services.exists()) {
      XMLCopy.copyTo(services, xml);
    }
  }

}
