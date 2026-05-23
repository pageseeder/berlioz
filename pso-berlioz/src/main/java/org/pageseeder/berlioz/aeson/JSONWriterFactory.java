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
import java.io.PrintWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that selects the most capable available {@link JSONWriter} implementation.
 *
 * <p>Discovery order:
 * <ol>
 *   <li>Jakarta JSON ({@code jakarta.json}) — preferred; supersedes Java EE</li>
 *   <li>Java EE JSON ({@code javax.json}) — legacy fallback</li>
 *   <li>Built-in writer — always available, no extra dependency</li>
 * </ol>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
public final class JSONWriterFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(JSONWriterFactory.class);

  private static final String JAKARTA_API = "jakarta.json.stream.JsonGenerator";
  private static final String J2EE_API    = "javax.json.stream.JsonGenerator";

  /** Status values */
  private static final int UNINITIALIZED = 0;
  private static final int JAKARTA       = 1;
  private static final int J2EE          = 2;
  private static final int BUILTIN       = 3;

  private static volatile int status = UNINITIALIZED;

  private JSONWriterFactory() {}

  /**
   * Returns a {@link JSONWriter} writing to the given stream.
   *
   * @param out the destination stream
   * @return a ready-to-use JSON writer
   */
  public static JSONWriter newInstance(OutputStream out) {
    if (status == UNINITIALIZED) init();
    if (status == JAKARTA) return JakartaJSONWriter.newInstance(out);
    if (status == J2EE)    return J2EEJSONWriter.newInstance(out);
    return new BuiltinJSONWriter(new PrintWriter(out));
  }

  /**
   * Returns a {@link JSONWriter} writing to the given character stream.
   *
   * @param writer the destination writer
   * @return a ready-to-use JSON writer
   */
  public static JSONWriter newInstance(Writer writer) {
    if (status == UNINITIALIZED) init();
    if (status == JAKARTA) return JakartaJSONWriter.newInstance(writer);
    if (status == J2EE)    return J2EEJSONWriter.newInstance(writer);
    return new BuiltinJSONWriter(new PrintWriter(writer));
  }

  /**
   * Probes the classpath and locks in the best available implementation.
   * Safe to call multiple times — only the first call does work.
   */
  public static synchronized void init() {
    if (status != UNINITIALIZED) return;
    LOGGER.debug("Initializing Aeson JSON writer");
    if (isAvailable(JAKARTA_API) && JakartaJSONWriter.init()) {
      LOGGER.info("Using Jakarta JSON API (jakarta.json)");
      status = JAKARTA;
    } else if (isAvailable(J2EE_API) && J2EEJSONWriter.init()) {
      LOGGER.info("Using J2EE JSON API (javax.json)");
      status = J2EE;
    } else {
      LOGGER.info("No JSON API found; using built-in writer");
      status = BUILTIN;
    }
  }

  private static boolean isAvailable(String className) {
    try {
      // Use the thread context class loader so that API jars in the web-app classloader
      // are found when Berlioz itself is loaded by the container classloader.
      // initialize=false: we only probe for presence — no need to run static initializers.
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class.forName(className, false, cl != null ? cl : JSONWriterFactory.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException ex) {
      LOGGER.debug("JSON API class {} not on classpath", className);
      return false;
    }
  }

}
