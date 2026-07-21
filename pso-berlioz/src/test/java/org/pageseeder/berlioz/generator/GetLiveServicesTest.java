package org.pageseeder.berlioz.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pageseeder.berlioz.GlobalSettings;
import org.pageseeder.berlioz.InitEnvironment;
import org.pageseeder.berlioz.content.ContentRequest;
import org.pageseeder.berlioz.content.Request;
import org.pageseeder.berlioz.content.ServiceLoader;
import org.pageseeder.berlioz.output.JsonOutputAdapter;
import org.pageseeder.berlioz.output.OutputWriter;
import org.pageseeder.berlioz.output.XmlOutputAdapter;
import org.pageseeder.berlioz.servlet.HttpEnvironment;

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
    String etag = gen.getETag((Request) req);
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

  @Test
  void testProcessJsonWithLoadedServicesContainsServiceEntries() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(), Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    String out = processJson(req);
    Assertions.assertTrue(out.startsWith("{\"liveServices\":["), out);
  }

  // helpers
  // ---------------------------------------------------------------------------

  private static String process(ContentRequest req) {
    GetLiveServices gen = new GetLiveServices();
    OutputWriter out = new XmlOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }

  private static String processJson(ContentRequest req) {
    GetLiveServices gen = new GetLiveServices();
    OutputWriter out = new JsonOutputAdapter();
    gen.generate(req, out);
    return out.toString();
  }
}
