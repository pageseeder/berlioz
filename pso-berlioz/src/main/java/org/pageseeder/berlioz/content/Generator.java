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
import org.pageseeder.berlioz.output.OutputWriter;

import java.util.Set;

/**
 * A format-agnostic generator that produces either XML or JSON depending on the request.
 *
 * <p>This is the preferred generator type for the majority of services. Berlioz selects the
 * output format at request time based on content negotiation (Accept header or URI extension)
 * and passes an {@link OutputWriter} backed by the appropriate format:</p>
 * <ul>
 *   <li>XML path: {@code out.isXml()} is true; XSLT may be applied to the result.</li>
 *   <li>JSON path: {@code out.isJson()} is true; the JSON is served directly without XSLT.</li>
 * </ul>
 *
 * <p>Use {@link XmlGenerator} or {@link JsonGenerator} when the XML and JSON representations
 * are structurally different enough that sharing a single method body through
 * {@link OutputWriter} would be awkward.</p>
 *
 * <p>Write failures are reported as unchecked {@link org.pageseeder.berlioz.util.WriteFailureException}
 * and are caught by the framework dispatch layer. Logical errors should be reported by
 * returning {@link Response#problem(ProblemDetails)} rather than by throwing exceptions.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public interface Generator extends BerliozGenerator {

  @Override
  default Set<OutputType> supported() {
    return Set.of(OutputType.XML, OutputType.JSON);
  }

  /**
   * Generates content for the given request in the format determined by the output writer.
   *
   * @param req the incoming request context
   * @param out the output writer; call {@link OutputWriter#isJson()} to branch on format if needed
   * @return the response describing the HTTP status, headers, or problem details
   */
  Response generate(RequestContext req, OutputWriter out);

}
