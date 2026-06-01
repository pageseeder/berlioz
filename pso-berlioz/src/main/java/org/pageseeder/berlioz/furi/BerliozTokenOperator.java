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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.pageseeder.berlioz.furi.Variable.Form;

/**
 * A token based on the operators defined in the latest draft.
 *
 * <pre>{@code
 *  instruction   = &quot;{&quot; [ operator ] variable-list &quot;}&quot;
 *  operator      = &quot;/&quot; / &quot;+&quot; / &quot;;&quot; / &quot;?&quot; / op-reserve
 *  variable-list =  varspec *( &quot;,&quot; varspec )
 *  varspec       =  [ var-type ] varname [ &quot;:&quot; prefix-len ] [ &quot;=&quot; default ]
 *  var-type      = &quot;@&quot; / &quot;%&quot; / type-reserve
 *  varname       = ALPHA *( ALPHA | DIGIT | &quot;_&quot; )
 *  prefix-len    = 1*DIGIT
 *  default       = *( unreserved / reserved )
 *  op-reserve    = &lt;anything else that isn't ALPHA or operator&gt;
 *  type-reserve  = &lt;anything else that isn't ALPHA, &quot;,&quot;, or operator&gt;
 * }</pre>
 *
 * @see <a href="http://code.google.com/p/uri-templates/source/browse/trunk/spec/draft-gregorio-uritemplate.xml">URI
 * Template Library draft specifications at Google Code</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
public class BerliozTokenOperator extends TokenBase implements TokenOperator, Matchable {

  /**
   * The list of operators currently supported.
   */
  public enum Operator {

    /**
     * The '?' operator for query parameters.
     *
     * <p>Example:
     * <pre>{@code
     *  undef = null;
     *  empty = &quot;&quot;;
     *  x     = &quot;1024&quot;;
     *  y     = &quot;768&quot;;
     *
     * {?x,y}                    ?x=1024&amp;y=768
     * {?x,y,empty}              ?x=1024&amp;y=768&amp;empty=
     * {?x,y,undef}              ?x=1024&amp;y=768
     * }</pre>
     */
    QUERY_PARAMETER('?') {
      @Override
      public String expand(List<Variable> variables, Parameters parameters) {
        StringBuilder expansion = new StringBuilder();
        boolean first = true;
        for (Variable variable : variables) {
          if (parameters.exists(variable.name())) {
            String[] values = variable.values(parameters);
            // Associative Array: odd indexed values are names, even are values
            if (variable.form() == Form.MAP) {
              for (int i = 0; i < values.length; i += 2) {
                expansion.append(first ? '?' : '&');
                expansion.append(URICoder.encode(values[i])).append('=');
                if (i + 1 < values.length) {
                  expansion.append(URICoder.encode(values[i + 1]));
                }
                first = false;
              }
            // List: names, automatically number the names
            } else if (variable.form() == Form.LIST) {
              for (int i = 0; i < values.length; i++) {
                expansion.append(first ? '?' : '&');
                expansion.append(variable.name());
                if (i > 0) {
                  expansion.append(i+1);
                }
                expansion.append('=').append(URICoder.encode(values[i]));
                first = false;
              }
            // String: join the values with a comma
            } else {
              expansion.append(first? '?' : '&');
              expansion.append(variable.name()).append('=');
              for (int i = 0; i < values.length; i++) {
                if (i > 0) {
                  expansion.append(',');
                }
                expansion.append(URICoder.encode(values[i]));
              }
              first = false;
            }

          }
        }
        return expansion.toString();
      }

      @Override
      boolean isResolvable(List<Variable> variables) {
        return true;
      }

      @Override
      boolean resolve(List<Variable> variables, String value, Map<Variable, Object> values) {
        for (Variable variable : variables) {
          Pattern p = Pattern.compile("(?<=[&?]"+variable.namePatternString()+"=)([^&#]*)");
          Matcher m = p.matcher(value);
          while (m.find()) {
            values.put(variable, m.group());
          }
        }
        return true;
      }

      @Override
      Pattern pattern(List<Variable> variables) {
        StringBuilder pattern = new StringBuilder();
        pattern.append("\\?(");
        for (Variable variable : variables) {
          pattern.append('(');
          pattern.append(variable.namePatternString());
          pattern.append("=[^&#]*)|");
        }
        pattern.append("&)*");
        return Pattern.compile(pattern.toString());
      }
    },

    /**
     * The ';' operator for path parameters.
     *
     * <p>Example:
     * <pre>{@code
     *  undef = null;
     *  empty = &quot;&quot;;
     *  x     = &quot;1024&quot;;
     *  y     = &quot;768&quot;;
     *
     * {;x,y}                    ;x=1024;y=768
     * {;x,y,empty}              ;x=1024;y=768;empty
     * {;x,y,undef}              ;x=1024;y=768
     * }</pre>
     */
    PATH_PARAMETER(';') {
      @Override
      String expand(List<Variable> variables, Parameters parameters) {
        StringBuilder expansion = new StringBuilder();
        for (Variable variable : variables) {
          if (parameters.exists(variable.name())) {
            // An associative array: odd index for names, even index for values
            if (variable.form() == Form.MAP) {
              String[] values = variable.values(parameters);
              for (int i = 0; i < values.length; i += 2) {
                expansion.append(';').append(URICoder.encode(values[i]));
                if (i + 1 < values.length) {
                  expansion.append('=').append(URICoder.encode(values[i + 1]));
                }
              }
            // A list
            } else if (variable.form() == Form.LIST) {
              String[] values = variable.values(parameters);
              for (String value : values) {
                expansion.append(';');
                expansion.append(variable.name());
                if (!value.isEmpty()) {
                  expansion.append('=').append(URICoder.encode(value));
                }
              }
            // A string
            } else {
              String[] values = variable.values(parameters);
              for (String value : values) {
                expansion.append(';').append(variable.name());
                if (!value.isEmpty()) {
                  expansion.append('=').append(URICoder.encode(value));
                }
              }
            }
          }
        }
        return expansion.toString();
      }

      @Override
      boolean isResolvable(List<Variable> vars) {
        return true;
      }

      @Override
      boolean resolve(List<Variable> variables, String value, Map<Variable, Object> values) {
        for (Variable variable : variables) {
          Pattern p = Pattern.compile("(?<=;"+variable.namePatternString()+"=)([^;/?#]*)");
          Matcher m = p.matcher(value);
          while(m.find()) {
            values.put(variable, m.group());
          }
        }
        return true;
      }

      @Override
      Pattern pattern(List<Variable> variables) {
        StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (Variable variable : variables) {
          pattern.append("(?:;");
          pattern.append(variable.namePatternString());
          pattern.append("=[^;/?#]*)|");
        }
        pattern.append(";)*");
        return Pattern.compile(pattern.toString());
      }
    },

    /**
     * The '/' operator for path segments.
     *
     * <p>Example:
     * <pre>{@code
     *  list  = [ &quot;val1&quot;, &quot;val2&quot;, &quot;val3&quot; ];
     *  x     = &quot;1024&quot;;
     *
     *  {/list,x}                 /val1/val2/val3/1024
     * }</pre>
     */
    PATH_SEGMENT('/') {

      @Override
      String expand(List<Variable> variables, Parameters parameters) {
        StringBuilder expansion = new StringBuilder();
        for (Variable variable : variables) {
          if (parameters.exists(variable.name())) {
            String[] values = variable.values(parameters);
            for (String value : values) {
              expansion.append('/');
              expansion.append(URICoder.encode(value));
            }
          }
        }
        return expansion.toString();
      }

      @Override
      boolean isResolvable(List<Variable> arg0) {
        return true;
      }

      @Override
      boolean resolve(List<Variable> vars, String value, Map<Variable, Object> values) {
        if (vars.size() != 1)
          throw new UnsupportedOperationException("Operator + cannot be resolved with multiple variables.");
        values.put(vars.get(0), URICoder.decode(value));
        return true;
      }

      @Override
      Pattern pattern(List<Variable> variables) {
        return Pattern.compile("(?:/[^/?#]*+)*+");
      }
    },

    /**
     * The '+' operator for URI inserts.
     *
     * <p>Example:
     * <pre>{@code
     * empty = &quot;&quot;
     * path  = &quot;/foo/bar&quot;
     * x     = &quot;1024&quot;
     *
     *  {+path}/here              /foo/bar/here
     *  {+path,x}/here            /foo/bar,1024/here
     *  {+path}{x}/here           /foo/bar1024/here
     *  {+empty}/here             /here
     * }</pre>
     */
    URI_INSERT('+') {

      @Override
      String expand(List<Variable> vars, Parameters parameters) {
        StringBuilder expansion = new StringBuilder();
        for (Iterator<Variable> i = vars.iterator(); i.hasNext();) {
          Variable variable = i.next();
          if (parameters.exists(variable.name())) {
            String[] values = variable.values(parameters);
            for (String value : values) {
              expansion.append(URICoder.minimalEncode(value));
            }
          }
          if (i.hasNext()) {
            expansion.append(',');
          }
        }
        return expansion.toString();
      }

      @Override
      boolean resolve(List<Variable> vars, String value, Map<Variable, Object> values) {
        values.put(vars.get(0), URICoder.decode(value));
        return true;
      }

      @Override
      boolean isResolvable(List<Variable> vars) {
        return vars.size() == 1;
      }

      @Override
      Pattern pattern(List<Variable> variables) {
        return Pattern.compile("[^?#]*");
      }
    },

    /**
     * The substitution operator is only used to aggregate variables.
     */
    SUBSTITUTION(' ') {

      @Override
      String expand(List<Variable> vars, Parameters parameters) {
        StringBuilder expansion = new StringBuilder();
        for (Iterator<Variable> i = vars.iterator(); i.hasNext();) {
          Variable variable = i.next();
          if (parameters.exists(variable.name())) {
            String[] values = variable.values(parameters);
            for (String value : values) {
              expansion.append(URICoder.encode(value));
            }
          }
          if (i.hasNext()) {
            expansion.append(',');
          }
        }
        return expansion.toString();
      }

      @Override
      boolean resolve(List<Variable> vars, String value, Map<Variable, Object> values) {
        values.put(vars.get(0), URICoder.decode(value));
        return true;
      }

      @Override
      boolean isResolvable(List<Variable> vars) {
        return vars.size() == 1;
      }

      @Override
      Pattern pattern(List<Variable> variables) {
        return Pattern.compile("[^;/?#,&]*");
      }
    };

    /**
     * The character used to represent this operator.
     */
    private final char c;

    /**
     * Creates a new operator.
     *
     * @param c The character used to represent this operator.
     */
    Operator(char c) {
      this.c = c;
    }

    /**
     * Returns the character.
     *
     * @return The character used to represent this operator.
     */
    public char character() {
      return this.c;
    }

    /**
     * Indicates whether the operator can be resolved.
     *
     * @param vars The variables for the operator.
     */
    abstract boolean isResolvable(List<Variable> vars);

    /**
     * Apply the expansion rules defined for the operator given the specified argument, variable and
     * parameters.
     *
     * @param vars The variables for the operator.
     * @param params The parameters to use.
     */
    abstract String expand(List<Variable> vars, Parameters params);

    /**
     * Returns the pattern for this operator given the specified list of variables.
     *
     * @param variables The variables for the operator.
     */
    abstract Pattern pattern(List<Variable> variables);

    /**
     * Returns the map of the string to values given  the specified data.
     */
    abstract boolean resolve(List<Variable> vars, String value, Map<Variable, Object> values);

  }

  /**
   * The operator.
   */
  private final Operator operator;

  /**
   * The variables for this token.
   */
  private final List<Variable> variables;

  /**
   * The pattern for this token.
   */
  private final Pattern pattern;

  /**
   * Creates a new operator token for one variable only.
   *
   * @param op  The operator to use.
   * @param variable The variable for this operator.
   *
   * @throws NullPointerException If any of the argument is <code>null</code>.
   */
  public BerliozTokenOperator(Operator op, Variable variable) {
    super(toExpression(op, variable));
    this.operator = op;
    this.variables = new ArrayList<>(1);
    this.variables.add(variable);
    this.pattern = op.pattern(this.variables);
  }

  /**
   * Creates a new operator token.
   *
   * @param op The operator to use.
   * @param variables The variables for this operator.
   *
   * @throws NullPointerException If any of the argument is <code>null</code>.
   */
  public BerliozTokenOperator(Operator op, List<Variable> variables) {
    super(toExpression(op, variables));
    this.operator = Objects.requireNonNull(op, "The operator is required");
    this.variables = variables;
    this.pattern = op.pattern(variables);
  }

  /**
   * Expands the token operator using the specified parameters.
   *
   * @param parameters The parameters for variable substitution.
   *
   * @return The corresponding expanded string.
   */
  @Override
  public String expand(Parameters parameters) {
    return this.operator.expand(this.variables, parameters);
  }

  /**
   * Returns the operator part of this token.
   *
   * @return the operator.
   */
  public Operator operator() {
    return this.operator;
  }

  /**
   * Returns the list of variables used in this token.
   *
   * @return the list of variables.
   */
  @Override
  public List<Variable> variables() {
    return this.variables;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isResolvable() {
    return this.operator.isResolvable(this.variables);
  }

  @Override
  public boolean resolve(String expanded, Map<Variable, Object> values) {
    if (isResolvable()) {
      this.operator.resolve(this.variables, expanded, values);
      return true;
    } else return false;
  }

  @Override
  public boolean match(String part) {
    return this.pattern.matcher(part).matches();
  }

  @Override
  public Pattern pattern() {
    return this.pattern;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return super.equals(o);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * Returns the operator if it is defined in this class.
   *
   * @param c The character representation of the operator.
   *
   * @return The corresponding operator instance.
   */
  public static Operator toOperator(char c) {
    for (Operator o : Operator.values()) {
      if (o.character() == c)
        return o;
    }
    // default on simple substitution
    return Operator.SUBSTITUTION;
  }

  /**
   * Parses the specified string and returns the corresponding token.
   *
   * <p>This method accepts both the raw expression or the expression wrapped in curly brackets.
   *
   * @param exp The expression to parse.
   *
   * @return The corresponding token.
   *
   * @throws URITemplateSyntaxException If the string cannot be parsed as a valid
   */
  public static BerliozTokenOperator parse(String exp) throws URITemplateSyntaxException {
    String sexp = strip(exp);
    if (sexp.length() < 2)
      throw new URITemplateSyntaxException(exp, "Cannot produce a valid token operator.");
    char c = sexp.charAt(0);
    Operator operator = BerliozTokenOperator.toOperator(c);
    List<Variable> variables = toVariables(operator == Operator.SUBSTITUTION? sexp : sexp.substring(1));
    return new BerliozTokenOperator(operator, variables);
  }

// private helpers --------------------------------------------------------------------------------

  /**
   * Generate the expression corresponding to the specified operator and variable.
   *
   * @param op The operator.
   * @param variable The variable.
   *
   * @throws NullPointerException If either argument is <code>null</code>
   */
  private static String toExpression(Operator op, Variable variable) {
    return "{"+op.character()+variable.name()+'}';
  }

  /**
   * Generate the expression corresponding to the specified operator, argument and variables.
   *
   * @param op The operator.
   * @param variables The variables.
   */
  private static String toExpression(Operator op, List<Variable> variables) {
    StringBuilder exp = new StringBuilder();
    exp.append('{');
    exp.append(op.character());
    boolean first = true;
    for (Variable v : variables) {
      if (!first) {
        exp.append(',');
      }
      exp.append(v);
      first = false;
    }
    exp.append('}');
    return exp.toString();
  }

}
