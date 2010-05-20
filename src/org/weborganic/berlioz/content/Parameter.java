package org.weborganic.berlioz.content;

/**
 * Specifications for a parameter to send to a content generator.
 * 
 * <p>The parameters that content generators can take is fixed, this class allows
 * content generators to receives parameters from different sources.
 * 
 * @author Christophe Lauret
 * @version 20 May 2010
 */
public class Parameter {

  /**
   * Defines the source of the parameter.
   */
  public enum Source {
    STRING,
    QUERY_STRING,
    URI_VARIABLE
  }

  private final String _name;
  
  private final String _value;
  
  private final Source _source;
  
  private final String _def;
  
  private Parameter(String name, String value, Source source, String def) {
    this._name   = name;
    this._value  = value;
    this._source = source;
    this._def    = def;
  }
  
  public String name(){
    return this._name;
  }

  public Source source(){
    return this._source;
  }
  
  public String value(){
    return this._value;
  }
  
  public String def(){
    return this._def;
  }

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
        source = this._source != null? Source.valueOf(this._source) : Source.QUERY_STRING;
      } catch (Exception ex) {
        throw new IllegalStateException(this._source+" is not valid source", ex);
      }
      return new Parameter(this._name, this._value, source, this._def);
    }
  }

}
