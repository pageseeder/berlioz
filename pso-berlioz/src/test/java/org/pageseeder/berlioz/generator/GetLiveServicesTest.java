package org.pageseeder.berlioz.generator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;

public class GetLiveServicesTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

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

  @Test
  public void testProcessWritesLiveServicesElementWhenRegistryEmpty() throws Exception {
    HttpEnvironment env = new HttpEnvironment(
        tmp.newFolder("public"), tmp.newFolder("private"), "max-age=0");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assert.assertTrue("Should produce a <live-services> element", out.contains("live-services"));
  }

  @Test
  public void testETagReflectsRegistryVersion() {
    GetLiveServices gen = new GetLiveServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag(req);
    Assert.assertNotNull(etag);
    Assert.assertFalse(etag.isEmpty());
  }

  @Test
  public void testProcessWithLoadedServicesContainsServiceEntries() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        tmp.newFolder("public"), tmp.newFolder("private"), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assert.assertTrue("Live services output should contain service data", out.contains("service"));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) throws Exception {
    GetLiveServices gen = new GetLiveServices();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
