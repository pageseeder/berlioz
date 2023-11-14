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
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;

import javax.json.JsonException;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a JSON Writer backed by a J2EE JSON API implementation.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
final class J2eeJsonWriter implements JsonWriter {

  /** Displays debug information. */
  private static final Logger LOGGER = LoggerFactory.getLogger(J2eeJsonWriter.class);

  /** The JSON generator */
  private static @Nullable JsonGeneratorFactory factory = null;

  /** Either true or false for Objects and Array respectively. */
  private boolean[] inObject = new boolean[64];

  /** Array index is current depth level, 0 is top level Object or Array. */
  private int level = -1;

  /** The JSON generator */
  private final JsonGenerator _json;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private J2eeJsonWriter(JsonGenerator json) {
    this._json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    this.inObject[++this.level] = false;
    this._json.writeStartArray(name);
    return this;
  }

  @Override
  public JsonWriter startArray() {
    this.inObject[++this.level] = false;
    this._json.writeStartArray();
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    this.inObject[++this.level] = true;
    this._json.writeStartObject(name);
    return this;
  }

  @Override
  public JsonWriter startObject() {
    this.inObject[++this.level] = true;
    this._json.writeStartObject();
    return this;
  }

  @Override
  public JsonWriter endArray() {
    this.level--;
    this._json.writeEnd();
    return this;
  }

  @Override
  public JsonWriter endObject() {
    this.level--;
    this._json.writeEnd();
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    this._json.writeKey(name);
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    this._json.writeNull(name);
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    this._json.writeNull();
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JsonWriter value(String value) {
    this._json.write(value);
    return this;
  }

  @Override
  public JsonWriter value(boolean number) {
    this._json.write(number);
    return this;
  }

  @Override
  public JsonWriter field(String name, String value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    this._json.write(name, value);
    return this;
  }

  @Override
  public boolean inObject() {
    return this.level >= 0 && this.inObject[this.level];
  }

  @Override
  public void close() {
    this._json.close();
  }

  @Override
  public void flush() {
    this._json.flush();
  }

  /**
   * Always return a JSON Writer.
   *
   * @param out The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static J2eeJsonWriter newInstance(OutputStream out) {
    JsonGenerator json = factory().createGenerator(out);
    return new J2eeJsonWriter(json);
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static J2eeJsonWriter newInstance(Writer writer) {
    JsonGenerator json = factory().createGenerator(writer);
    return new J2eeJsonWriter(json);
  }

  /**
   * Always return a JSON Writer.
   *
   * @return The JSON writer to use.
   */
  protected static synchronized boolean init() {
    try {
      factory();
      return true;
    } catch (UnsupportedOperationException ex) {
      return false;
    }
  }

  /**
   * Always return a JSON Writer.
   *
   * @return The JSON writer to use.
   *
   * @throws UnsupportedOperationException if it could not be loaded.
   */
  private static synchronized JsonGeneratorFactory factory() {
    JsonGeneratorFactory f = factory;
    if (f == null) {
      factory = f = loadFactory();
    }
    return f;
  }

  /**
   * Always return a JSON Writer.
   *
   * @return The JSON writer to use.
   *
   * @throws UnsupportedOperationException if no provider could be found.
   */
  private static synchronized JsonGeneratorFactory loadFactory() {
    try {
      // This method does not return null, it throws a JsonException instead
      JsonProvider provider = JsonProvider.provider();
      LOGGER.debug("JSON Provider found using {}", provider.getClass().getName());
      // XXX: We could supply configuration for the factory
      return provider.createGeneratorFactory(Collections.emptyMap());
    } catch (JsonException ex) {
      LOGGER.warn("JSON Provider not found: {}", ex.getMessage());
      throw new UnsupportedOperationException("Unable to find suitable provider");
    }
  }

}
