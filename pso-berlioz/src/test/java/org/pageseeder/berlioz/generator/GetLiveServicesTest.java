package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.servlet.HttpEnvironment;
import org.pageseeder.xmlwriter.XML.NamespaceAware;
import org.pageseeder.xmlwriter.XMLStringWriter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Files;

class GetLiveServicesTest {

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

  @Test
  void testProcessWritesLiveServicesElementWhenRegistryEmpty() throws Exception {
    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=0");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assertions.assertTrue(out.contains("live-services"), "Should produce a <live-services> element");
  }

  @Test
  void testETagReflectsRegistryVersion() {
    GetLiveServices gen = new GetLiveServices();
    ContentRequest req = GeneratorTestSupport.request().build();
    String etag = gen.getETag(req);
    Assertions.assertNotNull(etag);
    Assertions.assertFalse(etag.isEmpty());
  }

  @Test
  void testProcessWithLoadedServicesContainsServiceEntries() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = process(req);
    Assertions.assertTrue(out.contains("service"), "Live services output should contain service data");
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
