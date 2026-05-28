package org.pageseeder.berlioz.generator;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;

public class GetGlobalConfigTest {

  private static final File WEB_INF =
      new File("./src/test/resources/org/pageseeder/berlioz");

  @Before
  public void setUp() {
    GlobalSettings.setup(WEB_INF);
  }

  @After
  public void tearDown() {
    GlobalSettings.setup((InitEnvironment) null);
  }

  // process() tests
  // ---------------------------------------------------------------------------

  @Test
  public void testProcessAlwaysWritesPropertiesElement() throws Exception {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assert.assertTrue("Should contain <properties>", out.contains("<properties"));
  }

  @Test
  public void testProcessIncludesSourceAttributeWhenConfigExists() throws Exception {
    ContentRequest req = GeneratorTestSupport.request().build();
    String out = process(req);
    Assert.assertTrue("Should include source attribute when config file is found",
        out.contains("source="));
  }

  @Test
  public void testETagNotNullWhenConfigFileExists() {
    GetGlobalConfig gen = new GetGlobalConfig();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag(req);
    Assert.assertNotNull("ETag should not be null when a properties file is found", etag);
    Assert.assertFalse(etag.isEmpty());
  }

  @Test
  public void testETagNullWhenNoConfigFile() {
    GlobalSettings.setup((InitEnvironment) null);
    GetGlobalConfig gen = new GetGlobalConfig();
    ContentRequest req = GeneratorTestSupport.request().build();
    Assert.assertNull("ETag should be null when no properties file is configured",
        gen.getETag(req));
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) throws Exception {
    GetGlobalConfig gen = new GetGlobalConfig();
    XMLStringWriter xml = new XMLStringWriter(NamespaceAware.No);
    gen.process(req, xml);
    xml.flush();
    return xml.toString();
  }
}
