package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.*;
import org.pageseeder.berlioz.error.InvalidParameterException;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.servlet.HttpEnvironment;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

class GetMatchingServiceTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  @TempDir
  Path tmp;

  @BeforeEach
  void setUp() {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader.getInstance().clear();
  }

  @AfterEach
  void tearDown() {
    ServiceLoader.getInstance().clear();
    GlobalSettings.setup((InitEnvironment) null);
  }

  // Input validation
  // ---------------------------------------------------------------------------

  @Test
  void testMissingUrlParameterWritesError() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    ContentRequest req = builder.build();
    OutputWriter out = new XmlOutputAdapter();
    Generator generator = new GetMatchingService();
    Assertions.assertThrows(InvalidParameterException.class, () -> generator.generate(req, out));
  }

  @Test
  void testInvalidMethodParameterWritesError() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "INVALID");
    ContentRequest req = builder.build();
    OutputWriter out = new XmlOutputAdapter();
    Generator generator = new GetMatchingService();
    Assertions.assertThrows(InvalidParameterException.class, () -> generator.generate(req, out));
  }

  // No match
  // ---------------------------------------------------------------------------

  @Test
  void testNoMatchWritesMatchedFalse() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/unknown/path")
        .parameter("method", "GET");
    String out = process(builder);
    Assertions.assertTrue(out.contains("matching-service"), "Should still write a <matching-service> root element");
    Assertions.assertTrue(out.contains("matched=\"false\""), "Should report matched=\"false\" when no service matches");
  }

  @Test
  void testNoMatchWritesMatchedFalseJson() {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/unknown/path")
        .parameter("method", "GET");
    String out = processJson(builder);
    Assertions.assertEquals("{\"matched\":false}", out);
  }

  // Match found
  // ---------------------------------------------------------------------------

  @Test
  void testMatchFoundWritesMatchingServiceElement() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "GET")
        .environment(env);
    String out = process(builder);
    Assertions.assertTrue(out.contains("matching-service"), "Should write <matching-service> for a known URL");
    Assertions.assertTrue(out.contains("matched=\"true\""), "Should report matched=\"true\" when a service matches");
    Assertions.assertFalse(out.contains("<error"), "Should not write error element");
  }

  @Test
  void testMatchContainsResolvedUrlPattern() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "GET")
        .environment(env);
    String out = process(builder);
    Assertions.assertTrue(out.contains("pattern="), "Should contain the matched URL pattern");
    Assertions.assertTrue(out.contains("path=\"/home\""), "Should contain the request path");
  }

  @Test
  void testMatchFoundWritesMatchedTrueJson() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "GET")
        .environment(env);
    String out = processJson(builder);
    Assertions.assertTrue(out.startsWith("{\"matched\":true,"), out);
    Assertions.assertTrue(out.contains("\"path\":\"/home\""), out);
  }

  // ETag
  // ---------------------------------------------------------------------------

  @Test
  void testETagReflectsRegistryVersion() {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag((Request) req);
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.isEmpty());
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(GeneratorTestSupport.RequestBuilder builder) {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = builder.build();
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }

  private static String processJson(GeneratorTestSupport.RequestBuilder builder) {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = builder.build();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }
}
