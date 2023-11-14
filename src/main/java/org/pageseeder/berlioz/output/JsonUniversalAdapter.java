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
package org.pageseeder.berlioz.output;

import org.pageseeder.berlioz.Beta;
import org.pageseeder.berlioz.json.Json;
import org.pageseeder.berlioz.json.JsonWriter;

import java.io.StringWriter;
import java.io.Writer;

@Beta
public class JsonUniversalAdapter implements UniversalWriter {

  /**
   * The final output.
   */
  private final JsonWriter json;

  /**
   * Creates a new JSON writer to a <code>StringWriter</code>.
   */
  public JsonUniversalAdapter() {
    this(new StringWriter());
  }

  /**
   * Creates a new JSON writer with a custom writer.
   *
   * @param out Where the JSON goes.
   */
  public JsonUniversalAdapter(Writer out) {
    this.json = Json.newWriter(out);
  }

  /**
   * Always JSON.
   *
   * @return Always JSON (<code>application/json</code>)
   */
  @Override
  public final OutputType getType() {
    return OutputType.JSON;
  }

  @Override
  public final void field(String name, boolean value, FieldOption option) {
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, long value, FieldOption option) {
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, double value, FieldOption option) {
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, String value, FieldOption option) {
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public void startObject(String name, ContextOption option) {
    if (this.json.inObject()) {
      this.json.startObject(Json.camelify(name));
    } else {
      this.json.startObject();
    }
  }

  @Override
  public void endObject() {
    this.json.endObject();
  }

  @Override
  public void startArray(String name, ContextOption option) {
    if (this.json.inObject()) {
      this.json.startArray(Json.camelify(name));
    } else {
      this.json.startArray();
    }
  }

  @Override
  public void endArray() {
    this.json.endArray();
  }

  @Override
  public void flush() {
    this.json.flush();
  }

  @Override
  public void close() {
    this.json.close();
  }

  @Override
  public String toString() {
    flush();
    return super.toString();
  }

}
