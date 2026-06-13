package org.pageseeder.berlioz.content;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ParameterTemplateTest {

  @Test
  void testParameter_Null() {
    Assertions.assertThrows(NullPointerException.class, () -> ParameterTemplate.parameter(null, "x"));
  }

  @SuppressWarnings("java:S5976")
  @Test
  void testParameter_Nofallback1() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", null);
    Assertions.assertEquals("{test}", pt.toString());
    Assertions.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testParameter_Nofallback2() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "");
    Assertions.assertEquals("{test}", pt.toString());
    Assertions.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testParameter_Fallback() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "fallback");
    Assertions.assertEquals("{test=fallback}", pt.toString());
    Assertions.assertEquals("fallback", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testValue() {
    ParameterTemplate pt = ParameterTemplate.value("test");
    Assertions.assertEquals("test", pt.toString());
    Assertions.assertEquals("test", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("test", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testValue_Null() {
    Assertions.assertThrows(NullPointerException.class, () -> ParameterTemplate.value(null));
  }

  @Test
  void testParse_Literal() {
    ParameterTemplate pt = ParameterTemplate.parse("test");
    Assertions.assertEquals("test", pt.toString());
    Assertions.assertEquals("test", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("test", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testParse_Variable() {
    ParameterTemplate pt = ParameterTemplate.parse("{test}");
    Assertions.assertEquals("{test}", pt.toString());
    Assertions.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("abc", pt.toString(Collections.singletonMap("test", "abc")));
  }

  @Test
  void testParse_EmptyVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{}");
    Assertions.assertEquals("{}", pt.toString());
    Assertions.assertEquals("{}", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("{}", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("{}", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  void testParse_OpenVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{");
    Assertions.assertEquals("{", pt.toString());
    Assertions.assertEquals("{", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("{", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("{", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  void testParse_ClosedVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("}");
    Assertions.assertEquals("}", pt.toString());
    Assertions.assertEquals("}", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("}", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("}", pt.toString(Collections.singletonMap("", "x")));
  }

  @Test
  void testParse_Mix1() {
    ParameterTemplate pt = ParameterTemplate.parse("{a}{b}");
    Assertions.assertEquals("{a}{b}", pt.toString());
    Assertions.assertEquals("", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("x", pt.toString(Collections.singletonMap("a", "x")));
    Assertions.assertEquals("x", pt.toString(Collections.singletonMap("b", "x")));
  }

  @Test
  void testParse_Mix2() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{b}|");
    Assertions.assertEquals("|{a}-{b}|", pt.toString());
    Assertions.assertEquals("|-|", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("|-|", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("|x-|", pt.toString(Collections.singletonMap("a", "x")));
    Assertions.assertEquals("|-x|", pt.toString(Collections.singletonMap("b", "x")));
  }

  @Test
  void testParse_Mix3() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{a}|");
    Assertions.assertEquals("|{a}-{a}|", pt.toString());
    Assertions.assertEquals("|-|", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("|-|", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("|x-x|", pt.toString(Collections.singletonMap("a", "x")));
  }

  @Test
  void testParse_Mix4() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a=x}-{b=y}|");
    Assertions.assertEquals("|{a=x}-{b=y}|", pt.toString());
    Assertions.assertEquals("|x-y|", pt.toString(Collections.<String,String>emptyMap()));
    Assertions.assertEquals("|x-y|", pt.toString(Collections.singletonMap("test", "abc")));
    Assertions.assertEquals("|m-y|", pt.toString(Collections.singletonMap("a", "m")));
    Assertions.assertEquals("|x-n|", pt.toString(Collections.singletonMap("b", "n")));
  }

}
