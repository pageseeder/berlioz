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
import org.weborganic.berlioz.BerliozErrorID;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Parameter;
import org.weborganic.berlioz.content.Service;
import org.weborganic.berlioz.content.ServiceStatusRule;
import org.weborganic.berlioz.content.ServiceStatusRule.CodeRule;
import org.weborganic.berlioz.util.CollectedError.Level;
import org.weborganic.berlioz.util.CompoundBerliozException;
import org.weborganic.berlioz.util.ErrorCollector;
import org.weborganic.berlioz.util.Errors;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * An XML response produced from content generators.
 *
 * <p>This class is not thread-safe.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.11 - 21 December 2012
 * @since Berlioz 0.7
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
   * The service that was matched for the given request.
   */
  private final MatchingService _match;

  /**
   * The request to send to the generators.
   */
  private final List<HttpContentRequest> _requests;

  /**
   * Maps the etags to each HTTP request
   */
  private final Map<Integer, String> _etags = new HashMap<Integer, String>();

  /**
   * The request to send to the generators.
   */
  private ContentStatus _status = null;

  /**
   * The redirect URL.
   */
  private String _redirect = null;

  /**
   * Any exception caught while invoking the generators.
   */
  private BerliozException _ex = null;

  /**
   * Creates a new XML response for the specified arguments.
   *
   * @param req    The HTTP servlet request.
   * @param res    The HTTP servlet response.
   * @param config The Berlioz configuration environment.
   * @param match  The matching service
   */
  public XMLResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config, MatchingService match) {
    this._req = req;
    this._match = match;
    this._requests = configure(req, res, config.getEnvironment(), match);
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
   * @since Berlioz 0.8.0
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
          String localtag = getETag(request);
          if (localtag == null) return null;
          etag.append(localtag).append('/');
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
   * @since Berlioz 0.8.2
   */
  public ContentStatus getStatus() {
    return this._status == null? ContentStatus.OK : this._status;
  }

  /**
   * Returns a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   *
   * @return a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   */
  public BerliozException getError() {
    return this._ex;
  }

  /**
   * Returns the URL to redirect to.
   *
   * @return the URL to redirect to.
   */
  public String getRedirectURL() {
    return this._redirect;
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
    if (target != null) {
      xml.attribute("target", target);
    }

    // If cacheable, include etag
    if (generator instanceof Cacheable) {
      String etag = getETag(request);
      if (etag != null) {
        xml.attribute("etag", etag);
      }
    }

    // Detect if deprecated
    if (generator.getClass().isAnnotationPresent(Deprecated.class)) {
      xml.attribute("deprecated", "true");
    }

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
      error = handleError(ex, generator);
      status = ContentStatus.INTERNAL_SERVER_ERROR;
    } catch (Exception ex) {
      // We wrapping any exception in a Berlioz Exception
      error = handleError(ex, generator);
      status = ContentStatus.INTERNAL_SERVER_ERROR;
    }

    // Update Status
    boolean wasSet = handleStatus(status, generator, service);
    if (wasSet && ContentStatus.isRedirect(status)) {
      this._redirect = request.getRedirectURL();
    }
    xml.attribute("status", status.toString());

    // Write the XML
    if (error != null) {
      xml.openElement("berlioz-exception");
      Errors.toXML(error, xml, false);
      xml.closeElement();
    } else {
      xml.writeXML(result);
    }

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
    int order = 0;
    for (ContentGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        // No specific parameters, return a request using the common parameters
        requests.add(new HttpContentRequest(req, res, env, common, generator, match.service(), order));

      } else {
        // Some specific parameters, recompute the parameters
        Map<String, String> specific = new HashMap<String, String>(common);
        for (Parameter p : pconfig) {
          specific.put(p.name(), p.value(common));
        }
        requests.add(new HttpContentRequest(req, res, env, specific, generator, match.service(), order));
      }
      order++;
    }
    return requests;
  }

  /**
   * Handles an exception thrown by a generator.
   *
   * @param exception The exception to handle.
   * @param generator The generator which caused the exception.
   *
   * @return a Berlioz exception for immediate use.
   */
  @SuppressWarnings("unchecked")
  private BerliozException handleError(Exception exception, ContentGenerator generator) {
    LOGGER.warn("Handling "+exception.getClass().getName()+" thrown by "+generator.getClass().getName());
    // Ensure we have a berlioz exception we can deal with
    BerliozException bex;
    if (exception instanceof BerliozException) {
      bex = (BerliozException)exception;
      if (bex.id() == null) {
        bex.setId(BerliozErrorID.GENERATOR_ERROR_UNFORCED);
      }
    } else {
      bex = new BerliozException("Unexpected exception caught", exception, BerliozErrorID.GENERATOR_ERROR_UNCHECKED);
    }
    // Maintain the state of this Response
    if (this._ex == null) {
      this._ex = bex;

    // In less frequent case when multiple errors are thrown...
    } else {
      CompoundBerliozException compound;
      ErrorCollector<Exception> collector;
      if (this._ex instanceof CompoundBerliozException) {
        compound = (CompoundBerliozException)this._ex;
        collector = (ErrorCollector<Exception>)compound.getCollector();
      } else {
        collector = new ErrorCollector<Exception>();
        compound = new CompoundBerliozException("Multiple errors thrown by generators", BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
        collector.collectQuietly(Level.ERROR, (Exception)this._ex.getCause());
        this._ex = compound;
      }
      collector.collectQuietly(Level.ERROR, exception);
    }

    return bex;
  }

  /**
   * Handles the status of this generator.
   *
   * @param status    The status of the generator after it has been invoked.
   * @param generator The generator.
   * @param service   The service that the generator is part of.
   *
   * @return <code>true</code> if the overall status was set as a result of this method;
   *         <code>false</code> otherwise.
   */
  private boolean handleStatus(ContentStatus status, ContentGenerator generator, Service service) {
    boolean relevant = service.affectStatus(generator);
    if (relevant) {
      ServiceStatusRule r = service.rule();
      CodeRule rule = r.rule();
      // If null set it (works for all rules)
      if (this._status == null) {
        this._status = status;
        return true;
      } else if (rule == CodeRule.HIGHEST && status.code() > this._status.code()) {
        this._status = status;
        return true;
      } else if (rule == CodeRule.LOWEST && status.code() < this._status.code()) {
        this._status = status;
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the etag for the specified request.
   *
   * @param request The HTTP content request.
   * @return the corresponding etag if there is one or <code>null</code>.
   */
  private String getETag(HttpContentRequest request) {
    String etag = null;
    Integer key = Integer.valueOf(request.order());
    if (this._etags.containsKey(key)) {
      etag = this._etags.get(key);
    } else {
      ContentGenerator generator = request.generator();
      if (generator instanceof Cacheable) {
        etag = ((Cacheable)generator).getETag(request);
      }
      // Store for reuse (even if null)
      this._etags.put(key, etag);
    }
    return etag;
  }
}
