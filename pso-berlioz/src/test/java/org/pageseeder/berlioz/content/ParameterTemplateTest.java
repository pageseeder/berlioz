package org.pageseeder.berlioz.content;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

public final class ParameterTemplateTest {

  @Test(expected = NullPointerException.class)
  public void testParameter_Null() {
    ParameterTemplate.parameter(null, "x");
  }

  @Test
  public void testParameter_Nofallback1() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", null);
    Assert.assertEquals("{test}", pt.toString());
    Assert.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  public void testParameter_Nofallback2() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "");
    Assert.assertEquals("{test}", pt.toString());
    Assert.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  public void testParameter_Fallback() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "fallback");
    Assert.assertEquals("{test=fallback}", pt.toString());
    Assert.assertEquals("fallback", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  public void testValue() {
    ParameterTemplate pt = ParameterTemplate.value("test");
    Assert.assertEquals("test", pt.toString());
    Assert.assertEquals("test", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("test", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test(expected = NullPointerException.class)
  public void testValue_Null() {
    ParameterTemplate.value(null);
  }

  @Test
  public void testParse_Literal() {
    ParameterTemplate pt = ParameterTemplate.parse("test");
    Assert.assertEquals("test", pt.toString());
    Assert.assertEquals("test", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("test", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  public void testParse_Variable() {
    ParameterTemplate pt = ParameterTemplate.parse("{test}");
    Assert.assertEquals("{test}", pt.toString());
    Assert.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  public void testParse_EmptyVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{}");
    Assert.assertEquals("{}", pt.toString());
    Assert.assertEquals("{}", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("{}", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("{}", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  public void testParse_OpenVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{");
    Assert.assertEquals("{", pt.toString());
    Assert.assertEquals("{", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("{", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("{", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  public void testParse_ClosedVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("}");
    Assert.assertEquals("}", pt.toString());
    Assert.assertEquals("}", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("}", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("}", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  public void testParse_Mix1() {
    ParameterTemplate pt = ParameterTemplate.parse("{a}{b}");
    Assert.assertEquals("{a}{b}", pt.toString());
    Assert.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("x", pt.toString(Collections.singletonMap("a", "x")));
    Assert.assertEquals("x", pt.toString(Collections.singletonMap("b", "x")));
  }

  @Test
  public void testParse_Mix2() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{b}|");
    Assert.assertEquals("|{a}-{b}|", pt.toString());
    Assert.assertEquals("|-|", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("|-|", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("|x-|", pt.toString(Collections.singletonMap("a", "x")));
    Assert.assertEquals("|-x|", pt.toString(Collections.singletonMap("b", "x")));
  }

  @Test
  public void testParse_Mix3() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{a}|");
    Assert.assertEquals("|{a}-{a}|", pt.toString());
    Assert.assertEquals("|-|", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("|-|", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("|x-x|", pt.toString(Collections.singletonMap("a", "x")));
  }

  @Test
  public void testParse_Mix4() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a=x}-{b=y}|");
    Assert.assertEquals("|{a=x}-{b=y}|", pt.toString());
    Assert.assertEquals("|x-y|", pt.toString(Collections.<String,String>emptyMap()));
    Assert.assertEquals("|x-y|", pt.toString(Collections.singletonMap("test", "abc")));
    Assert.assertEquals("|m-y|", pt.toString(Collections.singletonMap("a", "m")));
    Assert.assertEquals("|x-n|", pt.toString(Collections.singletonMap("b", "n")));
  }

}
