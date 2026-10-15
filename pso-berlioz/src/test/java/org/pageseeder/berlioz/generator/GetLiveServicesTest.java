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
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Compatibility coverage for the deprecated {@link GetLiveServices} forwarding alias.
 *
 * <p>Full behavioral coverage of the generator now lives in {@link ListRuntimeServicesTest}.
 */
@SuppressWarnings("java:S5738") // Deliberately exercises a forRemoval=true alias to prove it still works.
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
  void deprecatedAliasProducesIdenticalOutputToListRuntimeServices() throws Exception {
    GlobalSettings.setup(WEB_INF);
    ServiceLoader.getInstance().load(new File(WEB_INF, "config/services.xml"));

    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(),
        Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=3600");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();

    GetLiveServices alias = new GetLiveServices();
    ListRuntimeServices primary = new ListRuntimeServices();

    Assertions.assertEquals(primary.getETag((Request) req), alias.getETag((Request) req));

    OutputWriter aliasXml = new XmlOutputAdapter();
    alias.generate(req, aliasXml);
    OutputWriter primaryXml = new XmlOutputAdapter();
    primary.generate(req, primaryXml);
    Assertions.assertEquals(primaryXml.toString(), aliasXml.toString());

    OutputWriter aliasJson = new JsonOutputAdapter();
    alias.generate(req, aliasJson);
    OutputWriter primaryJson = new JsonOutputAdapter();
    primary.generate(req, primaryJson);
    Assertions.assertEquals(primaryJson.toString(), aliasJson.toString());
  }

  @Test
  void deprecatedAliasRemainsUsableAsConfiguredGenerator() throws Exception {
    HttpEnvironment env = new HttpEnvironment(
        Files.createDirectory(tmp.resolve("public")).toFile(),
        Files.createDirectory(tmp.resolve("private")).toFile(), "max-age=0");
    ContentRequest req = GeneratorTestSupport.request().environment(env).build();
    GetLiveServices alias = new GetLiveServices();
    OutputWriter out = new XmlOutputAdapter();
    alias.generate(req, out);
    Assertions.assertTrue(out.toString().contains("live-services"));
  }
}
