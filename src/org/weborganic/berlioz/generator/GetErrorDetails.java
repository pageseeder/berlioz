/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.generator;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.weborganic.berlioz.BerliozErrorID;
import org.weborganic.berlioz.BerliozException;
import org.weborganic.berlioz.content.ContentGenerator;
import org.weborganic.berlioz.content.ContentRequest;
import org.weborganic.berlioz.content.ContentStatus;
import org.weborganic.berlioz.http.HttpStatusCodes;
import org.weborganic.berlioz.servlet.ErrorHandlerServlet;
import org.weborganic.berlioz.util.CollectedError;
import org.weborganic.berlioz.util.CompoundBerliozException;
import org.weborganic.berlioz.util.ErrorCollector;
import org.weborganic.berlioz.util.Errors;
import org.weborganic.berlioz.util.ISO8601;

import com.topologi.diffx.xml.XMLWriter;

/**
 * Generates no content.
 *
 * <p>This content generator is useful to display information about errors, when a
 * Berlioz service is used for error handling.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration directly associated with this generator.</p>
 * <p>However, since the purpose is to display servlet error details, the services
 * using this generator should have their URLs mapped in the Web descriptor for error
 * catching.
 * For example:
 * <pre>{@code
 * <error-page>
 *   <error-code>404</error-code>
 *   <location>/not-found.html</location>
 * </error-page>
 * }</pre>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use and require any parameter.
 *
 * <h3>Attributes</h3>
 * <p>This generator will try to retrieve values from the standard Servlet error request attributes.
 *
 * <h3>Returned XML</h3>
 * <p>This generator does not have any content, so the XML content is always empty.
 * <p>Since Berlioz always wraps generators' content, the final XML is always:
 * <pre>{@code
 * <content generator="org.weborganic.berlioz.generator.GetErrorDetails"
 *               name="[name]" target="[target]" status="ok">
 *   <error http-class="[http-class]" http-code="[http-code]" datetime="[iso8601-datetime]" id="[berlioz-id]">
 *     <title>Not Found</title>
 *     <message>Not Found</message>
 *     <request-uri>/fdhvjfdls</request-uri>
 *     <!-- Any exception will be serialised a XML here -->
 *   </error>
 * </content>
 * }</pre>
 *
 * <p><i>Note: since this generator does produce any data, the return status is always
 * <code>ok</code>.</i></p>
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.weborganic.berlioz.generator.GetErrorDetails"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator is not cacheble.</code>.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.27 - 17 December 2013
 * @since Berlioz 0.8.7
 */
public final class GetErrorDetails implements ContentGenerator {

  /**
   * Display the error details.
   *
   * {@inheritDoc}
   */
  @Override
  public void process(ContentRequest req, XMLWriter xml) throws IOException {

    // Grab data from attributes
    String message = (String)req.getAttribute(ErrorHandlerServlet.ERROR_MESSAGE);
    Integer code   = (Integer)req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);
    Exception exception = (Exception)req.getAttribute(ErrorHandlerServlet.ERROR_EXCEPTION);
    String requestURI = (String)req.getAttribute(ErrorHandlerServlet.ERROR_REQUEST_URI);
    String errorId = (String)req.getAttribute(ErrorHandlerServlet.BERLIOZ_ERROR_ID);

    // Ensure we have a status code
    if (code == null) {
      code = Integer.valueOf(HttpServletResponse.SC_OK);
    }

    xml.openElement("error");
    xml.attribute("http-class", getHTTPClass(code));
    xml.attribute("http-code", code);
    xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));

    // If it has a Berlioz ID
    if (exception instanceof BerliozException && ((BerliozException)exception).id() != null) {
      xml.attribute("id", ((BerliozException)exception).id().id());
    } else {
      xml.attribute("id", errorId != null? errorId : BerliozErrorID.UNEXPECTED.toString());
    }

    // Other informational elements
    String title = HttpStatusCodes.getTitle(code);
    xml.element("title", title != null? title : "Berlioz Status");
    if (message != null)
      xml.element("message", message);
    if (requestURI != null)
      xml.element("request-uri", requestURI);

    if (exception != null) {
      Errors.toXML(exception, xml);

      // If some errors were collected, let's include them
      if (exception instanceof CompoundBerliozException) {
        xml.openElement("collected-errors");
        ErrorCollector<? extends Exception> collector = ((CompoundBerliozException)exception).getCollector();
        for (CollectedError<? extends Exception> collected : collector.getErrors()) {
          collected.toXML(xml);
        }
        xml.closeElement();
      }

    }
    xml.closeElement();

    // Set the status code of the generator
    ContentStatus status = ContentStatus.forCode(code);
    if (status != null)
      req.setStatus(status);

  }

  /**
   * Return the root element name based on the status code.
   *
   * @param code the HTTP status code.
   * @return the root element name based on the HTTP status code or "unknown-status";
   */
  private static String getHTTPClass(Integer code) {
    String element = HttpStatusCodes.getClassOfStatus(code);
    return (element != null)? element.toLowerCase().replace(' ', '-') : "unknown-status";
  }
}
