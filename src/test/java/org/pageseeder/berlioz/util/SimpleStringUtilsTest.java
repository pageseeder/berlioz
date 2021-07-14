/*
 * Copyright 2021 Allette Systems (Australia)
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
package org.pageseeder.berlioz.util;


import org.junit.Assert;
import org.junit.Test;

/**
 * @author ccabral
 * @since 06 July 2021
 */
public class SimpleStringUtilsTest {

  @Test
  public void testIsBlank() {
    Assert.assertTrue(StringUtils.isBlank(null));
    Assert.assertTrue(StringUtils.isBlank(""));
    Assert.assertTrue(StringUtils.isBlank("  "));
    Assert.assertFalse(StringUtils.isBlank("abc"));
    Assert.assertFalse(StringUtils.isBlank("null"));
    Assert.assertFalse(StringUtils.isBlank("   h "));
  }

  @Test
  public void testSubstringAfter(){
    Assert.assertEquals("", StringUtils.substringAfter(null, ","));
    Assert.assertEquals("", StringUtils.substringAfter(" ", ","));
    Assert.assertEquals("", StringUtils.substringAfter("first second", null));
    Assert.assertEquals("", StringUtils.substringAfter(null, null));
    Assert.assertEquals("second", StringUtils.substringAfter("first,second", ","));
    Assert.assertEquals("second", StringUtils.substringAfter("first second", " "));
    Assert.assertEquals("first second", StringUtils.substringAfter("first second", ","));
    Assert.assertEquals("first", StringUtils.substringAfter("first", " "));
    Assert.assertEquals("", StringUtils.substringAfter("first ", " "));
    Assert.assertEquals("second third", StringUtils.substringAfter("first second third", " "));
    Assert.assertEquals("/simple-admin/api/auth/user.json", StringUtils.substringBefore("/simple-admin/api/auth/user.json", ""));
  }


  @Test
  public void testSubstringBefore(){
    Assert.assertEquals("", StringUtils.substringBefore(null, ","));
    Assert.assertEquals("", StringUtils.substringBefore(" ", ","));
    Assert.assertEquals("", StringUtils.substringBefore("first second", null));
    Assert.assertEquals("", StringUtils.substringBefore(null, null));
    Assert.assertEquals("first second", StringUtils.substringBefore("first second", ","));
    Assert.assertEquals("", StringUtils.substringBefore(",first,second", ","));
    Assert.assertEquals("first", StringUtils.substringBefore("first,second", ","));
    Assert.assertEquals("first", StringUtils.substringBefore("first second", " "));
    Assert.assertEquals("first", StringUtils.substringBefore("first second third", " "));
    Assert.assertEquals("first", StringUtils.substringBefore("first", " "));
    Assert.assertEquals("/simple-admin/api/auth/user.json", StringUtils.substringBefore("/simple-admin/api/auth/user.json", ""));
  }
}
