package org.pageseeder.berlioz.servlet;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

public class RelocationFilterTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private RelocationFilter initFilter(String relocationXml) throws Exception {
    File webRoot = tmp.newFolder();
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

  // Pass-through
  // ---------------------------------------------------------------------------

  @Test
  public void testPassThroughWhenNoRelocationMatches() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new\"/>"
        + "</relocation-mapping>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request().uri("/other").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertTrue("Chain should be invoked when no relocation matches", chainInvoked[0]);
    Assert.assertNull("No Content-Location header should be set",
        recorder.header("Content-Location"));
  }

  @Test
  public void testPassThroughWhenConfigIsEmpty() throws Exception {
    RelocationFilter filter = initFilter("<?xml version=\"1.0\"?><relocation-mapping/>");

    boolean[] chainInvoked = {false};
    HttpServletRequest req = ServletTestSupport.request().uri("/any/path").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertTrue(chainInvoked[0]);
  }

  // Relocation with RequestDispatcher
  // ---------------------------------------------------------------------------

  @Test
  public void testRelocationSetsContentLocationHeader() throws Exception {
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

    Assert.assertTrue("Dispatcher.forward should be called on relocation match", forwardCalled[0]);
    Assert.assertEquals("Content-Location header should be set to target", "/new",
        recorder.header("Content-Location"));
    // RelocationFilter always continues the chain after forwarding
    Assert.assertTrue("Chain should still be invoked after relocation", chainInvoked[0]);
  }

  @Test
  public void testRelocationWithNullDispatcherStillContinuesChain() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new\"/>"
        + "</relocation-mapping>");

    boolean[] chainInvoked = {false};
    // No dispatcher set — getRequestDispatcher() returns null
    HttpServletRequest req = ServletTestSupport.request().uri("/old").build();
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();

    filter.doHTTPFilter(req, recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assert.assertTrue("Chain should be invoked even when dispatcher is null", chainInvoked[0]);
    Assert.assertNull("Content-Location should not be set when dispatcher is null",
        recorder.header("Content-Location"));
  }
}
