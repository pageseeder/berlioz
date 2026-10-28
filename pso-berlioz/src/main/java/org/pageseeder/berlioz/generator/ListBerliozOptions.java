/*
 * Copyright 2026 Allette Systems (Australia)
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

import java.util.Locale;

import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.util.SHA256;

/**
 * Returns the catalog of the global options defined by this version of Berlioz as XML or JSON.
 *
 * <p>Each {@link BerliozOption} is listed in declaration order with its property name, type,
 * default value, a one-line description, and for deprecated options the version since when it is
 * deprecated and the version in which it will be removed.
 *
 * <p>Values in use are not included: the configured values are returned, redacted, by
 * {@link GetGlobalConfig}. A property listed here and absent from the configuration uses its
 * default value.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <berlioz-options>
 *   <option property="berlioz.http.compression" type="boolean" default="true"
 *           description="Whether Berlioz compresses responses ..."/>
 *   <option property="berlioz.http.get-via-post" type="boolean" default="false"
 *           deprecated="0.14.0" removal="1.0" description="Whether a POST request ..."/>
 *   ...
 * </berlioz-options>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "options": [
 *     {"property": "berlioz.http.compression", "type": "boolean", "default": true,
 *      "description": "Whether Berlioz compresses responses ..."},
 *     {"property": "berlioz.http.get-via-post", "type": "boolean", "default": false,
 *      "deprecated": "0.14.0", "removal": "1.0", "description": "Whether a POST request ..."},
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <p>The {@code type} is {@code boolean} or {@code string}. In JSON, the default of a boolean
 * option is a boolean. The {@code deprecated} and {@code removal} fields are only present when
 * they apply.
 *
 * <h3>Etag</h3>
 * <p>The catalog is constant for a given version of Berlioz; the etag is a hash of its content.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.ListBerliozOptions"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * @author Christophe Lauret
 *
 * @version 0.14.5
 * @since 0.14.5
 */
public final class ListBerliozOptions implements Generator, Cacheable {

  /**
   * The etag, computed once since the catalog cannot change at runtime.
   */
  private static final String ETAG = SHA256.hash(write(new XmlOutputAdapter()).toString());

  @Override
  public String getETag(Request req) {
    return ETAG;
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    write(out);
    return Response.ok();
  }

  /**
   * Writes the catalog of options.
   *
   * @param out The output writer
   * @return The same output writer
   */
  private static OutputWriter write(OutputWriter out) {
    out.startObject("berlioz-options");
    out.startArray("options", OutputWriter.ContextOption.JSON_ONLY);
    for (BerliozOption option : BerliozOption.values()) {
      out.startObject("option");
      out.field("property", option.property());
      out.field("type", option.type().name().toLowerCase(Locale.ROOT));
      Object defaultTo = option.defaultTo();
      if (defaultTo instanceof Boolean) {
        out.field("default", ((Boolean) defaultTo).booleanValue());
      } else {
        out.field("default", defaultTo.toString());
      }
      String deprecatedSince = option.deprecatedSince();
      if (deprecatedSince != null) out.field("deprecated", deprecatedSince);
      String removal = option.removal();
      if (removal != null) out.field("removal", removal);
      out.field("description", option.description());
      out.endObject();
    }
    out.endArray();
    out.endObject();
    return out;
  }

}
