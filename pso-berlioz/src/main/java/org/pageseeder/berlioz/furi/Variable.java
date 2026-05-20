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

import java.util.Objects;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * A variable in a URL pattern or template.
 *
 * <p>The variables can be typed by prefixing the variable name. Types are not required, if no type is
 * specified, the variable is considered untyped.
 *
 * <p>Note: there is no predefined list of types as the handling of types is out of scope. The syntax
 * simply allows variables to be associated with a type.
 *
 * <p>Examples of variables:
 * <pre>
 *   foo         - An untyped variable named 'foo'
 *   bar         - An untyped variable named 'bar'
 *   ping:foo    - A variable named 'foo' typed 'ping'
 *   ping:foo=1  - A variable named 'foo' typed 'ping' which default value is '1'
 *   foo=pong    - An untyped variable named 'foo' which default value is 'pong'
 * </pre>
 *
 * <p>Variables only appear in the context of the a template expansion.
 *
 * <p>Expansion rule (4.4.1):
 *
 * <pre>
 * &quot;In a variable ('var') expansion, if the variable is defined then substitute the value of
 * the variable, otherwise substitute the default value.
 * If no default value is given then substitute with the empty string.&quot;
 * </pre>
 *
 * <p>Syntax for variables:
 * <pre>
 * var         = [ vartype &quot;:&quot; ]  varname [ &quot;=&quot; vardefault ]
 * vars        = var [ *(&quot;,&quot; var) ]
 * vartype     = (ALPHA / DIGIT)* (ALPHA / DIGIT / &quot;.&quot; / &quot;_&quot; / &quot;-&quot; )
 * varname     = (ALPHA / DIGIT)* (ALPHA / DIGIT / &quot;.&quot; / &quot;_&quot; / &quot;-&quot; )
 * vardefault  = *(unreserved / pct-encoded)
 * </pre>
 *
 * @see <a
 *      href="http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html">URI
 *      Template (Internet Draft 3)</a>
 * @see <a href="http://tools.ietf.org/html/rfc3986">RFC 3986 - Uniform Resource Identifier (URI):
 *      Generic Syntax<a/>
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.13.0
 * @since Berlioz 0.9.32
 */
public class Variable {

  /**
   * Used for reserved variable names.
   */
  public enum Reserved {

    /**
     * The wildcard represented by the 'asterisk'.
     */
    WILDCARD("*");

    /**
     * The symbol for this reserved.
     */
    private final String symbol;

    /**
     * Construct a new reserved variable - keep it private.
     *
     * @param symbol The symbol used for this reserved variable name.
     */
    Reserved(String symbol) {
      this.symbol = symbol;
    }

    /**
     * @return the symbol used for this reserved variable name.
     */
    String symbol() {
      return this.symbol;
    }
  }

  /**
   * Indicate that the variable's value should be processed as a list ("@") or an associative array ("%").
   *
   * <p>This variable type is an instruction for the template processor.
   * It is not an indication of language or implementation type.
   */
  public enum Form {

    /**
     * Indicate that this variable can be expanded as a simple string (default).
     */
    STRING,

    /**
     * Indicate that this variable can be expanded as a list of strings.
     */
    LIST,

    /**
     * Indicates that this variable can be expanded as an associated array.
     */
    MAP;

    /**
     * Returns the type of this variable from the specified expression.
     *
     * <p>
     * This method does not return <code>null</code>
     *
     * @param exp The expression.
     * @return The type of this expression.
     */
    protected static Form getType(String exp) {
      if (exp.isEmpty()) return STRING;
      char c = exp.charAt(0);
      if (c == '@') return LIST;
      if (c == '%') return MAP;
      return STRING;
    }

  }

  /**
   * Indicate that the variable's value should be processed as a list ("@") or an associative array ("%").
   *
   * <p>This variable type is an instruction for the template processor.
   * It is not an indication of language or implementation type.
   */
  public enum Modifier {

    /**
     * Indicate that this variable can be expanded as a simple string (default).
     */
    SUBSTRING,

    /**
     * Indicate that this variable can be expanded as a list of strings.
     */
    REMAINDER

  }

