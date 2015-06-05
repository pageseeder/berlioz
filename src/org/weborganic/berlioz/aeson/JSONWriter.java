/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.aeson;

import java.io.Closeable;

/**
 * Simple interface used internally to pass JSON events to the actual JSON writer.
 *
 * <p>This class is required in order to adapt to the different kind of
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.32
 * @since Berlioz 0.9.32
 */
public interface JSONWriter extends Closeable {

  JSONWriter startArray(String name);

  JSONWriter startArray();

  JSONWriter startObject(String name);

  JSONWriter startObject();

  /**
   * Writes the end of the current context.
   * @return this instance.
   */
  JSONWriter end();

  JSONWriter writeNull(String name);

  JSONWriter writeNull();

  JSONWriter writeNull2(String name);

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
