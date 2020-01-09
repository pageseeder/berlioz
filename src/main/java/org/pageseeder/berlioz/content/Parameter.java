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
package org.pageseeder.berlioz.content;

import java.util.Map;
import java.util.Objects;

/**
 * Specifications for a parameter to send to a content generator.
 *
 * <p>The parameters that content generators can take is fixed, this class allows content
 * generators to receives parameters from different sources and effectively remap the parameters
 * send to a generator.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.10.7
 * @since Berlioz 0.8
 */
public final class Parameter {

  /**
   * The name of this parameter.
   */
  private final String _name;

  /**
   * The value of this parameter.
   */
  private final ParameterTemplate _template;

  /**
   * Creates a new parameter.
   *
   * @param name     The name of the parameter.
   * @param template The template to use to generate the value of the parameter.
   */
  private Parameter(String name, ParameterTemplate template) {
    this._name = Objects.requireNonNull(name, "Parameter name is required");
    this._template = Objects.requireNonNull(template, "Parameter value is required");
  }

  /**
   * Creates a new parameter.
   *
   * @param name     The name of the parameter.
   * @param template The value to use to generate the value of the parameter.
   */
  public Parameter(String name, String template) {
    this(name, ParameterTemplate.parse(template));
  }

  /**
   * @return The name of this parameter.
   */
  public String name() {
    return this._name;
  }

  /**
   * @return The unresolved value of this parameter.
   */
  public String value() {
    return this._template.toString();
  }

  /**
   * Resolves the value of this parameter using the specified map of parameters.
   *
   * @param parameters The map of parameters to use.
   * @return The resolved value of this parameter.
   */
  public String value(Map<String, String> parameters) {
    return this._template.toString(parameters);
  }

}
