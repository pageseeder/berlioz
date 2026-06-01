package org.pageseeder.berlioz.http;

import org.junit.Assert;
import org.junit.Test;
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
    Assert.assertFalse(HttpHeaderUtils.isCompressible(null));
  }

  @Test
  public void testIsCompressible_TextTypes() {
    Assert.assertTrue(HttpHeaderUtils.isCompressible("text/html"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("text/plain"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("text/css"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("text/javascript"));
  }

  @Test
  public void testIsCompressible_XmlTypes() {
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/xml"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/xhtml+xml"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("image/svg+xml"));
  }

  @Test
  public void testIsCompressible_JsonTypes() {
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/json"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/geo+json"));
  }

  @Test
  public void testIsCompressible_JavascriptTypes() {
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/javascript"));
    Assert.assertTrue(HttpHeaderUtils.isCompressible("application/x-javascript"));
  }

  @Test
  public void testIsCompressible_BinaryTypes() {
    Assert.assertFalse(HttpHeaderUtils.isCompressible("image/png"));
    Assert.assertFalse(HttpHeaderUtils.isCompressible("image/jpeg"));
    Assert.assertFalse(HttpHeaderUtils.isCompressible("application/pdf"));
    Assert.assertFalse(HttpHeaderUtils.isCompressible("application/octet-stream"));
  }

  // ---------------------------------------------------------------------------
  // getETagForGZip
  // ---------------------------------------------------------------------------

  @Test
  public void testGetETagForGZip_Null() {
    Assert.assertNull(HttpHeaderUtils.getETagForGZip(null));
  }

  @Test
  public void testGetETagForGZip_Normal() {
    Assert.assertEquals("\"abc-gzip\"", HttpHeaderUtils.getETagForGZip("\"abc\""));
  }

  @Test
  public void testGetETagForGZip_Hash() {
    Assert.assertEquals("\"a1b2c3-gzip\"", HttpHeaderUtils.getETagForGZip("\"a1b2c3\""));
  }

  @Test
  public void testGetETagForGZip_NoQuotes() {
    // ETag with no double-quote at all (lastIndexOf returns -1): returned unchanged
    Assert.assertEquals("abc", HttpHeaderUtils.getETagForGZip("abc"));
  }

  // ---------------------------------------------------------------------------
  // getETagForUncompressed
  // ---------------------------------------------------------------------------

  @Test
  public void testGetETagForUncompressed_Null() {
    Assert.assertNull(HttpHeaderUtils.getETagForUncompressed(null));
  }

  @Test
  public void testGetETagForUncompressed_GZipETag() {
    Assert.assertEquals("\"abc\"", HttpHeaderUtils.getETagForUncompressed("\"abc-gzip\""));
  }

  @Test
  public void testGetETagForUncompressed_Hash() {
    Assert.assertEquals("\"a1b2c3\"", HttpHeaderUtils.getETagForUncompressed("\"a1b2c3-gzip\""));
  }

  @Test
  public void testGetETagForUncompressed_PlainETag() {
    // No GZip suffix: returned unchanged
    Assert.assertEquals("\"abc\"", HttpHeaderUtils.getETagForUncompressed("\"abc\""));
  }

  @Test
  public void testGetETagForUncompressed_RoundTrip() {
    // getETagForGZip and getETagForUncompressed should be inverses of each other
    String original = "\"a1b2c3\"";
    Assert.assertEquals(original, HttpHeaderUtils.getETagForUncompressed(HttpHeaderUtils.getETagForGZip(original)));
  }

  // ---------------------------------------------------------------------------
  // toLastModified
  // ---------------------------------------------------------------------------

  @Test
  public void testToLastModified_Epoch() {
    // Unix epoch is Thursday 1 January 1970 00:00:00 GMT
    Assert.assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", HttpHeaderUtils.toLastModified(0L));
  }

  @Test
  public void testToLastModified_KnownDate() {
    // 2000-01-01T00:00:00Z = 946684800000 ms
    Assert.assertEquals("Sat, 01 Jan 2000 00:00:00 GMT", HttpHeaderUtils.toLastModified(946684800000L));
  }

  @Test
  public void testToLastModified_AlwaysGMT() {
    String result = HttpHeaderUtils.toLastModified(System.currentTimeMillis());
    Assert.assertTrue("Expected GMT timezone in header value", result.endsWith("GMT"));
  }

  @Test
  public void testToLastModified_Format() {
    // Verify the format matches: "EEE, dd MMM yyyy HH:mm:ss GMT"
    String result = HttpHeaderUtils.toLastModified(0L);
    Assert.assertTrue("Expected format 'Day, DD Mon YYYY HH:MM:SS GMT'",
        result.matches("[A-Z][a-z]{2}, \\d{2} [A-Z][a-z]{2} \\d{4} \\d{2}:\\d{2}:\\d{2} GMT"));
  }

  // ---------------------------------------------------------------------------
  // allow
  // ---------------------------------------------------------------------------

  @Test
  public void testAllow_Empty() {
    Assert.assertEquals("", HttpHeaderUtils.allow(Collections.emptyList()));
  }

  @Test
  public void testAllow_Single() {
    Assert.assertEquals("GET", HttpHeaderUtils.allow(Collections.singletonList("GET")));
  }

  @Test
  public void testAllow_Multiple() {
    List<String> methods = Arrays.asList("GET", "HEAD", "POST");
    Assert.assertEquals("GET,HEAD,POST", HttpHeaderUtils.allow(methods));
  }

  @Test
  public void testAllow_NoTrailingComma() {
    String result = HttpHeaderUtils.allow(Arrays.asList("GET", "POST"));
    Assert.assertFalse("Allow header must not end with a comma", result.endsWith(","));
    Assert.assertFalse("Allow header must not start with a comma", result.startsWith(","));
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

    Assert.assertTrue(HttpHeaderUtils.acceptsGZipCompression(accepted));
    Assert.assertFalse(HttpHeaderUtils.acceptsGZipCompression(rejected));
    Assert.assertFalse(HttpHeaderUtils.acceptsGZipCompression(absent));
  }

  // ---------------------------------------------------------------------------
  // setContentLength
  // ---------------------------------------------------------------------------

  @Test
  public void testSetContentLength_IntValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpHeaderUtils.setContentLength(response, 1024);

    Assert.assertEquals(1024, recorder.contentLength());
    Assert.assertNull(recorder.header(HttpHeaders.CONTENT_LENGTH));
  }

  @Test
  public void testSetContentLength_LongValue() {
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    HttpHeaderUtils.setContentLength(response, (long) Integer.MAX_VALUE + 1);

    Assert.assertEquals(-1, recorder.contentLength());
    Assert.assertEquals("2147483648", recorder.header(HttpHeaders.CONTENT_LENGTH));
  }

  // ---------------------------------------------------------------------------
  // checkIfHeaders
  // ---------------------------------------------------------------------------

  @Test
  public void testCheckIfHeaders_NoConditions() throws Exception {
    HttpServletRequest request = HttpTestSupport.request().build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assert.assertTrue(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assert.assertEquals(HttpServletResponse.SC_OK, recorder.status());
  }

  @Test
  public void testCheckIfHeaders_IfNoneMatchGet() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("GET")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assert.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assert.assertEquals("\"abc\"", recorder.header(HttpHeaders.ETAG));
    Assert.assertFalse(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfNoneMatchPost() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .method("POST")
        .header(HttpHeaders.IF_NONE_MATCH, "\"abc\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assert.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assert.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assert.assertTrue(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfMatchMismatch() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .header(HttpHeaders.IF_MATCH, "\"other\"")
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assert.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1000, "text/plain", "\"abc\"")));
    Assert.assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, recorder.status());
    Assert.assertTrue(recorder.errorSent());
  }

  @Test
  public void testCheckIfHeaders_IfModifiedSince() throws Exception {
    HttpServletRequest request = HttpTestSupport.request()
        .dateHeader(HttpHeaders.IF_MODIFIED_SINCE, 1000)
        .build();
    HttpTestSupport.ResponseRecorder recorder = HttpTestSupport.response();
    HttpServletResponse response = recorder.build();

    Assert.assertFalse(HttpHeaderUtils.checkIfHeaders(request, response, new GenericEntityInfo(1500, "text/plain", "\"abc\"")));
    Assert.assertEquals(HttpServletResponse.SC_NOT_MODIFIED, recorder.status());
    Assert.assertEquals("\"abc\"", recorder.header(HttpHeaders.ETAG));
  }
}
