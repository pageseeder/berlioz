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
import org.pageseeder.berlioz.content.Request;
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
 * <p>To prevent abuse, the following limits are applied: at most {@value #MAX_PARAMETERS} distinct
 * parameter names, at most {@value #MAX_VALUES} values per parameter, parameter names longer than
 * {@value #MAX_NAME_LENGTH} characters are skipped, and values longer than
 * {@value #MAX_VALUE_LENGTH} characters are truncated (the element will carry a
 * {@code truncated="true"} attribute). The same limits apply when computing the ETag to keep
 * cache behaviour consistent with the output.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.7
 */
public final class GetParameters implements ContentGenerator, Cacheable {

  private static final int MAX_PARAMETERS = 50;
  private static final int MAX_VALUES = 20;
  private static final int MAX_NAME_LENGTH = 100;
  private static final int MAX_VALUE_LENGTH = 2_000;

  @Override
  public String getETag(Request req) {
    StringBuilder hash = new StringBuilder("?");
    int paramCount = 0;
    for (String name : req.parameterNames()) {
      if (++paramCount > MAX_PARAMETERS) break;
      if (name.length() <= MAX_NAME_LENGTH) appendValuesToHash(hash, name, req.parameterValues(name));
    }
    return SHA256.hash(hash.toString());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    xml.openElement("parameters", true);
    int paramCount = 0;
    for (String name : req.parameterNames()) {
      if (++paramCount > MAX_PARAMETERS) break;
      if (name.length() <= MAX_NAME_LENGTH) writeParameterValues(xml, name, req.parameterValues(name));
    }
    xml.closeElement();
  }

  private static void appendValuesToHash(StringBuilder hash, String name, Iterable<String> values) {
    int valueCount = 0;
    for (String value : values) {
      if (++valueCount > MAX_VALUES) break;
      String effective = value.length() > MAX_VALUE_LENGTH ? value.substring(0, MAX_VALUE_LENGTH) : value;
      hash.append(name).append('=').append(effective).append('&');
    }
  }

  private static void writeParameterValues(XMLWriter xml, String name, Iterable<String> values) throws IOException {
    int valueCount = 0;
    for (String value : values) {
      if (++valueCount > MAX_VALUES) break;
      xml.openElement("parameter", false);
      xml.attribute("name", name);
      if (value.length() > MAX_VALUE_LENGTH) {
        xml.attribute("truncated", "true");
        xml.writeText(value.substring(0, MAX_VALUE_LENGTH));
      } else {
        xml.writeText(value);
      }
      xml.closeElement();
    }
  }

}
