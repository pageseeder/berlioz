package org.pageseeder.berlioz.servlet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.security.ControlAuthorization;

class MappingFiltersTest {

  @TempDir Path webRoot;

  @BeforeEach
  void setup() throws Exception {
    Files.createDirectories(this.webRoot.resolve("WEB-INF/config"));
    ServletTestSupport.clearMappingFilters();
  }

  @AfterEach
  void cleanup() {
    ServletTestSupport.clearMappingFilters();
    GlobalSettings.setup((InitEnvironment) null);
  }

  // Reloads that reach every filter
  // ---------------------------------------------------------------------------

  @Test
  void reloadThroughOneFilterReachesAnotherMappedElsewhere() throws Exception {
    RedirectFilter here = redirectFilter("here", "here.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/one\"/></redirect-mapping>");
    RedirectFilter elsewhere = redirectFilter("elsewhere", "elsewhere.xml", "<redirect-mapping><redirect from=\"/b\" to=\"/one\"/></redirect-mapping>");
    assertTrue(redirect(elsewhere, request("/b", null)).endsWith("/one"));
    write("elsewhere.xml", "<redirect-mapping><redirect from=\"/b\" to=\"/two\"/></redirect-mapping>");

    // The reload request only passes through the first filter
    redirect(here, request("/a", "redirects"));

    assertTrue(redirect(elsewhere, request("/b", null)).endsWith("/two"));
  }

  @Test
  void reloadThroughServletReachesEveryFilter() throws Exception {
    RedirectFilter redirects = redirectFilter("redirects", "redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/one\"/></redirect-mapping>");
    RelocationFilter relocator = relocationFilter("relocations", "relocation.xml", "<relocation-mapping><relocation from=\"/c\" to=\"/one\"/></relocation-mapping>");
    assertTrue(redirect(redirects, request("/a", null)).endsWith("/one"));
    assertEquals("/one", relocate(relocator, request("/c", null)));
    write("redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/two\"/></redirect-mapping>");
    write("relocation.xml", "<relocation-mapping><relocation from=\"/c\" to=\"/two\"/></relocation-mapping>");

    MappingFilters.requestReload(request("/hello.xml", "redirects"));

    assertTrue(redirect(redirects, request("/a", null)).endsWith("/two"));
    assertEquals("/two", relocate(relocator, request("/c", null)));
  }

  @Test
  void reloadIsCountedOncePerRequest() {
    HttpServletRequest req = request("/a", "redirects");
    long before = MappingFilters.generation();
    MappingFilters.requestReload(req);
    MappingFilters.requestReload(req);
    assertEquals(before + 1, MappingFilters.generation());
  }

