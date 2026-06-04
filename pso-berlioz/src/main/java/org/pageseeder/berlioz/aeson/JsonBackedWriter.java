/*
 * Copyright 2026 Allette Systems (Australia)
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

import java.util.Arrays;

import org.pageseeder.berlioz.json.JsonWriter;

/**
 * Adapter from the Aeson writer contract to the canonical Berlioz JSON writer.
 */
@SuppressWarnings("deprecation")
final class JsonBackedWriter implements JSONWriter {

  /**
   * Underlying JSON writer.
   */
  private final JsonWriter json;

  /**
   * End marker for each open container.
   */
  private char[] endStack = new char[32];

  /**
   * Index of the current container.
   */
  private int level = -1;

  JsonBackedWriter(JsonWriter json) {
    this.json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    this.json.startArray(name);
    push(']');
    return this;
  }

  @Override
  public JSONWriter startArray() {
    this.json.startArray();
    push(']');
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    this.json.startObject(name);
    push('}');
    return this;
  }

  @Override
  public JSONWriter startObject() {
    this.json.startObject();
    push('}');
    return this;
  }

  @Override
  public JSONWriter end() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    char end = this.endStack[this.level--];
    if (end == ']') {
      this.json.endArray();
    } else {
      this.json.endObject();
    }
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    this.json.nullValue(name);
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    this.json.nullValue();
    return this;
  }

  @Override
  public JSONWriter value(double number) {
    checkFinite(number);
    this.json.value(number);
    return this;
  }

  @Override
  public JSONWriter value(long number) {
    this.json.value(number);
    return this;
  }

  @Override
  public JSONWriter value(String value) {
    this.json.value(value);
    return this;
  }

  @Override
  public JSONWriter value(boolean value) {
    this.json.value(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, String value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    checkFinite(value);
    this.json.field(name, value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public void close() {
    this.json.close();
  }

  private void push(char end) {
    ensureCapacity();
    this.endStack[++this.level] = end;
  }

  private void ensureCapacity() {
    if (this.level + 1 >= this.endStack.length) {
      this.endStack = Arrays.copyOf(this.endStack, this.endStack.length * 2);
    }
  }

  private static void checkFinite(double number) {
    if (!Double.isFinite(number)) {
      throw new IllegalArgumentException("JSON does not support non-finite double values: " + number);
    }
  }

}
