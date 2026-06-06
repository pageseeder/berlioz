/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.pageseeder.berlioz.output.OutputType;
import org.pageseeder.berlioz.xml.XmlWriter;

import java.util.Set;

/**
 * A generator that produces XML output.
 *
 * <p>The XML produced by this generator may be further transformed by XSLT into any
 * text-based format (HTML, XML, plain text). Services that mix {@code XmlGenerator}
 * instances with {@link Generator} instances are restricted to the XML output path.</p>
 *
 * <p>A class may implement both {@code XmlGenerator} and {@link JsonGenerator} to provide
 * distinct XML and JSON implementations. In that case the compiler requires the class to
 * override {@link #supported()} and return {@code Set.of(OutputType.XML, OutputType.JSON)}.</p>
 *
 * <p>Write failures are reported as unchecked {@link org.pageseeder.berlioz.xml.XmlWriteFailureException}
 * and are caught by the framework dispatch layer. Logical errors should be reported by
 * returning {@link Response#problem(ProblemDetails)} rather than by throwing exceptions.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public interface XmlGenerator extends BerliozGenerator {

  @Override
  default Set<OutputType> supported() {
    return Set.of(OutputType.XML);
  }

  /**
   * Generates XML content for the given request.
   *
   * @param req the incoming request context
   * @param xml the XML writer to write content to
   * @return the response describing the HTTP status, headers, or problem details
   */
  Response generate(Request req, XmlWriter xml);

}
