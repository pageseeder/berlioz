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

import org.pageseeder.berlioz.json.JsonWriter;
import org.pageseeder.berlioz.output.OutputType;

import java.util.Set;

/**
 * A generator that produces JSON output directly.
 *
 * <p>JSON produced by this generator is served directly to the client. XSLT transformation
 * is never applied. Services containing a {@code JsonGenerator} cannot be combined with
 * {@link XmlGenerator} instances, as this would produce an empty supported-format
 * intersection and trigger a startup error.</p>
 *
 * <p>A class may implement both {@code JsonGenerator} and {@link XmlGenerator} to provide
 * distinct JSON and XML implementations. In that case the compiler requires the class to
 * override {@link #supported()} and return {@code Set.of(OutputType.XML, OutputType.JSON)}.</p>
 *
 * <p>Write failures are reported as unchecked {@link org.pageseeder.berlioz.json.JsonWriteFailureException}
 * and are caught by the framework dispatch layer. Logical errors should be reported by
 * returning {@link Response#problem(ProblemDetails)} rather than by throwing exceptions.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public interface JsonGenerator extends BerliozGenerator {

  @Override
  default Set<OutputType> supported() {
    return Set.of(OutputType.JSON);
  }

  /**
   * Generates JSON content for the given request.
   *
   * @param req  the incoming request context
   * @param json the JSON writer to write content to
   * @return the response describing the HTTP status, headers, or problem details
   */
  Response generate(Request req, JsonWriter json);

}
