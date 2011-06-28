/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Parameter;
import org.weborganic.berlioz.content.Service;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * An XML response produced from content generators.
 * 
 * <p>This class is not thread-safe.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 28 June 2011
 */
public final class XMLResponse {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLResponse.class);

  /**
   * The HTTP servlet request.
   */
  private final HttpServletRequest _req;

  /**
   * The HTTP servlet response.
   */
  private final HttpServletResponse _res;

  /**
   * The current environment.
   */
  private final Environment _env;

  /**
   * The service that was matched for the given request.
   */
  private final MatchingService _match;

  /**
   * The request to send to the generators.
   */
  private final List<HttpContentRequest> _requests;

  /**
   * The request to send to the generators.
   */
  private ContentStatus _status = ContentStatus.OK;

  /**
   * Creates a new XML response for the specified arguments.
   * 
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param env   The current environment.
   * @param match The matching service
   */
  public XMLResponse(HttpServletRequest req, HttpServletResponse res, Environment env, MatchingService match) {
    this._req = req;
    this._res = res;
    this._env = env;
    this._match = match;
    this._requests = configure(req, res, env, match);
  }

  /**
   * Returns the service corresponding to this response.
   * 
   * @return the service corresponding to this response.
   */
  public Service getService() {
    return this._match != null? this._match.service() : null;
  }

  /**
   * Returns the Etag for this response.
   * 
   * <p>The Etag is computed from the Etags returned by each generator.
   * 
   * <p>If any one of the generators is not cacheable, this response is not considered cacheable
   * and the Etag returned will be <code>null</code>.
   * 
   * @return the Etag for this response if it is cacheable; <code>null</code> if it is not.
   * 
   * @since 0.8.0
   */
  public String getEtag() {
    Service service = this._match.service();
    boolean cacheable = service.isCacheable();
    StringBuilder etag = new StringBuilder();
    if (cacheable) {
      for (HttpContentRequest request : this._requests) {
        ContentGenerator generator = request.generator();
        // Check if cacheable
        if (generator instanceof Cacheable) {
          etag.append(((Cacheable)generator).getETag(request)).append("/");
        } else {
          cacheable = false;
          break;
        }
      }
    }
    return cacheable? etag.toString() : null;
  }

  /**
   * Returns the status of this service response.
   * 
   * @return the status of this service response.
   * @since 0.8.2
   */
  public ContentStatus getStatus() {
    return this._status;
  }

  /**
   * Generates an XML response for the wrapped HTTP request and response objects.
   * 
   * @return The XML content for the appropriate content generator.
   * 
   * @throws IOException Should an I/O error occur.
   */
  public String generate() throws IOException {
    // Initialise the writer
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer);
    xml.xmlDecl();
    xml.openElement("root", true);

    // Get service
    Service service = this._match.service();
    XMLResponseHeader header = new XMLResponseHeader(this._req, service, this._match.result());
    header.toXML(xml);

    // Call each generator in turn
    for (HttpContentRequest request : this._requests) {
      toXML(request, service, xml);
    }

    // Close 'root' and finalise
    xml.closeElement();
    xml.flush();
    return writer.toString();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Generates the XML content for one generator.
   * 
   * @param request   The generator request to process.
   * @param service   The service it is part of.
   * @param xml       The XML Writer to use.
   * 
   * @throws IOException Should an I/O error occur while writing XML.
   */
  private void toXML(HttpContentRequest request, Service service, XMLWriter xml) throws IOException {
    ContentGenerator generator = request.generator();
    // Generate the main element
    xml.openElement("content", true);
    xml.attribute("generator", generator.getClass().getName());
    String name = service.name(generator);
    xml.attribute("name", name);
    String target = service.target(generator);
    if (target != null) xml.attribute("target", target);

    // If cacheable, include etag
    if (generator instanceof Cacheable) {
      String etag = ((Cacheable)generator).getETag(request);
      xml.attribute("etag", etag);
    }

    // Detect if deprecated
    if (generator.getClass().isAnnotationPresent(Deprecated.class))
      xml.attribute("deprecated", "true");

    // Let's invoke the generator
    String result = null;
    BerliozException error = null;
    ContentStatus status = ContentStatus.OK;
    try {
      // Normal response
      StringWriter writer = new StringWriter();
      XMLWriter ok = new XMLWriterImpl(writer);
      generator.process(request, ok);
      result = writer.toString();
      status = request.getStatus();
    } catch (BerliozException ex) {
      error = ex;
      status = ContentStatus.INTERNAL_SERVER_ERROR;
    } catch (Exception ex) {
      // We wrapping any exception in a Berlioz Exception
      error = new BerliozException("Unexpected exception caught", ex);
      status = ContentStatus.INTERNAL_SERVER_ERROR;
    }

    // Update Status
    if (status.code() > this._status.code()) {
      this._status = status;
    }
    xml.attribute("status", status.toString());

     // Write the XML
    if (error != null) error.toXML(xml);
    else xml.writeXML(result);

    xml.closeElement();
  }

  /**
   * Returns the list of content generator requests to process. 
   * 
   * @param req   The HTTP servlet request.
   * @param res   The HTTP servlet response.
   * @param env   The current environment.
   * @param match The matching service
   * @return the list of content generator requests to process.
   */
  private static List<HttpContentRequest> configure(HttpServletRequest req, HttpServletResponse res, Environment env,
      MatchingService match) {
    // Get the list of parameters
    Map<String, String> common = HttpRequestWrapper.toParameters(req, match.result());
    // Create a request for each generator
    Service service = match.service();
    List<HttpContentRequest> requests = new ArrayList<HttpContentRequest>();
    for (ContentGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        // No specific parameters, return a request using the common parameters
        requests.add(new HttpContentRequest(req, res, env, common, generator));

      } else {
        // Some specific parameters, recompute the parameters
        Map<String, String> specific = new HashMap<String, String>(common);
        for (Parameter p : pconfig) {
          specific.put(p.name(), p.value(common));
        }
        requests.add(new HttpContentRequest(req, res, env, specific, generator));
      }
    }
    return requests;
  }

}
