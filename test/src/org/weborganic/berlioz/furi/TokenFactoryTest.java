/*
 * This file is part of the URI Template library.
 *
 * For licensing information please see the file license.txt included in the release.
 * A copy of this licence can also be found at
 *   http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.weborganic.berlioz.furi;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.weborganic.berlioz.furi.TokenOperatorDX.Operator;

/**
 * A test class for the <code>TokenFactory</code>.
 *
 * @author Christophe Lauret
 * @version 9 February 2009
 */
public final class TokenFactoryTest {

  /**
   * Test that the <code>NewToken</code> method returns a <code>null</code> token for a
   * <code>null</code> expression.
   */
  @Test
  public void testNewToken_Null() {
    Assert.assertNull(TokenFactory.getInstance().newToken(null));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>null</code> token for an empty
   * string.
   */
  @Test
  public void testNewToken_EmptyString() {
    Assert.assertNull(TokenFactory.getInstance().newToken(""));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenLiteral</code> token
   * corresponding to the specified text.
   */
  @Test
  public void testNewToken_Literal() {
    Assert.assertEquals(new TokenLiteral("x"), TokenFactory.getInstance().newToken("x"));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenVariable</code> token
   * corresponding to the specified variable definition.
   */
  @Test
  public void testNewToken_Variable() {
    Variable x = new Variable("x");
    Assert.assertEquals(new TokenVariable(x), TokenFactory.getInstance().newToken("{x}"));
    Variable y = new Variable("y", "z");
    Assert.assertEquals(new TokenVariable(y), TokenFactory.getInstance().newToken("{y=z}"));
    Variable q = new Variable("q", "p", new VariableType("t"));
    Assert.assertEquals(new TokenVariable(q), TokenFactory.getInstance().newToken("{t:q=p}"));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenOperator</code> token
   * corresponding to the specified operator definition.
   */
  @Test
  public void testNewToken_Operator() {
    List<Variable> vars = new ArrayList<Variable>();
    Variable y = new Variable("y");
    vars.add(y);
    // make sure that all defined operators are supported
    for (Operator o : Operator.values()) {
      TokenFactory factory = TokenFactory.getInstance();
      TokenOperator t = new TokenOperatorDX(o, vars);
      Assert.assertEquals(t, factory.newToken("{-" + o.name().toLowerCase() + "|x|y}"));
    }
  }

}
