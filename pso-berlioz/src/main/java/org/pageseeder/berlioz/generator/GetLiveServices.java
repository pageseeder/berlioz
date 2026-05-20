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
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.xmlwriter.XMLWriter;

/**
 * Returns the current service configuration as XML.
 *
 * <p>This content generator is mostly useful for developers to see how the services are configured.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration associated with this generator.</p>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 *
 * <h3>Returned XML</h3>
 * <p>This generator contains the <code>/WEB-INF/config/services.xml</code> used by Berlioz to load
 * its services.</p>
 * <pre>{@code <services version="1.0"> ... </services>}</pre>
 * <p>The formatting of the XML may differ from the actual files as it is parsed before being
 * returned; the XML declaration and comments are stripped.</p>
 *
 * <h3>Error Handling</h3>
 * <p>Should there be any problem parsing or reading the file, the XML returned will be:
 * <pre>{@code <no-data error="[error]" details="[error-details]"/>}</pre>
 * <p>The error details are only shown if available.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator uses an etag based on the name, length and last modified date of the file.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.7
 * @since Berlioz 0.9.3
 */
@Beta
public final class GetLiveServices implements ContentGenerator, Cacheable {

  @Override
  public @NonNull String getETag(ContentRequest req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    return Long.toString(registry.version());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    xml.openElement("live-services", true);

    // Get the cache control
    HttpEnvironment httpEnv = (HttpEnvironment)req.getEnvironment();

    // For each HTTP method
    for (HttpMethod method : HttpMethod.mappable()) {
      List<Service> services = registry.getServices(method);

      // Iterate over the services
      for (Service service : services) {
        List<String> urls = registry.matches(service);
        service.toXML(xml, method, urls, httpEnv.getCacheControl());
      }

    }

    xml.closeElement();
  }

}
