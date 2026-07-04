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

import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.XmlGenerator;
import org.pageseeder.berlioz.error.LegacyError;
import org.pageseeder.berlioz.xml.XmlWriter;

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
 *   <berlioz version="[version]"/>
 *   <title>[http-status-title]</title>
 *   <message>[error-message]</message>           <!-- only if a message is available -->
 *   <request-uri>[request-uri]</request-uri>     <!-- only if a request URI is available -->
 *   <!-- exception XML at STANDARD/FULL detail level only -->
 *   <collected-errors>                           <!-- only for CompoundBerliozException -->
 *     ...
 *   </collected-errors>
 * </error>
 * }</pre>
 * <p>The {@code http-class} attribute is the kebab-case name of the HTTP status class
 * (e.g. {@code client-error}, {@code server-error}). The {@code id} attribute is taken from
 * {@link org.pageseeder.berlioz.BerliozException#id()} when available, or from the Berlioz
 * error ID attribute, falling back to {@link org.pageseeder.berlioz.BerliozErrorID#UNEXPECTED}.
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
 * @version 0.14.0
 * @since 0.8.7
 */
public final class GetErrorDetails implements XmlGenerator {

  @Override
  @SuppressWarnings("deprecation") // LegacyError removed in 1.0; intentional use until GetErrorDetails is retired
  public Response generate(Request req, XmlWriter xml) {
    LegacyError error = LegacyError.of(req);
    error.toXml(xml);
    ContentStatus status = ContentStatus.forCode(error.getCode());
    return status != null ? Response.status(status) : Response.ok();
  }
}
