package org.weborganic.berlioz.content;

/**
 * Specifications for a parameter to send to a content generator.
 * 
 * <p>The parameters that content generators can take is fixed, this class allows content 
 * generators to receives parameters from different sources and effectively remap the parameters
 * send to a generator.
 * 
 * @author Christophe Lauret
 * @version 21 May 2010
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
   * Creates a new parameter 
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
   * A builder for parameters - this is a single use builder.
   * 
   * @author Christophe Lauret
   * @version 21 May 2010
   */
  static class Builder {

    private final String _name;
    
    private String _value;
    
    private String _source;
    
    private String _def;

    public Builder(String name) {
      this._name = name;
    }

    public Builder value(String value) {
      this._value = value;
      return this;
    }
    
    public Builder source(String source) {
      this._source = source;
      return this;
    }
    
    public Builder def(String def) {
      this._def = def;
      return this;
    }

    Parameter build() throws IllegalStateException {
      if (this._name == null) throw new IllegalStateException("Cannot build a nameless parameter");
      if (this._value == null) throw new IllegalStateException("Cannot build a valueless parameter");
      Source source = Source.QUERY_STRING;
      try {
        source = this._source != null? Source.valueOf(this._source.toUpperCase().replace('-', '_')) : Source.QUERY_STRING;
      } catch (Exception ex) {
        throw new IllegalStateException(this._source+" is not valid source", ex);
      }
      return new Parameter(this._name, this._value, source, this._def);
    }
  }

}
