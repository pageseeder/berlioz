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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Parameter;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.ServiceStatusRule.CodeRule;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.http.ServerTimingHeader;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.util.CollectedError.Level;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.util.ProfileFormat;
import org.pageseeder.berlioz.xml.XmlAppendable;
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
 * @version 0.13.2
 * @since 0.7
 */
public final class XmlResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlResponse.class);

  /**
   * Headers that generators should not set directly — they are owned by the framework,
   * servlet filters, or service-level configuration. Setting them via {@code Response.header()}
   * is logged as a warning.
   */
  private static final Set<String> FRAMEWORK_HEADERS = new HashSet<>(Arrays.asList(
      "Location",           // use Response.redirect()
      "ETag",               // use Cacheable interface
      "Last-Modified",      // framework caching concern
      "Cache-Control",      // service-level cache="" attribute
      "Expires",            // framework caching concern
      "Vary",               // framework content-negotiation concern
      "Set-Cookie",         // security layer, not generator scope
      "Content-Encoding",   // compression layer (BerliozOption.HTTP_COMPRESSION)
      "Transfer-Encoding",  // container concern
      "Server",             // container concern
      "Date"                // container concern
  ));

  /**
   * May be used to collect information about how generators perform.
   */
  private static final AtomicReference<@Nullable GeneratorListener> listener = new AtomicReference<>(null);

  /**
   * The core HTTP details.
   */
  private final CoreHttpRequest core;

  /**
   * The service that was matched for the given request.
   */
  private final MatchingService match;

  /**
   * The request to send to the generators.
   */
  private final List<HttpContentRequest> requests;

  /**
   * Maps the etags to each HTTP request
   */
  private final Map<Integer, String> etags = new HashMap<>();

  /**
   * Whether to profile the content generators.
   */
  private final boolean profile;

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

  /**
   * Response headers accumulated from generator {@link Response} objects (last-writer-wins).
   * Applied via {@code HttpServletResponse.setHeader}.
   */
  private final Map<String, String> responseHeaders = new LinkedHashMap<>();

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
  public XmlResponse(HttpServletRequest req, HttpServletResponse res, BerliozConfig config, MatchingService match,
      boolean profile) {
    this.core = new CoreHttpRequest(req, res, config.getEnvironment());
    this.match = match;
    this.requests = configure(this.core, match);
    this.profile = profile;
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
    return this.match.service();
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
  public @Nullable String getEtag() {
    Service service = this.match.service();
    boolean cacheable = service.isCacheable();
    StringBuilder etag = new StringBuilder();
    if (cacheable) {
      for (HttpContentRequest request : this.requests) {
        BerliozGenerator generator = request.generator();
        // Check if cacheable
        if (generator instanceof Cacheable) {
          String localTag = retrieveETag(request);
          if (localTag.isEmpty()) return null;
          etag.append(localTag).append('/');
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
   */
  public ContentStatus getStatus() {
    ContentStatus s = this.status;
    return s == null? ContentStatus.OK : s;
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
   * Returns the response headers accumulated from generators (last-writer-wins per name).
   *
   * @return an unmodifiable map; never {@code null}
   */
  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(this.responseHeaders);
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
    Service service = this.match.service();
    xml.attribute("service", service.id());
    xml.attribute("group", service.group());
    if (!service.flags().isEmpty()) {
      xml.attribute("flags", service.flags());
    }

    XmlResponseHeader header = new XmlResponseHeader(this.core, service, this.match.result());
    header.toXML(xml);

    // Call each generator in turn
    int position = 0;
    for (HttpContentRequest request : this.requests) {
      toXML(request, ++position, service, xml);
    }

    // Close 'root' and finalize
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
  static void setListener(@Nullable GeneratorListener listener) {
    XmlResponse.listener.set(listener);
  }

  /**
   * @return the listener currently in use.
   */
  @Beta
  static @Nullable GeneratorListener getListener() {
    return listener.get();
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
    BerliozGenerator generator = request.generator();
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
      String etag = retrieveETag(request);
      if (!etag.isEmpty()) {
        xml.attribute("etag", etag);
      }
    }

    // Detect if deprecated
    if (generator.getClass().isAnnotationPresent(Deprecated.class)) {
      xml.attribute("deprecated", "true");
    }

    // Invoke the generator
    String result = null;
    BerliozException error = null;
    Response response = Response.ok();
    long start = System.nanoTime();
    try {
      StringWriter sw = new StringWriter();
      if (generator instanceof XmlGenerator) {
        XmlAppendable<StringWriter> xw = new XmlAppendable<>(sw);
        response = ((XmlGenerator) generator).generate(request, xw);
        xw.flush();
      } else if (generator instanceof Generator) {
        XmlOutputAdapter oa = new XmlOutputAdapter(new XmlAppendable<>(sw));
        response = ((Generator) generator).generate(request, oa);
        oa.flush();
      } else if (generator instanceof ContentGenerator) {
        XMLWriter legacyXml = new XMLWriterImpl(sw);
        ((ContentGenerator) generator).process(request, legacyXml);
        legacyXml.flush();
        response = legacyResponse(request);
      } else {
        LOGGER.warn("Unsupported generator type {} — no content written", generator.getClass().getName());
      }
      result = sw.toString();
    } catch (InvalidParameterException ex) {
      error = handleError(ex, generator);
      response = Response.status(ContentStatus.BAD_REQUEST);
    } catch (Exception ex) {
      error = handleError(ex, generator);
      response = Response.status(ContentStatus.INTERNAL_SERVER_ERROR);
    }

    long end = System.nanoTime();

    // Aggregate this generator's response into the service-level outcome.
    // handleStatus applies ServiceStatusRule (highest/lowest code wins) and returns
    // true only if this generator's status became the new service status. The redirect
    // is kept only from the generator whose status actually won.
    ContentStatus generatorStatus = response.status();
    boolean wasSet = handleStatus(generatorStatus, generator, service);
    if (wasSet && response.isRedirect()) {
      this.redirect = response.redirectLocation();
    }

    // Accumulate response headers (last-writer-wins). Warn on headers that belong
    // to the framework, service config, or security layers rather than generators.
    response.headers().forEach((headerName, value) -> {
      if (FRAMEWORK_HEADERS.contains(headerName)) {
        LOGGER.warn("Generator {} set header '{}' which is managed by the framework — ignoring",
            generator.getClass().getName(), headerName);
      } else {
        this.responseHeaders.put(headerName, value);
      }
    });
    xml.attribute("status", generatorStatus.toString());
    if (this.profile) {
      xml.attribute("profile-etag", ProfileFormat.format(request.getProfileEtag()));
      xml.attribute("profile-process", ProfileFormat.format(end - start));
      xml.attribute("profile", ProfileFormat.format(request.getProfileEtag() + end - start));
    }
    if (this.serverTiming) {
      String safeName = name.replaceAll("[^!#$%&'*+\\-.^_`|~0-9a-zA-Z]", "_");
      ServerTimingHeader.addMetricNano(this.core.response(), "xml"+position, "Source "+safeName, request.getProfileEtag() + end - start);
    }

    // Report if requested
    GeneratorListener l = listener.get();
    if (l != null) {
      l.generate(service, generator, generatorStatus, request.getProfileEtag(), end - start);
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
    for (BerliozGenerator generator : service.generators()) {
      List<Parameter> pconfig = service.parameters(generator);
      if (pconfig.isEmpty()) {
        // No specific parameters, return a request using the common parameters
        requests.add(new HttpContentRequest(core, common, generator, match.service(), order));

      } else {
        // Some specific parameters recompute the parameters
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
   * @param generator The generator that caused the exception.
   *
   * @return a Berlioz exception for immediate use.
   */
  private BerliozException handleError(Exception exception, BerliozGenerator generator) {
    LOGGER.warn("Handling {} thrown by {}", exception.getClass().getName(), generator.getClass().getName());
    BerliozException bex = toBerliozException(exception);
    accumulateError(bex);
    return bex;
  }

  private static BerliozException toBerliozException(Exception exception) {
    if (exception instanceof BerliozException) {
      BerliozException bex = (BerliozException) exception;
      if (bex.id() == null) bex.setId(BerliozErrorID.GENERATOR_ERROR_UNFORCED);
      return bex;
    }
    if (exception instanceof InvalidParameterException) {
      InvalidParameterException ipe = (InvalidParameterException) exception;
      return new BerliozException("Invalid parameter '" + ipe.getParameterName() + "': " + ipe.getMessage(),
          ipe, BerliozErrorID.INVALID_PARAMETER);
    }
    return new BerliozException("Unexpected exception caught", exception, BerliozErrorID.GENERATOR_ERROR_UNCHECKED);
  }

  private void accumulateError(BerliozException bex) {
    if (this.exception == null) {
      this.exception = bex;
    } else if (this.exception instanceof CompoundBerliozException) {
      collectCause(((CompoundBerliozException) this.exception).getCollector(), bex);
    } else {
      ErrorCollector<Throwable> collector = new ErrorCollector<>();
      BerliozException first = this.exception;
      this.exception = new CompoundBerliozException(
          "Multiple errors thrown by generators", BerliozErrorID.GENERATOR_ERROR_MULTIPLE, collector);
      collectCause(collector, first);
      collectCause(collector, bex);
    }
  }

  private static void collectCause(ErrorCollector<Throwable> collector, BerliozException bex) {
    Throwable cause = bex.getCause();
    collector.collectQuietly(Level.ERROR, cause != null ? cause : bex);
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
  private boolean handleStatus(ContentStatus status, BerliozGenerator generator, Service service) {
    if (!service.affectStatus(generator)) return false;
    CodeRule rule = service.rule().rule();
    ContentStatus current = this.status;
    // No status yet, or the new status wins under the configured rule (highest/lowest HTTP code)
    boolean update = current == null
        || (rule == CodeRule.HIGHEST && status.code() > current.code())
        || (rule == CodeRule.LOWEST && status.code() < current.code());
    if (update) this.status = status;
    return update;
  }

  /**
   * Converts the status set by a legacy {@link ContentGenerator} on the request into a {@link Response}.
   *
   * @param request the request the legacy generator wrote its status to.
   * @return the corresponding {@code Response}
   */
  private static Response legacyResponse(HttpContentRequest request) {
    ContentStatus status = request.getStatus();
    String redirect = request.getRedirectURL();
    if (redirect != null) return Response.redirect(status, redirect);
    if (status == ContentStatus.OK) return Response.ok();
    return Response.status(status);
  }

  /**
   * Returns the etag for the specified request.
   *
   * @param request The HTTP content request.
   * @return the corresponding etag if there is one or <code>null</code>.
   */
  private String retrieveETag(HttpContentRequest request) {
    String etag = null;
    Integer key = request.order();
    if (this.etags.containsKey(key)) {
      etag = this.etags.get(key);
    } else {
      BerliozGenerator generator = request.generator();
      if (generator instanceof Cacheable) {
        long start = System.nanoTime();
        etag = ((Cacheable)generator).getETag(request);
        long end = System.nanoTime();
        request.setProfileEtag(end-start);
      }
      // Store for reuse (even if null)
      this.etags.put(key, etag != null? etag : "");
    }
    return etag != null? etag : "";
  }
}