  /**
   * The pattern for a valid variable name.
   */
  private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9][\\w.-]*");

  /**
   * The pattern for a valid normalised variable value: any unreserved character or an escape
   * sequence. This pattern contains non-capturing parentheses to make it easier to get variable
   * values as a group.
   */
  protected static final Pattern VALID_VALUE = Pattern.compile("(?:[\\w.~@-]|(?:%[0-9A-F]{2}))+");

  /**
   * The default value is an empty string.
   */
  private static final String DEFAULT_VALUE = "";

  /**
   * The type of this variable.
   */
  private Form form;

  /**
   * The implementation type of this variable (eg. string, integer, etc... can be user-defined).
   *
   * <p>
   * Use <code>null</code> for untyped.
   */
  private @Nullable VariableType type;

  /**
   * The name of this variable.
   */
  private final String name;

  /**
   * The default value for this variable.
   */
  private final String defaultValue;

  /**
   * Creates a new untyped reserved variable.
   *
   * @param reserved The name of the variable.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public Variable(Reserved reserved) {
    this.name = reserved.symbol();
    this.defaultValue = DEFAULT_VALUE;
    this.form = Form.STRING;
    this.type = null;
  }

  /**
   * Creates a new untyped variable.
   *
   * @param name The name of the variable.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public Variable(String name) {
    this(name, DEFAULT_VALUE);
  }

  /**
   * Creates a new untyped variable.
   *
   * @param name The name of the variable.
   * @param def The default value for the variable.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public Variable(String name, @Nullable String def) {
    this(name, def, null);
  }

  /**
   * Creates a new variable.
   *
   * @param name The name of the variable.
   * @param def  The default value for the variable.
   * @param type The type of the variable.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public Variable(String name, @Nullable String def, @Nullable VariableType type) {
    this.name = Objects.requireNonNull(name, "A variable must have a name, but was null");
    this.defaultValue = Objects.toString(def, DEFAULT_VALUE);
    this.type = type;
    this.form = Form.getType(name);
    if (!isValidName(name))
      throw new IllegalArgumentException("The variable name is not valid: " + name);
  }

  /**
   * Creates a new variable.
   *
   * @param name The name of the variable.
   * @param def  The default value for the variable.
   * @param type The type of the variable.
   *
   * @throws NullPointerException If the specified name is <code>null</code>.
   * @throws IllegalArgumentException If the specified name is an empty string.
   */
  public Variable(String name, String def, VariableType type, Form form) {
    this.name = Objects.requireNonNull(name, "A variable must have a name, but was null");
    this.defaultValue = def != null ? def : DEFAULT_VALUE;
    this.type = type;
    this.form = form != null? form : Form.STRING;
    if (!isValidName(name))
      throw new IllegalArgumentException("The variable name is not valid: " + name);
  }

  /**
   * Returns the form of this variable.
   *
   * <p>
   * This method will never return <code>null</code>.
   *
   * @return The form of this variable.
   */
  public Form form() {
    return this.form;
  }

  /**
   * Returns the name of this variable.
   *
   * <p>
   * This method never return <code>null</code>.
   *
   * @return The name of this variable.
   */
  public String name() {
    return this.name;
  }

  /**
   * Returns the default value for this variable.
   *
   * This method never return <code>null</code>.
   *
   * @return The default value for this variable.
   */
  public String defaultValue() {
    return this.defaultValue;
  }

  /**
   * Returns the implementation type of this variable.
   *
   * <p>
   * This method will return <code>null</code> if the variable is untyped.
   *
   * @return The type of this variable.
   */
  public @Nullable VariableType type() {
    return this.type;
  }

  /**
   * Returns the expanded value of this variable.
   *
   * If no value is specified for this variable, the default value is returned instead.
   *
   * @param parameters The parameters.
   *
   * @return The value.
   */
  public String value(@Nullable Parameters parameters) {
    // No parameters: use the default value
    if (parameters == null)
      return this.defaultValue;
    // Defined and non-empty: return the first value in a list
    String[] values = parameters.getValues(this.name);
    if (values != null && values.length > 0 && values[0] != null) return values[0];
    // Empty or undefined: return the default
    else return this.defaultValue;
  }

  /**
   * Returns the expanded value of this variable.
   *
   * <p>If no values are specified for this variable, the default value is returned instead.
   *
   * @param parameters The parameters.
   *
   * @return The values.
   */
  public String[] values(@Nullable Parameters parameters) {
    // No parameters: use the default value
    if (parameters == null)
      return new String[] { this.defaultValue};
    String[] values = parameters.getValues(this.name);
    // Defined and non-empty: return the values
    if (values != null && values.length > 0 && values[0].length() > 0) return values;
    // Empty or undefined: return the default
    else return new String[] { this.defaultValue};
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this)
      return true;
    if ((o == null) || (o.getClass() != this.getClass()))
      return false;
    Variable v = (Variable) o;
    // name and default cannot be null
    return this.name.equals(v.name) && this.defaultValue.equals(v.defaultValue);
  }

  @Override
  public int hashCode() {
    return this.name.hashCode() + 7 * this.defaultValue.hashCode();
  }

  @Override
  public String toString() {
    if (this.defaultValue.isEmpty())
      return this.name;
    else
      return this.name + '=' + this.defaultValue;
  }

  // Static helpers
  // ==============================================================================================

  /**
   * Parses the given expression and returns the corresponding instance.
   *
   * @param exp The expression to parse.
   *
   * @return the corresponding variable.
   *
   * @throws URITemplateSyntaxException If the expression cannot be parsed
   */
  public static Variable parse(String exp) throws URITemplateSyntaxException {
    // Capture the form if any
    Form f = Form.getType(exp);
    if (f != Form.STRING) {
      exp = exp.substring(1);
    }
    int colon = exp.indexOf(':');
    // untyped
    if (colon < 0) {
      Variable v = parseUntyped(exp);
      v.form = f;
      return v;
    // ignore the empty type and treat as untyped
    } else if (colon == 0) {
      Variable v = parseUntyped(exp.substring(1));
      v.form = f;
      return v;
    // a type is specified
    } else {
      Variable v = parseUntyped(exp.substring(colon + 1));
      v.type = new VariableType(exp.substring(0, colon));
      v.form = f;
      return v;
    }
  }

  /**
   * Parses the given expression and returns the corresponding instance.
   *
   * @param exp The expression to parse.
   *
   * @return the corresponding variable.
   *
   * @throws URITemplateSyntaxException If the expression cannot be parsed
   */
  private static Variable parseUntyped(String exp) throws URITemplateSyntaxException {
    int equal = exp.indexOf('=');
    if (equal == 0)
      throw new URITemplateSyntaxException(exp, "Variable name is empty string");
    if (equal > 0) return new Variable(exp.substring(0, equal), exp.substring(equal + 1));
    else return new Variable(exp, null);
  }

  /**
   * Indicates whether the variable has a valid name according to the specifications.
   *
   * @param name The name of the variable.
   *
   * @return <code>true</code> if the name is valid; <code>false</code> otherwise.
   */
  public static boolean isValidName(@Nullable String name) {
    if (name == null)
      return false;
    return VALID_NAME.matcher(name).matches();
  }

  /**
   * Indicates whether the variable has a valid value according to the specifications.
   *
   * @param value The value of the variable.
   *
   * @return <code>true</code> if the name is not valid; <code>false</code> otherwise.
   */
  public static boolean isValidValue(@Nullable String value) {
    if (value == null)
      return false;
    return VALID_VALUE.matcher(value).matches();
  }

  // helpers -------------------------------------------------------------------

  /**
   * Returns the name of this variable as a regular expression pattern string for use in a regular
   * expression.
   *
   * <p>
   * Implementation note: this method replaces any character that could be interpreted as a regex
   * meta-character, it is more efficient than using quotation (\Q...\E) for the whole string.
   *
   * @return The regex pattern corresponding to this name.
   */
  protected String namePatternString() {
    return this.name.indexOf('.') < 0 ? this.name : name().replaceAll("\\.", "\\\\.");
  }

}
