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
import java.util.Enumeration;

import org.weborganic.berlioz.GlobalSettings;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentGeneratorBase;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.util.MD5;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Returns the global properties as XML.
 * 
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 * 
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 * 
 * <h3>Returned XML</h3>
 * <p>This generator returns a flat list of the global properties as XML as below: 
 * <pre>{@code
 * <properties source="[source]">
 *   <property name="[nameA]" value="[valueA]"/>
 *   <property name="[nameB]" value="[valueB]"/>
 *   <property name="[nameC]" value="[valueC]"/>
 *   ...
 * </properties>
 * }</pre>
 * 
 * <h3>Etag</h3>
 * <p>This generator uses a weak etag based on the name, length and last modified date of the
 * properties file being loaded.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 4 June 2011
 */
public final class GetGlobalConfig extends ContentGeneratorBase implements ContentGenerator, Cacheable {

  /**
   * Returns a ETag based on the last modified date and length of the global propertied file.
   * 
   * {@inheritDoc}
   */
  public String getETag(ContentRequest req) {
    File global = GlobalSettings.getPropertiesFile();
    return MD5.hash(global.length()+"x"+global.lastModified());
  }

  /**
   * {@inheritDoc}
   */
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    Enumeration<?> names = GlobalSettings.propertyNames();
    xml.openElement("properties", true);
    xml.attribute("source", GlobalSettings.getPropertiesFile().getName());
    while (names.hasMoreElements()) {
      String name = (String)names.nextElement();
      xml.openElement("property", false);
      xml.attribute("name", name);
      xml.attribute("value", GlobalSettings.get(name));
      xml.closeElement();
    }
    xml.closeElement();
  }

}
