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
package org.pageseeder.berlioz.system;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Properties;

import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Returns system properties as returned by the <code>System</code> class.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
@Beta
public final class GetSystemProperties implements ContentGenerator {

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    xml.openElement("system");

    // Enumerate the system properties
    Properties properties = System.getProperties();
    for (Entry<Object, Object> p : properties.entrySet()) {
      xml.openElement("property");
      xml.attribute("name",  (String)p.getKey());
      xml.attribute("value", (String)p.getValue());
      xml.closeElement();
    }

    xml.closeElement();
  }

}
