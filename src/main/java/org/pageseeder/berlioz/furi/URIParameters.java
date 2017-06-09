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
package org.pageseeder.berlioz.furi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

/**
 * A class to hold a collection of parameters for use during the expansion process.
 *
 * It provides more convenient functions than the underlying map and handles the rules for parameter
 * values.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public class URIParameters implements Parameters {

  /**
   * Maps the parameter names to the values.
   */
  private final Map<String, String[]> _parameters;

  /**
   * Creates a new instance.
   */
  public URIParameters() {
    this._parameters = new HashMap<>();
  }

  /**
   * Creates a new instance from the specified map.
   *
   * @param parameters The map of parameters to supply
   */
  public URIParameters(Map<String, String[]> parameters) {
    this._parameters = new HashMap<>(parameters);
  }

  @Override
  public void set(String name, @Nullable String value) {
    if (value == null) return;
    this._parameters.put(name, new String[] { value });
  }

  @Override
  public void set(String name, String @Nullable [] values) {
    if (values == null) return;
    this._parameters.put(name, values);
  }

  @Override
  public Set<String> names() {
    return Collections.unmodifiableSet(this._parameters.keySet());
  }

  @Override
  public @Nullable String getValue(String name) {
    String[] vals = this._parameters.get(name);
    if (vals == null || vals.length == 0)
      return null;
    else
      return vals[0];
  }

  @Override
  public String @Nullable [] getValues(String name) {
    return this._parameters.get(name);
  }

  @Override
  public boolean exists(String name) {
    return this._parameters.containsKey(name);
  }

  @Override
  public boolean hasValue(String name) {
    String[] values = this._parameters.get(name);
    return values != null && values.length > 0 && values[0].length() > 0;
  }

}
