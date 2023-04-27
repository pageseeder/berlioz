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
package org.pageseeder.berlioz.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Parameter;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceStatusRule;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.http.ServerTimingHeader;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.util.ProfileFormat;
import org.pageseeder.xmlwriter.XMLWriter;
import org.pageseeder.xmlwriter.XMLWriterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An XML response produced from content generators.
 *
 * <p>This class is not thread-safe.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.7
 */
public final class XMLResponse {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(XMLResponse.class);

  /**
   * May be used to collect information about how generators perform.
   */
  private static volatile @Nullable GeneratorListener listener = null;

  /**
   * The core HTTP details.
   */
  private final CoreHttpRequest _core;

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
  private final Map<Integer, String> _etags = new HashMap<>();

  /**
   * Whether to profile the content generators.
   */
  private final boolean _profile;

  /**
   * The request to send to the generators.
   */
  private @Nullable ContentStatus status = null;

  /**
   * The redirect URL.
   */
  private @Nullable String redirect = null;

  /**
   * Any exception caught while invoking the generators.
   */
  private @Nullable BerliozException exception = null;

  private boolean serverTiming;

  /**
   * Creates a new XML response for the specified arguments.
   *
   * @param req     The HTTP servlet request.
   * @param res     The HTTP servlet response.
   * @param config  The Berlioz configuration environment.
   * @param match   The matching service
   * @param profile Whether to enable profiling.
   */
  public XMLResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config, MatchingService match,
      boolean profile) {
    this._core = new CoreHttpRequest(req, res, config.getEnvironment());
    this._match = match;
    this._requests = configure(this._core, match);
    this._profile = profile;
  }

  public void enableServerTiming() {
    this.serverTiming = true;
  }

  /**
   * Returns the service corresponding to this response.
   *
   * @return the service corresponding to this response.
   */
  public Service getService() {
    return this._match.service();
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
  public @Nullable String getEtag() {
    Service service = this._match.service();
    boolean cacheable = service.isCacheable();
    StringBuilder etag = new StringBuilder();
    if (cacheable) {
      for (HttpContentRequest request : this._requests) {
        ContentGenerator generator = request.generator();
        // Check if cacheable
        if (generator instanceof Cacheable) {
          String localtag = getETag(request);
          if (localtag.isEmpty()) return null;
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
    ContentStatus status = this.status;
    return status == null? ContentStatus.OK : status;
  }

  /**
   * Returns a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   *
   * @return a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   */
  public @Nullable BerliozException getError() {
    return this.exception;
  }

  /**
   * Returns the URL to redirect to.
   *
   * @return the URL to redirect to.
   */
  public @Nullable String getRedirectURL() {
    return this.redirect;
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
    xml.attribute("service", service.id());
    xml.attribute("group", service.group());
    if (service.flags() != null) {
      xml.attribute("flags", service.flags());
    }

    XMLResponseHeader header = new XMLResponseHeader(this._core, service, this._match.result());
    header.toXML(xml);

    // Call each generator in turn
    int position = 0;
    for (HttpContentRequest request : this._requests) {
      toXML(request, ++position, service, xml);
    }

    // Close 'root' and finalise
    xml.closeElement();
    xml.flush();
    return writer.toString();
  }

  // Static configuration
  // ---------------------------------------------------------------------------------------------

  /**
   * @param listener the listener to set
   */
  @Beta
  static synchronized void setListener(GeneratorListener listener) {
    XMLResponse.listener = listener;
  }

  /**
   * @return the listener currently in use.
   */
  @Beta
  static synchronized @Nullable GeneratorListener getListener() {
    return listener;
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Generates the XML content for one generator.
   *
   * @param request   The generator request to process.
   * @param position  The 1-based position of the request in the service
   * @param service   The service it is part of.
   * @param xml       The XML Writer to use.
   *
   * @throws IOException Should an I/O error occur while writing XML.
   */
  private void toXML(HttpContentRequest request, int position, Service service, XMLWriter xml) throws IOException {
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
      if (etag.length() > 0) {
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
    long start = System.nanoTime();
    try {
      // Normal response
      StringWriter writer = new StringWriter();
      XMLWriter ok = new XMLWriterImpl(writer);
      generator.process(request, ok);
      result = writer.toString();
      status = request.getStatus();
    } catch (Exception ex) {
      // We wrap any exception in a Berlioz Exception
      error = handleError(ex, generator);
      status = ContentStatus.INTERNAL_SERVER_ERROR;
    }

    long end = System.nanoTime();

    // Update Status
    boolean wasSet = handleStatus(status, generator, service);
    if (wasSet && ContentStatus.isRedirect(status)) {
      this.redirect = request.getRedirectURL();
    }
    xml.attribute("status", status.toString());
    if (this._profile) {
      xml.attribute("profile-etag", ProfileFormat.format(request.getProfileEtag()));
      xml.attribute("profile-process", ProfileFormat.format(end - start));
      xml.attribute("profile", ProfileFormat.format(request.getProfileEtag() + end - start));
    }
    if (this.serverTiming) {
      String safeName = name.replaceAll("[^!#$%&'*+\\-.^_`|~0-9a-zA-Z]", "_");
      ServerTimingHeader.addMetricNano(this._core.response(), "xml"+position, "Source "+safeName, request.getProfileEtag() + end - start);
    }

    // Report if requested
    GeneratorListener l = listener;
    if (l != null) {
      l.generate(service, generator, status, request.getProfileEtag(), end - start);
    }

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
   * @param core  The core HTTP details
   * @param match The matching service
   * @return the list of content generator requests to process.
   */
  private static List<HttpContentRequest> configure(CoreHttpRequest core, MatchingService match) {
    // Get the list of parameters
    Map<String, String> common = HttpRequestWrapper.toParameters(core.request(), match.result());
    // Create a request for each generator
    Service service = match.service();
    List<HttpContentRequest> requests = new ArrayList<>();
    int order = 0;
    for (ContentGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        // No specific parameters, return a request using the common parameters
        requests.add(new HttpContentRequest(core, common, generator, match.service(), order));

      } else {
        // Some specific parameters, recompute the parameters
        Map<String, String> specific = new HashMap<>(common);
        for (Parameter p : pconfig) {
          specific.put(p.name(), p.value(common));
        }
        requests.add(new HttpContentRequest(core, specific, generator, match.service(), order));
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
    BerliozException ex = this.exception;
    if (ex == null) {
      this.exception = ex = bex;

    // In less frequent case when multiple errors are thrown...
    } else {
      CompoundBerliozException compound = ex instanceof CompoundBerliozException? (CompoundBerliozException)ex : null;
      ErrorCollector<Exception> collector;
      if (compound != null) {
        collector = (ErrorCollector<Exception>)compound.getCollector();
      } else {
        collector = new ErrorCollector<>();
        compound = new CompoundBerliozException("Multiple errors thrown by generators", BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
        // TODO This should be cleaned up
        Throwable t = ex.getCause();
        collector.collectQuietly(Level.ERROR, t != null? (Exception)t : ex);
        ex = compound;
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
      ContentStatus s = this.status;
      if (s == null) {
        this.status = status;
        return true;
      } else if (rule == CodeRule.HIGHEST && status.code() > s.code()) {
        this.status = status;
        return true;
      } else if (rule == CodeRule.LOWEST && status.code() < s.code()) {
        this.status = status;
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
    Integer key = request.order();
    if (this._etags.containsKey(key)) {
      etag = this._etags.get(key);
    } else {
      ContentGenerator generator = request.generator();
      if (generator instanceof Cacheable) {
        long start = System.nanoTime();
        etag = ((Cacheable)generator).getETag(request);
        long end = System.nanoTime();
        request.setProfileEtag(end-start);
      }
      // Store for reuse (even if null)
      this._etags.put(key, etag != null? etag : "");
    }
    return etag != null? etag : "";
  }
}
