package org.pageseeder.berlioz.servlet;

import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
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

  @Test void listBoundariesAreUnambiguous() {
    assertNotEquals(snapshot(DiagnosticConfiguration.fields("mappings", List.of("a b", "c"))).etag(),
        snapshot(DiagnosticConfiguration.fields("mappings", List.of("a", "b c"))).etag());
  }

  @Test void stylesheetSelectionSupportsRawXmlFallback(@TempDir Path root) {
    ErrorHandlerServlet.ErrorStylesheet missing = ErrorHandlerServlet.selectErrorStylesheet("missing.xsl", root.toFile(), null);
    assertNull(missing.url);
    assertEquals("none", missing.fields.get("selected"));
    assertEquals("missing-or-unreadable", missing.fields.get("fallback-reason"));
    ErrorHandlerServlet.ErrorStylesheet builtinMissing = ErrorHandlerServlet.selectErrorStylesheet("", null, null);
    assertEquals(Map.of("configured", "built-in", "selected", "none"), builtinMissing.fields);
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
