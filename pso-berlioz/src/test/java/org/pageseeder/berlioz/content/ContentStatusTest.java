/*
 * Copyright 2026 Allette Systems (Australia)
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
package org.pageseeder.berlioz.content;

import org.junit.Assert;
import org.junit.Test;

public final class ContentStatusTest {

  @Test
  public void testCode_selectedValues() {
    Assert.assertEquals(200, ContentStatus.OK.code());
    Assert.assertEquals(201, ContentStatus.CREATED.code());
    Assert.assertEquals(204, ContentStatus.NO_CONTENT.code());
    Assert.assertEquals(301, ContentStatus.MOVED_PERMANENTLY.code());
    Assert.assertEquals(302, ContentStatus.FOUND.code());
    Assert.assertEquals(303, ContentStatus.SEE_OTHER.code());
    Assert.assertEquals(307, ContentStatus.TEMPORARY_REDIRECT.code());
    Assert.assertEquals(400, ContentStatus.BAD_REQUEST.code());
    Assert.assertEquals(403, ContentStatus.FORBIDDEN.code());
    Assert.assertEquals(404, ContentStatus.NOT_FOUND.code());
    Assert.assertEquals(500, ContentStatus.INTERNAL_SERVER_ERROR.code());
  }

  @Test
  public void testForCode_found() {
    Assert.assertEquals(ContentStatus.OK, ContentStatus.forCode(200));
    Assert.assertEquals(ContentStatus.NOT_FOUND, ContentStatus.forCode(404));
    Assert.assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, ContentStatus.forCode(500));
    Assert.assertEquals(ContentStatus.MOVED_PERMANENTLY, ContentStatus.forCode(301));
  }

  @Test
  public void testForCode_unknown() {
    Assert.assertNull(ContentStatus.forCode(0));
    Assert.assertNull(ContentStatus.forCode(999));
    Assert.assertNull(ContentStatus.forCode(-1));
  }

  @Test
  public void testForCode_allValuesRoundTrip() {
    for (ContentStatus status : ContentStatus.values()) {
      Assert.assertEquals(status, ContentStatus.forCode(status.code()));
    }
  }

  @Test
  public void testIsRedirect_redirectCodes() {
    Assert.assertTrue(ContentStatus.isRedirect(ContentStatus.MOVED_PERMANENTLY));
    Assert.assertTrue(ContentStatus.isRedirect(ContentStatus.FOUND));
    Assert.assertTrue(ContentStatus.isRedirect(ContentStatus.SEE_OTHER));
    Assert.assertTrue(ContentStatus.isRedirect(ContentStatus.TEMPORARY_REDIRECT));
    Assert.assertTrue(ContentStatus.isRedirect(ContentStatus.MULTIPLE_CHOICE));
  }

  @Test
  public void testIsRedirect_nonRedirectCodes() {
    Assert.assertFalse(ContentStatus.isRedirect(ContentStatus.OK));
    Assert.assertFalse(ContentStatus.isRedirect(ContentStatus.CREATED));
    Assert.assertFalse(ContentStatus.isRedirect(ContentStatus.NOT_FOUND));
    Assert.assertFalse(ContentStatus.isRedirect(ContentStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  public void testToString_lowercaseWithHyphens() {
    Assert.assertEquals("ok", ContentStatus.OK.toString());
    Assert.assertEquals("not-found", ContentStatus.NOT_FOUND.toString());
    Assert.assertEquals("internal-server-error", ContentStatus.INTERNAL_SERVER_ERROR.toString());
    Assert.assertEquals("moved-permanently", ContentStatus.MOVED_PERMANENTLY.toString());
    Assert.assertEquals("non-authoritative-information", ContentStatus.NON_AUTHORITATIVE_INFORMATION.toString());
  }

  @Test
  public void testToString_noUpperCaseNoUnderscore() {
    for (ContentStatus status : ContentStatus.values()) {
      String s = status.toString();
      Assert.assertEquals("toString should be lowercase: " + s, s, s.toLowerCase());
      Assert.assertFalse("toString should not contain underscore: " + s, s.contains("_"));
    }
  }
}
