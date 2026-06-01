package org.pageseeder.berlioz.content;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ParameterTemplateTest {

  @Test
  void testParameter_Null() {
    try {
      ParameterTemplate.parameter(null, "x");
      Assertions.fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @SuppressWarnings("java:S5976")
  @Test
  void testParameter_Nofallback1() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", null);
    Assertions.assertEquals(pt.toString(), "{test}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "abc");
  }

  @Test
  void testParameter_Nofallback2() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "");
    Assertions.assertEquals(pt.toString(), "{test}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "abc");
  }

  @Test
  void testParameter_Fallback() {
    ParameterTemplate pt = ParameterTemplate.parameter("test", "fallback");
    Assertions.assertEquals(pt.toString(), "{test=fallback}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "fallback");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "abc");
  }

  @Test
  void testValue() {
    ParameterTemplate pt = ParameterTemplate.value("test");
    Assertions.assertEquals(pt.toString(), "test");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "test");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "test");
  }

  @Test
  void testValue_Null() {
    try {
      ParameterTemplate.value(null);
      Assertions.fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  void testParse_Literal() {
    ParameterTemplate pt = ParameterTemplate.parse("test");
    Assertions.assertEquals(pt.toString(), "test");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "test");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "test");
  }

  @Test
  void testParse_Variable() {
    ParameterTemplate pt = ParameterTemplate.parse("{test}");
    Assertions.assertEquals(pt.toString(), "{test}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "abc");
  }

  @Test
  void testParse_EmptyVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{}");
    Assertions.assertEquals(pt.toString(), "{}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "{}");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "{}");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("", "x")), "{}");
  }

  @Test
  void testParse_OpenVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("{");
    Assertions.assertEquals(pt.toString(), "{");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "{");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "{");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("", "x")), "{");
  }

  @Test
  void testParse_ClosedVariable() {
    ParameterTemplate pt = ParameterTemplate.parse("}");
    Assertions.assertEquals(pt.toString(), "}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "}");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "}");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("", "x")), "}");
  }

  @Test
  void testParse_Mix1() {
    ParameterTemplate pt = ParameterTemplate.parse("{a}{b}");
    Assertions.assertEquals(pt.toString(), "{a}{b}");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("a", "x")), "x");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("b", "x")), "x");
  }

  @Test
  void testParse_Mix2() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{b}|");
    Assertions.assertEquals(pt.toString(), "|{a}-{b}|");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "|-|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "|-|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("a", "x")), "|x-|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("b", "x")), "|-x|");
  }

  @Test
  void testParse_Mix3() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a}-{a}|");
    Assertions.assertEquals(pt.toString(), "|{a}-{a}|");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "|-|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "|-|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("a", "x")), "|x-x|");
  }

  @Test
  void testParse_Mix4() {
    ParameterTemplate pt = ParameterTemplate.parse("|{a=x}-{b=y}|");
    Assertions.assertEquals(pt.toString(), "|{a=x}-{b=y}|");
    Assertions.assertEquals(pt.toString(Collections.<String,String>emptyMap()), "|x-y|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("test", "abc")), "|x-y|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("a", "m")), "|m-y|");
    Assertions.assertEquals(pt.toString(Collections.singletonMap("b", "n")), "|x-n|");
  }

}
