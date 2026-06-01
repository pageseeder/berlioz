package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

public class GetMatchingServiceTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  @TempDir
  Path tmp;

  @BeforeEach
  public void setUp() {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader.getInstance().clear();
  }

  @AfterEach
  public void tearDown() {
    ServiceLoader.getInstance().clear();
    GlobalSettings.setup((InitEnvironment) null);
  }

  // Input validation
  // ---------------------------------------------------------------------------

  @Test
  public void testMissingUrlParameterWritesError() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    String out = process(builder);
    Assertions.assertTrue(out.contains("<error"), "Should write an error element");
    Assertions.assertTrue(out.contains("URL was not specified"));
    Assertions.assertEquals(ContentStatus.BAD_REQUEST, builder.capturedStatus);
  }

  @Test
  public void testInvalidMethodParameterWritesError() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "INVALID");
    String out = process(builder);
    Assertions.assertTrue(out.contains("<error"), "Should write an error element for invalid method");
    Assertions.assertTrue(out.contains("invalid"));
    Assertions.assertEquals(ContentStatus.BAD_REQUEST, builder.capturedStatus);
  }

  // No match
  // ---------------------------------------------------------------------------

  @Test
  public void testNoMatchWritesNoMatchingServiceElement() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/unknown/path")
        .parameter("method", "GET");
    String out = process(builder);
    Assertions.assertTrue(out.contains("no-matching-service"), "Should write <no-matching-service> when no service matches");
  }

  // Match found
  // ---------------------------------------------------------------------------

  @Test
  public void testMatchFoundWritesMatchingServiceElement() throws Exception {
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
    Assertions.assertFalse(out.contains("<error"), "Should not write error element");
  }

  @Test
  public void testMatchContainsResolvedUrlPattern() throws Exception {
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

  // ETag
  // ---------------------------------------------------------------------------

  @Test
  public void testETagReflectsRegistryVersion() {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag(req);
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.isEmpty());
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(GeneratorTestSupport.RequestBuilder builder) throws Exception {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = builder.build();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
