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

import java.io.StringWriter;

public final class JsonStringBuilder implements JsonWriter {

  private final StringWriter _s;

  private final JsonWriter _json;

  private JsonStringBuilder() {
    this._s = new StringWriter();
    this._json = Json.newWriter(this._s);
  }

  @Override
  public JsonWriter startArray(String name) {
    this._json.startArray(name);
    return this;
  }

  @Override
  public JsonWriter startArray() {
    this._json.startArray();
    return this;
  }

  @Override
  public JsonWriter endArray() {
    this._json.endArray();
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    this._json.startObject(name);
    return this;
  }

  @Override
  public JsonWriter startObject() {
    this._json.startObject();
    return this;
  }

  @Override
  public JsonWriter endObject() {
    this.endObject();
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    this._json.name(name);
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    this._json.nullValue();
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    this._json.nullValue();
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    this._json.value(number);
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    this._json.value(number);
    return this;
  }

  @Override
  public JsonWriter value(String number) {
    this._json.value(number);
    return this;
  }

  @Override
  public JsonWriter value(boolean number) {
    this._json.value(number);
    return this;
  }

  @Override
  public JsonWriter property(String name, String value) {
    this._json.property(name, value);
    return this;
  }

  @Override
  public JsonWriter property(String name, boolean value) {
    this._json.property(name, value);
    return this;
  }

  @Override
  public JsonWriter property(String name, double value) {
    this._json.property(name, value);
    return this;
  }

  @Override
  public JsonWriter property(String name, long value) {
    this._json.property(name, value);
    return this;
  }

  @Override
  public void close() {
  }

  @Override
  public void flush() {
    this._json.flush();
  }

  @Override
  public String toString() {
    return this._s.toString();
  };
}