  @Test
  void unauthorizedReloadIsIgnored() throws Exception {
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/one\"/></redirect-mapping>");
    long before = MappingFilters.generation();
    HttpServletRequest req = ServletTestSupport.request().uri("/a").parameter("berlioz-reload", "redirects").build();
    redirect(filter, req);
    assertEquals(before, MappingFilters.generation());
  }

  @Test
  void configuredFiltersAreTracked() throws Exception {
    assertFalse(MappingFilters.hasConfiguredFilter());
    RelocationFilter none = new RelocationFilter();
    none.init(ServletTestSupport.filterConfig("none", this.webRoot.toString(), Map.of()));
    assertFalse(MappingFilters.hasConfiguredFilter());
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping/>");
    assertTrue(MappingFilters.hasConfiguredFilter());
    filter.destroy();
    assertFalse(MappingFilters.hasConfiguredFilter());
  }

  @Test
  void filterWithoutRealPathFailsInsteadOfUsingTheWorkingDirectory() {
    RedirectFilter redirect = new RedirectFilter();
    RelocationFilter relocation = new RelocationFilter();
    Map<String, String> params = Map.of("config", "config/redirect.xml");
    FilterConfig redirectConfig = ServletTestSupport.filterConfig("r", null, params);
    FilterConfig relocationConfig = ServletTestSupport.filterConfig("l", null, params);
    assertThrows(IllegalStateException.class, () -> redirect.init(redirectConfig));
    assertThrows(IllegalStateException.class, () -> relocation.init(relocationConfig));
    assertFalse(MappingFilters.hasConfiguredFilter());
  }

  @Test
  void filterWithoutRealPathOrConfigHasNoEffect() {
    RedirectFilter filter = new RedirectFilter();
    filter.init(ServletTestSupport.filterConfig("none", null, Map.of()));
    assertFalse(MappingFilters.hasConfiguredFilter());
  }

  @Test
  void diagnosticViewOnlyComputesTheItemsRead() {
    int[] computed = {0};
    List<Map<String, Object>> view = MappingRules.diagnosticView(Collections.nCopies(5000, "x"),
        item -> { computed[0]++; return Map.of("item", item); });
    assertEquals(5000, view.size());
    assertEquals(0, computed[0]);
    view.subList(0, MappingFilters.RULE_LIMIT).forEach(item -> {});
    assertEquals(MappingFilters.RULE_LIMIT, computed[0]);
  }

  // Diagnostic snapshot
  // ---------------------------------------------------------------------------

  @Test
  void snapshotReportsEveryFilter() throws Exception {
    RedirectFilter redirects = redirectFilter("redirects", "/config/redirect.xml", "<redirect-mapping>"
        + "<redirect from=\"/a\" to=\"https://user:pw@example.org/x?token=t\" permanent=\"yes\"/>"
        + "<redirect from=\"/{+path}.psml\" to=\"/html/{+path}\"/></redirect-mapping>");
    relocationFilter("relocations", "relocation.xml", "<relocation-mapping><relocation from=\"/c\" to=\"/d\"/></relocation-mapping>");
    relocationFilter("broken", "broken.xml", "<relocation-mapping>");
    relocationFilter("missing", "missing.xml", null);
    RelocationFilter none = new RelocationFilter();
    none.init(ServletTestSupport.filterConfig("none", this.webRoot.toString(), Map.of()));
    redirect(redirects, request("/z", null));
    relocate(filter("broken"), request("/z", null));

    assertEquals("<redirect-config>"
        + "<filter name=\"redirects\" type=\"redirect\" config=\"config/redirect.xml\" exists=\"true\" status=\"loaded\" loaded=\"T\" rule-count=\"2\">"
        + "<rule from=\"/a\" to=\"https://user:[REDACTED]@example.org/x?token=[REDACTED]\" status=\"301\"/>"
        + "<rule from=\"/{+path}.psml\" to=\"/html/{+path}\" status=\"302\"/></filter>"
        + "<filter name=\"broken\" type=\"relocation\" config=\"broken.xml\" exists=\"true\" status=\"failed\" loaded=\"T\" failure=\"Unable to load the configuration file\"/>"
        + "<filter name=\"missing\" type=\"relocation\" config=\"missing.xml\" exists=\"false\" status=\"not-loaded\"/>"
        + "<filter name=\"none\" type=\"relocation\" status=\"no-config\"/>"
        + "<filter name=\"relocations\" type=\"relocation\" config=\"relocation.xml\" exists=\"true\" status=\"not-loaded\"/>"
        + "</redirect-config>", xml(MappingFilters.diagnosticSnapshot()));
  }

  @Test
  void snapshotJson() throws Exception {
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/b\"/></redirect-mapping>");
    redirect(filter, request("/z", null));
    assertEquals("{\"filters\":[{\"name\":\"redirects\",\"type\":\"redirect\",\"config\":\"redirect.xml\",\"exists\":true,"
        + "\"status\":\"loaded\",\"loaded\":\"T\",\"ruleCount\":1,\"rules\":[{\"from\":\"/a\",\"to\":\"/b\",\"status\":302}]}]}",
        json(MappingFilters.diagnosticSnapshot()));
  }

  @Test
  void snapshotReportsPendingReloadAndLoadedRules() throws Exception {
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/one\"/></redirect-mapping>");
    redirect(filter, request("/z", null));
    write("redirect.xml", "<redirect-mapping><redirect from=\"/a\" to=\"/two\"/></redirect-mapping>");
    MappingFilters.requestReload(request("/hello.xml", "redirects"));

    String pending = xml(MappingFilters.diagnosticSnapshot());
    assertTrue(pending.contains("reload-pending=\"true\""), pending);
    assertTrue(pending.contains("to=\"/one\""), pending);

    redirect(filter, request("/z", null));
    String reloaded = xml(MappingFilters.diagnosticSnapshot());
    assertFalse(reloaded.contains("reload-pending"), reloaded);
    assertTrue(reloaded.contains("to=\"/two\""), reloaded);
  }

  @Test
  void snapshotIncludesFailureMessageWithDetails() throws Exception {
    Files.writeString(this.webRoot.resolve("WEB-INF/config/config.properties"), "berlioz.errors.detail=standard\n");
    GlobalSettings.setup(this.webRoot.resolve("WEB-INF").toFile());
    RelocationFilter filter = relocationFilter("broken", "broken.xml", "<relocation-mapping>");
    relocate(filter, request("/z", null));
    String xml = xml(MappingFilters.diagnosticSnapshot());
    assertTrue(xml.contains("failure=\"Error during validation: XML document structures must start and end"), xml);
  }

  @Test
  void snapshotReportsIgnoredRules() throws Exception {
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping>"
        + "<redirect from=\"/a\" to=\"/b\"/>"
        + "<redirect from=\"/{!x}\" to=\"/c?token=t\"/>"
        + "<redirect from=\"/d\" to=\"/{!y}\"/></redirect-mapping>");
    redirect(filter, request("/z", null));
    assertEquals("<redirect-config>"
        + "<filter name=\"redirects\" type=\"redirect\" config=\"redirect.xml\" exists=\"true\" status=\"loaded\" loaded=\"T\" rule-count=\"1\" ignored-count=\"2\">"
        + "<rule from=\"/a\" to=\"/b\" status=\"302\"/>"
        + "<ignored-rule position=\"2\" reason=\"invalid-from\" from=\"/{!x}\" to=\"/c?token=[REDACTED]\"/>"
        + "<ignored-rule position=\"3\" reason=\"invalid-to\" from=\"/d\" to=\"/{!y}\"/>"
        + "</filter></redirect-config>", xml(MappingFilters.diagnosticSnapshot()));
    String json = json(MappingFilters.diagnosticSnapshot());
    assertTrue(json.contains("\"ignoredCount\":2,"), json);
    assertTrue(json.contains("\"ignoredRules\":[{\"position\":2,\"reason\":\"invalid-from\""), json);
  }

  @Test
  void snapshotBoundsTheRules() throws Exception {
    StringBuilder rules = new StringBuilder("<redirect-mapping>");
    for (int i = 0; i <= MappingFilters.RULE_LIMIT; i++) rules.append("<redirect from=\"/r").append(i).append("\" to=\"/t\"/>");
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", rules.append("</redirect-mapping>").toString());
    redirect(filter, request("/z", null));
    String xml = xml(MappingFilters.diagnosticSnapshot());
    assertTrue(xml.contains("rule-count=\"2001\" limit=\"2000\""), xml.substring(0, 300));
    assertEquals(MappingFilters.RULE_LIMIT, xml.split("<rule ", -1).length - 1);
  }

  @Test
  void snapshotEtagChangesWithLoad() throws Exception {
    RedirectFilter filter = redirectFilter("redirects", "redirect.xml", "<redirect-mapping/>");
    String before = MappingFilters.diagnosticSnapshot().etag();
    assertEquals(before, MappingFilters.diagnosticSnapshot().etag());
    redirect(filter, request("/z", null));
    assertNotEquals(before, MappingFilters.diagnosticSnapshot().etag());
  }

  // Helpers
  // ---------------------------------------------------------------------------

  private final Map<String, RelocationFilter> relocations = new HashMap<>();

  private RelocationFilter filter(String name) {
    return this.relocations.get(name);
  }

  private RedirectFilter redirectFilter(String name, String config, String xml) throws Exception {
    write(config, xml);
    RedirectFilter filter = new RedirectFilter();
    filter.init(ServletTestSupport.filterConfig(name, this.webRoot.toString(), Map.of("config", config)));
    return filter;
  }

  private RelocationFilter relocationFilter(String name, String config, String xml) throws Exception {
    if (xml != null) write(config, xml);
    RelocationFilter filter = new RelocationFilter();
    filter.init(ServletTestSupport.filterConfig(name, this.webRoot.toString(), Map.of("config", config)));
    this.relocations.put(name, filter);
    return filter;
  }

  private void write(String config, String xml) throws Exception {
    Path file = this.webRoot.resolve("WEB-INF").resolve(config.replaceFirst("^/+", ""));
    Files.createDirectories(file.getParent());
    Files.writeString(file, xml);
  }

  private static HttpServletRequest request(String uri, String reload) {
    RequestDispatcher dispatcher = (RequestDispatcher) Proxy.newProxyInstance(
        RequestDispatcher.class.getClassLoader(), new Class<?>[]{RequestDispatcher.class}, (proxy, m, args) -> null);
    ServletTestSupport.RequestBuilder builder = ServletTestSupport.request()
        .uri(uri).scheme("http").host("example.org").port(80).dispatcher(dispatcher)
        .attribute(ControlAuthorization.CONTROL_AUTHORIZED_ATTRIBUTE, Boolean.TRUE);
    if (reload != null) builder.parameter("berlioz-reload", reload);
    return builder.build();
  }

  /** @return the location of the redirect, or null */
  private static String redirect(RedirectFilter filter, HttpServletRequest req) throws Exception {
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});
    return String.valueOf(recorder.header("Location"));
  }

  /** @return the location of the relocation, or null */
  private static String relocate(RelocationFilter filter, HttpServletRequest req) throws Exception {
    ServletTestSupport.ResponseRecorder recorder = ServletTestSupport.response();
    filter.doHTTPFilter(req, recorder.build(), (r, s) -> {});
    return recorder.header("Content-Location");
  }

  private static String xml(ConfigurationSnapshot snapshot) {
    OutputWriter out = new XmlOutputAdapter();
    snapshot.writeTo(out);
    return out.toString().replaceAll("loaded=\"[^\"]+\"", "loaded=\"T\"");
  }

  private static String json(ConfigurationSnapshot snapshot) {
    OutputWriter out = new JsonOutputAdapter();
    snapshot.writeTo(out);
    return out.toString().replaceAll("\"loaded\":\"[^\"]+\"", "\"loaded\":\"T\"");
  }

}
