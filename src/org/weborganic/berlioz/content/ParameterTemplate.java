/*
 * This file is part of the Berlioz library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at 
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defines a simple template for parameter values. 
 * 
 * @author Christophe Lauret
 * @version 28 June 2011
 * 
 * @since Berlioz 0.8.2
 */
public final class ParameterTemplate {

  /**
   * A variable in a template.
   */
  private static final Pattern VARIABLE = Pattern.compile("\\{[^}{]+}");

  /**
   * The template split in tokens.
   */
  private final Token[] _tokens;

  /**
   * Creates a new parameter template.
   * 
   * @param tokens The tokens that make up this template.
   */
  private ParameterTemplate(Token[] tokens) {
    this._tokens = tokens;
  }

  /**
   * Resolves this template using the specified content request.
   * 
   * @param map A map of values to use.
   * @return the resolved template as a string.
   */
  public String toString(Map<String, String> map) {
    StringBuilder value = new StringBuilder();
    for (Token t : this._tokens) {
      value.append(t.toString(map));
    }
    return value.toString();
  }

  /**
   * Returns the value of this template using the content request.
   * 
   * @param template The template to parse.
   * @return the resolved template.
   */
  public static ParameterTemplate parse(String template) {
    // Does not contain any dynamic tokens
    if (template.indexOf('{') < 0) return ParameterTemplate.value(template);
    // Parse
    Matcher m = VARIABLE.matcher(template);
    List<Token> tokens = new ArrayList<Token>();
    int start = 0;
    while (m.find()) {
      if (m.start() > start) {
        String text = template.substring(start, m.start());
        tokens.add(Token.parse(text));
      }
      tokens.add(Token.parse(m.group()));
      start = m.end();
    }
    if (start < template.length()) {
      String text = template.substring(start, template.length());
      tokens.add(Token.parse(text));
    }
    return new ParameterTemplate(tokens.toArray(new Token[]{}));
  }

  /**
   * Returns a template for single constant string value.
   * 
   * <p>Use this method when you know that the template is made of a single plain value and does
   * not require to be parsed.
   * 
   * @param value The value.
   * @return the corresponding template.
   */
  public static ParameterTemplate value(String value) {
    Token[] tokens = new Token[]{Token.value(value)};
    return new ParameterTemplate(tokens);
  }

  /**
   * Returns a template for single constant string value.
   * 
   * <p>Use this method when you know that the template is made of a single plain value and does
   * not require to be parsed.
   * 
   * @param name The value.
   * @param def   The default value for the parameter (optional)
   * @return the corresponding template.
   */
  public static ParameterTemplate parameter(String name, String def) {
    Token[] tokens = new Token[]{Token.parameter(name, def)};
    return new ParameterTemplate(tokens);
  }

  // Token
  // ----------------------------------------------------------------------------------------------

  /**
   * A token which can be resolved using a content request. 
   */
  private static class Token {

    /** The types of tokens which can be used with this template */
    private enum Type {

      /** The token is a static string */
      STRING,

      /** The token is a parameter */
      PARAMETER
    }

    /**
     * The type of token.
     */
    private final Type _type;

    /**
     * The token content.
     */
    private final String _name;

    /**
     * The token content.
     */
    private final String _text;

    /**
     * Creates a new token. 
     * @param type the type of token
     * @param name the name of the parameter (may be <code>null</code>.
     * @param text the text or default value.
     */
    public Token(Type type, String name, String text) {
      this._type = type;
      this._name = name;
      this._text = text;
    }

    /**
     * Return the resolved value of this token.
     * @param map the map of values
     * @return the resolved value of this token. 
     */
    private String toString(Map<String, String> map) {
      switch (this._type) {
        case STRING: 
          return this._text;
        case PARAMETER:
          String value = map.get(this._name); 
          return value != null? value : this._text;
        default:
          return this._text;
      }
    }

    /**
     * Parses the specified token.
     * 
     * @param token the token to parse
     * @return the corresponding token instance.
     */
    public static Token parse(String token) {
      if (token == null || token.isEmpty()) return null;
      Type type = Type.STRING;
      String name = null;
      String text = token;
      if (token.length() > 1 && token.charAt(0) == '{' && token.charAt(token.length()-1) == '}') {
        int eq = token.indexOf('=');
        // Same as a static token
        if (eq == 1) {
          text = token.substring(2, token.length()-1);
        } else if (eq > 1) {
          type = Type.PARAMETER;
          name = token.substring(1, eq);
          text = token.substring(eq+1, token.length()-1);
        } else {
          type = Type.PARAMETER;
          name = token.substring(1, token.length()-1);
          text = "";
        }
      }
      return new Token(type, name, text);
    }

    /**
     * Returns a simple STRING token with the text field set to the specified value.
     * 
     * @param value the plain value to use to parse
     * @return the corresponding token instance.
     */
    public static Token value(String value) {
      return new Token(Type.STRING, null, value);
    }

    /**
     * Returns a simple PARAMETER token with the name and text fields set to the specified values.
     * 
     * @param name the name of the parameter.
     * @param text the default value for the parameter (optional)
     * @return the corresponding token instance.
     */
    public static Token parameter(String name, String text) {
      if (name == null || name.isEmpty()) return null;
      return new Token(Type.STRING, name, text);
    }

    @Override
    public String toString() {
      return this._type == Type.PARAMETER? '{'+this._name+'='+this._text+'}' : this._text;
    }
  }

}
