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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Defines a simple template for parameter values.
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
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
  private ParameterTemplate(Token... tokens) {
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

  @Override
  public String toString() {
    StringBuilder value = new StringBuilder();
    for (Token t : this._tokens) {
      value.append(t.toString());
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
    List<Token> tokens = new ArrayList<>();
    int start = 0;
    while (m.find()) {
      if (m.start() > start) {
        String text = template.substring(start, m.start());
        tokens.add(new Literal(text));
      }
      Token t = parseToken(m.group());
      if (t != null) {
        tokens.add(t);
      }
      start = m.end();
    }
    if (start < template.length()) {
      String text = template.substring(start);
      tokens.add(new Literal(text));
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
    Literal token = new Literal(value);
    return new ParameterTemplate(token);
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
    Variable token = new Variable(name, def);
    return new ParameterTemplate(token);
  }

  // Token
  // ----------------------------------------------------------------------------------------------

  private interface Token {

    /**
     * Return the resolved value of this token.
     *
     * @param map the map of values
     * @return the resolved value of this token.
     */
    String toString(Map<String, String> map);

  }


  /**
   * Parses the specified token.
   *
   * @param token the token to parse
   * @return the corresponding token instance.
   */
  public static @Nullable Token parseToken(@Nullable String token) {
    if (token == null || token.isEmpty()) return null;
    if (token.length() > 1 && token.charAt(0) == '{' && token.charAt(token.length()-1) == '}') {
      int eq = token.indexOf('=');
      // Same as a static token
      if (eq == 1) {
        String text = token.substring(2, token.length()-1);
        return new Literal(text);
      } else if (eq > 1) {
        String name = token.substring(1, eq);
        String text = token.substring(eq+1, token.length()-1);
        return new Variable(name, text);
      } else {
        String name = token.substring(1, token.length()-1);
        return new Variable(name, "");
      }
    }
    return new Literal(token);
  }

  /**
   * A token which can be resolved using a content request.
   */
  private static class Literal implements Token {

    /**
     * The token content.
     */
    private final String _text;

    /**
     * Creates a new literal token.
     *
     * @param text the text value.
     */
    private Literal(String text) {
      this._text = Objects.requireNonNull(text, "Literal must have a value");
    }

    @Override
    public String toString(Map<String, String> map) {
      return this._text;
    }

    @Override
    public String toString() {
      return this._text;
    }
  }

  /**
   * A token which can be resolved using a content request.
   */
  private static class Variable implements Token {

    /**
     * The name of the variable.
     */
    private final String _name;

    /**
     * The token content.
     */
    private final String _text;

    /**
     * Creates a new token.
     *
     * @param name the name of the parameter.
     * @param text the default value.
     */
    public Variable(String name, String text) {
      this._name = Objects.requireNonNull(name, "Variables require a name");
      this._text = Objects.toString(text, "");
    }

    @Override
    public String toString(Map<String, String> map) {
      String value = map.get(this._name);
      return value != null? value : this._text;
    }

    @Override
    public String toString() {
      if (this._text.length() > 0)
        return '{'+this._name+'='+this._text+'}';
      else
        return '{'+this._name+'}';
    }
  }

}
