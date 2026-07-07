package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

class HttpResponsesTest {

  // ---------------------------------------------------------------------------
  // toLastModified
  // ---------------------------------------------------------------------------

  @Test
  void testToLastModified_Epoch() {
    // Unix epoch is Thursday 1 January 1970 00:00:00 GMT
    Assertions.assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", HttpResponses.toLastModified(0L));
  }

  @Test
  void testToLastModified_KnownDate() {
    // 2000-01-01T00:00:00Z = 946684800000 ms
    Assertions.assertEquals("Sat, 01 Jan 2000 00:00:00 GMT", HttpResponses.toLastModified(946684800000L));
  }

  @Test
  void testToLastModified_AlwaysGMT() {
    String result = HttpResponses.toLastModified(System.currentTimeMillis());
    Assertions.assertTrue(result.endsWith("GMT"), "Expected GMT timezone in header value");
  }

  @Test
  void testToLastModified_Format() {
    // Verify the format matches: "EEE, dd MMM yyyy HH:mm:ss GMT"
    String result = HttpResponses.toLastModified(0L);
    Assertions.assertTrue(result.matches("[A-Z][a-z]{2}, \\d{2} [A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"), "Expected format 'Day, DD Mon YYYY HH:MM:SS GMT'");
  }

  // ---------------------------------------------------------------------------
  // allow
  // ---------------------------------------------------------------------------

  @Test
  void testAllow_Empty() {
    Assertions.assertEquals("", HttpResponses.allow(Collections.emptyList()));
  }

  @Test
  void testAllow_Single() {
    Assertions.assertEquals("GET", HttpResponses.allow(Collections.singletonList("GET")));
  }

  @Test
  void testAllow_Multiple() {
    List<String> methods = Arrays.asList("GET", "HEAD", "POST");
    Assertions.assertEquals("GET,HEAD,POST", HttpResponses.allow(methods));
  }

  @Test
  void testAllow_NoTrailingComma() {
    String result = HttpResponses.allow(Arrays.asList("GET", "POST"));
    Assertions.assertFalse(result.endsWith(","), "Allow header must not end with a comma");
    Assertions.assertFalse(result.startsWith(","), "Allow header must not start with a comma");
  }

  // ---------------------------------------------------------------------------
  // setContentLength
  // ---------------------------------------------------------------------------

  @Test
  void testSetContentLength_IntValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpResponses.setContentLength(response, 1024);

    Assertions.assertEquals(1024, recorder.contentLength());
    Assertions.assertNull(recorder.header(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  void testSetContentLength_LongValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpResponses.setContentLength(response, (long) Integer.MAX_VALUE + 1);

    Assertions.assertEquals(-1, recorder.contentLength());
    Assertions.assertEquals("2147483648", recorder.header(HttpHeaders.CONTENT_LENGTH));
  }

}
