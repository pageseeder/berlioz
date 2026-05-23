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
package org.pageseeder.berlioz.aeson;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collections;

import jakarta.json.JsonException;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonGeneratorFactory;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link JSONWriter} backed by a Jakarta JSON API (jakarta.json) implementation.
 *
 * <p>This class is preferred over {@link J2EEJSONWriter} when a Jakarta JSON provider is available
 * on the classpath, as Jakarta EE supersedes the older Java EE (javax.json) namespace.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
final class JakartaJSONWriter implements JSONWriter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JakartaJSONWriter.class);

  private static volatile @Nullable JsonGeneratorFactory factory = null;

  private final JsonGenerator json;

  private JakartaJSONWriter(JsonGenerator json) {
    this.json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    this.json.writeStartArray(name);
    return this;
  }

  @Override
  public JSONWriter startArray() {
    this.json.writeStartArray();
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    this.json.writeStartObject(name);
    return this;
  }

  @Override
  public JSONWriter startObject() {
    this.json.writeStartObject();
    return this;
  }

  @Override
  public JSONWriter end() {
    this.json.writeEnd();
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    this.json.write(name, JsonValue.NULL);
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    this.json.write(JsonValue.NULL);
    return this;
  }

  @Override
  public JSONWriter value(double number) {
    this.json.write(number);
    return this;
  }

  @Override
  public JSONWriter value(long number) {
    this.json.write(number);
    return this;
  }

  @Override
  public JSONWriter value(String value) {
    this.json.write(value);
    return this;
  }

  @Override
  public JSONWriter value(boolean value) {
    this.json.write(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, String value) {
    this.json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    this.json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    this.json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    this.json.write(name, value);
    return this;
  }

  @Override
  public void close() {
    this.json.close();
  }

  public static JakartaJSONWriter newInstance(OutputStream out) {
    return new JakartaJSONWriter(factory().createGenerator(out));
  }

  public static JakartaJSONWriter newInstance(Writer writer) {
    return new JakartaJSONWriter(factory().createGenerator(writer));
  }

  protected static boolean init() {
    try {
      factory();
      return true;
    } catch (UnsupportedOperationException ex) {
      return false;
    }
  }

  private static JsonGeneratorFactory factory() {
    JsonGeneratorFactory f = factory;
    if (f == null) {
      synchronized (JakartaJSONWriter.class) {
        f = factory;
        if (f == null) {
          factory = f = loadFactory();
        }
      }
    }
    return f;
  }

  private static JsonGeneratorFactory loadFactory() {
    try {
      JsonProvider provider = JsonProvider.provider();
      LOGGER.debug("jakarta.json provider: {}", provider.getClass().getName());
      return provider.createGeneratorFactory(Collections.emptyMap());
    } catch (JsonException ex) {
      LOGGER.debug("jakarta.json provider not found: {}", ex.getMessage());
      throw new UnsupportedOperationException("No jakarta.json provider found");
    }
  }

}
