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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;

/**
 * A {@link JSONWriter} implementation backed by a {@link PrintWriter}, requiring no external
 * dependencies. Used as the fallback when no Jakarta or Java EE JSON provider is available.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
final class BuiltinJSONWriter implements JSONWriter {


  private final PrintWriter json;

  private boolean first = true;

  private char[] itemStack = new char[32];

  private int level = -1;

  public BuiltinJSONWriter(PrintWriter json) {
    this.json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    ensureCapacity();
    this.itemStack[++this.level] = ']';
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    this.json.append('[');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startArray() {
    ensureCapacity();
    this.itemStack[++this.level] = ']';
    maybeAppendComma();
    this.json.append('[');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    ensureCapacity();
    this.itemStack[++this.level] = '}';
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    this.json.append('{');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startObject() {
    ensureCapacity();
    this.itemStack[++this.level] = '}';
    maybeAppendComma();
    this.json.append('{');
    this.first = true;
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
    maybeAppendComma();
    this.json.append("null");
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    this.json.append("null");
    return this;
  }

  @Override
  public JSONWriter value(double number) {
    maybeAppendComma();
    appendJSONDouble(number);
    return this;
  }

  @Override
  public JSONWriter value(long number) {
    maybeAppendComma();
    appendJSONLong(number);
    return this;
  }

  @Override
  public JSONWriter value(String value) {
    maybeAppendComma();
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter value(boolean value) {
    maybeAppendComma();
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, String value) {
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    appendJSONDouble(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    maybeAppendComma();
    appendJSONString(name);
    this.json.append(':');
    appendJSONLong(value);
    return this;
  }

  @Override
  public void close() {
    this.json.close();
    // PrintWriter swallows IOExceptions silently; checkError() reads the internal trouble flag
    // so that a broken pipe or full-disk error is not silently ignored by the caller.
    if (this.json.checkError()) {
      throw new UncheckedIOException(new IOException("Write error on underlying stream"));
    }
  }

  private void appendJSONString(String s) {
    this.json.append('"');
    final int length = s.length();
    int start = 0;
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      // Fast path: printable ASCII that needs no escaping — just accumulate.
      if (c >= 0x20 && c != '"' && c != '\\') continue;
      // Flush the clean run accumulated since 'start'.
      if (i > start) this.json.write(s, start, i - start);
      switch (c) {
        case '"':  this.json.write("\\\""); break;
        case '\\': this.json.write("\\\\"); break;
        case '\n': this.json.write("\\n");  break;
        case '\r': this.json.write("\\r");  break;
        case '\t': this.json.write("\\t");  break;
        case '\b': this.json.write("\\b");  break;
        case '\f': this.json.write("\\f");  break;
        default:   // remaining control characters (U+0000–U+001F)
          this.json.write("\\u00");
          this.json.append(HEX[(c >> 4) & 0xF]);
          this.json.append(HEX[c & 0xF]);
      }
      start = i + 1;
    }
    // Flush any remaining clean tail.
    if (start < length) this.json.write(s, start, length - start);
    this.json.append('"');
  }

  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private void appendJSONLong(long number) {
    this.json.append(Long.toString(number));
  }

  private void appendJSONDouble(double number) {
    if (Double.isNaN(number) || Double.isInfinite(number))
      throw new IllegalArgumentException("JSON does not support NaN or Infinite values: " + number);
    this.json.append(Double.toString(number));
  }

  private void appendJSONBoolean(boolean b) {
    this.json.append(Boolean.toString(b));
  }

  private void maybeAppendComma() {
    if (this.first) {
      this.first = false;
    } else {
      this.json.append(',');
    }
  }

  private void ensureCapacity() {
    if (this.level + 1 >= this.itemStack.length) {
      this.itemStack = Arrays.copyOf(this.itemStack, this.itemStack.length * 2);
    }
  }

}
