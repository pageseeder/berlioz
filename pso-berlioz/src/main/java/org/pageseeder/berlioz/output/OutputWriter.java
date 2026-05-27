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
package org.pageseeder.berlioz.output;

import org.jspecify.annotations.Nullable;

import java.io.Flushable;

/**
 * A format-agnostic writer that produces structured data as either XML or JSON.
 *
 * <p>The interface models structured output in terms of three concepts:</p>
 * <ul>
 *   <li><b>Objects</b> — key/value containers ({@link #startObject}/{@link #endObject}).
 *       In JSON these become JSON objects; in XML they become elements.</li>
 *   <li><b>Arrays</b> — ordered collections ({@link #startArray}/{@link #endArray}).
 *       In JSON these become JSON arrays; in XML they become wrapper elements.</li>
 *   <li><b>Fields</b> — leaf values ({@link #field(String, String, FieldOption) field}).
 *       In JSON these become properties; in XML they become attributes, text nodes, or child
 *       elements depending on the {@link FieldOption}.</li>
 * </ul>
 *
 * <p>Use {@link JsonOutputAdapter} or {@link XmlOutputAdapter} to get a concrete instance.</p>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.13.0
 */
public interface OutputWriter extends AutoCloseable, Flushable {

  /**
   * Formatting options applicable to fields.
   */
  enum FieldOption {

    /**
     * The field should be represented as a JSON property or XML attribute.
     */
    DEFAULT,

    /**
     * The field should be represented as a JSON property only and ignored in XML.
     *
     * <p>Use this for edge cases where the XML and JSON representations diverge and a value
     * is only meaningful in the JSON form.</p>
     */
    JSON_ONLY,

    /**
     * The field should be represented as a JSON property or a text node in XML.
     */
    XML_TEXT,

    /**
     * The field should be represented as a JSON property or an element in XML.
     */
    XML_ELEMENT,

    /**
     * The field should be represented as a JSON property or copied as XML content in XML.
     */
    XML_COPY,

    /**
     * The field should only be written for XML output and ignored in JSON.
     *
     * <p>Use this for edge cases where the XML and JSON representations diverge and a value
     * is only meaningful in the XML form.</p>
     */
    XML_ONLY
  }

  /**
   * Formatting options applicable to collections and objects.
   */
  enum ContextOption {

    /**
     * The entity should be represented as a JSON object/array or XML element with children.
     */
    DEFAULT,

    /**
     * The field should be represented as a JSON object/array only and ignored in XML.
     *
     * <p>Use this for edge cases where the XML and JSON representations diverge and an object
     * or array is only meaningful in the JSON form.</p>
     */
    JSON_ONLY,

    /**
     * The context should only be rendered for XML output and ignored in JSON.
     *
     * <p>Use this for edge cases where the XML and JSON representations diverge and an object
     * or array is only meaningful in the XML form.</p>
     */
    XML_ONLY

  }

  /**
   * Returns the output format produced by this writer.
   *
   * @return the output type ({@link OutputType#XML} or {@link OutputType#JSON})
   */
  OutputType getType();

  /**
   * Starts an object in the output.
   *
   * <ul>
   *   <li>JSON, start a JSON object</li>
   *   <li>XML, start a new element</li>
   * </ul>
   *
   * @param name   The name of the XML element or JSON property if context is a JSON object
   * @param option How this object should be serialized
   */
  void startObject(String name, ContextOption option);

  /**
   * Ends the current object
   *
   * <ul>
   *   <li>JSON, end the current JSON object</li>
   *   <li>XML, end the current element</li>
   * </ul>
   */
  void endObject();

  /**
   * Starts a collection of objects in the output.
   *
   * <ul>
   *   <li>JSON, start a JSON array</li>
   *   <li>XML, start a new element</li>
   * </ul>
   *
   * @param name   The name of the XML element or JSON property if context is a JSON object
   */
  void startArray(String name, ContextOption option);

  /**
   * Ends the current array.
   *
   * <ul>
   *   <li>JSON, end the current JSON array</li>
   *   <li>XML, end the current element</li>
   * </ul>
   */
  void endArray();

  /**
   * Write a field with a boolean value based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a boolean property on the object</li>
   *   <li>XML, write an attribute, element, text or copy the XML depending on the option</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   * @param option How to write the field for the output.
   */
  void field(String name, boolean value, FieldOption option);

