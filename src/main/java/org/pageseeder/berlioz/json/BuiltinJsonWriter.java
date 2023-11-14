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

import java.io.PrintWriter;
import java.util.Arrays;

/**
 * An implementation of a JSON Writer backed by
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.0
 */
final class BuiltinJsonWriter implements JsonWriter {

  /**
   * Underlying JSON writer
   */
  private final PrintWriter json;

  /**
   * Either '{' or '[' for Objects and Array respectively.
   */
  private char[] closer = new char[64];

  /**
   * Array index is current depth level, 0 is top level Object or Array.
   */
  private int level = -1;

  /**
   * Indicates whether a comma should be appended at the next opportunity
   */
  private boolean needComma = false;

  public BuiltinJsonWriter(PrintWriter json) {
    this.json = json;
  }

  @Override
  public JsonWriter startArray(String name) {
    push(']');
    maybeAppendComma(true);
    appendJSONString(name);
    this.json.append(':');
    this.json.append('[');
    return this;
  }

  @Override
  public JsonWriter startArray() {
    push(']');
    maybeAppendComma(true);
    this.json.append('[');
    return this;
  }

  @Override
  public JsonWriter endArray() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    this.json.append(this.closer[this.level--]);
    this.needComma = true;
    return this;
  }

  @Override
  public JsonWriter startObject(String name) {
    push('}');
    maybeAppendComma(true);
    appendJSONString(name);
    this.json.append(':');
    this.json.append('{');
    return this;
  }

  @Override
  public JsonWriter startObject() {
    push('}');
    maybeAppendComma(true);
    this.json.append('{');
    return this;
  }

  @Override
  public JsonWriter endObject() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    this.json.append(this.closer[this.level--]);
    this.needComma = true;
    return this;
  }

  @Override
  public JsonWriter nullValue(String name) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    this.json.append("null");
    return this;
  }

  @Override
  public JsonWriter nullValue() {
    maybeAppendComma(false);
    this.json.append("null");
    return this;
  }

  @Override
  public JsonWriter value(double number) {
    maybeAppendComma(false);
    appendJsonDouble(number);
    return this;
  }

  @Override
  public JsonWriter value(long number) {
    maybeAppendComma(false);
    appendJsonLong(number);
    return this;
  }

  @Override
  public JsonWriter value(String value) {
    maybeAppendComma(false);
    appendJSONString(value);
    return this;
  }

  @Override
  public JsonWriter value(boolean value) {
    maybeAppendComma(false);
    appendJsonBoolean(value);
    return this;
  }

  @Override
  public JsonWriter name(String name) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    this.needComma = false;
    return this;
  }

  @Override
  public JsonWriter field(String name, String value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJSONString(value);
    return this;
  }

  @Override
  public JsonWriter field(String name, boolean value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJsonBoolean(value);
    return this;
  }

  @Override
  public JsonWriter field(String name, double value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJsonDouble(value);
    return this;
  }

  @Override
  public JsonWriter field(String name, long value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJsonLong(value);
    return this;
  }

  @Override
  public boolean inObject() {
    return this.level >= 0 && this.closer[this.level] == '}';
  }

  @Override
  public void close() {
    this.json.close();
  }

  @Override
  public void flush() {
    this.json.flush();
  }

  private void appendJSONString(String s) {
    this.json.append('"');
    final int _length = s.length();
    for (int i = 0; i < _length; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\n':
          this.json.append('\\').append('n');
          break;
        case '\r':
          this.json.append('\\').append('r');
          break;
        case '\t':
          this.json.append('\\').append('t');
          break;
        case '"':
          this.json.append('\\').append('"');
          break;
        case '\\':
          this.json.append('\\').append('\\');
          break;
        default:
          if (c < 0x10) {
            this.json.append("\\u000").append(Integer.toHexString(c));
          } else if (c < 0x20) {
            this.json.append("\\u00").append(Integer.toHexString(c));
          } else {
            this.json.append(c);
          }
      }
    }
    this.json.append('"');
  }

  private void appendJsonLong(long number) {
    this.json.append(Long.toString(number));
  }

  private void appendJsonDouble(double number) {
    this.json.append(Double.toString(number));
  }

  private void appendJsonBoolean(boolean b) {
    this.json.append(Boolean.toString(b));
  }

  private void maybeAppendComma(boolean newContext) {
    if (this.needComma) {
      this.json.append(',');
    } else if (!newContext) {
      this.needComma = true;
    }
  }

  private void push(char c) {
    this.level++;
    if (this.level < this.closer.length) {
      this.closer[this.level] = c;
    } else {
      this.closer = Arrays.copyOf(this.closer, this.closer.length*2);
    }
  }

}
