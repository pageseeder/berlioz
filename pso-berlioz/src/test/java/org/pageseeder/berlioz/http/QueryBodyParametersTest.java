package org.pageseeder.berlioz.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

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
  void testParse_oversizedBody_returnsEmptyWithoutThrowing() {
    String hugeValue = "x".repeat(2 * 1024 * 1024);
    HttpServletRequest req = HttpTestSupport.request()
        .method("QUERY")
        .contentType("application/x-www-form-urlencoded")
        .body("q=" + hugeValue)
        .build();

    assertTrue(QueryBodyParameters.parse(req).isEmpty());
  }
}
