/*
 * Copyright 2016 Allette Systems (Australia)
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

public final class MD5Test {

  @Test(expected = NullPointerException.class)
  public void testHash_NullString() {
    MD5.hash((String)null);
  }

  @Test
  public void testHash_String() {
    Assert.assertEquals("d41d8cd98f00b204e9800998ecf8427e", MD5.hash(""));
    Assert.assertEquals("098f6bcd4621d373cade4e832627b4f6", MD5.hash("test"));
    Assert.assertEquals("942a46d563d50475e73c41765b35cbbf", MD5.hash("Licensed under the Apache License, Version 2.0 (the \"License\");"));
  }

}
