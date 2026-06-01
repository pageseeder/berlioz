package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StringsTest {

  @Test
  void testSubstringAfter(){
    Assertions.assertEquals("", Strings.substringAfter("", ","));
    Assertions.assertEquals(" ", Strings.substringAfter(" ", ","));
    Assertions.assertEquals("", Strings.substringAfter("first second", null));
    Assertions.assertEquals("second", Strings.substringAfter("first,second", ","));
    Assertions.assertEquals("second", Strings.substringAfter("first second", " "));
    Assertions.assertEquals("first second", Strings.substringAfter("first second", ","));
    Assertions.assertEquals("first", Strings.substringAfter("first", " "));
    Assertions.assertEquals("", Strings.substringAfter("first ", " "));
    Assertions.assertEquals("second third", Strings.substringAfter("first second third", " "));
    Assertions.assertEquals("first second", Strings.substringAfter("first second", ""));
  }

  @Test
  void testSubstringAfterChar(){
    Assertions.assertEquals("", Strings.substringAfter("", ','));
    Assertions.assertEquals(" ", Strings.substringAfter(" ", ','));
    Assertions.assertEquals("second", Strings.substringAfter("first,second", ','));
    Assertions.assertEquals("second", Strings.substringAfter("first second", ' '));
    Assertions.assertEquals("first second", Strings.substringAfter("first second", ','));
    Assertions.assertEquals("first", Strings.substringAfter("first", ' '));
    Assertions.assertEquals("", Strings.substringAfter("first ", ' '));
    Assertions.assertEquals("second third", Strings.substringAfter("first second third", ' '));
  }

  @Test
  void testSubstringBefore(){
    Assertions.assertEquals("", Strings.substringBefore("", ","));
    Assertions.assertEquals(" ", Strings.substringBefore(" ", ","));
    Assertions.assertEquals("", Strings.substringBefore("first second", null));
    Assertions.assertEquals("first second", Strings.substringBefore("first second", ","));
    Assertions.assertEquals("", Strings.substringBefore(",first,second", ","));
    Assertions.assertEquals("first", Strings.substringBefore("first,second", ","));
    Assertions.assertEquals("first", Strings.substringBefore("first second", " "));
    Assertions.assertEquals("first", Strings.substringBefore("first second third", " "));
    Assertions.assertEquals("first", Strings.substringBefore("first", " "));
    Assertions.assertEquals("/simple-admin/api/auth/user.json", Strings.substringBefore("/simple-admin/api/auth/user.json", ""));
  }

  @Test
  void testSubstringBeforeChar(){
    Assertions.assertEquals("", Strings.substringBefore("", ','));
    Assertions.assertEquals(" ", Strings.substringBefore(" ", ','));
    Assertions.assertEquals("first second", Strings.substringBefore("first second", ','));
    Assertions.assertEquals("", Strings.substringBefore(",first,second", ','));
    Assertions.assertEquals("first", Strings.substringBefore("first,second", ','));
    Assertions.assertEquals("first", Strings.substringBefore("first second", ' '));
    Assertions.assertEquals("first", Strings.substringBefore("first second third", ' '));
    Assertions.assertEquals("first", Strings.substringBefore("first", ' '));
  }

  @Test
  void testToKebabCase() {
    Assertions.assertEquals("no-content", Strings.toKebabCase("NoContent", "fallback"));
    Assertions.assertEquals("my-http-client", Strings.toKebabCase("MyHTTPClient", "fallback"));
    Assertions.assertEquals("get-user-id", Strings.toKebabCase("GetUserID", "fallback"));
    Assertions.assertEquals("xml-parser", Strings.toKebabCase("XMLParser", "fallback"));
    Assertions.assertEquals("o-auth2-handler", Strings.toKebabCase("OAuth2Handler", "fallback"));
    Assertions.assertEquals("generator", Strings.toKebabCase("", "generator"));
  }
}
