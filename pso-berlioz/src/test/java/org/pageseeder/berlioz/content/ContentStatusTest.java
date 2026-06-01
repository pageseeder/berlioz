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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class ContentStatusTest {

  @Test
  void testCode_selectedValues() {
    Assertions.assertEquals(200, ContentStatus.OK.code());
    Assertions.assertEquals(201, ContentStatus.CREATED.code());
    Assertions.assertEquals(204, ContentStatus.NO_CONTENT.code());
    Assertions.assertEquals(301, ContentStatus.MOVED_PERMANENTLY.code());
    Assertions.assertEquals(302, ContentStatus.FOUND.code());
    Assertions.assertEquals(303, ContentStatus.SEE_OTHER.code());
    Assertions.assertEquals(307, ContentStatus.TEMPORARY_REDIRECT.code());
    Assertions.assertEquals(400, ContentStatus.BAD_REQUEST.code());
    Assertions.assertEquals(403, ContentStatus.FORBIDDEN.code());
    Assertions.assertEquals(404, ContentStatus.NOT_FOUND.code());
    Assertions.assertEquals(500, ContentStatus.INTERNAL_SERVER_ERROR.code());
  }

  @Test
  void testForCode_found() {
    Assertions.assertEquals(ContentStatus.OK, ContentStatus.forCode(200));
    Assertions.assertEquals(ContentStatus.NOT_FOUND, ContentStatus.forCode(404));
    Assertions.assertEquals(ContentStatus.INTERNAL_SERVER_ERROR, ContentStatus.forCode(500));
    Assertions.assertEquals(ContentStatus.MOVED_PERMANENTLY, ContentStatus.forCode(301));
  }

  @Test
  void testForCode_unknown() {
    Assertions.assertNull(ContentStatus.forCode(0));
    Assertions.assertNull(ContentStatus.forCode(999));
    Assertions.assertNull(ContentStatus.forCode(-1));
  }

  @Test
  void testForCode_allValuesRoundTrip() {
    for (ContentStatus status : ContentStatus.values()) {
      Assertions.assertEquals(status, ContentStatus.forCode(status.code()));
    }
  }

  @Test
  void testIsRedirect_redirectCodes() {
    Assertions.assertTrue(ContentStatus.isRedirect(ContentStatus.MOVED_PERMANENTLY));
    Assertions.assertTrue(ContentStatus.isRedirect(ContentStatus.FOUND));
    Assertions.assertTrue(ContentStatus.isRedirect(ContentStatus.SEE_OTHER));
    Assertions.assertTrue(ContentStatus.isRedirect(ContentStatus.TEMPORARY_REDIRECT));
    Assertions.assertTrue(ContentStatus.isRedirect(ContentStatus.MULTIPLE_CHOICE));
  }

  @Test
  void testIsRedirect_nonRedirectCodes() {
    Assertions.assertFalse(ContentStatus.isRedirect(ContentStatus.OK));
    Assertions.assertFalse(ContentStatus.isRedirect(ContentStatus.CREATED));
    Assertions.assertFalse(ContentStatus.isRedirect(ContentStatus.NOT_FOUND));
    Assertions.assertFalse(ContentStatus.isRedirect(ContentStatus.INTERNAL_SERVER_ERROR));
  }

  @Test
  void testToString_lowercaseWithHyphens() {
    Assertions.assertEquals("ok", ContentStatus.OK.toString());
    Assertions.assertEquals("not-found", ContentStatus.NOT_FOUND.toString());
    Assertions.assertEquals("internal-server-error", ContentStatus.INTERNAL_SERVER_ERROR.toString());
    Assertions.assertEquals("moved-permanently", ContentStatus.MOVED_PERMANENTLY.toString());
    Assertions.assertEquals("non-authoritative-information", ContentStatus.NON_AUTHORITATIVE_INFORMATION.toString());
  }

  @Test
  void testToString_noUpperCaseNoUnderscore() {
    for (ContentStatus status : ContentStatus.values()) {
      String s = status.toString();
      Assertions.assertEquals(s, s.toLowerCase(), "toString should be lowercase: " + s);
      Assertions.assertFalse(s.contains("_"), "toString should not contain underscore: " + s);
    }
  }
}
