/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.Map;

/**
 * Specifications for a parameter to send to a content generator.
 * 
 * <p>The parameters that content generators can take is fixed, this class allows content 
 * generators to receives parameters from different sources and effectively remap the parameters
 * send to a generator.
 * 
 * @author Christophe Lauret
 * @version 28 June 2011
 */
public final class Parameter {

  /**
   * Defines the source of the parameter.
   * 
   * @deprecated Use the parameter template notation instead.
   */
  @Deprecated public enum Source {

    /** Use the string value as it is. */
    STRING,

    /** Use the corresponding parameter value from query string of the URL. */
    QUERY_STRING,

    /** Use the resolved URI variable. */
    URI_VARIABLE

  }

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
   * @return Always STRING.
   * 
   * @deprecated The source is no longer used.
   */
  @Deprecated public Source source() {
    return Source.STRING;
  }

  /**
   * @return The <code>null</code>.
   * @deprecated This class now uses templates. 
   */
  @Deprecated public String def() {
    return null;
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
    public Builder(String name) {
      this._name = name;
    }

    /**
     * Set the value for this parameter.
     * @param template The value template.
     * @return this builder
     */
    public Builder value(String template) {
      this._template = template;
      return this;
    }

    /**
     * Set the source for this parameter.
     * @param source The source of the value.
     * @return this builder
     */
    public Builder source(String source) {
      this._source = source;
      return this;
    }

    /**
     * Set the default value for this parameter.
     * @param def The default value (may be <code>null</code>)
     * @return this builder
     */
    public Builder def(String def) {
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
