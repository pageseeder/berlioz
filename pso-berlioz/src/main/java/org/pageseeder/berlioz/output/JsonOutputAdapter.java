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

/**
 * An {@link OutputWriter} that produces JSON output.
 *
 * <p>Field names are automatically converted to camelCase via {@link Json#camelify(String)}.
 * Fields and contexts flagged as {@link FieldOption#XML_ONLY} are silently skipped.</p>
 *
 * <p>By default output is buffered in an internal {@link java.io.StringWriter} and can be
 * retrieved via {@link #toString()}. Supply a custom {@link java.io.Writer} to direct output
 * elsewhere.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
@Beta
public class JsonOutputAdapter implements OutputWriter {

  /**
   * The final output.
   */
  private final JsonWriter json;

  /**
   * Depth counter for suppressed {@link ContextOption#XML_ONLY} contexts.
   * When positive, all output is suppressed until the matching end call unwinds to zero.
   */
  private int suppressedDepth = 0;

  /**
   * Creates a new JSON writer to a <code>StringWriter</code>.
   */
  public JsonOutputAdapter() {
    this(new StringWriter());
  }

  /**
   * Creates a new JSON writer with a custom writer.
   *
   * @param out Where the JSON goes.
   */
  public JsonOutputAdapter(Writer out) {
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
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, long value, FieldOption option) {
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, double value, FieldOption option) {
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public final void field(String name, String value, FieldOption option) {
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.json.field(Json.camelify(name), value);
  }

  @Override
  public void field(String name, String[] values, FieldOption option) {
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.startArray(name);
    for (String value : values) this.json.value(value);
    this.json.endArray();
  }

  @Override
  public void field(String name, Iterable<String> values, FieldOption option) {
    if (option == FieldOption.XML_ONLY || this.suppressedDepth > 0) return;
    this.startArray(name);
    for (String value : values) this.json.value(value);
    this.json.endArray();
  }

  @Override
  public void startObject(String name, ContextOption option) {
    if (option == ContextOption.XML_ONLY || this.suppressedDepth > 0) {
      this.suppressedDepth++;
      return;
    }
    if (this.json.inObject()) {
      this.json.startObject(Json.camelify(name));
    } else {
      this.json.startObject();
    }
  }

  @Override
  public void endObject() {
    if (this.suppressedDepth > 0) {
      this.suppressedDepth--;
      return;
    }
    this.json.endObject();
  }

  @Override
  public void startArray(String name, ContextOption option) {
    if (option == ContextOption.XML_ONLY || this.suppressedDepth > 0) {
      this.suppressedDepth++;
      return;
    }
    if (this.json.inObject()) {
      this.json.startArray(Json.camelify(name));
    } else {
      this.json.startArray();
    }
  }

  @Override
  public void endArray() {
    if (this.suppressedDepth > 0) {
      this.suppressedDepth--;
      return;
    }
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
