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
package org.pageseeder.berlioz.generator;

import java.io.IOException;

import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.util.SHA256;
import org.pageseeder.xmlwriter.XMLWriter;

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
 * @version 0.13.1
 * @since 0.7
 */
public final class GetParameters implements ContentGenerator, Cacheable {

  @Override
  public String getETag(ContentRequest req) {
    StringBuilder hash = new StringBuilder("?");
    for (String name : req.parameterNames()) {
      for (String value : req.parameterValues(name)) {
        hash.append(name).append('=').append(value).append('&');
      }
    }
    return SHA256.hash(hash.toString());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("parameters", true);
    for (String name : req.parameterNames()) {
      for (String value : req.parameterValues(name)) {
        xml.openElement("parameter", false);
        xml.attribute("name", name);
        xml.writeText(value);
        xml.closeElement();
      }
    }
    xml.closeElement();
  }

}
