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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a JSON Writer backed by Jackson library.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.0
 */
final class JacksonJsonWriter implements JsonWriter {

  /** Displays debug information. */
  private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJsonWriter.class);

  /** The JSON generator */
  private static final JsonFactory factory;
  static {
    try {
      factory = new JsonFactory();
    } catch (NoClassDefFoundError error) {
      LOGGER.error("To use the JacksonJsonWriter ensure that Jackson is only your classpath!");
      throw error;
    }
  }

  /** The JSON generator */
  private final JsonGenerator json;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private JacksonJsonWriter(JsonGenerator json) {
    this.json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    try {
      this.json.writeFieldName(name);
      this.json.writeStartArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startArray() {
    try {
      this.json.writeStartArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endArray() {
    try {
      this.json.writeEndArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    try {
      this.json.writeFieldName(name);
      this.json.writeStartObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject() {
    try {
      this.json.writeStartObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endObject() {
    try {
      this.json.writeEndObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    try {
      this.json.writeFieldName(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    try {
      this.json.writeNullField(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    try {
      this.json.writeNull();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    try {
      this.json.writeNumber(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    try {
      this.json.writeNumber(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(String value) {
    try {
      this.json.writeString(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(boolean number) {
    try {
      this.json.writeBoolean(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, String value) {
    try {
      this.json.writeStringField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    try {
      this.json.writeBooleanField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    try {
      this.json.writeNumberField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    try {
      this.json.writeNumberField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public boolean inObject() {
    return this.json.getOutputContext().inObject();
  }

  @Override
  public void close() {
    try {
      this.json.close();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

  @Override
  public void flush() {
    try {
      this.json.flush();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

  /**
   * Always return a JSON Writer.
   *
   * @param out The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JacksonJsonWriter newInstance(OutputStream out) {
    try {
      JsonGenerator json = factory.createGenerator(out);
      return new JacksonJsonWriter(json);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static JacksonJsonWriter newInstance(Writer writer) {
    try {
      JsonGenerator json = factory.createGenerator(writer);
      return new JacksonJsonWriter(json);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

}
