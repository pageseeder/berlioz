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

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.OutputWriter.ContextOption;
import org.pageseeder.berlioz.servlet.HttpEnvironment;

/**
 * Returns the live service registry as XML or JSON.
 *
 * <p>Unlike {@code GetServices}, which re-reads the services XML files from disk, this generator
 * exposes a safe {@code load-status} field (pending, ready, startup-failed, or reload-failed) and
 * reflects the current in-memory state of the service registry. It is intended for developers and
 * operations tooling to inspect which services are actually loaded and active.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.</p>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <live-services load-status="ready">
 *   <service id="[id]" group="[group]" method="[method]" cacheable="[true|false]" ...>
 *     <response-code use="[use]" rule="[rule]"/>
 *     <url pattern="[uri-pattern]"/>
 *     <generator class="[class]" name="[name]" type="[type]" cacheable="[true|false]" .../>
 *     ...
 *   </service>
 *   ...
 * </live-services>
 * }</pre>
 *
 * <h3>Returned JSON</h3>
 * <pre>{@code
 * {
 *   "loadStatus": "ready",
 *   "liveServices": [
 *     {
 *       "id": "[id]", "group": "[group]", "method": "[method]", "cacheable": [true|false], ...,
 *       "responseCode": {"use": "[use]", "rule": "[rule]"},
 *       "urls": [{"pattern": "[uri-pattern]"}, ...],
 *       "generators": [
 *         {"class": "[class]", "name": "[name]", "type": "[type]", "cacheable": [true|false], ...},
 *         ...
 *       ]
 *     },
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.ListRuntimeServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <p>{@code GetLiveServices} is a deprecated forwarding alias of this class, retained so that
 * existing {@code services.xml} generator references keep resolving unchanged until removal
 * at 1.0.
 *
 * <h3>Etag</h3>
 * <p>The ETag is the registry version counter, which increments each time the services are
 * reloaded, together with the load status. This invalidates the response when the configuration
 * or load status changes.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.3
 * @since 0.14.3
 */
@Beta
public class ListRuntimeServices implements Generator, Cacheable {

  @Override
  public String getETag(Request req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    return registry.version() + "-" + ServiceLoader.getInstance().getLoadStatus();
  }

  @Override
  public Response generate(Request req, OutputWriter out) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    HttpEnvironment httpEnv = (HttpEnvironment) req.getEnvironment();

    out.startObject("live-services");
    out.field("load-status", ServiceLoader.getInstance().getLoadStatus());
    out.startArray("live-services", ContextOption.JSON_ONLY);
    for (HttpMethod method : HttpMethod.mappable()) {
      List<Service> services = registry.getServices(method);
      for (Service service : services) {
        List<String> urls = registry.matches(service);
        service.writeTo(out, method, urls, httpEnv.getCacheControl());
      }
    }
    out.endArray();
    out.endObject();

    return Response.ok();
  }

}
