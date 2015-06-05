/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.aeson;

import java.io.PrintWriter;

/**
 * An implementation of a JSON Writer backed by
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
final class BuiltinJSONWriter implements JSONWriter {


  private final PrintWriter _json;

  private boolean first = true;

  private char[] c = new char[32];

  private int level = -1;

  public BuiltinJSONWriter(PrintWriter json) {
    this._json = json;
  }

  @Override
  public JSONWriter startArray(String name) {
    this.c[++this.level]=']';
    maybeAppendComma(true);
    appendJSONString(name);
    this._json.append(':');
    this._json.append('[');
    return this;
  }

  @Override
  public JSONWriter startArray() {
    this.c[++this.level]=']';
    maybeAppendComma(true);
    this._json.append('[');
    return this;
  }

  @Override
  public JSONWriter startObject(String name) {
    this.c[++this.level]='}';
    maybeAppendComma(true);
    appendJSONString(name);
    this._json.append(':');
    this._json.append('{');
    return this;
  }

  @Override
  public JSONWriter startObject() {
    this.c[++this.level]='}';
    maybeAppendComma(true);
    this._json.append('{');
    return this;
  }

  @Override
  public JSONWriter end() {
    if (this.level < 0) throw new IllegalStateException("Nothing to end!");
    this._json.append(this.c[this.level--]);
    this.first = false;
    return this;
  }

  @Override
  public JSONWriter writeNull(String name) {
    maybeAppendComma(false);
    appendJSONString(name);
    this._json.append(':');
    this._json.append("null");
    return this;
  }

  @Override
  public JSONWriter writeNull() {
    maybeAppendComma(false);
    this._json.append("null");
    return this;
  }

  @Override
  public JSONWriter writeNull2(String name) {
    maybeAppendComma(false);
    appendJSONString(name);
    this._json.append(':');
    this._json.append("null");
    return this;
  }

  @Override
  public JSONWriter writeNull2() {
    maybeAppendComma(false);
    this._json.append("null");
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
    this._json.append(':');
    appendJSONString(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, boolean value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this._json.append(':');
    appendJSONBoolean(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, double value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this._json.append(':');
    appendJSONDouble(value);
    return this;
  }

  @Override
  public JSONWriter property(String name, long value) {
    maybeAppendComma(false);
    appendJSONString(name);
    this._json.append(':');
    appendJSONLong(value);
    return this;
  }

  @Override
  public void close() {
    this._json.close();
  }

  private void appendJSONString(String s) {
    this._json.append('"');
    final int _length = s.length();
    for (int i = 0; i < _length; i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\n':
          this._json.append('\\').append('n');
          break;
        case '\r':
          this._json.append('\\').append('r');
          break;
        case '\t':
          this._json.append('\\').append('t');
          break;
        case '"':
          this._json.append('\\').append('"');
          break;
        case '\\':
          this._json.append('\\').append('\\');
          break;
        default:
          if (c < 0x10) {
            this._json.append("\\u000").append(Integer.toHexString(c));
          } else if (c < 0x20) {
            this._json.append("\\u00").append(Integer.toHexString(c));
          } else {
            this._json.append(c);
          }
      }
    }
    this._json.append('"');
  }

  private void appendJSONLong(long number) {
    this._json.append(Long.toString(number));
  }

  private void appendJSONDouble(double number) {
    this._json.append(Double.toString(number));
  }

  private void appendJSONBoolean(boolean b) {
    this._json.append(Boolean.toString(b));
  }

  private void maybeAppendComma(boolean newcontext) {
    if (this.first) {
      if (!newcontext) {
        this.first = false;
      }
    } else {
      this._json.append(',');
    }
  }

}
