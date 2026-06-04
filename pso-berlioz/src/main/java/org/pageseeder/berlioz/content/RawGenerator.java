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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

/**
 * A generator that writes raw bytes directly to the response output stream.
 *
 * <p>Raw generators bypass all Berlioz output processing: no XML envelope, no XSLT
 * transformation, no JSON wrapping. The generator is responsible for writing the complete
 * response body and should declare the appropriate media type via
 * {@code Response.ok().header("Content-Type", "image/png")} (or similar).
 * The default media type when no {@code Content-Type} header is set is
 * {@code application/octet-stream}.</p>
 *
 * <p>A service may contain at most one {@code RawGenerator}. Mixing a {@code RawGenerator}
 * with any other generator type produces an empty supported-format intersection and
 * triggers a startup error.</p>
 *
 * <p>Unlike the other generator types, {@code IOException} is declared on {@link #generate}
 * because {@link OutputStream} is a raw Java type with no abstraction layer to suppress it.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.2
 * @since 0.13.2
 */
public interface RawGenerator extends BerliozGenerator {

  @Override
  default Set<OutputType> supported() {
    return Set.of(OutputType.RAW);
  }

  /**
   * Writes raw bytes for the given request.
   *
   * @param req the incoming request context
   * @param out the response output stream
   * @return the response describing the HTTP status and headers; body is written directly to {@code out}
   * @throws IOException if an I/O error occurs while writing to the output stream
   */
  Response generate(RequestContext req, OutputStream out) throws IOException;

}
