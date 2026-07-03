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
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.content.ServiceRegistry;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.furi.URIPattern;
import org.pageseeder.berlioz.furi.URIResolveResult;
import org.pageseeder.berlioz.http.HttpMethod;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.berlioz.xml.XmlWriter;

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
 * <p>This generator uses an etag based on the name, length, and last modified date of the file.
 *
 * @author Christophe Lauret
 *
 * @version 0.14.0
 * @since 0.9.3
 */
@Beta
public final class GetMatchingService implements XmlGenerator, Cacheable {

  @Override
  public String getETag(Request req) {
    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    return Long.toString(registry.version());
  }

  @Override
  public Response generate(Request req, XmlWriter xml) {
    String url = req.parameter("url").asString().required();
    HttpMethod method = req.parameter("method").asEnum(HttpMethod.class).optional(HttpMethod.GET);

    ServiceRegistry registry = ServiceLoader.getInstance().getDefaultRegistry();
    MatchingService match = registry.get(url, method);

    if (match != null) {
      xml.openElement("matching-service", true);

      URIPattern pattern = match.pattern();
      xml.openElement("url", true);
      xml.attribute("path", url);
      xml.attribute("pattern", pattern.toString());
      URIResolveResult result = match.result();
      for (String name : result.names()) {
        String value = Objects.toString(result.get(name), "");
        xml.openElement("parameter")
            .attribute("name", name)
            .attribute("value", value)
            .closeElement();
      }
      xml.closeElement();

      Service service = match.service();
      List<String> urls = registry.matches(service);
      HttpEnvironment httpEnv = (HttpEnvironment) req.getEnvironment();
      service.toXml(xml, method, urls, httpEnv.getCacheControl());

      xml.closeElement();
    } else {
      xml.emptyElement("no-matching-service");
    }

    return Response.ok();
  }

}
