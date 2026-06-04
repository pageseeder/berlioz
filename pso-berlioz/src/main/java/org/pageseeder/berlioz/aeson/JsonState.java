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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;

/**
 * Maintains the state of the serialization.
 *
 * <p>Note: there is no reason to expose this class as public since it is
 * primarily used by the serializer.
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
final class JsonState {

  /**
   * How property values should be serialized.
   */
  enum JsonType { STRING, NUMBER, BOOLEAN, NULL, DEFAULT }

  /**
   * The current context.
   */
  enum JsonContext { ROOT, OBJECT, ARRAY, NULL, VALUE }

  /**
   * Keeps track of the context.
   */
  private final Deque<JsonContext> context = new ArrayDeque<>();

  /**
   * Maintains instructions for the JSON serialization at each level of the structure.
   */
  private final Deque<JsonTypeMap> types = new ArrayDeque<>();

  /**
   * Keeps track of the name of the current context.
   */
  private final Deque<String> names = new ArrayDeque<>();

  /**
   * Initialize the state with the ROOT context.
   */
  public void pushState() {
    this.context.push(JsonContext.ROOT);
    this.types.push(JsonTypeMap.EMPTY);
    this.names.push("");
  }

  /**
   * Push the state.
   *
   * @param context The new context.
   * @param atts    The attributes (may affect types)
   * @param name    The name of the context.
   */
  public void pushState(JsonContext context, Attributes atts, @Nullable String name) {
    this.context.push(context);
    JsonTypeMap map = JsonTypeMap.make(currentTypeMap(), atts);
    this.types.push(map);
    this.names.push(name != null? name : "");
  }

  /**
   * Remove all objects from the state.
   */
  public void popState() {
    this.context.pop();
    this.types.pop();
    this.names.pop();
  }

  /**
   * @return the current context.
   */
  public JsonContext currentContext() {
    JsonContext c = this.context.peek();
    if (c == null) throw new IllegalStateException("No JSON context!");
    return c;
  }

  /**
   * Indicates whether the current context is equal to the specified context.
   *
   * @param context The context to match
   * @return <code>true</code> if strictly equal;
   *         <code>false</code> otherwise.
   */
  public boolean isContext(JsonContext context) {
    return currentContext() == context;
  }

  /**
   * @return the name of the current context.
   */
  public String currentName() {
    String name = this.names.peek();
    if (name == null) throw new IllegalStateException("No JSON name");
    return name;
  }

  /**
   * @return the name of the current context.
   */
  private JsonTypeMap currentTypeMap() {
    JsonTypeMap type = this.types.peek();
    if (type == null) throw new IllegalStateException("No JSON type map");
    return type;
  }

  /**
   * Return the JSON type for the property name.
   *
   * @param name the name of the property
   * @return The corresponding type (never <code>null</code>)
   */
  public JsonType getType(String name) {
    return currentTypeMap().getType(name);
  }

  /**
   * @return the current state as a string.
   */
  @Override
  public String toString() {
    return currentContext()+"|"+currentTypeMap()+'|'+currentName();
  }

  // Helper inner classes
  // =============================================================================================

  /**
   * Stores instructions about the type of JSON values to be stored by name.
   */
  private static final class JsonTypeMap {

    /**
     * An empty set of instructions.
     */
    public static final JsonTypeMap EMPTY = new JsonTypeMap();

    /**
     * Names of elements to be converted to JavaScript types other than string.
     */
    private final Map<String, JsonType> map;

    /**
     * Keep private - only to create an empty set of instructions.
     */
    private JsonTypeMap() {
      this.map = Collections.emptyMap();
    }

    /**
     * Keep private, use factory method.
     *
     * @param map the internal mapping to use.
     */
    private JsonTypeMap(Map<String, JsonType> map) {
      this.map = map;
    }

    /**
     * Returns the type for the specified property name
     *
     * @param name the name of the property
     * @return The type this name is mapped to.
     */
    public JsonType getType(String name) {
      JsonType type = this.map.get(name);
      return type != null? type : JsonType.DEFAULT;
    }

    /**
     * Makes a map inheriting another map.
     *
     * <p>If there is no attribute specifying the type, then the same map is returned.
     *
     * @param inherited The property type map to inherit (might be <code>null</code>)
     * @param atts      The attributes to scan.
     *
     * @return the updated map or the inherited one if no attributes changed the types.
     */
    public static JsonTypeMap make(JsonTypeMap inherited, Attributes atts) {
      String toBoolean = atts.getValue(JSONSerializer.NS_URI, "boolean");
      String toNumber = atts.getValue(JSONSerializer.NS_URI, "number");
      String toString = atts.getValue(JSONSerializer.NS_URI, "string");
      String toNull = atts.getValue(JSONSerializer.NS_URI, "null");
      if (toBoolean == null && toNumber == null && toString == null && toNull == null)
        return inherited;
      Map<String, JsonType> updated = new HashMap<>(inherited.map);
      applyMapping(updated, toBoolean, JsonType.BOOLEAN);
      applyMapping(updated, toNumber, JsonType.NUMBER);
      applyMapping(updated, toString, JsonType.STRING);
      applyMapping(updated, toNull, JsonType.NULL);
      return new JsonTypeMap(updated);
    }

    private static void applyMapping(Map<String, JsonType> map, @Nullable String names, JsonType type) {
      if (names != null) {
        for (String name : names.split(" ")) {
          map.put(name, type);
        }
      }
    }

    @Override
    public String toString() {
      return this.map.toString();
    }
  }

}
