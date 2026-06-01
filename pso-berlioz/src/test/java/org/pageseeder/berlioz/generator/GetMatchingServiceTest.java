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
import org.pageseeder.berlioz.content.ContentStatus;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;

public class GetMatchingServiceTest {

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

  // Input validation
  // ---------------------------------------------------------------------------

  @Test
  public void testMissingUrlParameterWritesError() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request();
    String out = process(builder);
    Assert.assertTrue("Should write an error element", out.contains("<error"));
    Assert.assertTrue(out.contains("URL was not specified"));
    Assert.assertEquals(ContentStatus.BAD_REQUEST, builder.capturedStatus);
  }

  @Test
  public void testInvalidMethodParameterWritesError() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "INVALID");
    String out = process(builder);
    Assert.assertTrue("Should write an error element for invalid method", out.contains("<error"));
    Assert.assertTrue(out.contains("invalid"));
    Assert.assertEquals(ContentStatus.BAD_REQUEST, builder.capturedStatus);
  }

  // No match
  // ---------------------------------------------------------------------------

  @Test
  public void testNoMatchWritesNoMatchingServiceElement() throws Exception {
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/unknown/path")
        .parameter("method", "GET");
    String out = process(builder);
    Assert.assertTrue("Should write <no-matching-service> when no service matches",
        out.contains("no-matching-service"));
  }

  // Match found
  // ---------------------------------------------------------------------------

  @Test
  public void testMatchFoundWritesMatchingServiceElement() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        tmp.newFolder("public"), tmp.newFolder("private"), "max-age=3600");
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "GET")
        .environment(env);
    String out = process(builder);
    Assert.assertTrue("Should write <matching-service> for a known URL",
        out.contains("matching-service"));
    Assert.assertFalse("Should not write error element", out.contains("<error"));
  }

  @Test
  public void testMatchContainsResolvedUrlPattern() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        tmp.newFolder("public"), tmp.newFolder("private"), "max-age=3600");
    GeneratorTestSupport.RequestBuilder builder = GeneratorTestSupport.request()
        .parameter("url", "/home")
        .parameter("method", "GET")
        .environment(env);
    String out = process(builder);
    Assert.assertTrue("Should contain the matched URL pattern", out.contains("pattern="));
    Assert.assertTrue("Should contain the request path", out.contains("path=\"/home\""));
  }

  // ETag
  // ---------------------------------------------------------------------------

  @Test
  public void testETagReflectsRegistryVersion() {
    GetMatchingService gen = new GetMatchingService();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag(req);
    Assert.assertNotNull(etag);
    Assert.assertFalse(etag.isEmpty());
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
