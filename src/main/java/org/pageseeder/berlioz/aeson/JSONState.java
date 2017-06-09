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

import org.eclipse.jdt.annotation.Nullable;
import org.xml.sax.Attributes;

/**
 * Maintains the state of the serialization.
 *
 * <p>Note: there is no reason to expose this class as public since it is
 * primarily used by the serializer.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
final class JSONState {

  /**
   * How property values should be serialized.
   */
  public enum JSONType { STRING, NUMBER, BOOLEAN, NULL, DEFAULT };

  /**
   * The current context.
   */
  public enum JSONContext { ROOT, OBJECT, ARRAY, NULL, VALUE };

  /**
   * Keeps track of the context.
   */
  private final Deque<JSONContext> context = new ArrayDeque<>();

  /**
   * Maintains instructions for the JSON serialization at each level of the structure.
   */
  private final Deque<JSONTypeMap> types = new ArrayDeque<>();

  /**
   * Keeps track of the name of the current context.
   */
  private final Deque<String> names = new ArrayDeque<>();

  /**
   * Initialise the state with the ROOT context.
   */
  public void pushState() {
    this.context.push(JSONContext.ROOT);
    this.types.push(JSONTypeMap.EMPTY);
    this.names.push("");
  }

  /**
   * Push the state.
   *
   * @param context The new context.
   * @param atts    The attributes (may affect types)
   * @param name    The name of the context.
   */
  public void pushState(JSONContext context, Attributes atts, @Nullable String name) {
    this.context.push(context);
    JSONTypeMap map = JSONTypeMap.make(currentTypeMap(), atts);
    this.types.push(map);
    this.names.push(name != null? name : "");
  }

  /**
   * Remove all objects from state.
   */
  public void popState() {
    this.context.pop();
    this.types.pop();
    this.names.pop();
  }

  /**
   * @return the current context.
   */
  public JSONContext currentContext() {
    JSONContext c = this.context.peek();
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
  public boolean isContext(JSONContext context) {
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
  private JSONTypeMap currentTypeMap() {
    JSONTypeMap type = this.types.peek();
    if (type == null) throw new IllegalStateException("No JSON type map");
    return type;
  }

  /**
   * Return the JSON type for the property name.
   *
   * @param name the name of the property
   * @return The corresponding type (never <code>null</code>)
   */
  public JSONType getType(String name) {
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
  private static final class JSONTypeMap {

    /**
     * An empty set of instructions.
     */
    public static final JSONTypeMap EMPTY = new JSONTypeMap();

    /**
     * Names of elements to be converted to JavaScript types other than string.
     */
    private final Map<String, JSONType> map;

    /**
     * Keep private - only to create an empty set of instructions.
     */
    private JSONTypeMap() {
      this.map = Collections.emptyMap();
    }

    /**
     * Keep private - use factory method.
     *
     * @param map the internal mapping to use.
     */
    private JSONTypeMap(Map<String, JSONType> map) {
      this.map = map;
    }

    /**
     * Returns the type for the specified property name
     *
     * @param name the name of the property
     * @return The type this name is mapped to.
     */
    public JSONType getType(String name) {
      JSONType type = this.map.get(name);
      return type != null? type : JSONType.DEFAULT;
    }

    /**
     * Makes a map inheriting another map.
     *
     * <p>If there is not attribute specifying the type, then the same map is returned.
     *
     * @param inherited The property type map to inherit (may be <code>null</code>)
     * @param atts      The attributes to scan.
     *
     * @return the updated map or the inherited one if no attributes changed the types.
     */
    public static JSONTypeMap make(JSONTypeMap inherited, Attributes atts) {
      JSONTypeMap current = inherited;
      String toBoolean = atts.getValue(JSONSerializer.NS_URI, "boolean");
      String toNumber = atts.getValue(JSONSerializer.NS_URI, "number");
      String toString = atts.getValue(JSONSerializer.NS_URI, "string");
      String toNull = atts.getValue(JSONSerializer.NS_URI, "null");
      if (toBoolean == null && toNumber == null && toString == null && toNull == null) // Return the current if no new type mappings defined
      return current;
      else {
        // Update the mapping
        Map<String, JSONType> updated = new HashMap<>(current.map);
        if (toBoolean != null) {
          for (String name : toBoolean.split(" ")) {
            updated.put(name, JSONType.BOOLEAN);
          }
        }
        if (toNumber != null) {
          for (String name : toNumber.split(" ")) {
            updated.put(name, JSONType.NUMBER);
          }
        }
        if (toString != null) {
          for (String name : toString.split(" ")) {
            updated.put(name, JSONType.STRING);
          }
        }
        if (toNull != null) {
          for (String name : toNull.split(" ")) {
            updated.put(name, JSONType.NULL);
          }
        }
        return new JSONTypeMap(updated);
      }
    }

    @Override
    public String toString() {
      return this.map.toString();
    }
  }

}
