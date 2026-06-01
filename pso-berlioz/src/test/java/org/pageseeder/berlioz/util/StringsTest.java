package org.pageseeder.berlioz.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringsTest {

  @Test
  public void testSubstringAfter(){
    Assertions.assertEquals(Strings.substringAfter("", ","), "");
    Assertions.assertEquals(Strings.substringAfter(" ", ","), " ");
    Assertions.assertEquals(Strings.substringAfter("first second", null), "");
    Assertions.assertEquals(Strings.substringAfter("first,second", ","), "second");
    Assertions.assertEquals(Strings.substringAfter("first second", " "), "second");
    Assertions.assertEquals(Strings.substringAfter("first second", ","), "first second");
    Assertions.assertEquals(Strings.substringAfter("first", " "), "first");
    Assertions.assertEquals(Strings.substringAfter("first ", " "), "");
    Assertions.assertEquals(Strings.substringAfter("first second third", " "), "second third");
    Assertions.assertEquals(Strings.substringAfter("first second", ""), "first second");
  }

  @Test
  public void testSubstringAfterChar(){
    Assertions.assertEquals(Strings.substringAfter("", ','), "");
    Assertions.assertEquals(Strings.substringAfter(" ", ','), " ");
    Assertions.assertEquals(Strings.substringAfter("first,second", ','), "second");
    Assertions.assertEquals(Strings.substringAfter("first second", ' '), "second");
    Assertions.assertEquals(Strings.substringAfter("first second", ','), "first second");
    Assertions.assertEquals(Strings.substringAfter("first", ' '), "first");
    Assertions.assertEquals(Strings.substringAfter("first ", ' '), "");
    Assertions.assertEquals(Strings.substringAfter("first second third", ' '), "second third");
  }

  @Test
  public void testSubstringBefore(){
    Assertions.assertEquals(Strings.substringBefore("", ","), "");
    Assertions.assertEquals(Strings.substringBefore(" ", ","), " ");
    Assertions.assertEquals(Strings.substringBefore("first second", null), "");
    Assertions.assertEquals(Strings.substringBefore("first second", ","), "first second");
    Assertions.assertEquals(Strings.substringBefore(",first,second", ","), "");
    Assertions.assertEquals(Strings.substringBefore("first,second", ","), "first");
    Assertions.assertEquals(Strings.substringBefore("first second", " "), "first");
    Assertions.assertEquals(Strings.substringBefore("first second third", " "), "first");
    Assertions.assertEquals(Strings.substringBefore("first", " "), "first");
    Assertions.assertEquals(Strings.substringBefore("/simple-admin/api/auth/user.json", ""), "/simple-admin/api/auth/user.json");
  }

  @Test
  public void testSubstringBeforeChar(){
    Assertions.assertEquals(Strings.substringBefore("", ','), "");
    Assertions.assertEquals(Strings.substringBefore(" ", ','), " ");
    Assertions.assertEquals(Strings.substringBefore("first second", ','), "first second");
    Assertions.assertEquals(Strings.substringBefore(",first,second", ','), "");
    Assertions.assertEquals(Strings.substringBefore("first,second", ','), "first");
    Assertions.assertEquals(Strings.substringBefore("first second", ' '), "first");
    Assertions.assertEquals(Strings.substringBefore("first second third", ' '), "first");
    Assertions.assertEquals(Strings.substringBefore("first", ' '), "first");
  }

  @Test
  public void testToKebabCase() {
    Assertions.assertEquals(Strings.toKebabCase("NoContent", "fallback"), "no-content");
    Assertions.assertEquals(Strings.toKebabCase("MyHTTPClient", "fallback"), "my-http-client");
    Assertions.assertEquals(Strings.toKebabCase("GetUserID", "fallback"), "get-user-id");
    Assertions.assertEquals(Strings.toKebabCase("XMLParser", "fallback"), "xml-parser");
    Assertions.assertEquals(Strings.toKebabCase("OAuth2Handler", "fallback"), "o-auth2-handler");
    Assertions.assertEquals(Strings.toKebabCase("", "generator"), "generator");
  }
}
