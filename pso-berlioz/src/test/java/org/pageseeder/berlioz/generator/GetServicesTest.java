package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pageseeder.berlioz.BerliozOption;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.Response;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

class GetServicesTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

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

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  void testProcessWritesNothingWhenNoConfigured() {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals("", out, "No output expected when no service files are configured");
  }

  @Test
  void testProcessIncludesServicesXmlContent() {
    GlobalSettings.setup(WEB_INF);
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.contains("service-config"), "Should include service-config element from services.xml");
  }

  @Test
  void testETagIsStableForUnchangedFiles() {
    GlobalSettings.setup(WEB_INF);
    GetServices gen = new GetServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag((Request) req);
    String etag2 = gen.getETag((Request) req);
    Assertions.assertEquals(etag1, etag2, "ETag should be stable for unchanged files");
    Assertions.assertNotNull(etag1);
  }

  @Test
  void testGenerateWritesNothingWhenConfigDirectoryIsAbsent(@TempDir Path webInf) {
    GlobalSettings.setup(webInf.toFile());
    Assertions.assertEquals("", process(GeneratorTestSupport.request().build()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"minimal", "standard", "full"})
  void testGenerateReportsDiscoveryFailureAndRetainsWarnings(String detail, @TempDir Path webInf)
      throws Exception {
    Path config = Files.createDirectory(webInf.resolve("config"));
    Path properties = config.resolve("config.properties");
    Files.writeString(properties, BerliozOption.ERROR_DETAIL.property() + "=" + detail);
    GlobalSettings.setup(webInf.toFile());
    Assertions.assertTrue(GlobalSettings.load());
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services-invalid-direct.xml"));
    Assertions.assertFalse(ServiceLoader.getInstance().getLastLoadWarnings().isEmpty());

    // A regular file in place of the directory reliably causes discovery to fail.
    Files.delete(properties);
    Files.delete(config);
    Files.writeString(config, "not a directory");

    XmlStringBuilder xml = new XmlStringBuilder();
    Response response = new GetServices().generate(GeneratorTestSupport.request().build(), xml);
    Assertions.assertEquals(200, response.statusCode());
    String out = xml.toString();
    Assertions.assertTrue(out.startsWith("<copy-error reason=\"discovery\""), out);
    Assertions.assertEquals(!"minimal".equals(detail), out.contains("message=\""), out);
    Assertions.assertTrue(out.contains("<warnings>"), out);
    Assertions.assertTrue(out.contains("no output format"), out);
    Assertions.assertTrue(out.indexOf("<warnings>") > out.indexOf("<copy-error"), out);
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetServices gen = new GetServices();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    return xml.toString();
  }
}
