package org.pageseeder.berlioz.http;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.util.GenericEntityInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpHeaderUtilsTest {

  // ---------------------------------------------------------------------------
  // isCompressible
  // ---------------------------------------------------------------------------

  @Test
  public void testIsCompressible_Null() {
    Assertions.assertFalse(HttpHeaderUtils.isCompressible(null));
  }

  @Test
  public void testIsCompressible_TextTypes() {
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("text/html"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("text/plain"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("text/css"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("text/javascript"));
  }

  @Test
  public void testIsCompressible_XmlTypes() {
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/xml"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/xhtml+xml"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("image/svg+xml"));
  }

  @Test
  public void testIsCompressible_JsonTypes() {
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/json"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/geo+json"));
  }

  @Test
  public void testIsCompressible_JavascriptTypes() {
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/javascript"));
    Assertions.assertTrue(HttpHeaderUtils.isCompressible("application/x-javascript"));
  }

  @Test
  public void testIsCompressible_BinaryTypes() {
    Assertions.assertFalse(HttpHeaderUtils.isCompressible("image/png"));
    Assertions.assertFalse(HttpHeaderUtils.isCompressible("image/jpeg"));
    Assertions.assertFalse(HttpHeaderUtils.isCompressible("application/pdf"));
    Assertions.assertFalse(HttpHeaderUtils.isCompressible("application/octet-stream"));
  }

  // ---------------------------------------------------------------------------
  // getETagForGZip
  // ---------------------------------------------------------------------------

  @Test
  public void testGetETagForGZip_Null() {
    Assertions.assertNull(HttpHeaderUtils.getETagForGZip(null));
  }

  @Test
  public void testGetETagForGZip_Normal() {
    Assertions.assertEquals(HttpHeaderUtils.getETagForGZip("\"abc\""), "\"abc-gzip\"");
  }

  @Test
  public void testGetETagForGZip_Hash() {
    Assertions.assertEquals(HttpHeaderUtils.getETagForGZip("\"a1b2c3\""), "\"a1b2c3-gzip\"");
  }

  @Test
  public void testGetETagForGZip_NoQuotes() {
    // ETag with no double-quote at all (lastIndexOf returns -1): returned unchanged
    Assertions.assertEquals(HttpHeaderUtils.getETagForGZip("abc"), "abc");
  }

  // ---------------------------------------------------------------------------
  // getETagForUncompressed
  // ---------------------------------------------------------------------------

  @Test
  public void testGetETagForUncompressed_Null() {
    Assertions.assertNull(HttpHeaderUtils.getETagForUncompressed(null));
  }

  @Test
  public void testGetETagForUncompressed_GZipETag() {
    Assertions.assertEquals(HttpHeaderUtils.getETagForUncompressed("\"abc-gzip\""), "\"abc\"");
  }

  @Test
  public void testGetETagForUncompressed_Hash() {
    Assertions.assertEquals(HttpHeaderUtils.getETagForUncompressed("\"a1b2c3-gzip\""), "\"a1b2c3\"");
  }

  @Test
  public void testGetETagForUncompressed_PlainETag() {
    // No GZip suffix: returned unchanged
    Assertions.assertEquals(HttpHeaderUtils.getETagForUncompressed("\"abc\""), "\"abc\"");
  }

  @Test
  public void testGetETagForUncompressed_RoundTrip() {
    // getETagForGZip and getETagForUncompressed should be inverses of each other
    String original = "\"a1b2c3\"";
    Assertions.assertEquals(original, HttpHeaderUtils.getETagForUncompressed(HttpHeaderUtils.getETagForGZip(original)));
  }

  // ---------------------------------------------------------------------------
  // toLastModified
  // ---------------------------------------------------------------------------

  @Test
  public void testToLastModified_Epoch() {
    // Unix epoch is Thursday 1 January 1970 00:00:00 GMT
    Assertions.assertEquals(HttpHeaderUtils.toLastModified(0L), "Thu, 01 Jan 1970 00:00:00 GMT");
  }

  @Test
  public void testToLastModified_KnownDate() {
    // 2000-01-01T00:00:00Z = 946684800000 ms
    Assertions.assertEquals(HttpHeaderUtils.toLastModified(946684800000L), "Sat, 01 Jan 2000 00:00:00 GMT");
  }

  @Test
  public void testToLastModified_AlwaysGMT() {
    String result = HttpHeaderUtils.toLastModified(System.currentTimeMillis());
    Assertions.assertTrue(result.endsWith("GMT"), "Expected GMT timezone in header value");
  }

  @Test
  public void testToLastModified_Format() {
    // Verify the format matches: "EEE, dd MMM yyyy HH:mm:ss GMT"
    String result = HttpHeaderUtils.toLastModified(0L);
    Assertions.assertTrue(result.matches("[A-Z][a-z]{2}, \\d{2} [A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"), "Expected format 'Day, DD Mon YYYY HH:MM:SS GMT'");
  }

  // ---------------------------------------------------------------------------
  // allow
  // ---------------------------------------------------------------------------

  @Test
  public void testAllow_Empty() {
    Assertions.assertEquals(HttpHeaderUtils.allow(Collections.emptyList()), "");
  }

  @Test
  public void testAllow_Single() {
    Assertions.assertEquals(HttpHeaderUtils.allow(Collections.singletonList("GET")), "GET");
  }

  @Test
  public void testAllow_Multiple() {
    List<String> methods = Arrays.asList("GET", "HEAD", "POST");
    Assertions.assertEquals(HttpHeaderUtils.allow(methods), "GET,HEAD,POST");
  }

  @Test
  public void testAllow_NoTrailingComma() {
    String result = HttpHeaderUtils.allow(Arrays.asList("GET", "POST"));
    Assertions.assertFalse(result.endsWith(","), "Allow header must not end with a comma");
    Assertions.assertFalse(result.startsWith(","), "Allow header must not start with a comma");
  }

  // ---------------------------------------------------------------------------
  // acceptsGZipCompression
  // ---------------------------------------------------------------------------

  @Test
  public void testAcceptsGZipCompression() {
    HttpServletRequest accepted = HttpTestSupport.request()
        .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
        .build();
    HttpServletRequest rejected = HttpTestSupport.request()
        .header(HttpHeaders.ACCEPT_ENCODING, "gzip;q=0")
        .build();
    HttpServletRequest absent = HttpTestSupport.request().build();

    Assertions.assertTrue(HttpHeaderUtils.acceptsGZipCompression(accepted));
    Assertions.assertFalse(HttpHeaderUtils.acceptsGZipCompression(rejected));
    Assertions.assertFalse(HttpHeaderUtils.acceptsGZipCompression(absent));
  }

  // ---------------------------------------------------------------------------
  // setContentLength
  // ---------------------------------------------------------------------------

  @Test
  public void testSetContentLength_IntValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpHeaderUtils.setContentLength(response, 1024);

    Assertions.assertEquals(1024, recorder.contentLength());
    Assertions.assertNull(recorder.header(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  public void testSetContentLength_LongValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpHeaderUtils.setContentLength(response, (long) Integer.MAX_VALUE + 1);

    Assertions.assertEquals(-1, recorder.contentLength());
    Assertions.assertEquals(recorder.header(HttpHeaders.CONTENT_LENGTH), "2147483648");
  }

  // ---------------------------------------------------------------------------
  // checkIfHeaders
  // ---------------------------------------------------------------------------

  @Test
  public void testCheckIfHeaders_NoConditions() throws Exception {
    HttpServletRequest request = HttpTestSupport.request().build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertTrue(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_OK, recorder.status());
  }

  @Test
  public void testCheckIfHeaders_IfNoneMatchGet() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("GET")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assertions.assertEquals(recorder.header(HttpHeaders.ETAG), "\"abc\"");
    Assertions.assertFalse(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfNoneMatchPost() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("POST")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assertions.assertTrue(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfMatchMismatch() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .header(HttpHeaders.IF_MATCH, "\"other\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assertions.assertTrue(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfModifiedSince() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .dateHeader(HttpHeaders.IF_MODIFIED_SINCE, 1000)
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assertions.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1500, "text/plain", "\"abc\"")));
    Assertions.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assertions.assertEquals(recorder.header(HttpHeaders.ETAG), "\"abc\"");
  }
}
