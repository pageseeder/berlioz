package org.pageseeder.berlioz.servlet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.security.ControlAuthorization;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
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

  @BeforeEach
  void setup() {
    ServletTestSupport.clearMappingFilters();
  }

  @AfterEach
  void cleanup() {
    ServletTestSupport.clearMappingFilters();
  }

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

  // Missing configuration
  // ---------------------------------------------------------------------------

  @Test
  void testPassThroughWithoutConfigParameter() throws Exception {
    File webRoot = Files.createTempDirectory(tmp, "d").toFile();
    RelocationFilter filter = new RelocationFilter();
    filter.init(ServletTestSupport.filterConfig(webRoot.getAbsolutePath(), Collections.emptyMap()));

    boolean[] chainInvoked = {false};
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest("redirects"), recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Chain should be invoked when the filter has no config");
    Assertions.assertEquals(200, recorder.status);
    Assertions.assertNull(recorder.header(ReloadScope.HEADER));
  }

  @Test
  void testPassThroughWhenConfigFileIsMissing() throws Exception {
    File webRoot = Files.createTempDirectory(tmp, "d").toFile();
    RelocationFilter filter = new RelocationFilter();
    filter.init(ServletTestSupport.filterConfig(webRoot.getAbsolutePath(),
        Collections.singletonMap("config", "missing.xml")));

    boolean[] chainInvoked = {false};
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest(null), recorder.build(), ServletTestSupport.recordingChain(chainInvoked));

    Assertions.assertTrue(chainInvoked[0], "Chain should be invoked when the config file does not exist");
    Assertions.assertNull(recorder.header("Content-Location"));
  }

  // Scoped reload
  // ---------------------------------------------------------------------------

  @Test
  void testReloadRedirectsScopeReloadsRules() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/new\"/></relocation-mapping>");
    ServletTestSupport.ResponseRecorder first = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest(null), first.build(), (r, s) -> {});
    Assertions.assertEquals("/new", first.header("Content-Location"));

    // Rewrite the rules in the directory created by initFilter
    File webRoot = Files.list(tmp).findFirst().orElseThrow().toFile();
    Files.write(new File(webRoot, "WEB-INF/relocation.xml").toPath(),
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/newer\"/></relocation-mapping>"
            .getBytes(StandardCharsets.UTF_8));

    // Other scopes do not reload the rules
    ServletTestSupport.ResponseRecorder services = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest("services"), services.build(), (r, s) -> {});
    Assertions.assertEquals("/new", services.header("Content-Location"));
    Assertions.assertNull(services.header(ReloadScope.HEADER));

    ServletTestSupport.ResponseRecorder reloaded = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest("services,redirects"), reloaded.build(), (r, s) -> {});
    Assertions.assertEquals("/newer", reloaded.header("Content-Location"));
    Assertions.assertEquals("redirects", reloaded.header(ReloadScope.HEADER));
  }

  private static HttpServletRequest oldRequest(String reload) {
    RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
        RequestDispatcher.class.getClassLoader(),
        new Class<?>[]{RequestDispatcher.class},
        (proxy, m, args) -> null);
    ServletTestSupport.RequestBuilder builder = ServletTestSupport.request()
        .uri("/old").dispatcher(dispatcher)
        .attribute(ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE, Boolean.TRUE);
    if (reload != null) builder.parameter("berlioz-reload", reload);
    return builder.build();
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

  @Test
  void testRelocationRejectsDoubleEncodedProtectedPath() throws Exception {
    assertProtectedTargetIgnored("%252fWEB-INF%252fweb.xml");
  }

  @Test
  void testRelocationRejectsProtectedFolders() throws Exception {
    assertProtectedTargetIgnored("/WEB-INF");
    assertProtectedTargetIgnored("/META-INF");
    assertProtectedTargetIgnored("/META-INF/MANIFEST.MF");
  }

  @Test
  void testRelocationIgnoresMalformedEncoding() throws Exception {
    assertProtectedTargetIgnored("/new%zz");
  }

  @Test
  void testRelocationIgnoresMalformedEncodingOnceDecoded() throws Exception {
    // Valid once, malformed after the first decoding: rejected rather than dispatched
    assertProtectedTargetIgnored("/new%25zz");
  }

  // Filter interface and lifecycle
  // ---------------------------------------------------------------------------

  @Test
  void testDoFilterRelocatesHttpRequests() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/new\"/></relocation-mapping>");
    boolean[] chainInvoked = {false};
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doFilter(oldRequest(null), recorder.build(), ServletTestSupport.recordingChain(chainInvoked));
    Assertions.assertEquals("/new", recorder.header("Content-Location"));
    Assertions.assertFalse(chainInvoked[0]);
  }

  @Test
  void testDoFilterIgnoresNonHttpRequests() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/new\"/></relocation-mapping>");
    boolean[] chainInvoked = {false};
    Assertions.assertDoesNotThrow(() -> filter.doFilter(stub(ServletRequest.class), stub(ServletResponse.class),
        ServletTestSupport.recordingChain(chainInvoked)));
  }

  @Test
  void testUninitializedFilterPassesThrough() throws Exception {
    RelocationFilter filter = new RelocationFilter();
    boolean[] chainInvoked = {false};
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest(null), recorder.build(), ServletTestSupport.recordingChain(chainInvoked));
    Assertions.assertTrue(chainInvoked[0]);
    Assertions.assertNull(recorder.header("Content-Location"));
    // Destroying a filter that was never initialized has no effect
    filter.destroy();
  }

  @Test
  void testDestroyUnregistersAndPassesThrough() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/new\"/></relocation-mapping>");
    Assertions.assertTrue(MappingFilters.hasConfiguredFilter());

    filter.destroy();

    Assertions.assertFalse(MappingFilters.hasConfiguredFilter());
    Assertions.assertFalse(xml(RelocationFilter.diagnosticSnapshot()).contains("<filter"));
    boolean[] chainInvoked = {false};
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(oldRequest(null), recorder.build(), ServletTestSupport.recordingChain(chainInvoked));
    Assertions.assertTrue(chainInvoked[0], "A destroyed filter should pass requests through");
    Assertions.assertNull(recorder.header("Content-Location"));
  }

  @Test
  void testInitAgainReplacesRegistration() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping><relocation from=\"/old\" to=\"/new\"/></relocation-mapping>");
    File webRoot = Files.list(tmp).findFirst().orElseThrow().toFile();
    filter.init(ServletTestSupport.filterConfig(webRoot.getAbsolutePath(), Collections.singletonMap("config", "relocation.xml")));
    String xml = xml(RelocationFilter.diagnosticSnapshot());
    Assertions.assertEquals(xml.indexOf("<filter"), xml.lastIndexOf("<filter"), "One filter expected: " + xml);
  }

  // Diagnostics
  // ---------------------------------------------------------------------------

  @Test
  void testDiagnosticSnapshotReportsLoadedRulesWithRedactedTargets() throws Exception {
    RelocationFilter filter = initFilter(
        "<?xml version=\"1.0\"?><relocation-mapping>"
        + "<relocation from=\"/old\" to=\"/new?token=s3cr3t\"/>"
        + "</relocation-mapping>");
    // Load the rules
    filter.doHTTPFilter(ServletTestSupport.request().uri("/other").build(), ServletTestSupport.response().build(), (r, s) -> {});

    String xml = xml(RelocationFilter.diagnosticSnapshot());
    Assertions.assertTrue(xml.contains("type=\"relocation\""), xml);
    Assertions.assertTrue(xml.contains("status=\"loaded\""), xml);
    Assertions.assertTrue(xml.contains("from=\"/old\""), xml);
    Assertions.assertTrue(xml.contains("to=\"/new?token=[REDACTED]\""), xml);
    Assertions.assertFalse(xml.contains("s3cr3t"), xml);
  }

  private static String xml(ConfigurationSnapshot snapshot) {
    OutputWriter out = new XmlOutputAdapter();
    snapshot.writeTo(out);
    return out.toString();
  }

  private static <T> T stub(Class<T> type) {
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
        (proxy, m, args) -> ServletTestSupport.defaultValue(m.getReturnType())));
  }
}
