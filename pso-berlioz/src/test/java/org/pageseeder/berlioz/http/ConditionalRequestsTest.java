package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.GenericEntityInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

class ConditionalRequestsTest {

  @Test
  void testCheckIfHeaders_IfMatchGZipTokenInList() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .header(HttpHeaders.IF_MATCH, "\"other\",  \"abc-gzip\"  ")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();

    Assertions.assertTrue(ConditionalRequests.checkIfHeaders(request, recorder.build(),
        new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertFalse(recorder.errorSent());
  }

  @Test
  void testCheckIfHeaders_IfNoneMatchGZipPreservesMethodAndRepresentation() throws Exception {
    for (String method : new String[]{"GET", "HEAD", "POST"}) {
      HttpServletRequest request = HttpTestSupport.request()
          .method(method)
          .header(HttpHeaders.IF_NONE_MATCH, "\"other\",  \"abc-gzip\"  ")
          .build();
      HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();

      Assertions.assertFalse(ConditionalRequests.checkIfHeaders(request, recorder.build(),
          new GenericEntityInfo(1000, "text/plain", "\"abc\"")), method);
      if ("POST".equals(method)) {
        Assertions.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
        Assertions.assertTrue(recorder.errorSent());
      } else {
        Assertions.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
        Assertions.assertEquals("\"abc-gzip\"", recorder.header(HttpHeaders.ETAG));
        Assertions.assertFalse(recorder.errorSent());
      }
    }
  }

  @Test
  void testCheckIfHeaders_EmbeddedGZipMarkerDoesNotMatchBaseETag() throws Exception {
    for (String header : new String[]{HttpHeaders.IF_MATCH, HttpHeaders.IF_NONE_MATCH}) {
      HttpServletRequest request = HttpTestSupport.request()
          .method("GET")
          .header(header, "\"abc-gzip\"extra")
          .build();
      HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();

      boolean proceed = ConditionalRequests.checkIfHeaders(request, recorder.build(),
          new GenericEntityInfo(1000, "text/plain", "\"abc\""));
      Assertions.assertEquals(HttpHeaders.IF_NONE_MATCH.equals(header), proceed, header);
      Assertions.assertEquals(HttpHeaders.IF_MATCH.equals(header), recorder.errorSent(), header);
    }
  }

  @Test
  void testCheckIfHeaders_NoConditions() throws Exception {
    HttpServletRequest request = HttpTestSupport.request().build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertTrue(ConditionalRequests.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_OK, recorder.status());
  }

  @Test
  void testCheckIfHeaders_IfNoneMatchGet() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("GET")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(ConditionalRequests.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assertions.assertEquals("\"abc\"", recorder.header(HttpHeaders.ETAG));
    Assertions.assertFalse(recorder.errorSent());
  }

  @Test
  void testCheckIfHeaders_IfNoneMatchPost() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("POST")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(ConditionalRequests.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assertions.assertTrue(recorder.errorSent());
  }

  @Test
  void testCheckIfHeaders_IfMatchMismatch() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .header(HttpHeaders.IF_MATCH, "\"other\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(ConditionalRequests.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assertions.assertTrue(recorder.errorSent());
  }

  @Test
  void testCheckIfHeaders_IfModifiedSince() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .dateHeader(HttpHeaders.IF_MODIFIED_SINCE, 1000)
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(ConditionalRequests.checkIfHeaders(request, response, new GenericEntityInfo(1500, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assertions.assertEquals("\"abc\"", recorder.header(HttpHeaders.ETAG));
  }

}
