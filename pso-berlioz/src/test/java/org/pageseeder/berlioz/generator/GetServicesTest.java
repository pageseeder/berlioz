package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;

public class GetServicesTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

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

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  public void testProcessWritesNothingWhenNoConfigured() throws Exception {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertEquals("", out, "No output expected when no service files are configured");
  }

  @Test
  public void testProcessIncludesServicesXmlContent() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assertions.assertTrue(out.contains("service-config"), "Should include service-config element from services.xml");
  }

  @Test
  public void testETagIsStableForUnchangedFiles() {
    GlobalSettings.setup(WEB_INF);
    GetServices gen = new GetServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag(req);
    String etag2 = gen.getETag(req);
    Assertions.assertEquals(etag1, etag2, "ETag should be stable for unchanged files");
    Assertions.assertNotNull(etag1);
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) throws Exception {
    GetServices gen = new GetServices();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
