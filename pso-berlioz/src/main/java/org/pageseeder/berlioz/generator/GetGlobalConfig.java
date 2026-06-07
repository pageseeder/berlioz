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

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.util.SHA256;
import org.pageseeder.xmlwriter.XMLWriter;

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
 *   <property name="[nameB]" value="[REDACTED]"/>
 *   <property name="[nameC]" value="[valueC]"/>
 *   ...
 * </properties>
 * }</pre>
 *
 * <p>Property values whose names match a sensitive keyword pattern (e.g. {@code password},
 * {@code secret}, {@code token}, {@code apikey}, {@code credential}, {@code privatekey})
 * are replaced with {@code [REDACTED]} to avoid exposing secrets via misconfiguration.
 *
 * <h3>Etag</h3>
 * <p>This generator uses an etag based on the name, length and last modified date of the
 * properties file being loaded or <code>null</code> if no config file could be found.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.8
 */
public final class GetGlobalConfig implements ContentGenerator, Cacheable {

  private static final Pattern SENSITIVE_NAME = Pattern.compile(
      "(?i)(password|passwd|secret|api[._\\-]?key|token|credential|private[._\\-]?key)");

  static final String REDACTED = "[REDACTED]";

  @Override
  public @Nullable String getETag(Request req) {
    File global = GlobalSettings.getPropertiesFile();
    if (global == null) return null;
    return SHA256.hash(global.length()+"x"+global.lastModified());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    File global = GlobalSettings.getPropertiesFile();

    xml.openElement("properties", true);
    if (global != null) {
      xml.attribute("source", global.getName());
    }

    for (Entry<String, String> e : GlobalSettings.getAll().entrySet()) {
      String name = e.getKey();
      String value = SENSITIVE_NAME.matcher(name).find() ? REDACTED : e.getValue();
      xml.openElement("property", false);
      xml.attribute("name", name);
      xml.attribute("value", value);
      xml.closeElement();
    }

    xml.closeElement();
  }

}
