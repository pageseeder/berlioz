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
import org.pageseeder.berlioz.error.ProblemDetails;

import java.util.Map;

final class ResponseTest {

  // --- ok() ---

  @Test
  void testOk_hasStatusOk() {
    Assertions.assertEquals(ContentStatus.OK, Response.ok().status());
  }

  @Test
  void testOk_noRedirect() {
    Response r = Response.ok();
    Assertions.assertNull(r.redirectLocation());
    Assertions.assertFalse(r.isRedirect());
  }

  @Test
  void testOk_noProblem() {
    Response r = Response.ok();
    Assertions.assertNull(r.problem());
    Assertions.assertFalse(r.isProblem());
  }

  @Test
  void testOk_headersEmpty() {
    Assertions.assertTrue(Response.ok().headers().isEmpty());
  }

  // --- status() ---

  @Test
  void testStatus_setsStatus() {
    Response r = Response.status(ContentStatus.NOT_FOUND);
    Assertions.assertEquals(ContentStatus.NOT_FOUND, r.status());
  }

  @Test
  void testStatus_nonRedirectStatuses() {
    ContentStatus[] nonRedirects = {
        ContentStatus.OK,
        ContentStatus.BAD_REQUEST,
        ContentStatus.NOT_FOUND,
        ContentStatus.INTERNAL_SERVER_ERROR
    };
    for (ContentStatus s : nonRedirects) {
      Assertions.assertDoesNotThrow(() -> Response.status(s), s.name());
    }
  }

  @Test
  void testStatus_redirectStatusThrows() {
    ContentStatus[] redirects = {
        ContentStatus.MOVED_PERMANENTLY,
        ContentStatus.FOUND,
        ContentStatus.SEE_OTHER,
        ContentStatus.TEMPORARY_REDIRECT,
        ContentStatus.PERMANENT_REDIRECT
    };
    for (ContentStatus s : redirects) {
      Assertions.assertThrows(IllegalArgumentException.class, () -> Response.status(s), s.name());
    }
  }

