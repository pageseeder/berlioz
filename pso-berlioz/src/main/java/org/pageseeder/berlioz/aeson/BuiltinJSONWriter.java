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
package org.pageseeder.berlioz.aeson;

import java.io.PrintWriter;

/**
 * An implementation of a JSON Writer backed by
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
final class BuiltinJSONWriter implements JSONWriter {


  private final PrintWriter json;

  private boolean first = true;

  private final char[] itemStack = new char[32];

  private int level = -1;

  public BuiltinJSONWriter(PrintWriter json) {
    this.json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    this.itemStack[++this.level]=']';
    maybeAppendComma(true);
    appendJSONString(name);
    this.json.append(':');
    this.json.append('[');
    return this;
  }

  @Override
  public JSONWriter startArray() {
    this.itemStack[++this.level]=']';
    maybeAppendComma(true);
    this.json.append('[');
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    this.itemStack[++this.level]='}';
    maybeAppendComma(true);
    appendJSONString(name);
    this.json.append(':');
    this.json.append('{');
    return this;
  }

  @Override
  public JSONWriter startObject() {
    this.itemStack[++this.level]='}';
    maybeAppendComma(true);
    this.json.append('{');
    return this;
  }

  @Override
  public JSONWriter end() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    this.json.append(this.itemStack[this.level--]);
    this.first = false;
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    maybeAppendComma(false);
    this.json.append("null");
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    this.json.append("null");
    return this;
  }

  @Override
  public JSONWriter value(double number) {
    maybeAppendComma(false);
    appendJSONDouble(number);
    return this;
  }

  @Override
  public JSONWriter value(long number) {
    maybeAppendComma(false);
    appendJSONLong(number);
    return this;
  }

  @Override
  public JSONWriter value(String value) {
    maybeAppendComma(false);
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter value(boolean value) {
    maybeAppendComma(false);
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, String value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJSONDouble(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this.json.append(':');
    appendJSONLong(value);
    return this;
  }

  @Override
  public void close() {
    this.json.close();
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

  private void appendJSONLong(long number) {
    this.json.append(Long.toString(number));
  }

  private void appendJSONDouble(double number) {
    this.json.append(Double.toString(number));
  }

  private void appendJSONBoolean(boolean b) {
    this.json.append(Boolean.toString(b));
  }

  private void maybeAppendComma(boolean newcontext) {
    if (this.first) {
      if (!newcontext) {
        this.first = false;
      }
    } else {
      this.json.append(',');
    }
  }

}
