/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

/**
 * Specifications for a parameter to send to a content generator.
 * 
 * <p>The parameters that content generators can take is fixed, this class allows content 
 * generators to receives parameters from different sources and effectively remap the parameters
 * send to a generator.
 * 
 * @author Christophe Lauret
 * @version 12 April 2011
 */
public final class Parameter {

  /**
   * Defines the source of the parameter.
   */
  public enum Source {

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
  private final String _value;

  /**
   * The source of the value of this parameter.
   */
  private final Source _source;

  /**
   * The default value of this parameter.
   */
  private final String _def;

  /**
   * Creates a new parameter.
   * 
   * @param name   The name of the parameter.
   * @param value  How the value of the parameter.
   * @param source Where the value of the parameter comes from.
   * @param def    The default value for this parameter.
   */
  private Parameter(String name, String value, Source source, String def) {
    this._name   = name;
    this._value  = value;
    this._source = source;
    this._def    = def;
  }

  /**
   * @return The name of this parameter.
   */
  public String name() {
    return this._name;
  }

  /**
   * @return The source of the value of this parameter.
   */
  public Source source() {
    return this._source;
  }

  /**
   * @return The value of this parameter.
   */
  public String value() {
    return this._value;
  }

  /**
   * @return The default value of this parameter.
   */
  public String def() {
    return this._def;
  }

  /**
   * A builder for parameters.
   * 
   * <p>this is a single use builder: this builder should not be used after the {@link #build()}
   * method has been invoked.
   * 
   * @author Christophe Lauret
   * @version 21 May 2010
   */
  static class Builder {

    /** Name of the parameter (required) */
    private final String _name;

    /** Value of the parameter */
    private String _value;

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
     * @param value The value (depends on the source)
     * @return this builder
     */
    public Builder value(String value) {
      this._value = value;
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
      if (this._value == null) throw new IllegalStateException("Cannot build a valueless parameter");
      Source source = Source.QUERY_STRING;
      if (this._source != null) {
        try {
          source = Source.valueOf(this._source.toUpperCase().replace('-', '_'));
        } catch (IllegalArgumentException ex) {
          throw new IllegalStateException(this._source+" is not valid source", ex);
        }
      }
      return new Parameter(this._name, this._value, source, this._def);
    }
  }

}
