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

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.pageseeder.berlioz.furi.Variable.Form;

/**
 * A test class for variables.
 *
 * @author Christophe Lauret
 * @version 5 November 2009
 */
public class VariableTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> variable name.
   */
  @Test
  public void testNew_NullName() {
    boolean nullThrown = false;
    try {
      new Variable(null, null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assert.assertTrue(nullThrown);
    }
  }

  /**
   * Test that the constructor throws an IllegalArgumentException for an empty string as a name.
   */
  @Test
  public void testNew_EmptyString() {
    boolean illegalThrown = false;
    try {
      new Variable("", null);
    } catch (IllegalArgumentException ex) {
      illegalThrown = true;
    } finally {
      Assert.assertTrue(illegalThrown);
    }
  }

  /**
   * Test that the constructor sets the name appropriately.
   * The variable should remain untyped.
   */
  @Test
  public void testNew_Default() {
    // default value specified and null
    Variable var = new Variable("name", null);
    Assert.assertEquals("", var.defaultValue());
    Assert.assertEquals(null, var.type());
    // default value unspecified
    var = new Variable("name");
    Assert.assertEquals("", var.defaultValue());
    Assert.assertEquals(null, var.type());
    Assert.assertEquals(Form.STRING, var.form());
  }

  /**
   * Test that the <code>isValidName</code> method work as specified.
   */
  @Test
  public void testIsValidName() {
    // invalid
    Assert.assertFalse(Variable.isValidName(null));
    Assert.assertFalse(Variable.isValidName(""));
    Assert.assertFalse(Variable.isValidName("_"));
    Assert.assertFalse(Variable.isValidName("-"));
    Assert.assertFalse(Variable.isValidName("."));
    // valid
    Assert.assertTrue(Variable.isValidName("a"));
    Assert.assertTrue(Variable.isValidName("abc"));
    Assert.assertTrue(Variable.isValidName("a-"));
    Assert.assertTrue(Variable.isValidName("a_"));
    Assert.assertTrue(Variable.isValidName("a."));
  }

  /**
   * Test the <code>equals</code> method for variables with no default value.
   */
  @Test
  public void testEquals_noDefault() {
    Variable x = new Variable("n");
    Variable y = new Variable("n");
    Variable z = new Variable("m");
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * Test the <code>equals</code> method for variables with a default value.
   */
  @Test
  public void testEquals_default() {
    Variable x = new Variable("n", "x");
    Variable y = new Variable("n", "x");
    Variable z = new Variable("n", "y");
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * Test the <code>parse</code> method for normal situations.
   */
  @Test
  public void testParse_OK() {
    VariableType t = new VariableType("t");
    Assert.assertEquals(new Variable("x"),      Variable.parse("x"));
    Assert.assertEquals(new Variable("x", "y"), Variable.parse("x=y"));
    Assert.assertEquals(new Variable("x", ""),  Variable.parse("x="));
    // typed
    Assert.assertEquals(new Variable("x", null), Variable.parse("t:x"));
    Assert.assertEquals(new Variable("x", null), Variable.parse("t:x="));
    Assert.assertEquals(new Variable("x", "y"),  Variable.parse("t:x=y"));
    Assert.assertEquals(new Variable("x", "y"),  Variable.parse(":x=y"));
    // with different form
    Assert.assertEquals(new Variable("x", null, null, Form.LIST), Variable.parse("@x"));
    Assert.assertEquals(new Variable("x", null, null, Form.MAP ), Variable.parse("%x="));
    Assert.assertEquals(new Variable("x", null, null, Form.LIST), Variable.parse("@t:x"));
    Assert.assertEquals(new Variable("x", null, null, Form.MAP ), Variable.parse("%t:x="));
    Assert.assertEquals(new Variable("x", "y", null, Form.LIST ),  Variable.parse("@t:x=y"));
    Assert.assertEquals(new Variable("x", "y", null, Form.MAP ),  Variable.parse("%:x=y"));
    // type does not affect equality
    Assert.assertEquals(t, Variable.parse("t:x").type());
    Assert.assertEquals(t, Variable.parse("t:x=").type());
    Assert.assertEquals(t, Variable.parse("t:x=y").type());

    Assert.assertEquals(null,  Variable.parse(":x=y").type());
  }

  /**
   * Test the <code>parse</code> method for a <code>null</code> value.
   */
  @Test
  public void testParse_ErrorNull() {
    try {
      Variable.parse(null);
      Assert.fail("No exception was thrown");
    } catch (Exception ex) {
      Assert.assertEquals(NullPointerException.class, ex.getClass());
    }
  }

  /**
   * Test the <code>parse</code> method with syntax error.
   */
  @Test
  public void testParse_ErrorSyntax() {
    try {
      Variable.parse("=y");
      Assert.fail("No exception was thrown");
    } catch (Exception ex) {
      Assert.assertEquals(URITemplateSyntaxException.class, ex.getClass());
    }
  }

  /**
   * Test the <code>value</code> method.
   */
  @Test
  public void testValue() {
    // setup
    Parameters params = new URIParameters();
    params.set("a", new String[] {});
    params.set("b", "");
    params.set("c", "m");
    params.set("d", new String[] { "m", "n" });
    params.set("e", new String[] { "m", "", "n" });
    // test
    Assert.assertEquals("", new Variable("a").value(params));
    Assert.assertEquals("x", new Variable("a", "x").value(params));
    Assert.assertEquals("", new Variable("b").value(params));
    Assert.assertEquals("", new Variable("b", "x").value(params));
    Assert.assertEquals("m", new Variable("c").value(params));
    Assert.assertEquals("m", new Variable("c", "x").value(params));
    Assert.assertEquals("m", new Variable("d").value(params));
    Assert.assertEquals("m", new Variable("d", "x").value(params));
    Assert.assertEquals("m", new Variable("e").value(params));
    Assert.assertEquals("m", new Variable("e", "x").value(params));
  }

  /**
   * Test the <code>values</code> method.
   */
  @Test
  public void testValues() {
    // setup
    Parameters params = new URIParameters();
    params.set("a", new String[] {});
    params.set("b", "");
    params.set("c", "m");
    params.set("d", new String[] { "m", "n" });
    params.set("e", new String[] { "m", "", "n" });
    // test
    assertArrayEquals(new String[] {}, new Variable("a").values(params));
    assertArrayEquals(new String[] { "x" }, new Variable("a", "x").values(params));
    assertArrayEquals(new String[] {}, new Variable("b").values(params));
    assertArrayEquals(new String[] { "x" }, new Variable("b", "x").values(params));
    assertArrayEquals(new String[] { "m" }, new Variable("c").values(params));
    assertArrayEquals(new String[] { "m" }, new Variable("c", "x").values(params));
    assertArrayEquals(new String[] { "m", "n" }, new Variable("d").values(params));
    assertArrayEquals(new String[] { "m", "n" }, new Variable("d", "x").values(params));
    assertArrayEquals(new String[] { "m", "", "n" }, new Variable("e").values(params));
    assertArrayEquals(new String[] { "m", "", "n" }, new Variable("e", "x").values(params));
  }

  // private helpers
  // --------------------------------------------------------------------------

  /**
   * Asserts that the arrays are equal by comparing the string value of their components.
   *
   * @param exp The expected string array.
   * @param act The actual string array.
   */
  private void assertArrayEquals(String[] exp, String[] act) {
    Assert.assertEquals(Arrays.deepToString(exp), Arrays.deepToString(act));
  }
}
