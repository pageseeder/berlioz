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
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.furi.BerliozTokenOperator.Operator;

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
    try {
      TokenFactory.newToken(null);
      Assertions.fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>null</code> token for an empty
   * string.
   */
  @Test
  public void testNewToken_EmptyString() {
    Assertions.assertEquals(TokenLiteral.EMPTY, TokenFactory.newToken(""));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenLiteral</code> token
   * corresponding to the specified text.
   */
  @Test
  public void testNewToken_Literal() {
    Assertions.assertEquals(new TokenLiteral("x"), TokenFactory.newToken("x"));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenVariable</code> token
   * corresponding to the specified variable definition.
   */
  @Test
  public void testNewToken_Variable() {
    Variable x = new Variable("x");
    Assertions.assertEquals(new TokenVariable(x), TokenFactory.newToken("{x}"));
    Variable y = new Variable("y", "z");
    Assertions.assertEquals(new TokenVariable(y), TokenFactory.newToken("{y=z}"));
    Variable q = new Variable("q", "p", new VariableType("t"));
    Assertions.assertEquals(new TokenVariable(q), TokenFactory.newToken("{t:q=p}"));
  }

  /**
   * Test that the <code>NewToken</code> method returns a <code>TokenOperator</code> token
   * corresponding to the specified operator definition.
   */
  @Test
  public void testNewToken_Operator() {
    List<Variable> vars = new ArrayList<>();
    Variable y = new Variable("y");
    vars.add(y);
    for (Operator o : Operator.values()) {
      TokenOperator t = new BerliozTokenOperator(o, vars);
      Assertions.assertNotNull(t);
    }
  }

}
