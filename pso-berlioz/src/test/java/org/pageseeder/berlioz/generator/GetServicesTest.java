package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.xml.XmlStringBuilder;

import java.io.File;

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

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetServices gen = new GetServices();
    XmlStringBuilder xml = new XmlStringBuilder();
    gen.generate(req, xml);
    return xml.toString();
  }
}