  @Test
  void testStatus_nullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> Response.status(null));
  }

  @Test
  void testStatus_noRedirect() {
    Response r = Response.status(ContentStatus.NOT_FOUND);
    Assertions.assertNull(r.redirectLocation());
    Assertions.assertFalse(r.isRedirect());
  }

  @Test
  void testStatus_noProblem() {
    Response r = Response.status(ContentStatus.NOT_FOUND);
    Assertions.assertNull(r.problem());
    Assertions.assertFalse(r.isProblem());
  }

  // --- redirect() ---

  @Test
  void testRedirect_setsStatusAndLocation() {
    Response r = Response.redirect(ContentStatus.SEE_OTHER, "/login");
    Assertions.assertEquals(ContentStatus.SEE_OTHER, r.status());
    Assertions.assertEquals("/login", r.redirectLocation());
  }

  @Test
  void testRedirect_isRedirectTrue() {
    Response r = Response.redirect(ContentStatus.SEE_OTHER, "/login");
    Assertions.assertTrue(r.isRedirect());
  }

  @Test
  void testRedirect_redirectStatuses() {
    ContentStatus[] redirects = {
        ContentStatus.MOVED_PERMANENTLY,
        ContentStatus.FOUND,
        ContentStatus.SEE_OTHER,
        ContentStatus.TEMPORARY_REDIRECT,
        ContentStatus.PERMANENT_REDIRECT
    };
    for (ContentStatus s : redirects) {
      Assertions.assertDoesNotThrow(() -> Response.redirect(s, "/target"), s.name());
    }
  }

  @Test
  void testRedirect_nonRedirectStatusThrows() {
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> Response.redirect(ContentStatus.OK, "/target"));
    Assertions.assertThrows(IllegalArgumentException.class,
        () -> Response.redirect(ContentStatus.NOT_FOUND, "/target"));
  }

  @Test
  void testRedirect_nullStatusThrows() {
    Assertions.assertThrows(NullPointerException.class,
        () -> Response.redirect(null, "/target"));
  }

  @Test
  void testRedirect_nullLocationThrows() {
    Assertions.assertThrows(NullPointerException.class,
        () -> Response.redirect(ContentStatus.SEE_OTHER, null));
  }

  @Test
  void testRedirect_noProblem() {
    Response r = Response.redirect(ContentStatus.SEE_OTHER, "/login");
    Assertions.assertNull(r.problem());
    Assertions.assertFalse(r.isProblem());
  }

  // --- problem() ---

  @Test
  void testProblem_statusFromProblemDetails() {
    ProblemDetails pd = ProblemDetails.of(ContentStatus.NOT_FOUND);
    Response r = Response.problem(pd);
    Assertions.assertEquals(ContentStatus.NOT_FOUND, r.status());
    Assertions.assertEquals(404, r.statusCode());
  }

  @Test
  void testProblem_statusCodePreservesValidHttpStatusNotInContentStatus() {
    ProblemDetails pd = ProblemDetails.of(412);
    Response r = Response.problem(pd);
    Assertions.assertEquals(412, r.statusCode());
  }

  @Test
  void testProblem_statusUsesStatusClassForValidHttpStatusNotInContentStatus() {
    ProblemDetails pd = ProblemDetails.of(412);
    Response r = Response.problem(pd);
    Assertions.assertEquals(ContentStatus.BAD_REQUEST, r.status());
  }

  @Test
  void testProblem_isProblemTrue() {
    Response r = Response.problem(ProblemDetails.of(ContentStatus.BAD_REQUEST));
    Assertions.assertTrue(r.isProblem());
  }

  @Test
  void testProblem_returnsProblemDetails() {
    ProblemDetails pd = ProblemDetails.of(ContentStatus.BAD_REQUEST).title("Bad input");
    Response r = Response.problem(pd);
    Assertions.assertSame(pd, r.problem());
  }

  @Test
  void testProblem_nullThrows() {
    Assertions.assertThrows(NullPointerException.class, () -> Response.problem(null));
  }

  @Test
  void testProblem_noRedirect() {
    Response r = Response.problem(ProblemDetails.of(ContentStatus.BAD_REQUEST));
    Assertions.assertNull(r.redirectLocation());
    Assertions.assertFalse(r.isRedirect());
  }

  // --- header() ---

  @Test
  void testHeader_validHeader() {
    Response r = Response.ok()
        .header("Content-Location", "/articles/42")
        .header("WWW-Authenticate", "Bearer realm=\"api\"");

    Assertions.assertEquals("/articles/42", r.headers().get("Content-Location"));
    Assertions.assertEquals("Bearer realm=\"api\"", r.headers().get("WWW-Authenticate"));
  }

  @Test
  void testHeader_replacesExistingHeader() {
    Response r = Response.ok()
        .header("X-Test", "one")
        .header("X-Test", "two");

    Assertions.assertEquals(1, r.headers().size());
    Assertions.assertEquals("two", r.headers().get("X-Test"));
  }

  @Test
  void testHeader_returnsNewInstance() {
    Response original = Response.ok();
    Response updated = original.header("X-Test", "value");
    Assertions.assertNotSame(original, updated);
    Assertions.assertTrue(original.headers().isEmpty());
  }

  @Test
  void testHeader_preservesOtherFields() {
    ProblemDetails pd = ProblemDetails.of(ContentStatus.BAD_REQUEST);
    Response r = Response.problem(pd).header("X-Trace", "abc");
    Assertions.assertTrue(r.isProblem());
    Assertions.assertSame(pd, r.problem());
    Assertions.assertEquals("abc", r.headers().get("X-Trace"));
  }

  @Test
  void testHeader_mapIsUnmodifiable() {
    Map<String, String> headers = Response.ok().header("X-Test", "value").headers();
    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> headers.put("X-Extra", "value"));
  }

  @Test
  void testHeader_rejectsInvalidNames() {
    String[] names = {
        "",
        "Bad Header",
        "Bad:Header",
        "Bad/Header",
        "Bad(Header)",
        "Bad\rHeader",
        "Bad\nHeader",
        "Bad\u007fHeader",
        "BadéHeader"
    };
    for (String name : names) {
      Response r = Response.ok();
      Assertions.assertThrows(IllegalArgumentException.class, () -> r.header(name, "value"), name);
    }
  }

  @Test
  void testHeader_rejectsInvalidValues() {
    String[] values = {
        "one\r\nSet-Cookie: session=attacker",
        "one\nX-Injected: yes",
        "one\rX-Injected: yes",
        "one\u0000two",
        "one\u001ftwo",
        "one\u007ftwo"
    };
    for (String value : values) {
      Response r = Response.ok();
      Assertions.assertThrows(IllegalArgumentException.class, () -> r.header("X-Test", value), value);
    }
  }

  @Test
  void testHeader_allowsSpaceAndTabInValues() {
    Response r = Response.ok().header("X-Test", "one two\tthree");
    Assertions.assertEquals("one two\tthree", r.headers().get("X-Test"));
  }

  @Test
  void testHeaders_mergesMapWithReplaceSemantics() {
    Response original = Response.ok()
        .header("X-Existing", "old")
        .header("X-Preserved", "yes");

    Response updated = original.headers(Map.of(
        "X-Existing", "new",
        "Retry-After", "30"));

    Assertions.assertEquals("new", updated.headers().get("X-Existing"));
    Assertions.assertEquals("yes", updated.headers().get("X-Preserved"));
    Assertions.assertEquals("30", updated.headers().get("Retry-After"));
    Assertions.assertEquals("old", original.headers().get("X-Existing"));
  }

  @Test
  void testHeaders_rejectsInvalidEntryAtomically() {
    Response original = Response.ok().header("X-Existing", "value");

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> original.headers(Map.of("Retry-After", "30", "Bad Header", "value")));
    Assertions.assertEquals(Map.of("X-Existing", "value"), original.headers());
  }

}
