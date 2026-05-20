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
 * This factory method will try to find the most suitable <code>JSONEmitter</code> implementation
 * to write JSON.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public final class JSONWriterFactory {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(JSONWriterFactory.class);

  /**
   * Name of the J2EE API class to look for.
   */
  private static final String J2EE_API = "javax.json.stream.JsonGenerator";

  /** Indicates whether we have checked whether Aeson is available. */
  private static volatile int status = 0;

  /** No public constructor */
  private JSONWriterFactory() {
  }

  /**
   * Always return a JSON Writer.
   *
   * @param out The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JSONWriter newInstance(OutputStream out) {
    if (status == 0) {
      init();
    }
    if (status == 1) return J2EEJSONWriter.newInstance(out);
    else return new BuiltinJSONWriter(new PrintWriter(out));
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The writer receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JSONWriter newInstance(Writer writer) {
    if (status == 0) {
      init();
    }
    if (status == 1) return J2EEJSONWriter.newInstance(writer);
    return new BuiltinJSONWriter(new PrintWriter(writer));
  }

  /**
   * Initializes this class.
   */
  public static synchronized void init() {
    LOGGER.debug("Initializing Aeson");
    try {
      Class.forName(J2EE_API);
      LOGGER.info("JSON API found");
      boolean hasProvider = J2EEJSONWriter.init();
      status = hasProvider? 1 : 2;
    } catch (ClassNotFoundException x) {
      LOGGER.warn("JSON API not found - ");
      status = 2;
    }
  }

}