  /**
   * Write a field with a numeric value based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a numeric property on the object</li>
   *   <li>XML, write an attribute, element, text or copy the XML depending on the option</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The long of the field
   * @param option How to write the field for the output.
   */
  void field(String name, long value, FieldOption option);

  /**
   * Write a field with a numeric value based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a numeric property on the object</li>
   *   <li>XML, write an attribute, element, text or copy the XML depending on the option</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The long of the field
   * @param option How to write the field for the output.
   */
  void field(String name, double value, FieldOption option);

  /**
   * Write a field with a string value based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a string property on the object</li>
   *   <li>XML, write an attribute, element, text or copy the XML depending on the option</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   * @param option How to write the field for the output.
   */
  void field(String name, String value, FieldOption option);

  /**
   * Write a field with multiple string values based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a property with a string array on the object</li>
   *   <li>XML, write an attribute, element, or text using comma-separated values</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param values The values of the field
   * @param option How to write the field for the output.
   */
  void field(String name, String[] values, FieldOption option);

  /**
   * Write a field with multiple string values based on the specified field option.
   *
   * <ul>
   *   <li>JSON, write a property with a string array on the object</li>
   *   <li>XML, write an attribute, element, or text using comma-separated values</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param values The values of the field
   * @param option How to write the field for the output.
   */
  void field(String name, Iterable<String> values, FieldOption option);

  // Short-hand methods
  // ----------------------------------------------------------------------------------------------

  /**
   * Starts an object in the output.
   *
   * <ul>
   *   <li>JSON, start a JSON object</li>
   *   <li>XML, start a new element</li>
   * </ul>
   *
   * @param name The name of the XML element or JSON property if context is a JSON object
   */
  default void startObject(String name){
    startObject(name, ContextOption.DEFAULT);
  }

  /**
   * Starts a collection of objects in the output.
   *
   * <ul>
   *   <li>JSON, start a JSON array</li>
   *   <li>XML, start a new element</li>
   * </ul>
   *
   * @param name   The name of the XML element or JSON property if context is a JSON object
   */
  default void startArray(String name) {
    startArray(name, ContextOption.DEFAULT);
  }

  /**
   * Write a field with a string value using the default option.
   *
   * <ul>
   *   <li>JSON, write a string property on the object</li>
   *   <li>XML, write an attribute</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   */
  default void field(String name, boolean value) {
    field(name, value, FieldOption.DEFAULT);
  }

  /**
   * Write a field with a long value using the default option.
   *
   * <ul>
   *   <li>JSON, write a numeric property on the object</li>
   *   <li>XML, write an attribute</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   */
  default void field(String name, long value) {
    field(name, value, FieldOption.DEFAULT);
  }

  /**
   * Write a field with a double value using the default option.
   *
   * <ul>
   *   <li>JSON, write a numeric property on the object</li>
   *   <li>XML, write an attribute</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   */
  default void field(String name, double value) {
    field(name, value, FieldOption.DEFAULT);
  }

  /**
   * Write a field with a boolean value using the default option.
   *
   * <ul>
   *   <li>JSON, write a boolean property on the object</li>
   *   <li>XML, write an attribute with value "true" or "false"</li>
   * </ul>
   *
   * @param name   The name of the field
   * @param value  The value of the field
   */
  default void field(String name, String value) {
    field(name, value, FieldOption.DEFAULT);
  }

  /**
   * Write a field with multiple string values using the default option.
   *
   * @param name   The name of the field
   * @param values The values of the field
   */
  default void field(String name, String[] values) {
    field(name, values, FieldOption.DEFAULT);
  }

  /**
   * Write a field with multiple string values using the default option.
   *
   * @param name   The name of the field
   * @param values The values of the field
   */
  default void field(String name, Iterable<String> values) {
    field(name, values, FieldOption.DEFAULT);
  }

  /**
   * Write a field with a string value only if the value is non-null.
   *
   * @param name   The name of the field
   * @param value  The value of the field, or {@code null} to skip the field entirely
   * @param option How to write the field for the output.
   */
  default void optionalField(String name, @Nullable String value, FieldOption option) {
    if (value != null) field(name, value, option);
  }

  /**
   * Write a field with a string value only if the value is non-null, using the default option.
   *
   * @param name  The name of the field
   * @param value The value of the field, or {@code null} to skip the field entirely
   */
  default void optionalField(String name, @Nullable String value) {
    if (value != null) field(name, value, FieldOption.DEFAULT);
  }

}
