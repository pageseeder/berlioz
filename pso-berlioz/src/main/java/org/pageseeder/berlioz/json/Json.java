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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for JSON.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.12.0
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
     * Jakarta JSON API implementation (jakarta.json).
     */
    JAKARTA_JSONP("jakarta.json.stream.JsonGenerator") {
      @Override
      public boolean available() {
        return hasClass(className()) && JakartaJsonWriter.init();
      }
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return JakartaJsonWriter.newInstance(out);
      }
      @Override
      public JsonWriter newWriter(Writer writer) {
        return JakartaJsonWriter.newInstance(writer);
      }
    },

    /**
     * Oracle's JSONP implementation (javax.json).
     */
    JSONP("javax.json.stream.JsonGenerator") {
      @Override
      public boolean available() {
        return hasClass(className()) && J2eeJsonWriter.init();
      }
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
     * Builtin fallback implementation.
     */
    BUILTIN("org.pageseeder.berlioz.json.BuiltinJsonWriter") {
      @Override
      public boolean available() {
        return true;
      }
      @Override
      public JsonWriter newWriter(OutputStream out) {
        return new BuiltinJsonWriter(new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8)));
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
    private final String className;

    JsonProvider(String className) {
      this.className = className;
    }

    public String className() {
      return this.className;
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

    /**
     * Indicates whether this provider can be used.
     *
     * @return {@code true} if this provider is available.
     */
    public boolean available() {
      return hasClass(className());
    }

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
   * Indicates whether the specified bare media type identifies JSON content.
   *
   * <p>A media type is considered JSON if:</p>
   * <ul>
   *   <li>it is {@code application/json}, the canonical JSON media type (RFC 8259 §8.1); or</li>
   *   <li>its subtype ends with the {@code +json} structured syntax suffix (RFC 6839 §3.1),
   *       covering types such as {@code application/geo+json} or {@code application/problem+json}.</li>
   * </ul>
   *
   * <p>The comparison is case-insensitive as required by RFC 2045 §5.1.
   * Media type parameters (e.g. {@code ;charset=utf-8}) must be stripped before calling this method.</p>
   *
   * @param mediaType the bare media type to test, without parameters
   * @return {@code true} if the media type represents JSON content; {@code false} for {@code null} or non-JSON
   */
  public static boolean isJsonMediaType(@Nullable String mediaType) {
    if (mediaType == null) return false;
    String type = mediaType.trim().toLowerCase(Locale.ROOT);
    return "application/json".equals(type) || type.endsWith("+json");
  }

  /**
   * Converts a hyphen-separated name to camelCase.
   *
   * <p>For example, {@code "hello-world"} becomes {@code "helloWorld"}.</p>
   *
   * @param name the hyphenated name to convert
   * @return the camelCase equivalent
   * @throws NullPointerException if the name is null
   */
  public static String camelify(String name) {
    int dash = name.indexOf('-');
    return dash < 0? name : camelify(name, dash);
  }

  private static String camelify(String name, int from) {
    StringBuilder sb = new StringBuilder(name);
    for (int i = from; i < sb.length()-1; i++) {
      if (sb.charAt(i) == '-') {
        sb.deleteCharAt(i);
        sb.replace(i, i+1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
      }
    }
    return sb.toString();
  }

  /**
   * Returns the name of the active JSON provider.
   *
   * <p>Calls {@link #init()} if the provider has not been detected yet. Returns one of
   * {@code "JACKSON"}, {@code "GSON"}, {@code "JSONP"}, {@code "JAKARTA_JSONP"},
   * {@code "BUILTIN"}, or {@code "UNKNOWN"}.</p>
   *
   * @return the provider name.
   */
  public static String providerName() {
    if (provider == JsonProvider.UNKNOWN) {
      init();
    }
    return provider.name();
  }

  /**
   * Detects and initializes the first available JSON provider.
   *
   * <p>Provider priority (highest to lowest): Jackson, Gson, Jakarta JSON
   * (jakarta.json), JSONP (javax.json), builtin. Only the first detected provider is used.</p>
   */
  public static synchronized void init() {
    if (provider != JsonProvider.UNKNOWN) return;
    LOGGER.debug("Identifying Json provider");
    for (JsonProvider p : JsonProvider.values()) {
      if (p == JsonProvider.UNKNOWN) continue;
      if (p.available()) {
        Json.provider = p;
        if (p == JsonProvider.BUILTIN) {
          LOGGER.warn("No JSON implementation found - falling back on builtin implementation");
        } else {
          LOGGER.info("Using {} as JSON provider", p.className());
        }
        return;
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
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      Class.forName(className, false, cl != null ? cl : Json.class.getClassLoader());
      return true;
    } catch (ClassNotFoundException | LinkageError ex) {
      return false;
    }
  }

  static void checkFinite(double number) {
    if (!Double.isFinite(number)) {
      throw new IllegalArgumentException("JSON does not support non-finite double values: " + number);
    }
  }

}
