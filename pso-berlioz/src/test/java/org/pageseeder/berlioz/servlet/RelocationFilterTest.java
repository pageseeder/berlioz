package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

class RelocationFilterTest {

  @TempDir
  Path tmp;

  private RelocationFilter initFilter(String relocationXml) throws Exception {
    File webRoot = Files.createTempDirectory(tmp, "d").toFile();
    File webinf  = new File(webRoot, "WEB-INF");
    webinf.mkdirs();
    Files.write(new File(webinf, "relocation.xml").toPath(),
        relocationXml.getBytes(StandardCharsets.UTF_8));
    FilterConfig cfg = ServletTestSupport.filterConfig(
        webRoot.getAbsolutePath(),
        Collections.singletonMap("config", "relocation.xml"));
    RelocationFilter filter = new RelocationFilter();
    filter.init(cfg);
    return filter;
  }

  private void assertProtectedTargetIgnored(String target) throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"" + target + "\"/>"
        + "</relocation-mapping>");

    boolean[] forwardCalled = {false};
    RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
        RequestDispatcher.class.getClassLoader(),
        new Class<?>[]{RequestDispatcher.class},
        (proxy, m, args) -> {
          if ("forward".equals(m.getName())) forwardCalled[0] = true;
          return null;
        });

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").dispatcher(dispatcher).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertFalse(forwardCalled[0], "Dispatcher.forward should not expose protected paths");
    Assertions.assertTrue(chainInvoked[0], "Chain should continue when relocation target is unsafe");
    Assertions.assertNull(recorder.header("Content-Location"), "Unsafe target must not be exposed");
  }

  // Pass-through
  // ---------------------------------------------------------------------------

  @Test
  void testPassThroughWhenNoRelocationMatches() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new\"/>"
        + "</relocation-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request().uri("/other").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Chain should be invoked when no relocation matches");
    Assertions.assertNull(recorder.header("Content-Location"), "No Content-Location header should be set");
  }

  @Test
  void testPassThroughWhenConfigIsEmpty() throws Exception {
    RelocationFilter filter = initFilter("<?xml version=\"1.0\"?><relocation-mapping/>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request().uri("/any/path").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0]);
  }

  // Relocation with RequestDispatcher
  // ---------------------------------------------------------------------------

  @Test
  void testRelocationSetsContentLocationHeader() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new\"/>"
        + "</relocation-mapping>");

    boolean[] forwardCalled = {false};
    RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
        RequestDispatcher.class.getClassLoader(),
        new Class<?>[]{RequestDispatcher.class},
        (proxy, m, args) -> {
          if ("forward".equals(m.getName())) forwardCalled[0] = true;
          return null;
        });

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request()
        .uri("/old").dispatcher(dispatcher).build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(forwardCalled[0], "Dispatcher.forward should be called on relocation match");
    Assertions.assertEquals("/new", recorder.header("Content-Location"), "Content-Location header should be set to target");
    Assertions.assertFalse(chainInvoked[0], "Chain should NOT be invoked after a successful forward");
  }

  @Test
  void testRelocationWithNullDispatcherStillContinuesChain() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new\"/>"
        + "</relocation-mapping>");

    boolean[] chainInvoked = {false};
    // No dispatcher set — getRequestDispatcher() returns null
    HttpServletRequest req = ServletTestSupport.request().uri("/old").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Chain should be invoked even when dispatcher is null");
    Assertions.assertNull(recorder.header("Content-Location"), "Content-Location should not be set when dispatcher is null");
  }

  @Test
  void testRelocationRejectsProtectedAbsolutePath() throws Exception {
    assertProtectedTargetIgnored("/WEB-INF/web.xml");
  }

  @Test
  void testRelocationRejectsProtectedRelativePath() throws Exception {
    assertProtectedTargetIgnored("WEB-INF/web.xml");
  }

  @Test
  void testRelocationRejectsEncodedProtectedPath() throws Exception {
    assertProtectedTargetIgnored("%2fWEB-INF%2fweb.xml");
  }

  @Test
  void testRelocationRejectsTraversalToProtectedPath() throws Exception {
    assertProtectedTargetIgnored("/safe/../WEB-INF/web.xml");
  }
}
