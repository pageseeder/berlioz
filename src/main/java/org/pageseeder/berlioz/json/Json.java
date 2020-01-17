/*
 * Copyright 2020 Allette Systems (Australia)
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
package org.pageseeder.berlioz.json;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An utility class for JSON.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
public class Json {

  /**
   * Displays debug information.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(Json.class);

  enum JsonProvider {

    /**
     * Jackson implementation.
     */
    JACKSON("com.fasterxml.jackson.core.JsonGenerator") {
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return JacksonJsonWriter.newInstance(out);
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        return JacksonJsonWriter.newInstance(writer);
      }
    },

    /**
     * Google's Gson implementation.
     */
    GSON("com.google.gson.stream.JsonWriter") {
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return GsonJsonWriter.newInstance(out);
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        return GsonJsonWriter.newInstance(writer);
      }
    },

    /**
     * Oracle's JSONP implementation.
     */
    JSONP("javax.json.stream.JsonGenerator") {
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return J2eeJsonWriter.newInstance(out);
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        return J2eeJsonWriter.newInstance(writer);
      }
    },

    /**
     * Builtin (buggy implementation)
     */
    BUILTIN("org.pageseeder.berlioz.json.BuiltinJsonWriter") {
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return new BuiltinJsonWriter(new PrintWriter(out));
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        return new BuiltinJsonWriter(new PrintWriter(writer));
      }
    },

    UNKNOWN("") {
      @Override
      public JsonWriter newWriter(OutputStream out) {
        throw new UnsupportedOperationException("No JSON provider available!");
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        throw new UnsupportedOperationException("No JSON provider available!");
      }
    };

    /**
     * Class name to look for
     */
    private final String _className;

    JsonProvider(String className) {
      this._className = className;
    }

    public String className() {
      return this._className;
    }

    /**
     * Always return a JSON Writer.
     *
     * @param out The stream receiving the JSON output.
     *
     * @return The JSON writer to use.
     */
    public abstract JsonWriter newWriter(OutputStream out);

    /**
     * Always return a JSON Writer.
     *
     * @param writer The writer receiving the JSON output.
     *
     * @return The JSON writer to use.
     */
    public abstract JsonWriter newWriter(Writer writer);

  }

  /** Indicates whether we have identified our json provider. */
  private static volatile JsonProvider provider = JsonProvider.UNKNOWN;

  /** No public constructor */
  private Json() {
  }

  /**
   * Always return a JSON Writer.
   *
   * @param out The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JsonWriter newWriter(OutputStream out) {
    if (provider == JsonProvider.UNKNOWN) {
      init();
    }
    return provider.newWriter(out);
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The writer receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JsonWriter newWriter(Writer writer) {
    if (provider == JsonProvider.UNKNOWN) {
      init();
    }
    return provider.newWriter(writer);
  }

  /**
   * Initializes this class.
   */
  public static synchronized void init() {
    LOGGER.debug("Identifying Json provider");
    for (JsonProvider p : JsonProvider.values()) {
      if (hasClass(p.className())) {
        LOGGER.info("Using {} as JSON provider", p.className());
        Json.provider = p;
        if (p == JsonProvider.BUILTIN) {
          LOGGER.warn("No JSON implementation found - falling back on builtin implementation");
        }
      }
    }
  }

  /**
   * Indicates whether a class is available in the classpath.
   *
   * @param className the name of the class to look for
   *
   * @return <code>true</code> if available; <code>false</code> otherwise.
   */
  private static boolean hasClass(String className) {
    try  {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException ex) {
      return false;
    }
  }

}
