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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * An implementation of a JSON Writer backed by the Google Gson implementation.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.12.0
 * @since Berlioz 0.12.0
 */
final class GsonJsonWriter implements JsonWriter {

  /** The JSON generator */
  private final com.google.gson.stream.JsonWriter _json;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private GsonJsonWriter(com.google.gson.stream.JsonWriter json) {
    this._json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    try {
      this._json.name(name).beginArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startArray() {
    try {
      this._json.beginArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endArray() {
    try {
      this._json.endArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    try {
      this._json.name(name).beginObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject() {
    try {
      this._json.beginObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endObject() {
    try {
      this._json.endObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter writeNull(String name) {
    try {
      this._json.name(name).nullValue();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter writeNull() {
    try {
      this._json.nullValue();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    try {
    this._json.value(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    try {
    this._json.value(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(String value) {
    try {
    this._json.value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(boolean value) {
    try {
    this._json.value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, String value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, boolean value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, double value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter property(String name, long value) {
    try {
      this._json.name(name).value(value);
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
  public static GsonJsonWriter newInstance(OutputStream out) {
    com.google.gson.stream.JsonWriter json = new com.google.gson.stream.JsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    return new GsonJsonWriter(json);
  }

  /**
   * Always return a JSON Writer.
   *
   * @param writer The stream receiving the JSON output.
   *
   * @return The JSON writer to use.
   */
  public static GsonJsonWriter newInstance(Writer writer) {
    return new GsonJsonWriter(new com.google.gson.stream.JsonWriter(writer));
  }

}
