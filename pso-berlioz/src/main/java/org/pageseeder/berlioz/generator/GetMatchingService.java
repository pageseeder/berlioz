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

import java.util.List;
import java.util.Objects;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.OutputWriter.ContextOption;
import org.pageseeder.berlioz.servlet.HttpEnvironment;

/**
 * Returns the service that matches a given URL as XML or JSON.
 *
 * <p>This generator looks up the live service registry and reports which service (if any) would
 * handle the specified URL and HTTP method, together with the URI template variables extracted
 * from the match. It is intended for developer tooling and diagnostics.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <dl>
 *   <dt>{@code url}</dt>
 *   <dd>Required. The URL path to match against the service registry.</dd>
 *   <dt>{@code method}</dt>
 *   <dd>Optional. The HTTP method to use for matching (default: {@code GET}).</dd>
 * </dl>
 *
 * <h3>Returned XML</h3>
 * <p>The root element always reports whether a service matched via the {@code matched} attribute.
 * When a matching service is found:
 * <pre>{@code
 * <matching-service matched="true">
 *   <url path="[url]" pattern="[uri-pattern]">
 *     <parameter name="[var]" value="[extracted-value]"/>
 *     ...
 *   </url>
 *   <service id="[id]" group="[group]" method="[method]" ...>
 *     ...
 *   </service>
 * </matching-service>
 * }</pre>
 * <p>When no service matches:
 * <pre>{@code <matching-service matched="false"/>}</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "matched": true,
 *   "url": {
 *     "path": "[url]", "pattern": "[uri-pattern]",
 *     "parameters": [{"name": "[var]", "value": "[extracted-value]"}, ...]
 *   },
 *   "service": {...}
 * }
 * }</pre>
 * <p>When no service matches:
 * <pre>{@code {"matched": false}}</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetMatchingService"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>The ETag is the registry version counter, which increments each time the services are
 * reloaded.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.1
 * @since 0.9.3
 */
@Beta
public final class GetMatchingService implements Generator, Cacheable {

  @Override
  public String getETag(Request req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    return Long.toString(registry.version());
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    String url = req.parameter("url").asString().required();
    HttpMethod method = req.parameter("method").asEnum(HttpMethod.class).optional(HttpMethod.GET);

    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    MatchingService match = registry.get(url, method);

    out.startObject("matching-service");
    out.field("matched", match != null);

    if (match != null) {
      URIPattern pattern = match.pattern();
      out.startObject("url");
      out.field("path", url);
      out.field("pattern", pattern.toString());
      URIResolveResult result = match.result();
      if (!result.names().isEmpty()) {
        out.startArray("parameters", ContextOption.JSON_ONLY);
        for (String name : result.names()) {
          String value = Objects.toString(result.get(name), "");
          out.startObject("parameter");
          out.field("name", name);
          out.field("value", value);
          out.endObject();
        }
        out.endArray();
      }
      out.endObject();

      Service service = match.service();
      List<String> urls = registry.matches(service);
      HttpEnvironment httpEnv = (HttpEnvironment) req.getEnvironment();
      service.writeTo(out, method, urls, httpEnv.getCacheControl());
    }

    out.endObject();

    return Response.ok();
  }

}
