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
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Arrays;

/**
 * A {@link JSONWriter} implementation backed by a {@link Writer}, requiring no external
 * dependencies. Used as the fallback when no Jakarta or Java EE JSON provider is available.
 *
 * <p>Any {@link IOException} thrown by the underlying writer is rethrown as an
 * {@link UncheckedIOException} so that the {@link JSONWriter} interface stays exception-free.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
final class BuiltinJSONWriter implements JSONWriter {

  private final Writer json;

  private boolean first = true;

  private char[] itemStack = new char[32];

  private int level = -1;

  BuiltinJSONWriter(Writer json) {
    this.json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    ensureCapacity();
    this.itemStack[++this.level] = ']';
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    write('[');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startArray() {
    ensureCapacity();
    this.itemStack[++this.level] = ']';
    maybeAppendComma();
    write('[');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    ensureCapacity();
    this.itemStack[++this.level] = '}';
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    write('{');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter startObject() {
    ensureCapacity();
    this.itemStack[++this.level] = '}';
    maybeAppendComma();
    write('{');
    this.first = true;
    return this;
  }

  @Override
  public JSONWriter end() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    write(this.itemStack[this.level--]);
    this.first = false;
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    maybeAppendComma();
    write("null");
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    write("null");
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
    write(':');
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    appendJSONDouble(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    maybeAppendComma();
    appendJSONString(name);
    write(':');
    appendJSONLong(value);
    return this;
  }

  @Override
  public void close() {
    try {
      this.json.close();
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  // Private helpers
  // =============================================================================================

  private void appendJSONString(String s) {
    write('"');
    final int length = s.length();
    int start = 0;
    for (int i = 0; i < length; i++) {
      char c = s.charAt(i);
      // Fast path: printable ASCII that needs no escaping — just accumulate.
      if (c >= 0x20 && c != '"' && c != '\\') continue;
      // Flush the clean run accumulated since 'start'.
      if (i > start) write(s, start, i - start);
      switch (c) {
        case '"':  write("\\\""); break;
        case '\\': write("\\\\"); break;
        case '\n': write("\\n");  break;
        case '\r': write("\\r");  break;
        case '\t': write("\\t");  break;
        case '\b': write("\\b");  break;
        case '\f': write("\\f");  break;
        default:   // remaining control characters (U+0000–U+001F)
          write("\\u00");
          write(HEX[(c >> 4) & 0xF]);
          write(HEX[c & 0xF]);
      }
      start = i + 1;
    }
    // Flush any remaining clean tail.
    if (start < length) write(s, start, length - start);
    write('"');
  }

  private static final char[] HEX = "0123456789abcdef".toCharArray();

  private void appendJSONLong(long number) {
    write(Long.toString(number));
  }

  private void appendJSONDouble(double number) {
    if (Double.isNaN(number) || Double.isInfinite(number))
      throw new IllegalArgumentException("JSON does not support NaN or Infinite values: " + number);
    write(Double.toString(number));
  }

  private void appendJSONBoolean(boolean b) {
    write(b ? "true" : "false");
  }

  private void maybeAppendComma() {
    if (this.first) {
      this.first = false;
    } else {
      write(',');
    }
  }

  private void ensureCapacity() {
    if (this.level + 1 >= this.itemStack.length) {
      this.itemStack = Arrays.copyOf(this.itemStack, this.itemStack.length * 2);
    }
  }

  private void write(char c) {
    try { this.json.write(c); } catch (IOException ex) { throw new UncheckedIOException(ex); }
  }

  private void write(String s) {
    try { this.json.write(s); } catch (IOException ex) { throw new UncheckedIOException(ex); }
  }

  private void write(String s, int off, int len) {
    try { this.json.write(s, off, len); } catch (IOException ex) { throw new UncheckedIOException(ex); }
  }

}
