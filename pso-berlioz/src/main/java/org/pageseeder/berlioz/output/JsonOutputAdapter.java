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
   * Creates a new JSON writer.
   *
   * <p>To capture the output as a string, supply a {@link java.io.StringWriter} via
   * {@link #JsonOutputAdapter(Writer)} and call {@link java.io.StringWriter#toString()} on it.</p>
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
   * Creates a new JSON writer with a custom writer.
   */
  public JsonOutputAdapter(JsonWriter json) {
    this.json = json;
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
  public final OutputWriter field(String name, boolean value, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      this.json.field(Json.camelify(name), value);
    }
    return this;
  }

  @Override
  public final OutputWriter field(String name, long value, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      this.json.field(Json.camelify(name), value);
    }
    return this;
  }

  @Override
  public final OutputWriter field(String name, double value, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      this.json.field(Json.camelify(name), value);
    }
    return this;
  }

  @Override
  public final OutputWriter field(String name, String value, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      this.json.field(Json.camelify(name), value);
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, String[] values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (String value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, Iterable<String> values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (String value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, int[] values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (int value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, long[] values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (long value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, boolean[] values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (boolean value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public OutputWriter field(String name, double[] values, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      startArray(name);
      for (double value : values) this.json.value(value);
      endArray();
    }
    return this;
  }

  @Override
  public final OutputWriter nullField(String name, FieldOption option) {
    if (option != FieldOption.XML_ONLY && this.suppressedDepth == 0) {
      if (this.json.inObject()) {
        this.json.nullValue(Json.camelify(name));
      } else {
        this.json.nullValue();
      }
    }
    return this;
  }

  @Override
  public OutputWriter startObject(String name, ContextOption option) {
    if (option == ContextOption.XML_ONLY || this.suppressedDepth > 0) {
      this.suppressedDepth++;
    } else if (this.json.inObject()) {
      this.json.startObject(Json.camelify(name));
    } else {
      this.json.startObject();
    }
    return this;
  }

  @Override
  public OutputWriter endObject() {
    if (this.suppressedDepth > 0) {
      this.suppressedDepth--;
    } else {
      this.json.endObject();
    }
    return this;
  }

  @Override
  public OutputWriter startArray(String name, ContextOption option) {
    if (option == ContextOption.XML_ONLY || this.suppressedDepth > 0) {
      this.suppressedDepth++;
    } else if (this.json.inObject()) {
      this.json.startArray(Json.camelify(name));
    } else {
      this.json.startArray();
    }
    return this;
  }

  @Override
  public OutputWriter endArray() {
    if (this.suppressedDepth > 0) {
      this.suppressedDepth--;
    } else {
      this.json.endArray();
    }
    return this;
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
    return this.json.toString();
  }
}
