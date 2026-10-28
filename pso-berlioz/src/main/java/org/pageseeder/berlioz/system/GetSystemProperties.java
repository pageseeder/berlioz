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

import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.util.Redaction;

/**
 * Returns JVM system properties as XML or JSON.
 *
 * <p>Exposes the properties available via {@link System#getProperties()}, sorted by name.
 * Values whose names match a sensitive keyword pattern (e.g. {@code password}, {@code secret},
 * {@code token}, {@code apikey}) are replaced with {@code [REDACTED]}, as in
 * {@link org.pageseeder.berlioz.generator.GetGlobalConfig}. The remaining values may still
 * include environment details (e.g. user home directory, OS details) — restrict access to this
 * generator in production environments.
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <system>
 *   <property name="[nameA]" value="[valueA]"/>
 *   <property name="[nameB]" value="[REDACTED]"/>
 *   ...
 * </system>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "properties": [
 *     {"name": "[nameA]", "value": "[valueA]"},
 *     {"name": "[nameB]", "value": "[REDACTED]"}
 *   ]
 * }
 * }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.system.GetSystemProperties"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.9.32
 */
@Beta
public final class GetSystemProperties implements Generator {

  @Override
  public Response generate(Request req, OutputWriter out) {
    Properties properties = System.getProperties();
    // Only string entries: other entries could not be serialized as values anyway
    Set<String> names = new TreeSet<>(properties.stringPropertyNames());

    out.startObject("system");
    out.startArray("properties", OutputWriter.ContextOption.JSON_ONLY);
    for (String name : names) {
      String value = properties.getProperty(name);
      if (value == null) continue; // removed concurrently
      out.startObject("property");
      out.field("name", name);
      out.field("value", Redaction.redact(name, value));
      out.endObject();
    }
    out.endArray();
    out.endObject();
    return Response.ok();
  }

}
