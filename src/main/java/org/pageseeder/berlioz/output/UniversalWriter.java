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

import java.io.Flushable;

public interface UniversalWriter extends AutoCloseable, Flushable {

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
    XML_COPY
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
     */
    JSON_ONLY

  }

  /**
   * @return The output type returned by this writer.
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
   * Ends the current object
   *
   * <ul>
   *   <li>JSON, end the current JSON object</li>
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

}
