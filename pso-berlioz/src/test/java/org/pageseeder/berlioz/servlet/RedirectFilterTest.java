package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

class RedirectFilterTest {

  @TempDir
  Path tmp;

  private RedirectFilter initFilter(String redirectXml) throws Exception {
    File webRoot = Files.createTempDirectory(tmp, "d").toFile();
    File webinf  = new File(webRoot, "WEB-INF");
    webinf.mkdirs();
    Files.write(new File(webinf, "redirect.xml").toPath(),
        redirectXml.getBytes(StandardCharsets.UTF_8));
    FilterConfig cfg = ServletTestSupport.filterConfig(
        webRoot.getAbsolutePath(),
        Collections.singletonMap("config", "redirect.xml"));
    RedirectFilter filter = new RedirectFilter();
    filter.init(cfg);
    return filter;
  }

  // Pass-through
  // ---------------------------------------------------------------------------

  @Test
  void testPassThroughWhenNoRedirectMatches() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/other").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Chain should be invoked when no redirect matches");
    Assertions.assertEquals(200, recorder.status);
    Assertions.assertNull(recorder.header("Location"), "No Location header should be set");
  }

  @Test
  void testPassThroughWhenConfigIsEmpty() throws Exception {
    RedirectFilter filter = initFilter("<?xml version=\"1.0\"?><redirect-mapping/>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/any/path").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0]);
    Assertions.assertEquals(200, recorder.status);
  }

  // Temporary redirect (302)
  // ---------------------------------------------------------------------------

  @Test
  void testTemporaryRedirect() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertFalse(chainInvoked[0], "Chain must not be invoked when a redirect is issued");
    Assertions.assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, recorder.status);
    Assertions.assertNotNull(recorder.header("Location"), "Location header must be set");
    Assertions.assertTrue(recorder.header("Location").endsWith("/new"));
  }

  @Test
  void testTemporaryRedirectSetsAbsoluteLocation() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/index.html\" to=\"/html/home\"/>"
        + "</redirect-mapping>");

    HttpServletRequest req = ServletTestSupport.request()
        .uri("/index.html").scheme("http").host("example.org").port(8080).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});

    String location = recorder.header("Location");
    Assertions.assertNotNull(location);
    Assertions.assertTrue(location.startsWith("http://example.org:8080/html/home"), "Location should be absolute");
  }

  @Test
  void testTemporaryRedirectSetsCacheControlHeader() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/b\"/>"
        + "</redirect-mapping>");

    HttpServletRequest req = ServletTestSupport.request()
        .uri("/a").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});

    Assertions.assertNotNull(recorder.header("Cache-Control"), "Cache-Control header must be set on redirect");
  }

  // Permanent redirect (301)
  // ---------------------------------------------------------------------------

  @Test
  void testPermanentRedirect() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\" permanent=\"yes\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertFalse(chainInvoked[0], "Chain must not be invoked on permanent redirect");
    Assertions.assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, recorder.status);
    Assertions.assertNotNull(recorder.header("Location"));
  }

  // URI pattern matching
  // ---------------------------------------------------------------------------

  @Test
  void testRedirectMatchesOnlyExactPath() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\"/>"
        + "</redirect-mapping>");

    // /old/sub should not match /old
    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old/sub").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Sub-path should not match the /old pattern");
    Assertions.assertEquals(200, recorder.status);
  }

  @Test
  void testMultipleRedirectRules() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/alpha\"/>"
        + "<redirect from=\"/b\" to=\"/beta\"/>"
        + "</redirect-mapping>");

    HttpServletRequest reqA = ServletTestSupport.request()
        .uri("/a").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recA = ServletTestSupport.response();
    filter.doHTTPFilter(reqA, recA.build(), (r, s) -> {});
    Assertions.assertTrue(recA.header("Location").endsWith("/alpha"));

    HttpServletRequest reqB = ServletTestSupport.request()
        .uri("/b").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recB = ServletTestSupport.response();
    filter.doHTTPFilter(reqB, recB.build(), (r, s) -> {});
    Assertions.assertTrue(recB.header("Location").endsWith("/beta"));
  }
}
