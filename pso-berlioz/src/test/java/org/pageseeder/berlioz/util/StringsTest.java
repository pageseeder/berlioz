package org.pageseeder.berlioz.util;

import org.junit.Assert;
import org.junit.Test;

public class StringsTest {

  @Test
  public void testSubstringAfter(){
    Assert.assertEquals("", Strings.substringAfter("", ","));
    Assert.assertEquals(" ", Strings.substringAfter(" ", ","));
    Assert.assertEquals("", Strings.substringAfter("first second", null));
    Assert.assertEquals("second", Strings.substringAfter("first,second", ","));
    Assert.assertEquals("second", Strings.substringAfter("first second", " "));
    Assert.assertEquals("first second", Strings.substringAfter("first second", ","));
    Assert.assertEquals("first", Strings.substringAfter("first", " "));
    Assert.assertEquals("", Strings.substringAfter("first ", " "));
    Assert.assertEquals("second third", Strings.substringAfter("first second third", " "));
    Assert.assertEquals("first second", Strings.substringAfter("first second", ""));
  }

  @Test
  public void testSubstringAfterChar(){
    Assert.assertEquals("", Strings.substringAfter("", ','));
    Assert.assertEquals(" ", Strings.substringAfter(" ", ','));
    Assert.assertEquals("second", Strings.substringAfter("first,second", ','));
    Assert.assertEquals("second", Strings.substringAfter("first second", ' '));
    Assert.assertEquals("first second", Strings.substringAfter("first second", ','));
    Assert.assertEquals("first", Strings.substringAfter("first", ' '));
    Assert.assertEquals("", Strings.substringAfter("first ", ' '));
    Assert.assertEquals("second third", Strings.substringAfter("first second third", ' '));
  }

  @Test
  public void testSubstringBefore(){
    Assert.assertEquals("", Strings.substringBefore("", ","));
    Assert.assertEquals(" ", Strings.substringBefore(" ", ","));
    Assert.assertEquals("", Strings.substringBefore("first second", null));
    Assert.assertEquals("first second", Strings.substringBefore("first second", ","));
    Assert.assertEquals("", Strings.substringBefore(",first,second", ","));
    Assert.assertEquals("first", Strings.substringBefore("first,second", ","));
    Assert.assertEquals("first", Strings.substringBefore("first second", " "));
    Assert.assertEquals("first", Strings.substringBefore("first second third", " "));
    Assert.assertEquals("first", Strings.substringBefore("first", " "));
    Assert.assertEquals("/simple-admin/api/auth/user.json", Strings.substringBefore("/simple-admin/api/auth/user.json", ""));
  }

  @Test
  public void testSubstringBeforeChar(){
    Assert.assertEquals("", Strings.substringBefore("", ','));
    Assert.assertEquals(" ", Strings.substringBefore(" ", ','));
    Assert.assertEquals("first second", Strings.substringBefore("first second", ','));
    Assert.assertEquals("", Strings.substringBefore(",first,second", ','));
    Assert.assertEquals("first", Strings.substringBefore("first,second", ','));
    Assert.assertEquals("first", Strings.substringBefore("first second", ' '));
    Assert.assertEquals("first", Strings.substringBefore("first second third", ' '));
    Assert.assertEquals("first", Strings.substringBefore("first", ' '));
  }

  @Test
  public void testToKebabCase() {
    Assert.assertEquals("no-content", Strings.toKebabCase("NoContent", "fallback"));
    Assert.assertEquals("my-http-client", Strings.toKebabCase("MyHTTPClient", "fallback"));
    Assert.assertEquals("get-user-id", Strings.toKebabCase("GetUserID", "fallback"));
    Assert.assertEquals("xml-parser", Strings.toKebabCase("XMLParser", "fallback"));
    Assert.assertEquals("o-auth2-handler", Strings.toKebabCase("OAuth2Handler", "fallback"));
    Assert.assertEquals("generator", Strings.toKebabCase("", "generator"));
  }
}
