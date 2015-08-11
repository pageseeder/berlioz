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

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.furi.TokenOperatorDX.Operator;

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
      // FIXME
//      Assert.assertEquals(t, factory.newToken("{-" + o.name().toLowerCase() + "|x|y}"));
    }
  }

}
