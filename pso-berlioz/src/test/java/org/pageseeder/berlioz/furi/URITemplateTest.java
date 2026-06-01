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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for the <code>URITemplate</code> class.
 * <p>
 * Some test cases are built directly from examples in the specifications
 *
 * @see <a
 *      href="http://bitworking.org/projects/URI-Templates/spec/draft-gregorio-uritemplate-03.html#examples">URI
 *      Template (Draft 3) - examples</a>
 *
 * @author Christophe Lauret
 * @version 21 October 2009
 */
class URITemplateTest {

  /**
   * Parameters examples from the specifications.
   */
  private final Parameters vars = new URIParameters();

  @BeforeEach
  void setUp() {
    // set up the parameters from the specifications.
    this.vars.set("foo", new String[] { "\u03d3" });
    this.vars.set("bar", new String[] { "fred" });
    this.vars.set("baz", new String[] { "10,20,30" });
    this.vars.set("qux", new String[] { "10", "20", "30" });
    this.vars.set("corge", new String[] {});
    this.vars.set("grault", new String[] { "" });
    this.vars.set("garply", new String[] { "a/b/c" });
    this.vars.set("waldo", new String[] { "ben & jerrys" });
    this.vars.set("fred", new String[] { "fred", "", "wilma" });
    this.vars.set("plugh", new String[] { "\u017F\u0307", "\u0073\u0307" });
    this.vars.set("1-a_b.c", new String[] { "200" });
  }

  /**
   * Test that a null pointer exception is thrown by the constructor.
   */
  @Test
  void testNew_Null() {
    Assertions.assertThrows(NullPointerException.class, () -> new URITemplate((String)null));
  }

  /**
   * Test that it can construct a template from an empty string.
   */
  @Test
  void testNew_EmptyString() {
    Assertions.assertDoesNotThrow(() -> new URITemplate(""));
  }

  /**
   * Test that the <code>digest</code> method returns an empty token list for an empty string.
   */
  @Test
  void testDigest_EmptyString() {
    List<Token> tokens = new ArrayList<>();
    Assertions.assertEquals(tokens, URITemplate.digest(""));
  }

  /**
   * Test that the <code>digest</code> method returns one literal token list for simple text.
   */
  @Test
  void testDigest_OneTokenLiteral() {
    List<Token> tokens = new ArrayList<Token>();
    tokens.add(new TokenLiteral("http://acme.com/"));
    Assertions.assertEquals(tokens, URITemplate.digest("http://acme.com/"));
  }

  /**
   * Test that the <code>digest</code> method returns a variable token for a variable expression.
   */
  @Test
  void testDigest_OneTokenVariable() {
    List<Token> tokens = new ArrayList<>();
    tokens.add(new TokenVariable("x"));
    Assertions.assertEquals(tokens, URITemplate.digest("{x}"));
  }

  /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text followed by
   * one variable.
   */
  @Test
  void testDigest_TwoToken() {
    List<Token> tokens = new ArrayList<>();
    tokens.add(new TokenLiteral("http://acme.com/"));
    tokens.add(new TokenVariable("x"));
    Assertions.assertEquals(tokens, URITemplate.digest("http://acme.com/{x}"));
  }

  /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text with one
   * variable in the middle.
   */
  @Test
  void testDigest_OneTokenInTheMiddle() {
    List<Token> tokens = new ArrayList<Token>();
    tokens.add(new TokenLiteral("http://acme.com/"));
    tokens.add(new TokenVariable("x"));
    tokens.add(new TokenLiteral("/text"));
    Assertions.assertEquals(tokens, URITemplate.digest("http://acme.com/{x}/text"));
  }

  /**
   * Test that the <code>digest</code> method returns the appropriate tokens for text including two
   * variables.
   */
  @Test
  void testDigest_TwoTokens() {
    List<Token> tokens = new ArrayList<Token>();
    tokens.add(new TokenLiteral("http://acme.com/"));
    tokens.add(new TokenVariable("x"));
    tokens.add(new TokenLiteral("/"));
    tokens.add(new TokenVariable("y"));
    Assertions.assertEquals(tokens, URITemplate.digest("http://acme.com/{x}/{y}"));
  }

  /**
   * Test that the <code>digest</code> method returns the appropriate tokens for two consecutive
   * variables.
   */
  @Test
  void testDigest_TwoConsecutiveTokens() {
    List<Token> tokens = new ArrayList<Token>();
    tokens.add(new TokenLiteral("http://acme.com/"));
    tokens.add(new TokenVariable("x"));
    tokens.add(new TokenVariable("y"));
    Assertions.assertEquals(tokens, URITemplate.digest("http://acme.com/{x}{y}"));
  }

  /**
   * Test the <code>equals</code> method.
   */
  @Test
  void testEquals_Contract() {
    URITemplate x = new URITemplate("http://ps.com/{X}");
    URITemplate y = new URITemplate("http://ps.com/{X}");
    URITemplate z = new URITemplate("http://ps.com/{Y}");
    TestUtils.assertEqualsContract(x, y, z);
  }

  /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   *
   * <pre>{@code
   *   http://example.org/?q={bar}
   *   http://example.org/?q=fred
   * }</pre>
   */
  @Test
  void testExpand_Spec1() {
    assertExpand("http://example.org/?q={bar}", this.vars, "http://example.org/?q=fred");
  }

  /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   *
   * <pre>{@code
   * http://example.org/{bar}{bar}/{garply}
   * http://example.org/fredfred/a%2Fb%2Fc
   * }</pre>
   */
  @Test
  void testExpand_Spec6() {
    assertExpand("http://example.org/{bar}{bar}/{garply}", this.vars,
        "http://example.org/fredfred/a%2Fb%2Fc");
  }

  /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   *
   * <pre>{@code
   * ../{waldo}/
   * ../ben%20%26%20jerrys/
   * }</pre>
   */
  @Test
  void testExpand_Spec9() {
    assertExpand("../{waldo}/", this.vars, "../ben%20%26%20jerrys/");
  }

  /**
   * Test the <code>expand</code> method using test cases in the specifications.
   * <p>
   * This method tests:
   *
   * <pre>{@code
   * :{1-a_b.c}:
   * :200:
   * }</pre>
   */
  @Test
  void testExpand_Spec11() {
    assertExpand(":{1-a_b.c}:", this.vars, ":200:");
  }

  /**
   * Test the <code>expand</code> method when a type is in use.
   * <p>
   * This method tests:
   *
   * <pre>{@code
   * /type/{x:bar}
   * /type/fred
   * }</pre>
   */
  @Test
  void testExpand_Type() {
    assertExpand("/type/{x:bar}", this.vars, "/type/fred");
  }

  // private helpers
  // --------------------------------------------------------------------------

  /**
   * Expand the specified template with the given parameters and checks that it matches the
   * specified URL.
   *
   * @param template The template to expand.
   * @param parameters The parameters to use.
   * @param url The expected URL.
   */
  private void assertExpand(String template, Parameters parameters, String url) {
    Assertions.assertEquals(url, URITemplate.expand(template, parameters));
  }
}
