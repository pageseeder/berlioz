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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author ccabral
 * @since 06 July 2021
 */
@SuppressWarnings({"deprecation", "removal"})
class StringUtilsTest {

  @Test
  void testIsBlank() {
    Assertions.assertTrue(StringUtils.isBlank(null));
    Assertions.assertTrue(StringUtils.isBlank(""));
    Assertions.assertTrue(StringUtils.isBlank("  "));
    Assertions.assertFalse(StringUtils.isBlank("abc"));
    Assertions.assertFalse(StringUtils.isBlank("null"));
    Assertions.assertFalse(StringUtils.isBlank("   h "));
  }

  @Test
  void testSubstringAfter(){
    Assertions.assertEquals("", StringUtils.substringAfter(null, ","));
    Assertions.assertEquals("", StringUtils.substringAfter(" ", ","));
    Assertions.assertEquals("", StringUtils.substringAfter("first second", null));
    Assertions.assertEquals("", StringUtils.substringAfter(null, null));
    Assertions.assertEquals("second", StringUtils.substringAfter("first,second", ","));
    Assertions.assertEquals("second", StringUtils.substringAfter("first second", " "));
    Assertions.assertEquals("first second", StringUtils.substringAfter("first second", ","));
    Assertions.assertEquals("first", StringUtils.substringAfter("first", " "));
    Assertions.assertEquals("", StringUtils.substringAfter("first ", " "));
    Assertions.assertEquals("second third", StringUtils.substringAfter("first second third", " "));
    Assertions.assertEquals("/simple-admin/api/auth/user.json", StringUtils.substringBefore("/simple-admin/api/auth/user.json", ""));
  }


  @Test
  void testSubstringBefore(){
    Assertions.assertEquals("", StringUtils.substringBefore(null, ","));
    Assertions.assertEquals("", StringUtils.substringBefore(" ", ","));
    Assertions.assertEquals("", StringUtils.substringBefore("first second", null));
    Assertions.assertEquals("", StringUtils.substringBefore(null, null));
    Assertions.assertEquals("first second", StringUtils.substringBefore("first second", ","));
    Assertions.assertEquals("", StringUtils.substringBefore(",first,second", ","));
    Assertions.assertEquals("first", StringUtils.substringBefore("first,second", ","));
    Assertions.assertEquals("first", StringUtils.substringBefore("first second", " "));
    Assertions.assertEquals("first", StringUtils.substringBefore("first second third", " "));
    Assertions.assertEquals("first", StringUtils.substringBefore("first", " "));
    Assertions.assertEquals("/simple-admin/api/auth/user.json", StringUtils.substringBefore("/simple-admin/api/auth/user.json", ""));
  }

}
