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
package org.pageseeder.berlioz.aeson;

import java.io.OutputStream;
import java.io.Writer;

import org.pageseeder.berlioz.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for Aeson-compatible JSON writers.
 *
 * @deprecated since 0.13.0. Use {@link Json#newWriter(OutputStream)} or
 *             {@link Json#newWriter(Writer)} instead.
 *
 * <p>The returned writers delegate to {@link Json#newWriter(OutputStream)} or
 * {@link Json#newWriter(Writer)}, so Aeson uses the same JSON provider selection as the rest of
 * Berlioz while preserving the original {@link JSONWriter} API.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
@Deprecated(since = "0.13.0")
public final class JSONWriterFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JSONWriterFactory.class);

  private JSONWriterFactory() {}

  /**
   * Returns a {@link JSONWriter} writing to the given stream.
   *
   * @param out the destination stream
   * @return a ready-to-use JSON writer
   */
  public static JSONWriter newInstance(OutputStream out) {
    return new JsonBackedJSONWriter(Json.newWriter(out));
  }

  /**
   * Returns a {@link JSONWriter} writing to the given character stream.
   *
   * @param writer the destination writer
   * @return a ready-to-use JSON writer
   */
  public static JSONWriter newInstance(Writer writer) {
    return new JsonBackedJSONWriter(Json.newWriter(writer));
  }

  /**
   * Probes the classpath and locks in the best available implementation.
   * Safe to call multiple times.
   */
  public static synchronized void init() {
    LOGGER.debug("Initializing Aeson JSON writer through org.pageseeder.berlioz.json");
    Json.init();
  }

}
