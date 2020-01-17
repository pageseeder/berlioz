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

/**
 * An implementation of a JSON Writer backed by Jackson library.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
final class JacksonJsonWriter implements JsonWriter {

  /** The JSON generator */
  private static JsonFactory factory = new JsonFactory();

  /** The JSON generator */
  private final JsonGenerator _json;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private JacksonJsonWriter(JsonGenerator json) {
    this._json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    try {
      this._json.writeFieldName(name);
      this._json.writeStartArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startArray() {
    try {
      this._json.writeStartArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endArray() {
    try {
      this._json.writeEndObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    try {
      this._json.writeStartObject(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject() {
    try {
      this._json.writeStartObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endObject() {
    try {
      this._json.writeEndObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter writeNull(String name) {
    try {
      this._json.writeNullField(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter writeNull() {
    try {
      this._json.writeNull();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    try {
      this._json.writeNumber(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    try {
      this._json.writeNumber(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(String value) {
    try {
      this._json.writeNumber(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(boolean number) {
    try {
      this._json.writeBoolean(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, String value) {
    try {
      this._json.writeStringField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, boolean value) {
    try {
      this._json.writeBooleanField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, double value) {
    try {
      this._json.writeNumberField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, long value) {
    try {
      this._json.writeNumberField(name, value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public void close() {
    try {
      this._json.close();
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
