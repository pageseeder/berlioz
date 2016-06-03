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

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for the <code>URIPattern</code>.
 *
 * @author Christophe Lauret
 */
public final class URIPatternTest {

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> expression.
   */
  @Test
  public void testNew_NullString() {
    boolean nullThrown = false;
    try {
      new URIPattern((String) null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assert.assertTrue(nullThrown);
    }
  }

  /**
   * Test that the constructor throws a NullPointerException for a <code>null</code> template.
   */
  @Test
  public void testNew_NullTemplate() {
    boolean nullThrown = false;
    try {
      new URIPattern((URITemplate) null);
    } catch (NullPointerException ex) {
      nullThrown = true;
    } finally {
      Assert.assertTrue(nullThrown);
    }
  }

  /**
   * Test that the constructor handles an empty string.
   */
  @Test
  public void testNew_EmptyString() {
    new URIPattern("");
  }

  /**
   * Test the <code>equals</code> method.
   */
  @Test
  public void testEquals_Contract() {
    URIPattern x = new URIPattern("http://example.com/{X}");
    URIPattern y = new URIPattern("http://example.com/{X}");
    URIPattern z = new URIPattern("http://example.com/{Y}");
    TestUtils.satisfyEqualsContract(x, y, z);
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatchSingle() {
    URIPattern x = new URIPattern("http://example.com/{X}");
    Assert.assertTrue(x.match("http://example.com/toast"));
    Assert.assertTrue(x.match("http://example.com/~clauret"));
    Assert.assertTrue(x.match("http://example.com/%45"));
    Assert.assertTrue(x.match("http://example.com/toast.xml"));
    Assert.assertTrue(x.match("http://example.com/test@example.org"));
    Assert.assertFalse(x.match("http://example.com/toast/"));
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatchDouble() {
    URIPattern y = new URIPattern("http://example.com/{X}/{Y}/home");
    Assert.assertTrue(y.match("http://example.com/user/clauret/home"));
    Assert.assertTrue(y.match("http://example.com/dir-x/_/home"));
    Assert.assertFalse(y.match("http://example.com/toast//home"));
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatchTyped() {
    URIPattern x = new URIPattern("http://example.com/{t:X}");
    Assert.assertTrue(x.match("http://example.com/toast"));
    Assert.assertTrue(x.match("http://example.com/~clauret"));
    Assert.assertTrue(x.match("http://example.com/%45"));
    Assert.assertTrue(x.match("http://example.com/test@example.org"));
    Assert.assertFalse(x.match("http://example.com/toast/"));
  }

  /**
   * Test the <code>match</code> method.
   */
  @Test
  public void testMatch_URIInsert() {
    URIPattern x = new URIPattern("http://example.com/{+X}");
    Assert.assertTrue(x.match("http://example.com/this/is/a/path"));
    Assert.assertTrue(x.match("http://example.com/email@example.org"));
  }

  /**
   * Test the <code>match</code> method
   */
  @Test
  public void testMatch_Wildcard() {
    URIPattern x = new URIPattern("http://example.com/*");
    Assert.assertTrue(x.match("http://example.com/this/is/a/path"));
    Assert.assertTrue(x.match("http://example.com/email@example.org"));
    Assert.assertTrue(x.match("http://example.com/dir/subdir/doc.html"));
  }

  /**
   * Test the <code>match</code> method
   */
  @Test
  public void testMatch_PathParameter() {
    URIPattern y = new URIPattern("http://example.com/filter{;x,y,z}/list");
    Assert.assertTrue(y.match("http://example.com/filter;x=1;y=2;z=5/list"));
    Assert.assertTrue(y.match("http://example.com/filter;y=1;z=2;x=5/list"));
    Assert.assertTrue(y.match("http://example.com/filter;y=1;z=2/list"));
    Assert.assertTrue(y.match("http://example.com/filter;y=1;z=2/list"));
  }

}
