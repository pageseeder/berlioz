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

import java.io.Flushable;
import java.util.Map;

/**
 * Simple interface used internally to pass JSON events to the actual JSON writer.
 *
 * <p>This class is required in order to handle the case when a JSON generator
 * implementation is not available. Aeson uses this interface so that it is not coupled
 * directly the <code>JsonGenerator</code> and can revert back to its internal JSON writer.
 *
 * <p>The methods are similar to the <code>JsonGenerator</code> interface on purpose.
 *
 * <p>This class uses a fluent-style API for easy method chaining.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.12.0
 */
public interface JsonWriter extends AutoCloseable, Flushable {

  /**
   * Start writing a JSON array in the context of an object.
   *
   * @param name The name of the array
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter startArray(String name);

  /**
   * Start writing a JSON array in the context of an array.
   *
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter startArray();

  /**
   * Writes the end of the current array context.
   *
   * @return this instance.
   * @throws IllegalStateException     if the current context is not an array.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter endArray();

  /**
   * Start writing a JSON object in the context of an object.
   *
   * @param name The name of the object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter startObject(String name);

  /**
   * Start writing a JSON object in the context of an array or as a top-level value.
   *
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter startObject();

  /**
   * Writes the end of the current object context.
   *
   * @return this instance.
   * @throws IllegalStateException     if the current context is not an object.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter endObject();

  /**
   * Writes a JSON null value paired with the given name in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter nullValue(String name);

  /**
   * Writes a JSON null value in the current array context.
   *
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter nullValue();

  /**
   * Writes the specified value as a JSON number within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   * @throws IllegalArgumentException  if number is NaN or infinite.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter value(double number);

  /**
   * Writes the specified value as a JSON number within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter value(long number);

  /**
   * Writes the specified value as a JSON string within the current array context.
   *
   * @param value the value to write.
   * @return this instance.
   * @throws NullPointerException      if value is null.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter value(String value);

  /**
   * Writes the specified value as a JSON boolean within the current array context.
   *
   * @param value the value to write.
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter value(boolean value);

  /**
   * Writes a JSON field name in the current object context.
   *
   * <p>Must be followed by one of the {@code value()}, {@link #startObject()}, or
   * {@link #startArray()} methods.</p>
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter name(String name);

  /**
   * Writes a JSON name/string value pair in the current object context.
   *
   * @param name  the name in the JSON name/value pair to be written in current JSON object
   * @param value the value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws NullPointerException      if value is null.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter field(String name, String value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name  the name in the JSON name/value pair to be written in current JSON object
   * @param value the value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter field(String name, boolean value);

  /**
   * Writes a JSON name/double value pair in the current object context.
   *
   * @param name  the name in the JSON name/value pair to be written in current JSON object
   * @param value the value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws IllegalArgumentException  if value is NaN or infinite.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter field(String name, double value);

  /**
   * Writes a JSON name/long value pair in the current object context.
   *
   * @param name  the name in the JSON name/value pair to be written in current JSON object
   * @param value the value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  JsonWriter field(String name, long value);

  /**
   * Writes the specified int value as a JSON number within the current array context.
   *
   * <p>Delegates to {@link #value(long)}.</p>
   *
   * @param value the value to write.
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  default JsonWriter value(int value) {
    return value((long) value);
  }

  /**
   * Writes a JSON name/int value pair in the current object context.
   *
   * <p>Delegates to {@link #field(String, long)}.</p>
   *
   * @param name  the name in the JSON name/value pair to be written in current JSON object
   * @param value the value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  default JsonWriter field(String name, int value) {
    return field(name, (long) value);
  }

  /**
   * Writes a map of string name/value pairs into the current object context.
   *
   * @param map  a map of name/value pairs.
   * @return this instance.
   * @throws JsonWriteFailureException if an I/O error occurs.
   */
  default JsonWriter properties(Map<String, String> map) {
    for (Map.Entry<String, String> field : map.entrySet()) {
      field(field.getKey(), field.getValue());
    }
    return this;
  }

  /**
   * Indicates whether the writer is currently within a JSON object context.
   *
   * @return {@code true} if the current context is a JSON object; {@code false} if it is an array
   *         or no context has been started.
   */
  boolean inObject();

  /**
   * Closes this writer and flushes any buffered content to the underlying target.
   *
   * @throws JsonWriteFailureException if an I/O error occurs during close.
   */
  @Override
  void close();

  /**
   * Flushes any buffered content to the underlying target.
   *
   * @throws JsonWriteFailureException if an I/O error occurs during flush.
   */
  @Override
  void flush();

}