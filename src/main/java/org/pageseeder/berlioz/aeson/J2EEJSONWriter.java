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

import javax.json.JsonException;
import javax.json.JsonValue;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a JSON Writer backed by a J2EE JSON API implementation.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
final class J2EEJSONWriter implements JSONWriter {

  /** Displays debug information. */
  private static final Logger LOGGER = LoggerFactory.getLogger(J2EEJSONWriter.class);

  /** The JSON generator */
  private static JsonGeneratorFactory factory = null;

  /** The JSON generator */
  private final JsonGenerator _json;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private J2EEJSONWriter(JsonGenerator json) {
    this._json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    this._json.writeStartArray(name);
    return this;
  }

  @Override
  public JSONWriter startArray() {
    this._json.writeStartArray();
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    this._json.writeStartObject(name);
    return this;
  }

  @Override
  public JSONWriter startObject() {
    this._json.writeStartObject();
    return this;
  }

  @Override
  public JSONWriter end() {
    this._json.writeEnd();
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    this._json.writeNull(name);
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    this._json.writeNull();
    return this;
  }

  @Override
  public JSONWriter writeNull2(String name) {
    this._json.write(name, JsonValue.NULL);
    return this;
  }

  @Override
  public JSONWriter writeNull2() {
    this._json.write(JsonValue.NULL);
    return this;
  }

  @Override
  public JSONWriter value(double number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JSONWriter value(long number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JSONWriter value(String value) {
    this._json.write(value);
    return this;
  }

  @Override
  public JSONWriter value(boolean number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JSONWriter property(String name, String value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public void close() {
    this._json.close();
  }

  /**
   * Always return a JSON Writer.
   *
   * @param out The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static J2EEJSONWriter newInstance(OutputStream out) {
    if (factory == null && !init()) throw new UnsupportedOperationException("Unable to find suitable provider");
    JsonGenerator json = factory.createGenerator(out);
    return new J2EEJSONWriter(json);
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static J2EEJSONWriter newInstance(Writer writer) {
    if (factory == null && !init()) throw new UnsupportedOperationException("Unable to find suitable provider");
    JsonGenerator json = factory.createGenerator(writer);
    return new J2EEJSONWriter(json);
  }

  /**
   * Always return a JSON Writer.
   *
   * @return The JSON writer to use.
   */
  public static synchronized boolean init() {
    try {
      // This method does not return null, it throws a JsonException instead
      JsonProvider provider = JsonProvider.provider();
      LOGGER.debug("JSON Provider found using {}", provider.getClass().getName());
      // XXX: We could supply configuration for the factory
      factory = provider.createGeneratorFactory(null);
      return true;
    } catch (JsonException ex) {
      LOGGER.warn("JSON Provider not found: {}", ex.getMessage());
      return false;
    }
  }

}
