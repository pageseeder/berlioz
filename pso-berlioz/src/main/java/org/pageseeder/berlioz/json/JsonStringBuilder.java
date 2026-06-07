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

import java.io.StringWriter;

/**
 * A {@link JsonWriter} that accumulates JSON output into a {@link String}.
 *
 * <p>Create an instance with {@link #create()} and retrieve the result via {@link #toString()}.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.12.0
 */
public final class JsonStringBuilder implements JsonWriter {

  private final StringWriter sw;

  private final JsonWriter json;

  /** Tracks whether a comma is needed before the next {@link #fieldRaw} entry. */
  private boolean rawCommaNeeded = false;

  private JsonStringBuilder() {
    this.sw = new StringWriter();
    this.json = Json.newWriter(this.sw);
  }

  /**
   * Creates a new {@code JsonStringBuilder}.
   *
   * @return a fresh instance backed by the available JSON provider.
   */
  public static JsonStringBuilder create() {
    return new JsonStringBuilder();
  }

  @Override
  public JsonWriter startArray(String name) {
    this.json.startArray(name);
    return this;
  }

  @Override
  public JsonWriter startArray() {
    this.json.startArray();
    return this;
  }

  @Override
  public JsonWriter endArray() {
    this.json.endArray();
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    this.json.startObject(name);
    return this;
  }

  @Override
  public JsonWriter startObject() {
    this.json.startObject();
    return this;
  }

  @Override
  public JsonWriter endObject() {
    this.json.endObject();
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    this.json.name(name);
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    this.json.nullValue(name);
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    this.json.nullValue();
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    this.json.value(number);
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    this.json.value(number);
    return this;
  }

  @Override
  public JsonWriter value(@Nullable String value) {
    this.json.value(value);
    return this;
  }

  @Override
  public JsonWriter value(boolean number) {
    this.json.value(number);
    return this;
  }

  @Override
  public JsonWriter field(String name, @Nullable String value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    this.json.field(name, value);
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    this.json.field(name, value);
    return this;
  }

  /**
   * Writes a JSON field whose value is a pre-serialized JSON fragment directly into the buffer.
   *
   * <p>Intended for assembling envelope objects where each value is already a complete JSON
   * fragment (object, array, or scalar). The field name is properly escaped; the value is
   * written verbatim — callers must ensure it is valid JSON.</p>
   *
   * <p>Do not mix with the regular {@link #name}/{@link #value} methods in the same object
   * context; comma tracking for {@code fieldRaw} is independent of the inner writer's state.</p>
   *
   * @param name    the field name; special characters are JSON-escaped.
   * @param rawJson a valid, pre-serialized JSON fragment used as the field value.
   * @return this instance.
   */
  public JsonStringBuilder fieldRaw(String name, String rawJson) {
    // Flush any buffered structural tokens (e.g. '{' from startObject) before raw-writing.
    this.json.flush();
    if (this.rawCommaNeeded) this.sw.append(',');
    this.rawCommaNeeded = true;
    this.sw.append('"');
    for (int i = 0, len = name.length(); i < len; i++) {
      char c = name.charAt(i);
      if      (c == '"')  this.sw.append("\\\"");
      else if (c == '\\') this.sw.append("\\\\");
      else if (c < 0x20)  this.sw.append(toUnicodeEscape(c));
      else                this.sw.append(c);
    }
    this.sw.append("\":");
    this.sw.append(rawJson);
    return this;
  }

  private static String toUnicodeEscape(char c) {
    char[] hex = "0123456789abcdef".toCharArray();
    return "\\u" + hex[(c >> 12) & 0xf] + hex[(c >> 8) & 0xf] + hex[(c >> 4) & 0xf] + hex[c & 0xf];
  }

  @Override
  public boolean inObject() {
    return this.json.inObject();
  }

  @Override
  public void close() {
    this.json.close();
  }

  @Override
  public void flush() {
    this.json.flush();
  }

  @Override
  public String toString() {
    return this.sw.toString();
  }
}
