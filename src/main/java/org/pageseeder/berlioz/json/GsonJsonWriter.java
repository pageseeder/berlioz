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
import java.util.Arrays;

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

  /** Either true or false for Objects and Array respectively. */
  private boolean[] inObject = new boolean[64];

  /** Array index is current depth level, 0 is top level Object or Array. */
  private int level = -1;

  /** Indicates whether a comma should be appended at the next opportunity. */
  private boolean needComma = false;

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
    this.inObject[++this.level] = false;
    try {
      this._json.name(name).beginArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startArray() {
    this.inObject[++this.level] = false;
    try {
      this._json.beginArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endArray() {
    this.level--;
    try {
      this._json.endArray();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    this.inObject[++this.level] = true;
    try {
      this._json.name(name).beginObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject() {
    this.inObject[++this.level] = true;
    try {
      this._json.beginObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endObject() {
    this.level--;
    try {
      this._json.endObject();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    try {
      this._json.name(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    try {
      this._json.name(name).nullValue();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue() {
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
  public JsonWriter field(String name, String value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    try {
      this._json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public boolean inObject() {
    return this.level >= 0 && this.inObject[this.level];
  }

  @Override
  public void close() {
    try {
      this._json.close();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

  @Override
  public void flush() {
    try {
      this._json.flush();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
  }

  /**
   * Always return a JSON Writer.
   *
   * <p>This method will force the output encoding to be UTF-8</p>
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

  private void push(boolean isObject) {
    this.level++;
    if (this.level < this.inObject.length) {
      this.inObject[this.level] = isObject;
    } else {
      this.inObject = Arrays.copyOf(this.inObject, this.inObject.length*2);
    }
  }

}
