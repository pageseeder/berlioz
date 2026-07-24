package org.pageseeder.berlioz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.error.HttpException;

class QueryBodyParametersTest {

  @Test
  void testParse_notQueryMethod_returnsEmpty() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("POST")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();
    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }

  @Test
  void testParse_wrongContentType_returnsEmpty() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/json")
        .body("{\"q\":\"hello\"}")
        .build();
    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }

  @Test
  void testParse_noContentType_returnsEmpty() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .body("q=hello")
        .build();
    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }

  @Test
  void testParse_parsesFormUrlEncodedBody() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded;charset=UTF-8")
        .body("q=hello+world&page=2")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("hello world", params.get("q"));
    assertEquals("2", params.get("page"));
  }

  @Test
  void testParse_repeatedBodyParameter_firstValueWins() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body("q=first&q=last")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("first", params.get("q"));
    assertEquals(1, params.size());
  }

  @Test
  void testParse_engineAlreadyExposesBody_bodyIsNotParsed() {
    // Simulates a container with native QUERY support: getParameterMap() already contains a
    // parameter that the (empty) URL query string does not account for.
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .parameter("q", "already-exposed-by-container")
        .body("q=raw-body-should-be-ignored")
        .build();

    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }

  @Test
  void testParse_parameterMapMatchesQueryStringOnly_bodyIsStillParsed() {
    // getParameterMap() contains exactly what the URL query string alone would produce, so the
    // container has not parsed the body — Berlioz should still parse it itself.
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("page=2")
        .parameter("page", "2")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("hello", params.get("q"));
  }

  @Test
  void testParse_repeatedQueryStringParameter_bodyIsStillParsed() {
    // A URL query string with a repeated key (e.g. "?tag=a&tag=b") legitimately produces a
    // multi-valued getParameterMap() entry with no body involved at all; that must not be
    // mistaken for the container having parsed the QUERY body.
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("tag=a&tag=b")
        .parameter("tag", "a", "b")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("hello", params.get("q"));
  }

  @Test
  void testParse_containerUsesDifferentQueryValueEncoding_bodyIsStillParsed() {
    // The raw query is UTF-8 for "é", while the simulated container decoded the same bytes as
    // ISO-8859-1. A decoded-value comparison would mistake that difference for a body parameter.
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("city=%C3%A9")
        .parameter("city", "Ã©")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("hello", params.get("q"));
  }

  @Test
  void testParse_containerUsesDifferentQueryNameEncoding_bodyIsStillParsed() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("caf%C3%A9=value")
        .parameter("cafÃ©", "value")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals("hello", params.get("q"));
  }

  @Test
  void testParse_engineAggregatesBodyWithSameNamedQueryParameter_bodyIsNotParsed() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("q=query")
        .parameter("q", "query", "already-exposed-body")
        .contentType("application/x-www-form-urlencoded")
        .body("q=raw-body-should-be-ignored")
        .build();

    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }

  @Test
  void testParse_oversizedBody_throwsPayloadTooLarge() {
    String hugeValue = "x".repeat(2 * 1024 * 1024);
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .contentLength(-1)
        .body("q=" + hugeValue)
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(413, ex.getHttpCode());
  }

  @Test
  void testParse_declaredOversizedBody_rejectedBeforeReadingStream() {
    HttpTestSupport.RequestBuilder builder = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .contentLength((long) Integer.MAX_VALUE + 1)
        .body("q=small");
    HttpServletRequest req = builder.build();

    HttpException ex = assertThrows(HttpException.class,
        () -> QueryBodyParameters.parse(req));

    assertEquals(413, ex.getHttpCode());
    assertFalse(builder.inputStreamAccessed());
  }

  @Test
  void testParse_maximumParameterOccurrences_succeeds() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body(formParameters(1_000, false))
        .build();

    Map<String, String> params = QueryBodyParameters.parse(req);

    assertEquals(1_000, params.size());
    assertEquals("999", params.get("p999"));
  }

  @Test
  void testParse_tooManyDistinctParameters_throwsPayloadTooLarge() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body(formParameters(1_001, false))
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(413, ex.getHttpCode());
  }

  @Test
  void testParse_tooManyRepeatedParameters_throwsPayloadTooLarge() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body(formParameters(1_001, true))
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(413, ex.getHttpCode());
  }

  @Test
  void testParse_malformedExcessParameter_rejectedBeforeDecoding() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body(formParameters(1_000, false) + "&bad=%")
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(413, ex.getHttpCode());
  }

  @Test
  void testParse_malformedPercentEscape_throwsBadRequest() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body("q=%2")
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(400, ex.getHttpCode());
  }

  @Test
  void testParse_malformedQueryString_throwsBadRequest() {
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .queryString("page=%2")
        .contentType("application/x-www-form-urlencoded")
        .body("q=hello")
        .build();

    HttpException ex = assertThrows(HttpException.class, () -> QueryBodyParameters.parse(req));
    assertEquals(400, ex.getHttpCode());
  }

  private static String formParameters(int count, boolean repeatedName) {
    StringBuilder form = new StringBuilder(count * 10);
    for (int i = 0; i < count; i++) {
      if (i > 0) form.append('&');
      form.append(repeatedName ? "p" : "p" + i).append('=').append(i);
    }
    return form.toString();
  }
}
