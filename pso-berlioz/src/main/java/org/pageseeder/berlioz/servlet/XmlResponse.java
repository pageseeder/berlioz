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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.content.BerliozGenerator;
import org.pageseeder.berlioz.content.Cacheable;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Generator;
import org.pageseeder.berlioz.content.GeneratorListener;
import org.pageseeder.berlioz.content.InvalidParameterException;
import org.pageseeder.berlioz.content.UpstreamException;
import org.pageseeder.berlioz.content.MatchingService;
import org.pageseeder.berlioz.content.Problems;
import org.pageseeder.berlioz.content.ProblemDetails;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.Service;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.http.ServerTimingHeader;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
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
 * @version 0.13.5
 * @since 0.7
 */
public final class XmlResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlResponse.class);

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

  private final GeneratorOutcome outcome = new GeneratorOutcome();

  /**
   * Response headers accumulated from generator {@link Response} objects (last-writer-wins).
   * Applied via {@code HttpServletResponse.setHeader}.
   */
  private final Map<String, String> responseHeaders = new LinkedHashMap<>();
  private final Map<String, String> responseHeadersView = Collections.unmodifiableMap(this.responseHeaders);

  private boolean serverTiming;

  /** Non-null only for direct services whose sole generator produced a problem response. */
  private @Nullable ProblemDetails topLevelProblem = null;

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
    this.requests = GeneratorDispatch.configure(this.core, match);
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
    return this.outcome.getStatus();
  }

  /**
   * Returns a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   *
   * @return a Berlioz Exception wrapping any error(s) that may have been thrown by the generators.
   */
  public @Nullable BerliozException getError() {
    return this.outcome.getError();
  }

  /**
   * Returns the URL to redirect to.
   *
   * @return the URL to redirect to.
   */
  public @Nullable String getRedirectURL() {
    return this.outcome.getRedirectURL();
  }

  /**
   * Returns the top-level problem for this response, or {@code null} if the response is not a
   * problem response.
   *
   * <p>Only set for direct services where the sole generator returned {@code Response.problem()}.
   * For non-direct services, generator problems are serialized as inline {@code <problem>}
   * elements inside the {@code <content>} wrapper, and the XSLT pipeline runs as normal.
   *
   * @return the problem details, or {@code null}
   */
  public @Nullable ProblemDetails getProblem() {
    return this.topLevelProblem;
  }

  /**
   * Returns the response headers accumulated from generators (last-writer-wins per name).
   *
   * @return an unmodifiable map; never {@code null}
   */
  public Map<String, String> getHeaders() {
    return this.responseHeadersView;
  }

  /**
   * Generates an XML response for the wrapped HTTP request and response objects.
   *
   * @return The XML content for the appropriate content generator.
   *
   * @throws IOException Should an I/O error occur.
   */
  public String generate() throws IOException {
    Service service = this.match.service();
    if (service.isDirect()) return generateDirect(service);

    // Envelope path: <?xml ...><root service="..." group="..."><header>...</header><content>...</content></root>
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer);
    xml.xmlDecl();
    xml.openElement("root", true);
    xml.attribute("service", service.id());
    xml.attribute("group", service.group());
    if (!service.flags().isEmpty()) {
      xml.attribute("flags", service.flags());
    }

    XmlResponseHeader header = new XmlResponseHeader(this.core, service, this.match.result());
    header.toXML(xml);

    int position = 0;
    for (HttpContentRequest request : this.requests) {
      toXML(request, ++position, service, xml);
    }

    xml.closeElement();
    xml.flush();
    return writer.toString();
  }

  /**
   * Direct path: single generator output IS the complete response — no {@code <root>} wrapper,
   * no {@link XmlResponseHeader}, no {@code <content>} element.
   *
   * <p>If the generator returns a problem response (explicitly or via a caught exception), the
   * output is a {@code <problem>} XML document and {@link #getProblem()} will return the details
   * so that the caller can set the {@code application/problem+xml} content type.
   */
  private String generateDirect(Service service) throws IOException {
    if (this.requests.isEmpty()) return "";
    HttpContentRequest request = this.requests.get(0);
    BerliozGenerator generator = request.generator();
    StringWriter sw = new StringWriter();
    Response response = Response.ok();
    long start = System.nanoTime();
    try {
      response = dispatchXml(generator, request, sw);
    } catch (InvalidParameterException ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forInvalidParameter(ex));
    } catch (UpstreamException ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forUpstreamException(ex));
    } catch (Exception ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forGeneratorError());
    }
    long end = System.nanoTime();
    outcome.handleStatus(response, generator, service);
    GeneratorDispatch.accumulateHeaders(generator, response, this.responseHeaders);
    GeneratorListener l = listener.get();
    if (l != null) l.generate(service, generator, response.status(), request.getProfileEtag(), end - start);

    if (response.isProblem()) {
      ProblemDetails problem = response.problem();
      this.topLevelProblem = problem;
      StringWriter sw2 = new StringWriter();
      XmlAppendable<StringWriter> problemXml = new XmlAppendable<>(sw2);
      problemXml.declaration();
      problem.toXml(problemXml);
      problemXml.flush();
      return sw2.toString();
    }
    return sw.toString();
  }

  /**
   * Dispatches a single generator for XML output, writing its content into {@code sw} and
   * returning the generator's {@link Response}. Handles all typed generator subtypes and the
   * legacy {@link ContentGenerator}.
   */
  private static Response dispatchXml(BerliozGenerator generator, HttpContentRequest request,
      StringWriter sw) throws IOException, BerliozException {
    if (generator instanceof XmlGenerator) {
      XmlAppendable<StringWriter> xw = new XmlAppendable<>(sw);
      Response resp = ((XmlGenerator) generator).generate(request, xw);
      xw.flush();
      return resp;
    }
    if (generator instanceof Generator) {
      XmlOutputAdapter oa = new XmlOutputAdapter(new XmlAppendable<>(sw));
      Response resp = ((Generator) generator).generate(request, oa);
      oa.flush();
      return resp;
    }
    if (generator instanceof ContentGenerator) {
      XMLWriter legacyXml = new XMLWriterImpl(sw);
      ((ContentGenerator) generator).process(request, legacyXml);
      legacyXml.flush();
      return legacyResponse(request);
    }
    LOGGER.warn("Unsupported generator type {} — no content written", generator.getClass().getName());
    return Response.ok();
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
    Response response = Response.ok();
    long start = System.nanoTime();
    StringWriter sw = new StringWriter();
    try {
      response = dispatchXml(generator, request, sw);
      result = sw.toString();
    } catch (InvalidParameterException ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forInvalidParameter(ex));
    } catch (UpstreamException ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forUpstreamException(ex));
    } catch (Exception ex) {
      outcome.handleError(ex, generator);
      response = Response.problem(Problems.forGeneratorError());
    }

    long end = System.nanoTime();

    ContentStatus generatorStatus = response.status();
    outcome.handleStatus(response, generator, service);

    // Accumulate response headers (last-writer-wins). Framework-owned headers are warned and dropped.
    GeneratorDispatch.accumulateHeaders(generator, response, this.responseHeaders);
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


    // Write the XML: inline problem element if the generator signalled a problem,
    // otherwise the generator's own XML output.
    if (response.isProblem()) {
      StringWriter problemSw = new StringWriter();
      response.problem().toXml(new XmlAppendable<>(problemSw));
      xml.writeXML(problemSw.toString());
      // TODO we used to have `Errors.toXML(error, xml, false);` which provided more information about the issue
    } else if (result != null) {
      xml.writeXML(result);
    }

    xml.closeElement();
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
    if (redirect != null && ContentStatus.isRedirect(status)) {
      return Response.redirect(status, redirect);
    }
    if (redirect != null) {
      // Redirect URL set without a redirect status — discard the URL (old behaviour: URL was ignored)
      LOGGER.warn("Legacy generator set redirect URL with non-redirect status {} — URL ignored", status);
    }
    if (ContentStatus.isRedirect(status)) {
      // Redirect status set without a URL — cannot construct a valid redirect response
      LOGGER.warn("Legacy generator set redirect status {} without a redirect URL", status);
      return Response.status(ContentStatus.BAD_REQUEST);
    }
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
        etag = ((Cacheable)generator).getETag((Request)request);
        long end = System.nanoTime();
        request.setProfileEtag(end-start);
      }
      // Store for reuse (even if null)
      this.etags.put(key, etag != null? etag : "");
    }
    return etag != null? etag : "";
  }
}
