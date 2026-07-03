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

import java.util.Map.Entry;
import java.util.Properties;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns system properties as returned by the <code>System</code> class.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.32
 */
@Beta
public final class GetSystemProperties implements XmlGenerator {

  @Override
  public Response generate(Request req, XmlWriter xml) {
    xml.openElement("system");

    Properties properties = System.getProperties();
    for (Entry<Object, Object> p : properties.entrySet()) {
      xml.openElement("property");
      xml.attribute("name",  (String) p.getKey());
      xml.attribute("value", (String) p.getValue());
      xml.closeElement();
    }

    xml.closeElement();
    return Response.ok();
  }

}
