/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import java.io.IOException;
import java.util.List;

import org.weborganic.berlioz.Beta;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentManager;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Service;
import org.weborganic.berlioz.content.ServiceRegistry;
import org.weborganic.berlioz.furi.URIPattern;
import org.weborganic.berlioz.furi.URIResolveResult;
import org.weborganic.berlioz.http.HttpMethod;
import org.weborganic.berlioz.servlet.HttpEnvironment;

import com.topologi.diffx.xml.XMLWriter;

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
 * <pre>{@code <generator class="org.weborganic.berlioz.generator.GetServices"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator uses a weak etag based on the name, length and last modified date of the file.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.3
 */
@Beta
public final class GetMatchingService implements ContentGenerator, Cacheable {

  @Override
  public String getETag(ContentRequest req) {
    ServiceRegistry registry = ContentManager.getDefaultRegistry();
    return Long.toString(registry.version());
  }

  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    // Parameters
    String url = getURL(req, xml);
    HttpMethod method = getMethod(req, xml);

    // Nothing left to do
    if (url == null || method == null) return;

    // Identify the service
    ServiceRegistry registry = ContentManager.getDefaultRegistry();
    MatchingService match = registry.get(url, method);

    // Display info as XML
    if (match != null) {

      xml.openElement("matching-service", true);

      // URI Pattern
      URIPattern pattern = match.pattern();
      xml.openElement("url", true);
      xml.attribute("path", url);
      xml.attribute("pattern", pattern.toString());
      URIResolveResult result = match.result();
      for (String name : result.names()) {
        xml.openElement("parameter", true);
        xml.attribute("name", name);
        Object o = result.get(name);
        xml.attribute("value", o != null? o.toString() : "");
        xml.closeElement();
      }
      xml.closeElement();

      // The service
      Service service = match.service();
      List<String> urls = registry.matches(service);
      HttpEnvironment httpEnv = (HttpEnvironment)req.getEnvironment();
      service.toXML(xml, method, urls, httpEnv.getCacheControl());

      // close 'matching-service'
      xml.closeElement();

    } else {
      xml.emptyElement("no-matching-service");
    }

  }

  /**
   * Returns the HTTP method if valid (defaults to GET).
   *
   * @param req The content request.
   * @param xml The XML writer.
   *
   * @return the HTTP method or <code>null</code>.
   *
   * @throws IOException if an error occurs while writing the XML error message
   */
  private HttpMethod getMethod(ContentRequest req, XMLWriter xml) throws IOException {
    String method = req.getParameter("method", "GET");
    try {
      return HttpMethod.valueOf(method);
    } catch (IllegalArgumentException ex) {
      xml.openElement("error");
      xml.attribute("type", "client");
      xml.attribute("message", "The specified HTTP method is invalid: "+method);
      xml.closeElement();
      req.setStatus(ContentStatus.BAD_REQUEST);
      return null;
    }
  }

  /**
   * Returns the URL if specified and not empty.
   *
   * @param req The content request.
   * @param xml The XML writer.
   *
   * @return the url or <code>null</code>.
   *
   * @throws IOException if an error occurs while writing the XML error message
   */
  private String getURL(ContentRequest req, XMLWriter xml) throws IOException {
    String url = req.getParameter("url", "");
    if (url.isEmpty()) {
      xml.openElement("error");
      xml.attribute("type", "client");
      xml.attribute("message", "The URL was not specified");
      xml.closeElement();
      req.setStatus(ContentStatus.BAD_REQUEST);
      return null;
    }
    return url;
  }

}
