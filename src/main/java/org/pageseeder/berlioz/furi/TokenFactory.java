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

import org.pageseeder.berlioz.furi.Variable.Reserved;

/**
 * A factory for URI tokens.
 *
 * <p>Tokens can be instantiated from an expression which is specific to each token.
 *
 * @see TokenLiteral
 * @see TokenVariable
 *
 * @author Christophe Lauret
 *
 * @version Berlioz 0.11.2
 * @since Berlioz 0.9.32
 */
public final class TokenFactory {

  /**
   * A syntax to use for creating tokens.
   *
   * @author Christophe Lauret
   * @version 6 November 2009
   */
  public enum Syntax {

    /**
     * Use a syntax based on the draft as of 29 October 2009.
     */
    DRAFTX {

      @Override
      protected Token newExpansion(String exp) {
        // possibly Roy Fielding's operators
        if (!Character.isLetter(exp.charAt(0)) && !Character.isDigit(exp.charAt(0))) return BerliozTokenOperator.parse(exp);
        // maybe a collection
        else if (exp.indexOf(',') >= 0) return BerliozTokenOperator.parse(exp);
        // assume a variable
        else return new TokenVariable(Variable.parse(exp));
      }

    };

    /**
     * Generates a template expansion token corresponding to the specified expression.
     *
     * @param exp The expression within the curly brackets {}.
     *
     * @return The corresponding token instance.
     *
     * @throws URITemplateSyntaxException If the expression could not be parsed as a valid token.
     */
    protected abstract Token newExpansion(String exp);

  }

  /**
   * Factories for reuse.
   */
  private static final TokenFactory FACTORY = new TokenFactory(Syntax.DRAFTX);

  /**
   * The URI template syntax to use for generating tokens.
   */
  private final Syntax _syntax;

  /**
   * Prevents creation of instances.
   *
   * @param syntax The URI template syntax to use for generating tokens.
   */
  private TokenFactory(Syntax syntax) {
    this._syntax = syntax;
  }

  /**
   * Generates the token corresponding to the specified expression.
   *
   * @param exp The expression.
   *
   * @return The corresponding token instance.
   *
   * @throws URITemplateSyntaxException If the expression could not be parsed as a valid token.
   * @throws NullPointerException If the expression is <code>null</code>.
   */
  public Token newToken(String exp) {
    // no expression: no token!
    if (exp.length() == 0)
      return TokenLiteral.EMPTY;
    // intercept the wild card
    if ("*".equals(exp))
      return newWildcard();
    // too short to be anything but a literal
    int len = exp.length();
    if (len < 2)
      return new TokenLiteral(exp);
    // a template expansion token
    if (exp.charAt(0) == '{' && exp.charAt(len - 1) == '}') // defer to the underlying syntax
    return this._syntax.newExpansion(exp.substring(1, len - 1));
    // a literal text token
    return new TokenLiteral(exp);
  }

  /**
   * Creates a new 'wildcard' token for legacy purposes.
   *
   * <p>This is used for conventional URI patterns which have been implemented using "*".
   *
   * @return A new 'wildcard' token.
   */
  private static Token newWildcard() {
    return new BerliozTokenOperator(BerliozTokenOperator.Operator.URI_INSERT, new Variable(Reserved.WILDCARD));
  }

  /**
   * Returns a token factory instance using the default syntax.
   *
   * @return a token factory instance using the default syntax.
   */
  public static TokenFactory getInstance() {
    return FACTORY;
  }

  /**
   * Returns a token factory instance.
   *
   * @param syntax The syntax to use for the token factory.
   *
   * @return a token factory instance.
   */
  public static TokenFactory getInstance(Syntax syntax) {
    return new TokenFactory(syntax);
  }

}
