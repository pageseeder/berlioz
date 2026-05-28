package org.pageseeder.berlioz.generator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

  @Before
  public void setUp() {
    GlobalSettings.setup((InitEnvironment) null);
    ServiceLoader.getInstance().clear();
  }

  @After
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
    Assert.assertEquals("No output expected when no service files are configured", "", out);
  }

  @Test
  public void testProcessIncludesServicesXmlContent() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assert.assertTrue("Should include service-config element from services.xml",
        out.contains("service-config"));
  }

  @Test
  public void testETagIsStableForUnchangedFiles() {
    GlobalSettings.setup(WEB_INF);
    GetServices gen = new GetServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag1 = gen.getETag(req);
    String etag2 = gen.getETag(req);
    Assert.assertEquals("ETag should be stable for unchanged files", etag1, etag2);
    Assert.assertNotNull(etag1);
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
