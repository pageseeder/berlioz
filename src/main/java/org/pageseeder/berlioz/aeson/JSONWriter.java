/*
 * Copyright 2015 Allette Systems (Australia)
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
 * Simple interface used internally to pass JSON events to the actual JSON writer.
 *
 * <p>This class is required in order to handle the case when a <code>JsonGenerator</code>
 * implementation is not available. Aeson uses this interface so that it is not coupled
 * directly the <code>JsonGenerator</code> and can revert back to its internal JSON writer.
 *
 * <p>The methods are intendedly similar to the <code>JsonGenerator</code> interface.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
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
   * @param name The name of the array
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
   * Start writing a JSON object in the context of an array.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter writeNull(String name);

  /**
   * Start writing a JSON object in the context of an array.
   *
   * @return this instance.
   */
  JSONWriter writeNull();

  /**
   * Writes a value-pair which value is <code>null</code>.
   *
   * @param name a name in the JSON name/value pair to be written in current JSON object
   * @return this instance.
   */
  JSONWriter writeNull2(String name);

  /**
   * Writes a <code>null</code> value.
   *
   * @return this instance.
   */
  JSONWriter writeNull2();

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
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
   * @param number the value to write.
   * @return this instance.
   */
  JSONWriter value(String number);

  /**
   * Writes the specified value as a JSON value within the current array context.
   *
   * @param number the value to write.
   * @return this instance.
   */
  JSONWriter value(boolean number);

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
