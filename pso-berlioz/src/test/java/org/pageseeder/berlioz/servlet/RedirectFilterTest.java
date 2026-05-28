package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

public class RedirectFilterTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private RedirectFilter initFilter(String redirectXml) throws Exception {
    File webRoot = tmp.newFolder();
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
  public void testPassThroughWhenNoRedirectMatches() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/other").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertTrue("Chain should be invoked when no redirect matches", chainInvoked[0]);
    Assert.assertEquals(200, recorder.status);
    Assert.assertNull("No Location header should be set", recorder.header("Location"));
  }

  @Test
  public void testPassThroughWhenConfigIsEmpty() throws Exception {
    RedirectFilter filter = initFilter("<?xml version=\"1.0\"?><redirect-mapping/>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/any/path").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertTrue(chainInvoked[0]);
    Assert.assertEquals(200, recorder.status);
  }

  // Temporary redirect (302)
  // ---------------------------------------------------------------------------

  @Test
  public void testTemporaryRedirect() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertFalse("Chain must not be invoked when a redirect is issued", chainInvoked[0]);
    Assert.assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, recorder.status);
    Assert.assertNotNull("Location header must be set", recorder.header("Location"));
    Assert.assertTrue(recorder.header("Location").endsWith("/new"));
  }

  @Test
  public void testTemporaryRedirectSetsAbsoluteLocation() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/index.html\" to=\"/html/home\"/>"
        + "</redirect-mapping>");

    HttpServletRequest req = ServletTestSupport.request()
        .uri("/index.html").scheme("http").host("example.org").port(8080).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});

    String location = recorder.header("Location");
    Assert.assertNotNull(location);
    Assert.assertTrue("Location should be absolute", location.startsWith("http://example.org:8080/html/home"));
  }

  @Test
  public void testTemporaryRedirectSetsCacheControlHeader() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/b\"/>"
        + "</redirect-mapping>");

    HttpServletRequest req = ServletTestSupport.request()
        .uri("/a").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});

    Assert.assertNotNull("Cache-Control header must be set on redirect",
        recorder.header("Cache-Control"));
  }

  // Permanent redirect (301)
  // ---------------------------------------------------------------------------

  @Test
  public void testPermanentRedirect() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/old\" to=\"/new\" permanent=\"yes\"/>"
        + "</redirect-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertFalse("Chain must not be invoked on permanent redirect", chainInvoked[0]);
    Assert.assertEquals(HttpServletResponse.SC_MOVED_PERMANENTLY, recorder.status);
    Assert.assertNotNull(recorder.header("Location"));
  }

  // URI pattern matching
  // ---------------------------------------------------------------------------

  @Test
  public void testRedirectMatchesOnlyExactPath() throws Exception {
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

    Assert.assertTrue("Sub-path should not match the /old pattern", chainInvoked[0]);
    Assert.assertEquals(200, recorder.status);
  }

  @Test
  public void testMultipleRedirectRules() throws Exception {
    RedirectFilter filter = initFilter(
        "<?xml version=\"1.0\"?><redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/alpha\"/>"
        + "<redirect from=\"/b\" to=\"/beta\"/>"
        + "</redirect-mapping>");

    HttpServletRequest reqA = ServletTestSupport.request()
        .uri("/a").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recA = ServletTestSupport.response();
    filter.doHTTPFilter(reqA, recA.build(), (r, s) -> {});
    Assert.assertTrue(recA.header("Location").endsWith("/alpha"));

    HttpServletRequest reqB = ServletTestSupport.request()
        .uri("/b").scheme("http").host("example.org").port(80).build();
    ServletTestSupport.ResponseRecorder recB = ServletTestSupport.response();
    filter.doHTTPFilter(reqB, recB.build(), (r, s) -> {});
    Assert.assertTrue(recB.header("Location").endsWith("/beta"));
  }
}
