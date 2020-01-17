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
 * @version Berlioz 0.12.0
 * @version Berlioz 0.12.0
 */
public interface JsonWriter extends AutoCloseable {

  /**
   * Start writing a JSON array in the context of an object.
   *
   * @param name The name of the array
   * @return this instance.
   */
  JsonWriter startArray(String name);

  /**
   * Start writing a JSON array in the context of an array.
   *
   * @return this instance.
   */
  JsonWriter startArray();

  /**
   * Writes the end of the current context.
   * @return this instance.
   */
  JsonWriter endArray();

  /**
   * Start writing a JSON object in the context of an object.
   *
   * @param name The name of the object
   * @return this instance.
   */
  JsonWriter startObject(String name);

  /**
   * Start writing a JSON object in the default context of an array.
   *
   * @return this instance.
   */
  JsonWriter startObject();

  /**
   * Writes the end of the current context.
   * @return this instance.
   */
  JsonWriter endObject();

  /**
   * Write a null value in the context of an object.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JsonWriter nullValue(String name);

  /**
   * Write a null value.
   *
   * @return this instance.
   */
  JsonWriter nullValue();

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JsonWriter value(double number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JsonWriter value(long number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JsonWriter value(String number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JsonWriter value(boolean number);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JsonWriter property(String name, String value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JsonWriter property(String name, boolean value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JsonWriter property(String name, double value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JsonWriter property(String name, long value);

  /**
   * Closes this object and frees any resources associated with it.
   */
  @Override
  void close();

  /**
   * Flush any buffered content to itself and the underlying target.
   */
  void flush();

}