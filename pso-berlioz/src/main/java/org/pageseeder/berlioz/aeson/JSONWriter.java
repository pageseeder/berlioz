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

import java.io.Closeable;

/**
 * Simple interface used to pass JSON events to the actual JSON writer.
 *
 * @deprecated since 0.13.0. Use {@link org.pageseeder.berlioz.json.JsonWriter} instead.
 *
 * <p>This interface is kept as a compatibility facade for existing Aeson callers. The
 * XML-to-JSON serializer now writes through {@link org.pageseeder.berlioz.json.JsonWriter}
 * directly.</p>
 *
 * @author Christophe Lauret
 *
 * @version 0.12.0
 * @since 0.9.32
 */
@Deprecated(since = "0.13.0")
public interface JSONWriter extends Closeable {

  /**
   * Start writing a JSON array in the context of an object.
   *
   * @param name The name of the array
   * @return this instance.
   */
  JSONWriter startArray(String name);

  /**
   * Start writing a JSON array in the context of an array.
   *
   * @return this instance.
   */
  JSONWriter startArray();

  /**
   * Start writing a JSON object in the context of an object.
   *
   * @param name The name of the object
   * @return this instance.
   */
  JSONWriter startObject(String name);

  /**
   * Start writing a JSON object in the context of an array.
   *
   * @return this instance.
   */
  JSONWriter startObject();

  /**
   * Writes the end of the current context.
   * @return this instance.
   */
  JSONWriter end();

  /**
   * Writes a JSON null value with the given name in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter writeNull(String name);

  /**
   * Writes a JSON null value in the current array context.
   *
   * @return this instance.
   */
  JSONWriter writeNull();

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   * @throws IllegalArgumentException if {@code number} is NaN or infinite.
   */
  JSONWriter value(double number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JSONWriter value(long number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param value the value to write.
   * @return this instance.
   */
  JSONWriter value(String value);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param value the value to write.
   * @return this instance.
   */
  JSONWriter value(boolean value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter property(String name, String value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter property(String name, boolean value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   * @throws IllegalArgumentException if {@code value} is NaN or infinite.
   */
  JSONWriter property(String name, double value);

  /**
   * Writes a JSON name/boolean value pair in the current object context.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @param value a value in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter property(String name, long value);

  /**
   * Closes this object and frees any resources associated with it.
   */
  @Override
  void close();

}
