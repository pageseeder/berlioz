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
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.berlioz.xml.XmlWriter;

/**
 * Returns the live service registry as XML.
 *
 * <p>Unlike {@code GetServices}, which re-reads the services XML files from disk, this generator
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
 * <live-services>
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
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetLiveServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>The ETag is the registry version counter, which increments each time the services are
 * reloaded. This ensures the response is invalidated whenever the service configuration changes.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.3
 */
@Beta
public final class GetLiveServices implements XmlGenerator, Cacheable {

  @Override
  public String getETag(Request req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    return Long.toString(registry.version());
  }

  @Override
  public Response generate(Request req, XmlWriter xml) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    xml.openElement("live-services", true);

    HttpEnvironment httpEnv = (HttpEnvironment) req.getEnvironment();

    for (HttpMethod method : HttpMethod.mappable()) {
      List<Service> services = registry.getServices(method);
      for (Service service : services) {
        List<String> urls = registry.matches(service);
        service.toXml(xml, method, urls, httpEnv.getCacheControl());
      }
    }

    xml.closeElement();
    return Response.ok();
  }

}
