package org.pageseeder.berlioz.servlet;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.xslt.StylesheetLocation;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationSnapshotTest {
  @Test void deepCopyProtectsContentAndFingerprint() {
    List<String> mappings = new ArrayList<>(List.of("*.xml"));
    Map<String, Object> style = DiagnosticConfiguration.fields("allocation", "identity");
    Map<String, Object> entry = DiagnosticConfiguration.fields("name", "xml", "mappings", mappings, "stylesheet", style);
    ConfigurationSnapshot snapshot = new ConfigurationSnapshot("berlioz-config", List.of(entry));
    String etag = snapshot.etag();
    mappings.add("*.json");
    style.put("allocation", "global");
    entry.put("name", "changed");
    assertEquals(etag, snapshot.etag());
    JsonOutputAdapter out = new JsonOutputAdapter(); snapshot.writeTo(out);
    assertEquals("{\"servlets\":[{\"name\":\"xml\",\"mappings\":[\"*.xml\"],\"stylesheet\":{\"allocation\":\"identity\"}}]}", out.toString());
  }

  @Test void everyEmittedFieldAffectsFingerprint() {
    Map<String, Object> entry = DiagnosticConfiguration.fields("name", "html", "mappings", List.of("*.html"),
        "media-type", "text/html", "charset", "UTF-8", "cache-control", "no-cache", "compression", true,
        "xslt-cache", "manual", "stylesheet", DiagnosticConfiguration.fields("allocation", "group", "source", "classpath", "path", "classpath:xslt/{GROUP}.xsl"),
        "fallback", DiagnosticConfiguration.fields("source", "classpath", "path", "classpath:fallback.xsl"));
    verifyChanges(entry);
    verifyChanges(DiagnosticConfiguration.fields("name", "errors", "mappings", List.of("/errors/*"), "problem-xml", true,
        "detail", "minimal", "forward", DiagnosticConfiguration.fields("source", "explicit", "extensions", List.of("html"), "default-extension", "html"),
        "ignore", DiagnosticConfiguration.fields("extensions", List.of("css")),
        "stylesheet", DiagnosticConfiguration.fields("configured", "custom", "path", "error.xsl", "selected", "built-in", "fallback-reason", "missing-or-unreadable")));
  }

  @SuppressWarnings("unchecked")
  private void verifyChanges(Map<String, Object> entry) {
    String before = snapshot(entry).etag();
    for (String key : entry.keySet()) {
      Map<String, Object> changed = new LinkedHashMap<>(entry);
      Object value = entry.get(key);
      if (value instanceof Map) {
        Map<String, Object> nested = (Map<String, Object>) value;
        for (String nestedKey : nested.keySet()) {
          Map<String, Object> newNested = new LinkedHashMap<>(nested);
          newNested.put(nestedKey, "changed"); changed.put(key, newNested);
          assertNotEquals(before, snapshot(changed).etag(), key + "." + nestedKey);
        }
      } else {
        changed.put(key, value instanceof Boolean ? false : value instanceof List ? List.of("changed") : "changed");
        assertNotEquals(before, snapshot(changed).etag(), key);
      }
      changed.remove(key);
      assertNotEquals(before, snapshot(changed).etag(), "omitted " + key);
    }
  }

  @Test void objectListsAreWrittenAndCopied() {
    Map<String, Object> selection = DiagnosticConfiguration.fields("name", "g", "source", "classpath");
    List<Map<String, Object>> selections = new ArrayList<>(List.of(selection));
    Map<String, Object> style = DiagnosticConfiguration.fields("allocation", "group", "selections", selections);
    Map<String, Object> processor = DiagnosticConfiguration.fields("factory", "x.Factory");
    ConfigurationSnapshot snapshot = new ConfigurationSnapshot("berlioz-config",
        List.of(DiagnosticConfiguration.fields("name", "html", "stylesheet", style)),
        DiagnosticConfiguration.fields("xslt-processor", processor));
    String etag = snapshot.etag();
    selection.put("name", "changed");
    selections.add(DiagnosticConfiguration.fields("name", "other"));
    processor.put("factory", "changed");
    assertEquals(etag, snapshot.etag());
    JsonOutputAdapter json = new JsonOutputAdapter(); snapshot.writeTo(json);
    assertEquals("{\"servlets\":[{\"name\":\"html\",\"stylesheet\":{\"allocation\":\"group\","
        + "\"selections\":[{\"name\":\"g\",\"source\":\"classpath\"}]}}],\"xsltProcessor\":{\"factory\":\"x.Factory\"}}", json.toString());
    XmlOutputAdapter xml = new XmlOutputAdapter(); snapshot.writeTo(xml);
    assertEquals("<berlioz-config><servlet name=\"html\"><stylesheet allocation=\"group\">"
        + "<selection name=\"g\" source=\"classpath\"/></stylesheet></servlet>"
        + "<xslt-processor factory=\"x.Factory\"/></berlioz-config>", xml.toString());
    assertNotEquals(etag, new ConfigurationSnapshot("berlioz-config",
        List.of(DiagnosticConfiguration.fields("name", "html", "stylesheet", style))).etag());
  }

  @Test void selectionsReportOnlySafePaths(@TempDir Path root) throws Exception {
    Path webinf = root.resolve("WEB-INF");
    assertEquals(DiagnosticConfiguration.fields("name", "g", "source", "filesystem", "path", "xslt/html/g.xsl"),
        DiagnosticConfiguration.selection("g", StylesheetLocation.forFile(webinf.resolve("xslt/html/g.xsl")), webinf));
    assertEquals(DiagnosticConfiguration.fields("name", "g", "source", "filesystem"),
        DiagnosticConfiguration.selection("g", StylesheetLocation.forFile(root.resolve("outside/g.xsl")), webinf));
    assertEquals(DiagnosticConfiguration.fields("name", "g", "source", "filesystem"),
        DiagnosticConfiguration.selection("g", StylesheetLocation.forFile(webinf.resolve("../SECRET/g.xsl")), webinf));
    assertEquals(DiagnosticConfiguration.fields("name", "g", "source", "classpath", "path", "classpath:META-INF/berlioz/xslt/html/g.xsl"),
        DiagnosticConfiguration.selection("g", StylesheetLocation.forClasspath(null, "classpath:META-INF/berlioz/xslt/html/g.xsl"), webinf));
  }

  @Test void uninitializedListsOnlyRegisteredBerliozServlets() {
    Map<String, javax.servlet.ServletRegistration> registrations = new LinkedHashMap<>();
    registrations.put("html", registration(BerliozServlet.class.getName(), List.of("*.html", "/a/*")));
    registrations.put("json", registration(BerliozServlet.class.getName(), List.of("*.json")));
    registrations.put("init", registration(InitServlet.class.getName(), List.of()));
    javax.servlet.ServletContext context = (javax.servlet.ServletContext) java.lang.reflect.Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{javax.servlet.ServletContext.class},
        (proxy, method, args) -> method.getName().equals("getServletRegistrations") ? registrations : null);
    assertEquals(List.of(DiagnosticConfiguration.fields("name", "html", "mappings", List.of("*.html", "/a/*"), "initialised", false)),
        DiagnosticConfiguration.uninitialized(context, BerliozServlet.class.getName(), Set.of("json")));
    javax.servlet.ServletContext unsupported = (javax.servlet.ServletContext) java.lang.reflect.Proxy.newProxyInstance(
        getClass().getClassLoader(), new Class<?>[]{javax.servlet.ServletContext.class},
        (proxy, method, args) -> { throw new UnsupportedOperationException(); });
    assertEquals(List.of(), DiagnosticConfiguration.uninitialized(unsupported, BerliozServlet.class.getName(), Set.of()));
    assertEquals(List.of(), DiagnosticConfiguration.uninitialized(null, BerliozServlet.class.getName(), Set.of()));
  }

  @Test void configuredFilesystemPathsAreReportedRelativeToWebInf() {
    assertEquals(Map.of("source", "filesystem", "path", "xslt/html/{GROUP}.xsl"), DiagnosticConfiguration.stylesheet("/xslt/html/{GROUP}.xsl"));
    assertEquals(Map.of("source", "filesystem", "path", "xslt/a.xsl"), DiagnosticConfiguration.stylesheet("xslt/a.xsl"));
    for (String unsafe : List.of("/../a.xsl", "/x/../../a.xsl", "\\a.xsl", "/C:/a.xsl", "/x\n.xsl")) {
      assertEquals(Map.of("source", "filesystem"), DiagnosticConfiguration.stylesheet(unsafe), unsafe);
    }
    assertEquals(Map.of("source", "classpath"), DiagnosticConfiguration.stylesheet("classpath:/../a.xsl"));
  }

  private static javax.servlet.ServletRegistration registration(String className, List<String> mappings) {
    return (javax.servlet.ServletRegistration) java.lang.reflect.Proxy.newProxyInstance(
        ConfigurationSnapshotTest.class.getClassLoader(), new Class<?>[]{javax.servlet.ServletRegistration.class},
        (proxy, method, args) -> {
          switch (method.getName()) {
            case "getClassName": return className;
            case "getMappings": return mappings;
            default: return null;
          }
        });
  }

  @Test void containerIsOmittedWhenUnknown(@TempDir Path root) {
    assertEquals(Map.of(), DiagnosticConfiguration.container(new HttpEnvironment(root.toFile(), root.toFile(), "")));
  }

  @Test void listBoundariesAreUnambiguous() {
    assertNotEquals(snapshot(DiagnosticConfiguration.fields("mappings", List.of("a b", "c"))).etag(),
        snapshot(DiagnosticConfiguration.fields("mappings", List.of("a", "b c"))).etag());
  }

  @Test void stylesheetSelectionSupportsRawXmlFallback(@TempDir Path root) {
    ErrorHandlerServlet.ErrorStylesheet missing = ErrorHandlerServlet.selectErrorStylesheet("missing.xsl", root.toFile(), null);
    assertNull(missing.url);
    assertEquals("none", missing.toDiagnosticFields().get("selected"));
    assertEquals("missing-or-unreadable", missing.toDiagnosticFields().get("fallback-reason"));
    ErrorHandlerServlet.ErrorStylesheet builtinMissing = ErrorHandlerServlet.selectErrorStylesheet("", null, null);
    assertEquals(Map.of("configured", "built-in", "selected", "none"), builtinMissing.toDiagnosticFields());
  }

  @Test void stylesheetDiagnosticsPreserveSelectionAndFallback(@TempDir Path root) throws Exception {
    java.net.URL builtin = root.resolve("builtin.xsl").toUri().toURL();
    ErrorHandlerServlet.ErrorStylesheet defaults = ErrorHandlerServlet.selectErrorStylesheet("", root.toFile(), builtin);
    assertEquals(builtin, defaults.url);
    assertFalse(defaults.isFallback());
    assertEquals(Map.of("configured", "built-in", "selected", "built-in"), defaults.toDiagnosticFields());

    ErrorHandlerServlet.ErrorStylesheet missing = ErrorHandlerServlet.selectErrorStylesheet("missing.xsl", root.toFile(), builtin);
    assertEquals(builtin, missing.url);
    assertTrue(missing.isFallback());
    assertEquals(Map.of("configured", "custom", "path", "missing.xsl", "selected", "built-in",
        "fallback-reason", "missing-or-unreadable"), missing.toDiagnosticFields());

    Path custom = java.nio.file.Files.createFile(root.resolve("custom.xsl"));
    ErrorHandlerServlet.ErrorStylesheet selected = ErrorHandlerServlet.selectErrorStylesheet("custom.xsl", root.toFile(), builtin);
    assertEquals(custom.toUri().toURL(), selected.url);
    assertFalse(selected.isFallback());
    assertEquals(Map.of("configured", "custom", "path", "custom.xsl", "selected", "custom"), selected.toDiagnosticFields());
  }

  @Test void stylesheetDiagnosticsOmitUnsafeConfiguredPaths() {
    for (String path : List.of("../private.xsl", "https://example.test/style.xsl")) {
      ErrorHandlerServlet.ErrorStylesheet selection = ErrorHandlerServlet.selectErrorStylesheet(path, null, null);
      assertEquals(Map.of("configured", "custom", "selected", "none", "fallback-reason", "missing-or-unreadable"),
          selection.toDiagnosticFields());
    }
  }

  @Test void unsafePathsAreOmitted() {
    for (String path : List.of("/opt/private/style.xsl", "../style.xsl", "x/../style.xsl", "C:\\secret.xsl",
        "jar:file:/app.jar!/style.xsl", "https://example.test/style.xsl", "classpath:../secret", "x\nsecret")) {
      assertNull(DiagnosticConfiguration.safePath(path), path);
    }
    assertEquals("classpath:xslt/a.xsl", DiagnosticConfiguration.safePath("resource:/xslt/a.xsl"));
    assertEquals("xslt/a.xsl", DiagnosticConfiguration.safePath("xslt/a.xsl"));
  }

  private ConfigurationSnapshot snapshot(Map<String, Object> fields) {
    return new ConfigurationSnapshot("config", List.of(fields));
  }
}
