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

import org.jspecify.annotations.Nullable;

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
 * @version 0.13.0
 * @since 0.12.0
 */
final class GsonJsonWriter implements JsonWriter {

  /** The JSON generator */
  private final com.google.gson.stream.JsonWriter json;

  /** Either '}' or ']' for objects and arrays respectively. */
  private char[] closer = new char[32];

  /** Array index is current depth level, 0 is top level Object or Array. */
  private int level = -1;

  /**
   * Creates new JSON writer.
   *
   * @param json The generator to use.
   */
  private GsonJsonWriter(com.google.gson.stream.JsonWriter json) {
    this.json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    try {
      this.json.name(name).beginArray();
      push(']');
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startArray() {
    try {
      this.json.beginArray();
      push(']');
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endArray() {
    checkEnd(']');
    try {
      this.json.endArray();
      this.level--;
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    try {
      this.json.name(name).beginObject();
      push('}');
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter startObject() {
    try {
      this.json.beginObject();
      push('}');
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter endObject() {
    checkEnd('}');
    try {
      this.json.endObject();
      this.level--;
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    try {
      this.json.name(name);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    try {
      this.json.name(name).nullValue();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    try {
      this.json.nullValue();
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    Json.checkFinite(number);
    try {
      this.json.value(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    try {
      this.json.value(number);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(@Nullable String value) {
    try {
      this.json.value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter value(boolean value) {
    try {
      this.json.value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, @Nullable String value) {
    try {
      this.json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    try {
      this.json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    Json.checkFinite(value);
    try {
      this.json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    try {
      this.json.name(name).value(value);
    } catch (IOException ex) {
      throw new JsonWriteFailureException(ex);
    }
    return this;
  }

  @Override
  public boolean inObject() {
    return this.level >= 0 && this.closer[this.level] == '}';
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

  private void push(char c) {
    this.level++;
    if (this.level >= this.closer.length) {
      this.closer = Arrays.copyOf(this.closer, this.closer.length * 2);
    }
    this.closer[this.level] = c;
  }

  private void checkEnd(char expected) {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    if (this.closer[this.level] != expected) {
      String current = this.closer[this.level] == '}' ? "object" : "array";
      String requested = expected == '}' ? "object" : "array";
      throw new IllegalStateException("Current context is an " + current + ", not an " + requested);
    }
  }

}
