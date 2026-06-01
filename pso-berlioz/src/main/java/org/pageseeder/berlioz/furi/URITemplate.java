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
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;


/**
 * A URI Template for constructing URIs following the same structure.
 *
 * <p>Instances of this class implement the URI templates as defined by the URI Template (Draft 3) by
 * Joe Gregorio.
 *
 * <p>A URI Template follows the URI syntax and can be expanded given a set of variable values.
 *
 * @see <a
 *      href="http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html">URI
 *      Template (draft 3)</a>
 *
 * @author Christophe Lauret
 *
 * @version 0.13.0
 * @since 0.9.32
 */
public class URITemplate implements Expandable {

  /**
   * The regular expression pattern to identify template expansions within the template.
   */
  private static final Pattern EXPANSION_PATTERN = Pattern.compile("\\{[^}]*}");

  /**
   * The string representation of the URL template.
   */
  private final String template;

  /**
   * The list of tokens corresponding to this URL template.
   */
  private final List<Token> tokens;

  /**
   * Creates a new URI Template instance from an existing instance
   *
   * @param original The original template URI template syntax.
   *
   * @throws NullPointerException If the specified template is <code>null</code>.
   */
  URITemplate(URITemplate original) {
    this.template = original.template;
    this.tokens = original.tokens;
  }

  /**
   * Creates a new URI Template instance.
   *
   * @param template A String following the URI template syntax.
   *
   * @throws NullPointerException If the specified template is <code>null</code>.
   * @throws URITemplateSyntaxException If the string provided does not follow the proper syntax.
   */
  public URITemplate(String template) {
    this.template = Objects.requireNonNull(template, "Cannot create a URI template with a null template");
    this.tokens = digest(template);
  }

  /**
   * Expands the template to produce a URI as defined by the URI Template specifications.
   *
   * @param parameters The list of variables and their values for substitution.
   */
  @Override
  public final String expand(Parameters parameters) {
    StringBuilder uri = new StringBuilder();
    for (Token t : this.tokens) {
      uri.append(t.expand(parameters));
    }
    return uri.toString();
  }

  /**
   * Method provided for convenience.
   *
   * <p>It returns the same as:
   * <pre>
   * return new URITemplate(template).expand(variables);
   * </pre>
   *
   * @param template The URI template.
   * @param parameters The parameter values to use for substitution.
   *
   * @return The corresponding expanded URI.
   */
  public static String expand(String template, Parameters parameters) {
    return new URITemplate(template).expand(parameters);
  }

  /**
   * Returns the list of tokens corresponding to the specified URI template.
   *
   * @param template The URI template to digest.
   *
   * @return The corresponding list of URL tokens.
   *
   * @throws URITemplateSyntaxException If the string cannot be parsed.
   */
  public static List<Token> digest(String template) throws URITemplateSyntaxException {
    List<Token> tokens = new ArrayList<>();
    Matcher m = EXPANSION_PATTERN.matcher(template);
    int start = 0;
    while (m.find()) {
      if (m.start() > start) {
        tokens.add(new TokenLiteral(template.substring(start, m.start())));
      }
      Token t = TokenFactory.newToken(m.group());
      if (t != TokenLiteral.EMPTY) {
        tokens.add(t);
      }
      start = m.end();
    }
    if (start < template.length()) {
      String text = template.substring(start);
      if (text.endsWith("*")) {
        tokens.add(new TokenLiteral(text.substring(0, text.length() - 1)));
        tokens.add(TokenFactory.newToken("*"));
      } else {
        tokens.add(new TokenLiteral(text));
      }
    }
    return tokens;
  }

  /**
   * Returns the underlying list of tokens.
   *
   * <p>
   * Note: this method exposes the underlying structure of this class and should remain protected.
   *
   * @return The underlying list of tokens.
   */
  protected final List<Token> tokens() {
    return this.tokens;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (o == this)
      return true;
    if ((o == null) || (o.getClass() != this.getClass()))
      return false;
    URITemplate t = (URITemplate) o;
    return (Objects.equals(this.template, t.template));
  }

  @Override
  public int hashCode() {
    return 127 * this.template.hashCode() + this.template.hashCode();
  }

  @Override
  public String toString() {
    return this.template;
  }
}
