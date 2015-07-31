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

/**
 * Specifications for a parameter to send to a content generator.
 *
 * <p>The parameters that content generators can take is fixed, this class allows content
 * generators to receives parameters from different sources and effectively remap the parameters
 * send to a generator.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.9.0 - 13 October 2011
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
    this._name = name;
    this._template = template;
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

  /**
   * A builder for parameters.
   *
   * <p>this is a single use builder: this builder should not be used after the {@link #build()}
   * method has been invoked.
   *
   * @author Christophe Lauret
   * @version 28 June 2011
   */
  static class Builder {

    /** Name of the parameter (required) */
    private final String _name;

    /** Value of the parameter */
    private String _template;

    /** Source of the parameter */
    private String _source;

    /** Default value for the parameter */
    private String _def;

    /**
     * Creates a new builder.
     * @param name the name of the parameter to build.
     */
    Builder(String name) {
      this._name = name;
    }

    /**
     * Set the value for this parameter.
     * @param template The value template.
     * @return this builder
     */
    Builder value(String template) {
      this._template = template;
      return this;
    }

    /**
     * Set the source for this parameter.
     * @param source The source of the value.
     * @return this builder
     */
    Builder source(String source) {
      this._source = source;
      return this;
    }

    /**
     * Set the default value for this parameter.
     * @param def The default value (may be <code>null</code>)
     * @return this builder
     */
    Builder def(String def) {
      this._def = def;
      return this;
    }

    /**
     * Creates an immutable parameter.
     * @return The corresponding parameter.
     * @throws IllegalStateException If either of the name or value is <code>null</code>;
     *                               or the source is not parsable.
     */
    Parameter build() throws IllegalStateException {
      if (this._name == null) throw new IllegalStateException("Cannot build a nameless parameter");
      if (this._template == null) throw new IllegalStateException("Cannot build a valueless parameter");
      ParameterTemplate template = null;
      // Backward compatibility
      if (this._source != null && !"string".endsWith(this._source)) {
        template = ParameterTemplate.parameter(this._template, this._def);
      } else {
        template = ParameterTemplate.parse(this._template);
      }
      return new Parameter(this._name, template);
    }
  }

}
