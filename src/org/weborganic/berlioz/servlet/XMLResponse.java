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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.Cacheable;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentManager;
import org.weborganic.berlioz.content.Environment;
import org.weborganic.berlioz.content.MatchingService;
import org.weborganic.berlioz.content.Service;

import com.topologi.diffx.xml.XMLWriter;
import com.topologi.diffx.xml.XMLWriterImpl;

/**
 * An XML response produced from content generators.
 * 
 * <p>This class is not thread-safe.
 * 
 * @author Christophe Lauret (Weborganic)
 * @version 31 May 2010
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
   * Wraps the request and response to be supplied to the generators.
   */
  private final HttpRequestWrapper wrapper;

  /**
   * Indicates whether we have attempted to match the specified service.
   */
  private transient boolean attemptedMatch = false;

  /**
   * The service that was matched for the given request.
   */
  private transient MatchingService match = null;

  /**
   * Creates a new XML response for the specified arguments.
   * 
   * @param req The HTTP servlet request.
   * @param res The HTTP servlet response.
   * @param env The current environment.
   */
  public XMLResponse(HttpServletRequest req, HttpServletResponse res, Environment env) {
    this._req = req;
    this._res = res;
    this._env = env;
    this.wrapper = new HttpRequestWrapper(this._req, this._res, this._env);
  }

  /**
   * Indicates whether this response is cacheable.
   * 
   * <p>A response is cacheable only is the service has been found and all its generators are 
   * cacheable.
   * 
   * @return <code>true</code> if this response is cacheable;
   *         <code>false</code> otherwise.
   */
  public boolean isCacheable() {
    Service service = getService();
    if (service == null) return false;
    for (ContentGenerator generator : service.generators()) {
      if (!(generator instanceof Cacheable)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the Etag for this response.
   * 
   * <p>The Etag is computed from the Etags returned by each generator.
   * If any one of the generators is not cacheable, this response is not considered cacheable
   * and the Etag returned will be <code>null</code>.
   * 
   * @return the Etag for this response if it is cacheable; <code>null</code> if it is not.
   */
  public String getEtag() {
    Service service = getService();
    boolean cacheable = service != null;
    StringBuilder etag = new StringBuilder();
    if (cacheable) {
      for (ContentGenerator generator : service.generators()) {
        // Set the request parameters (if necessary)
        wrapper.configure(this.match, generator);
        // Check if cacheable
        if (generator instanceof Cacheable) {
          etag.append(((Cacheable)generator).getETag(wrapper)).append("/");
        } else {
          cacheable = false;
          break;
        }
      }
    }
    return cacheable? etag.toString() : null;
  }

  /**
   * Returns the service corresponding to this response.
   * 
   * @return the service corresponding to this response.
   */
  public Service getService() {
    match();
    return this.match != null? this.match.service() : null;
  }

  /**
   * Generates an XML response for the wrapped HTTP request and response objects.
   * 
   * @return The XML content for the appropriate content generator.
   * 
   * @throws IOException Should an I/O error occur.
   */
  public String generate() throws IOException {
    match();
    try {
      // Initialise the writer
      StringWriter writer = new StringWriter();
      XMLWriter xml = new XMLWriterImpl(writer);
      xml.xmlDecl();
      xml.openElement("root", true);

      // if the service exists
      if (this.match != null) {
        Service service = this.match.service();
        LOGGER.debug(this._req.getPathInfo()+" -> "+service);
        XMLResponseHeader header = new XMLResponseHeader(this._req, service, this.match.result());
        header.toXML(xml);

        for (ContentGenerator generator : service.generators()) {
          // Set the request parameters
          this.wrapper.configure(this.match, generator);
          toXML(generator, service, xml);
        }

      // the content generator does not exist
      } else {
        LOGGER.info("No service for "+this._req.getPathInfo());
        XMLResponseHeader header = new XMLResponseHeader(this._req, "404-error");
        header.toXML(xml);
      }

      xml.closeElement(); // close 'root'
      xml.flush();
      return writer.toString();

    // if an error occurs generate the proper content
    } catch (Exception ex) {
      return generateError(this._req, this._res, ex);
    }
  }

  /**
   * Generates an XML response corresponding to the specified HTTP request.
   * 
   * @deprecated
   * 
   * @param req The HTTP servlet request.
   * @param res The HTTP servlet response.
   * 
   * @return The XML content for the appropriate content generator.
   * 
   * @throws IOException Should an I/O error occur.
   */
  public static String generate(HttpServletRequest req, HttpServletResponse res, Environment env) throws IOException {
    XMLResponse response = new XMLResponse(req, res, env);
    return response.generate();
  }

  /**
   * Generates the XML content for when an error occurs while generating the content.
   * 
   * @param req The HTTP servlet request.
   * @param res The HTTP servlet response.
   * @param ex  The exception that was thrown.
   * 
   * @return The corresponding content.
   * 
   * @throws IOException Should an I/O error occur.
   */
  private String generateError(HttpServletRequest req, HttpServletResponse res, Exception ex) throws IOException {
    StringWriter writer = new StringWriter();
    XMLWriter xml = new XMLWriterImpl(writer);
    xml.xmlDecl();
    xml.openElement("root", true);
    new XMLResponseHeader(req, "500-error").toXML(xml);
    xml.openElement("content", true);
    if (ex instanceof BerliozException) {
      ((BerliozException)ex).toXML(xml);
    } else {
      new BerliozException("An unexcepted error occurred", ex).toXML(xml);
    }
    xml.closeElement(); // close 'content'
    xml.closeElement(); // close 'root'
    xml.flush();
    return writer.toString();
  }

  // Private helpers
  // ----------------------------------------------------------------------------------------------

  /**
   * Generates the XML content for one generator.
   * 
   * @param generator The generator to invoke.
   * @param service   The service it is part of.
   * @param xml       The XML Writer to use.
   * 
   * @throws IOException      Should an I/O error occur while writing XML.
   * @throws BerliozException Any exception occurring during processing will be wrapped in a BerliozException.
   */
  private void toXML(ContentGenerator generator, Service service, XMLWriter xml) throws IOException, BerliozException {
    // Generate the main element
    xml.openElement("content", true);
    xml.attribute("generator", generator.getClass().getName());
    String name = service.name(generator);
    xml.attribute("name", name);
    String target = service.target(generator);
    if (target != null) xml.attribute("target", target);

    // Detect if deprecated
    if (generator.getClass().isAnnotationPresent(Deprecated.class))
      xml.attribute("deprecated", "true");

    // Let's invoke the generator
    String result = null;
    BerliozException error = null;
    try {
      // Normal response
      StringWriter writer = new StringWriter();
      XMLWriter ok = new XMLWriterImpl(writer);
      generator.process(wrapper, ok);
      result = writer.toString();
    } catch (BerliozException ex) {
      error = ex;
    } catch (Exception ex) {
      // We wrapping any exception in a Berlioz Exception
      error = new BerliozException("Unexpected exception caught", ex);
    }

    // Write the XML
    xml.attribute("status", error != null? "error" : "ok");
    if (error != null) error.toXML(xml);
    else xml.writeXML(result);

    xml.closeElement();
  }

  /**
   * Attempts to find the service corresponding to the request.
   */
  private void match() {
    if (!this.attemptedMatch) {
      this.match = ContentManager.getInstance(this._req.getPathInfo());
      this.attemptedMatch = true;
    }
  }

}
