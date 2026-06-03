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

import org.jspecify.annotations.Nullable;

import org.pageseeder.berlioz.BerliozErrorID;
import org.pageseeder.berlioz.BerliozException;
import org.pageseeder.berlioz.ErrorID;
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
 * Returns HTTP error details as XML.
 *
 * <p>This content generator is designed for use in error-handling services, where it reads
 * the standard servlet error request attributes and serializes them as XML.
 *
 * <h3>Configuration</h3>
 * <p>There is no configuration directly associated with this generator.</p>
 * <p>Since the purpose is to display servlet error details, services using this generator
 * should have their URLs mapped in the web descriptor for error catching. For example:
 * <pre>{@code
 * <error-page>
 *   <error-code>404</error-code>
 *   <location>/not-found.html</location>
 * </error-page>
 * }</pre>
 *
 * <h3>Parameters</h3>
 * <p>This generator does not use any parameter.
 *
 * <h3>Attributes</h3>
 * <p>This generator reads the following standard servlet error request attributes:
 * <ul>
 *   <li>{@code javax.servlet.error.status_code} — the HTTP status code</li>
 *   <li>{@code javax.servlet.error.message} — the error message</li>
 *   <li>{@code javax.servlet.error.exception} — the exception that caused the error</li>
 *   <li>{@code javax.servlet.error.request_uri} — the URI that triggered the error</li>
 *   <li>{@code org.pageseeder.berlioz.error_id} — an optional Berlioz-specific error ID</li>
 * </ul>
 *
 * <h3>Returned XML</h3>
 * <pre>{@code
 * <error http-class="[http-class]" http-code="[http-code]" datetime="[iso8601-datetime]" id="[error-id]">
 *   <title>[http-status-title]</title>
 *   <message>[error-message]</message>           <!-- only if a message is available -->
 *   <request-uri>[request-uri]</request-uri>     <!-- only if a request URI is available -->
 *   <!-- exception XML if an exception was thrown -->
 *   <collected-errors>                           <!-- only for CompoundBerliozException -->
 *     ...
 *   </collected-errors>
 * </error>
 * }</pre>
 * <p>The {@code http-class} attribute is the kebab-case name of the HTTP status class
 * (e.g. {@code client-error}, {@code server-error}). The {@code id} attribute is taken from
 * {@link BerliozException#id()} when available, or from the Berlioz error ID attribute,
 * falling back to {@link BerliozErrorID#UNEXPECTED}.
 *
 * <h3>Usage</h3>
 * <p>To use this generator in Berlioz (in <code>/WEB-INF/config/services.xml</code>):
 * <pre>{@code <generator class="org.pageseeder.berlioz.generator.GetErrorDetails"
 *                         name="[name]" target="[target]"/>}</pre>
 *
 * <h3>Etag</h3>
 * <p>This generator is not cacheable.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.8.7
 */
public final class GetErrorDetails implements ContentGenerator {

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
      code = HttpServletResponse.SC_OK;
    }

    xml.openElement("error");
    xml.attribute("http-class", getHTTPClass(code));
    xml.attribute("http-code", code);
    xml.attribute("datetime", ISO8601.format(System.currentTimeMillis(), ISO8601.DATETIME));
    xml.attribute("id", resolveErrorId(exception, errorId));

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
      writeException(exception, xml);
    }
    xml.closeElement();

    // Set the status code of the generator
    ContentStatus status = ContentStatus.forCode(code);
    if (status != null) {
      req.setStatus(status);
    }

  }

  private static String resolveErrorId(@Nullable Throwable exception, @Nullable String errorId) {
    if (exception instanceof BerliozException) {
      ErrorID id = ((BerliozException) exception).id();
      if (id != null) return id.id();
    }
    return errorId != null? errorId : BerliozErrorID.UNEXPECTED.toString();
  }

  private static void writeException(Throwable exception, XMLWriter xml) throws IOException {
    Errors.toXML(exception, xml, true);
    if (exception instanceof CompoundBerliozException) {
      xml.openElement("collected-errors");
      ErrorCollector<? extends Throwable> collector = ((CompoundBerliozException) exception).getCollector();
      for (CollectedError<? extends Throwable> collected : collector.getErrors()) {
        collected.toXML(xml);
      }
      xml.closeElement();
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
