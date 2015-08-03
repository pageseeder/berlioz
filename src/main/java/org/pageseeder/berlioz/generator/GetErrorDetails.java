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

import javax.servlet.http.HttpServletResponse;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.content.ContentGenerator;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.http.HttpStatusCodes;
import org.pageseeder.berlioz.servlet.ErrorHandlerServlet;
import org.pageseeder.berlioz.util.CollectedError;
import org.pageseeder.berlioz.util.CompoundBerliozException;
import org.pageseeder.berlioz.util.ErrorCollector;
import org.pageseeder.berlioz.util.Errors;
import org.pageseeder.berlioz.util.ISO8601;
import org.pageseeder.xmlwriter.XMLWriter;

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
 * <content generator="org.pageseeder.berlioz.generator.GetErrorDetails"
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
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetErrorDetails"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator is not cacheble.</code>.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32 - 29 January 2015
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

    // XXX: Copy of error handler!

    // Grab data from attributes
    String message = (String)req.getAttribute(ErrorHandlerServlet.ERROR_MESSAGE);
    Integer code   = (Integer)req.getAttribute(ErrorHandlerServlet.ERROR_STATUS_CODE);
    Throwable exception = (Throwable)req.getAttribute(ErrorHandlerServlet.ERROR_EXCEPTION);
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
    if (message != null) {
      xml.element("message", message);
    }
    if (requestURI != null) {
      xml.element("request-uri", requestURI);
    }

    if (exception != null) {
      Errors.toXML(exception, xml, true);

      // If some errors were collected, let's include them
      if (exception instanceof CompoundBerliozException) {
        xml.openElement("collected-errors");
        ErrorCollector<? extends Throwable> collector = ((CompoundBerliozException)exception).getCollector();
        for (CollectedError<? extends Throwable> collected : collector.getErrors()) {
          collected.toXML(xml);
        }
        xml.closeElement();
      }

    }
    xml.closeElement();

    // Set the status code of the generator
    ContentStatus status = ContentStatus.forCode(code);
    if (status != null) {
      req.setStatus(status);
    }

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
